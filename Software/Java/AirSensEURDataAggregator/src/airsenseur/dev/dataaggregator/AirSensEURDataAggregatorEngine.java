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
import airsenseur.dev.json.ChemSensorClient;
import airsenseur.dev.json.FreeMemory;
import airsenseur.dev.json.SampleData;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SamplePersisterFile;
import airsenseur.dev.persisters.SamplesPersister;
import airsenseur.dev.persisters.sql.SamplePersisterSQL;
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
    private final SamplesPersister sqlDataPersister = new SamplePersisterSQL(Configuration.getConfig().getPersisterPath());
    
    private final List<SampleDataContainer> samples = new ArrayList<>();
    private final TimeStampAggregator timeStampAggregator = new TimeStampAggregator();
    
    static final Logger log = LoggerFactory.getLogger(AirSensEURDataAggregatorEngine.class);
    
    private int cumulatedPollsWithValidHostConnection = 0;  // Periodically reconnect to sensor server seems improve the sensor server stability
    private int cumulatedPollsWithZeroTimestamp = 0;
    
    /**
     * Main initialization routine. This should be called at the beginning of the process
     * @return 
     */
    public boolean init() {
        
        Configuration config = Configuration.getConfig();
        
        boolean result = true;
        result &= gpsDataCollector.connect(config.getGPSHostname(), config.getGPSPort());
        result &= sensorDataCollector.connect(config.getSensorHostname(), config.getSensorPort());
        
        if (result) {
            
            try {
                result &= dataPersister.startNewLog();
                result &= sqlDataPersister.startNewLog();
            } catch (PersisterException ex) {
                return false;
            }
        }
        
        // Retrieve the number of sensors configured on remote sensorDataCollector process
        if (result) {
            
            Integer numSensors = sensorDataCollector.getNumSensors();
            if ((numSensors == null) || (numSensors.intValue() == 0)) {
                Date date = new Date();
                log.info(date.toString() + ": Error retrieving the number of configured sensors ");
                return false;
            }
            
            initializeSampleContainers(numSensors);            
            
            sensorDataCollector.startSampling(); 
        }

        cumulatedPollsWithZeroTimestamp = 0;
        cumulatedPollsWithValidHostConnection = 0;
        
        return result;
    }
    
    /**
     * Main task routine. It retrieve samples and persist them.
     */
    @Override
    public void taskMain() {
        
        // Check and connect/reconnect to GPS Server
        checkAndConnectToGPSServer();

        // Check and connect/reconnect to Host Server
        if (!checkAndConnectToHostServer()) {
            return;
        }
        
        // Retrieve samples from the Host Server
        boolean oneSampleWithZeroTimestampFound = false;
        for (int channel = 0; channel < samples.size(); channel++) {
            
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
                processAndStoreSample(remoteSample, channel);
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

    private void processAndStoreSample(SampleData remoteSample, int channel) {
        
        // Update sample value for that channel
        SampleDataContainer localSample = samples.get(channel);                
        if (localSample.updateSample(remoteSample.value, remoteSample.evalSampleVal, remoteSample.timeStamp)) {
            
            // Get the collection timestamp
            Long collectedTs = timeStampAggregator.searchJavaTimeStampFor(remoteSample.timeStamp);
            if (collectedTs == null) {
                long now = new Date().getTime();
                timeStampAggregator.associateJavaTimeStamp(remoteSample.timeStamp, now);
                collectedTs = now;
            }
            
            // Calibrated value is a placeholder for data that will be processed
            // in the future by a calibration algorithm. Populate with a dummy value.
            localSample.setCalibratedVal(remoteSample.evalSampleVal);
            
            double timeStamp = gpsDataCollector.getLastTimeStamp();
            double longitude = gpsDataCollector.getLastLongitude();
            double latitude = gpsDataCollector.getLastLatitude();
            double altitude = gpsDataCollector.getLastAltitude();
            
            localSample.setName(remoteSample.name);
            localSample.setSerial(remoteSample.serial);
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
        
        // By retrieving free memory we can check
        // for connection validity with sensors
        FreeMemory freeMemory = sensorDataCollector.getFreeMemory();
        if (freeMemory == null) {
            
            // Something goes wrong. Try to reconnect to sensor board
            log.info((new Date()).toString() + ": Sensor server not available. Trying to reconnect.");            
            if (!reconnectToHostServer()) { 
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
    
    /**
     * Initialize sample containers
     */
    private void initializeSampleContainers(int numSensors) {
        
        // Allocate containers if required
        if (samples.size() != numSensors) {
            samples.clear();
            for (int channel = 0; channel < numSensors; channel++) {
                samples.add(new SampleDataContainer(channel));
            }
        }
        
        timeStampAggregator.clear();
    }
}