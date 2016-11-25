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
    
    private final List<SampleDataContainer> samples = new ArrayList<>(Configuration.getConfig().getNumSensors());
    
    static final Logger log = LoggerFactory.getLogger(AirSensEURDataAggregatorEngine.class);
    
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
            
            sensorDataCollector.startSampling();
        }
        
        initializeSampleContainers();
        
        return result;
    }
    
    /**
     * Main task routine. It retrieve samples and persist them.
     */
    @Override
    public void taskMain() {
        
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
                
        // By retrieving free memory we can check 
        // for connection validity with sensors
        FreeMemory freeMemory = sensorDataCollector.getFreeMemory();
        if (freeMemory == null) {
            
            // Something goes wrong. Try to reconnect to sensor board
            if (!sensorDataCollector.connect(config.getSensorHostname(), config.getSensorPort())) {
                Date date = new Date();
                log.info(date.toString() + ": Sensor server not available.");
                return;
            }
        }
        
        // Retrieve samples
        int channel = 0;
        for (SampleDataContainer sample:samples) {
            
            SampleData lastSampleVal = sensorDataCollector.getLastSample(channel);
            
            // Valid samples have timestamp different than zero
            if ((lastSampleVal != null) && (lastSampleVal.timeStamp != 0)) {
                                
                // Get the collection timestamp
                long collectedTs = new Date().getTime();
                                
                // Update sample value for that channel
                if (sample.updateSample(lastSampleVal.value, lastSampleVal.evalSampleVal, lastSampleVal.timeStamp)) {
                    
                    // Calibrated value is a placeholder for data that will be processed
                    // in the future by a calibration algorithm. Populate with a dummy value.
                    sample.setCalibratedVal(lastSampleVal.evalSampleVal);                    
                    
                    double timeStamp = gpsDataCollector.getLastTimeStamp();
                    double longitude = gpsDataCollector.getLastLongitude();
                    double latitude = gpsDataCollector.getLastLatitude();
                    double altitude = gpsDataCollector.getLastAltitude();

                    sample.setName(lastSampleVal.name);
                    sample.updateGPSValues(timeStamp, latitude, longitude, altitude);
                    sample.setCollectedTimestamp(collectedTs);
                    
                    // Persist sample value
                    try {
                        dataPersister.addSample(sample);
                        
                        if (!sqlDataPersister.addSample(sample)) {
                            
                            // Something goes wrong with the SQL logger
                            // Try to reconnect
                            Date date = new Date();
                            log.info(date.toString() + ": Trying to reconnect with the SQL database");
                            sqlDataPersister.startNewLog();
                        }
                        
                    } catch (PersisterException ex) {
                        Date date = new Date();
                        log.info(date.toString() + ": Error when persisting read sample for sensor " + channel);
                    }
                }
                
            } else {
                Date date = new Date();
                log.info(date.toString() + ": Error retrieving data for sensor " + channel);
            }
            
            channel++;
        }
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
    private void initializeSampleContainers() {
        
        // Allocate containers if required
        if (samples.size() != Configuration.getConfig().getNumSensors()) {
            samples.clear();
            for (int channel = 0; channel < Configuration.getConfig().getNumSensors(); channel++) {
                samples.add(new SampleDataContainer(channel));
            }
        }
    }
}