/* ===========================================================================
 * Copyright 2015 EUROPEAN UNION
 *
 * Licensed under the EUPL, Version 1.1 or subsequent versions of the
 * EUPL (the "License"); You may not use this work except in compliance
 * with the License. You may obtain a copy of the License at
 * http://ec.europa.eu/idabc/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Date: 02/04/2015
 * Authors:
 * - Michel Gerboles, michel.gerboles@jrc.ec.europa.eu, 
 *   Laurent Spinelle, laurent.spinelle@jrc.ec.europa.eu and 
 *   Alexander Kotsev, alexander.kotsev@jrc.ec.europa.eu:
 *			European Commission - Joint Research Centre, 
 * - Marco Signorini, marco.signorini@liberaintentio.com
 *
 * ===========================================================================
 */

package airsenseur.dev.dataaggregator;


import airsenseur.dev.dataaggregator.collectors.GPSDataCollector;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.helpers.TaskScheduler;
import airsenseur.dev.json.BoardInfo;
import airsenseur.dev.json.ChemSensorClient;
import airsenseur.dev.json.HostStatus;
import airsenseur.dev.json.SampleData;
import airsenseur.dev.json.SensorConfig;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SamplePersisterFile;
import airsenseur.dev.persisters.SamplesPersister;
import airsenseur.dev.persisters.sql.SensorConfigPersisterSQL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main data aggregation engine
 * @author marco
 */
public class AirSensEURDataAggregatorEngine extends TaskScheduler {
    
    private final GPSDataCollector gpsDataCollector = new GPSDataCollector();
    private final ChemSensorClient sensorDataCollector = new ChemSensorClient();
    private final SamplesPersister dataPersister = new SamplePersisterFile(Configuration.getConfig().getPersisterPath());
    private final SensorConfigPersisterSQL sqlDataPersister = new SensorConfigPersisterSQL(Configuration.getConfig().getPersisterPath());
    
    private final List<ChannelDataContainer> channels =new ArrayList<>();
    private final List<SensorConfig> sensorsConfig = new ArrayList<>();
    private final TimeStampAggregator timeStampAggregator = new TimeStampAggregator();
    
    private List<BoardInfo> boardInfo;
    private Integer numOfSensors;
    
    static final Logger log = LoggerFactory.getLogger(AirSensEURDataAggregatorEngine.class);
    
    private int cumulatedPollsWithValidHostConnection = 0;  // Periodically reconnect to sensor server seems improve the sensor server stability
    private int cumulatedPollsWithZeroTimestamp = 0;
    private boolean applyTimestampCorrection = true;
    
    /**
     * Main initialization routine. This should be called at the beginning of the process
     * @return 
     */
    public boolean init() {
        
        Configuration config = Configuration.getConfig();
        
        applyTimestampCorrection = config.applyTimestampCorrection();
        
        boolean result = true;
        result &= gpsDataCollector.connect(config.getGPSHostname(), config.getGPSPort());
        result &= sensorDataCollector.connect(config.getSensorHostname(), config.getSensorPort());
        
        if (result) {
            
            try {
                result &= dataPersister.startNewLog();
                // result &= sqlDataPersister.startNewLog(); // NOTE: We can't start here a SQLite new log because this is not the correct thread
            } catch (PersisterException ex) {
                return false;
            }
        }
                
        if (result) {
            
            // Wait for host server readyness
            int retry = 20;
            while(!checkAndConnectToHostServer()) {
                
                Date date = new Date();
                if (retry != 0) {
                    log.info(date.toString() + ": Retrying in 2 seconds");
                    
                } else {
                    log.info(date.toString() + ": Too much retries. Exiting");
                }
                retry--;
                
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                }
            }
            
            // Retrieve the number of sensors configured on remote sensorDataCollector process
            if (!getNumberOfConnectedSensors()) {
                return false;
            }
            
            // Retrieve information about connected sensor boards
            getSensorBoardsInfo();
            
            // Retrieve information about connected sensors
            getSensorsConfigurationInfo();
            
            // Persist boards and sensors config
            sqlDataPersister.insertBoardsInfo(boardInfo);
            sqlDataPersister.insertSensorsConfig(sensorsConfig);
            
            // Initialize the sensors dataset
            initializeSampleContainers(numOfSensors);
                                    
            sensorDataCollector.startSampling();
        }

        cumulatedPollsWithZeroTimestamp = 0;
        cumulatedPollsWithValidHostConnection = 0;
        
        return result;
    }

    /**
     * Main task routine. It retrieve samples and persist them. It's expected to run each second
     */
    @Override
    public void taskMain() {
        
        // Check and connect/reconnect to Host Server
        if (!checkAndConnectToHostServer()) {
            return;
        }
        
        // Check and connect/reconnect to GPS Server
        checkAndConnectToGPSServer();
        
        // Retrieve samples from the remote sensorDataCollector process
        boolean oneSampleWithZeroTimestampFound = false;
        for (int channel = 0; channel < numOfSensors; channel++) {
            
            // Skip disabled sensors - TBD            
            
            // Check if it's time to poll this sensor, based on the known sensor periodicity
            long now = System.currentTimeMillis();
            
            // It's not time to poll this sensor
            if ((now - channels.get(channel).getLastPollTimestamp()) < channels.get(channel).getPollPeriod()) {
                continue;
            }
            
            // Ok. It's time to poll this sensor
            channels.get(channel).setLastPollTimestamp(now);            
            
            SampleData remoteSample = sensorDataCollector.getLastSample(channel);
            if (remoteSample == null) {
                
                // Log this error. 
                // This is probaly due to a loss of host connection.
                // We expect a host connection handling on next poll.
                Date date = new Date();
                log.info(date.toString() + ": Error retrieving data for sensor " + channel);
                break;
            }
            
            // Valid samples have timestamp different than zero.
            // Mark this poll time with a warning flag
            if (remoteSample.timeStamp == 0) {
                
                oneSampleWithZeroTimestampFound = true;
            } else {
            
                // Process and store the sample
                processAndStoreSample(remoteSample, channel, now);
            }
        }
        
        // If at least one sample with zero timestamp has been found
        if (oneSampleWithZeroTimestampFound) {
            
            // We don't expect to have zero timestamp for several minutes.
            // If not, this means at least one sensor board is not in sampling
            // status (due to a reset, for example).
            // After a grace, configurable period we ask for a start sample command
            cumulatedPollsWithZeroTimestamp++;
            if (cumulatedPollsWithZeroTimestamp > 100) {
                cumulatedPollsWithZeroTimestamp = 0;
                sensorDataCollector.startSampling();
                
                Date date = new Date();
                log.info(date.toString() + ": Asking sensor server for start sampling due to repeated samples with empty timestamp found");
            }
        } else {
            cumulatedPollsWithZeroTimestamp = 0;
        }
    }

    private void processAndStoreSample(SampleData readSample, int channel, long now) {
        
        // Update sample value for that channel
        SampleDataContainer localSample = channels.get(channel).getValue();
        if (localSample.updateSample(readSample.value, readSample.evalSampleVal, readSample.timeStamp)) {
            
            // Collection timestamp is the UNIX (host) timestamp counted when the host
            // knows the sample. Shields have multiple channels that are sampled coherently and, for these samples
            // we should maintain the timestamp coherency. This is done by a timestamp dictionary who
            // contains boardTimestamp to UNIX associations.
            // If not found on this dictionary, take current UNIX timestamp
            Long collectedTs = timeStampAggregator.searchJavaTimeStampFor(readSample.timeStamp);
            if (collectedTs == null) {
                timeStampAggregator.associateJavaTimeStamp(readSample.timeStamp, now);
                collectedTs = now;
            }
            
            // To be more accurate, try to correct the collectedTs with the help of the channel sampling time
            long samplingPeriod = channels.get(channel).getSamplingPreriod();
            if (applyTimestampCorrection && (samplingPeriod != 0)) {
                long lastCollectedTs = localSample.getCollectedTimestamp();
                long expectedNewCollectedTs = lastCollectedTs + samplingPeriod;
                
                // Validate the expectedNewCollectedTs. We accept it if the difference
                // between the collectedTs and the expectedNewTs is less than a half of the samplingPeriod
                // This rule should be validated by looking at the resulting data for a long period
                if (Math.abs(expectedNewCollectedTs - collectedTs) < samplingPeriod/2) {
                    collectedTs = expectedNewCollectedTs;
                }
            } 
                        
            // Calibrated value is a placeholder for data that will be processed
            // in the future by a calibration algorithm. Populate with a dummy value.
            localSample.setCalibratedVal(readSample.evalSampleVal);
            
            double timeStamp = gpsDataCollector.getLastTimeStamp();
            double longitude = gpsDataCollector.getLastLongitude();
            double latitude = gpsDataCollector.getLastLatitude();
            double altitude = gpsDataCollector.getLastAltitude();
            
            localSample.setName(readSample.name);
            localSample.setSerial(readSample.serial);
            localSample.updateGPSValues(timeStamp, latitude, longitude, altitude);
            localSample.setCollectedTimestamp(collectedTs);
            
            // Persist sample value
            try {
                dataPersister.addSample(localSample);
                
                if (!sqlDataPersister.addSample(localSample)) {
                    
                    // Something goes wrong with the SQL logger
                    // Try to reconnect
                    Date date = new Date();
                    log.info(date.toString() + ": Trying to reconnect with the SQL database");
                    sqlDataPersister.startNewLog();
                }
                
            } catch (PersisterException ex) {
                Date date = new Date();
                log.info(date.toString() + ": Error when persisting read sample for sensor " + channel + " (" + ex.getErrorMessage() + ")");
            }
        }
    }

    private boolean checkAndConnectToHostServer() {
        
        // Check connection validity with host process and readyness 
        HostStatus hostStatus = sensorDataCollector.getHostStatus();
        if (hostStatus == null) {
            
            // Something goes wrong. Try to reconnect to sensor board
            log.info((new Date()).toString() + ": Sensor server not available. Trying to reconnect.");            
            if (!reconnectToHostServer()) { 
                return false;
            }
        } else {
            if (hostStatus.status == HostStatus.STATUS_BUSY) {
                log.info((new Date()).toString() + ": Sensor server busy. Waiting for idle.");
                return false;
            }
        }
        
        // Periodically release the host connection seems increasing
        // the host sensor stability
        cumulatedPollsWithValidHostConnection++;
        if (cumulatedPollsWithValidHostConnection > 3600) {
            log.info((new Date()).toString() + ": Periodically reconnecting to sensor server");
            reconnectToHostServer();
        }
        
        return true;
    }

    private boolean reconnectToHostServer() {
        
        sensorDataCollector.disconnect();
        Configuration config = Configuration.getConfig();
        if (!sensorDataCollector.connect(config.getSensorHostname(), config.getSensorPort())) {
            log.info((new Date()).toString() + ": No re-connect success. Sensor server not available.");
            return false;
        }
        
        cumulatedPollsWithValidHostConnection = 0;
        return true;
    }

    private Configuration checkAndConnectToGPSServer() {
        
        Configuration config = Configuration.getConfig();
        
        // We need to collect GPS information
        if (!gpsDataCollector.poll()) {
            
            // Something goes wrong. Try to reconnect to the GPS server
            if (!gpsDataCollector.connect(config.getGPSHostname(), config.getGPSPort())) {
                Date date = new Date();
                log.info(date.toString() + ": GPS server not available. Samples will not be gps-located");
            }
        }
        // Check for fixes (only for info message; gps data
        // are empty if no fix is available)
        if (!gpsDataCollector.isFixed()) {
            Date date = new Date();
            log.info(date.toString() + ": No GPS Fix. Samples will not be gps-located");
        }
        return config;
    }
    
    // From TaskScheduler
    @Override
    public String getTaskName() {
        return "AirSensEURDataAggregatorEngine";
    }

    /**
     * Termination routine
     */
    public void terminate() {
        
        stop();
        
        sensorDataCollector.stopSampling();
        gpsDataCollector.disconnect();
        sensorDataCollector.disconnect();
        dataPersister.stop();
        sqlDataPersister.stop();
    }
    
    private boolean getNumberOfConnectedSensors() {
        
        // Retrieve the number of sensors configured on remote sensorDataCollector process
        numOfSensors = sensorDataCollector.getNumSensors();
        if ((numOfSensors == null) || (numOfSensors == 0)) {
            Date date = new Date();
            log.info(date.toString() + ": Error retrieving the number of configured sensors ");
            return false;
        }
        
        return true;
    }

    private void getSensorBoardsInfo() {
        
        // Retrieve information about the connected sensor boards
        boardInfo = sensorDataCollector.getSensorBoardsInfo();
        if ((boardInfo == null) || boardInfo.isEmpty()) {
            Date date = new Date();
            log.info(date.toString() + ": Error retrieving board information from the host. Board info will not be persisted ");
        }
    }

    private void getSensorsConfigurationInfo() {
        
        // Retrieve information about connected sensors
        for (int sensorId = 0; sensorId < numOfSensors; sensorId++) {
            SensorConfig sensorData = sensorDataCollector.getSensorConfig(sensorId);
            if (sensorData == null) {
                Date date = new Date();
                log.info(date.toString() + ": Error retrieving sensor information from the host. Sensor info for sensor Id " + sensorId + " will not be persisted ");
                sensorData = new SensorConfig(sensorId);
            } 
            
            sensorsConfig.add(sensorData);
        }
    }
        
    /**
     * Initialize sample containers
     */
    private void initializeSampleContainers(int numSensors) {
                
        // Allocate containers if required
        if (channels.size() != numSensors) {
            channels.clear();
            for (int sensorId = 0; sensorId < numSensors; sensorId++) {
                long samplingPeriod = 0;
                if (sensorsConfig.size() == numSensors) {
                    samplingPeriod = sensorsConfig.get(sensorId).samplingPeriod;
                }
                channels.add(new ChannelDataContainer(new SampleDataContainer(sensorId), samplingPeriod));
            }
        }
        
        timeStampAggregator.clear();
    }
}