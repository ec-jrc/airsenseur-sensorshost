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

package airsenseur.dev.datapush;

import airsenseur.dev.exceptions.ConfigurationException;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.history.HistoryPersister;
import airsenseur.dev.history.sql.HistoryPersisterSQL;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SampleLoader;
import airsenseur.dev.persisters.SamplesPersister;
import airsenseur.dev.persisters.influxdb.SamplePersisterInfluxDB;
import airsenseur.dev.persisters.sosdb.HistorySOSDB;
import airsenseur.dev.persisters.sosdb.SamplePersisterSOSDB;
import airsenseur.dev.persisters.sql.SampleLoaderSQL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marcos
 */
public class AirSensEURDataPush {

    private static final Logger log = LoggerFactory.getLogger(AirSensEURDataPush.class);
    
    private static SampleLoader sampleLoader;
    private static HistoryPersister history;
    private static SamplesPersister samplePersister;
    private static Configuration config;
    private static Configuration.workingMode workingMode;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        log.info("AirSensEURDataPush started");
        
        // Load the configuration file
        config = Configuration.getConfig();
        try {
            config.init(args[0]);
        } catch (ConfigurationException ex) {
            log.error("Please specify a valid configuration file.");
            return;
        }
        
        // The sample loader from the database
        sampleLoader = new SampleLoaderSQL(config.getPersisterPath());
        try {
            if (!sampleLoader.openLog()) {
                throw new PersisterException("Impossible to open the database");
            }
        } catch (PersisterException ex) {
            log.error(ex.getErrorMessage() + " Terminating.");
            return;
        }
        
        // Retrieve the main working mode (SOS or InfluxDB)
        workingMode = config.getWorkingMode();
        
        // Generate the main business objects
        List<Integer> channels = new ArrayList<>();
        if (workingMode == workingMode.INFLUX) {
            log.info("WorkingMode: INFLUX DB");
            
            history = new HistoryPersisterSQL(config.getHistoryPath());
            samplePersister = new SamplePersisterInfluxDB(config.getInfluxDbDataSetName(), 
                                                           config.getInfluxDbHost(), 
                                                           config.getInfluxDbPort(), 
                                                           config.getInfluxDbName(), 
                                                           config.getInfluxDbUsername(), 
                                                           config.getInfluxDbPassword(),
                                                           config.getInfluxDbUseLineProtocol(),
                                                           config.getUseHTTPSProtocol());
            channels.add(SampleLoader.CHANNEL_INVALID);
        } else {
            log.info("WorkingMode: 52°North SOS");
            
            try {
                history = new HistorySOSDB(config);                
                samplePersister = new SamplePersisterSOSDB(config);
            } catch (PersisterException ex) {
                log.error(ex.getErrorMessage() + " Terminating.");
                return;
            }
            
            for (int channel = 0; channel < config.getSensorsObservedProp().size(); channel++) {
                channels.add(channel);
            }
        }        
        
        // Start the history persister
        try {
            history.openLog(true);
        } catch (PersisterException ex) {
            log.error(ex.getErrorMessage() + " Terminating.");
            return;
        }
        
        try {
            for (int channel:channels) {
                processChannelSamples(channel);
            }
        } catch (PersisterException ex) {
            log.error(ex.getErrorMessage() + " Terminating.");
        } finally {
            
            // Close logger
            history.closeLog();
            samplePersister.stop();
            sampleLoader.stop();

            log.info("AirSensEURDataPush terminated");
        }
    }
    
    private static String getPersisterMarker(int channel) {
        if (workingMode == workingMode.INFLUX) {
            return "latestInfluxDbTs";
        } else {
            return "" + channel;
        }
    }

    private static boolean processChannelSamples(int channel) throws NumberFormatException, PersisterException {

        // Retrieve the minimum and maximum timestamp present in the database for that channel
        long minTs = sampleLoader.getMinimumTimestamp(channel);
        long maxTs = sampleLoader.getMaximumTimestamp(channel);
                
        long latestAddedTs = 0;
        HistoryEventContainer event = history.loadEvent(getPersisterMarker(channel));
        if (event != null) {
            latestAddedTs = Long.valueOf(event.getEventValue());
        }
        
        // Take the maximum value between latest added and available ts
        minTs = (minTs < latestAddedTs)? latestAddedTs : minTs;
        maxTs = (maxTs > minTs)? maxTs : minTs;
        log.info("Skipping data older than " + minTs + " for channel " + channel);
        log.info("Running until " + maxTs + " is reached " + " for channel " + channel);
        
        // Process all samples in the specific range
        processAllSamplesInTheRange(minTs, maxTs, channel);
        
        return false;
    }

    private static void processAllSamplesInTheRange(long minTs, long maxTs, int channel) throws PersisterException {
        
        // Loop on all available samples and store them to the remote server
        int timeSpan = config.loadDataTimeSpan();
        long lastTsUpdated = -1;
        List<SampleDataContainer> samplesList = new ArrayList<>();
        do {
            
            // Retry several times before aborting. This way we will be able to 
            // handle table locks in a safer way
            int retry = 0;
            while (retry < config.getMaxDatabaseRetry()) {
                try {
                    samplesList = sampleLoader.loadSamples(channel, minTs, minTs+timeSpan);
                    retry = config.getMaxDatabaseRetry();
                    
                } catch (PersisterException ex) {
                    log.info("Error retrieving samples from database. Database locked? Retrying.");
                    retry++;
                    
                    try {
                        TimeUnit.MILLISECONDS.sleep(1333);
                    } catch (InterruptedException ie){
                        retry = config.getMaxDatabaseRetry();
                    }
                }
                
            }
            if (samplesList.isEmpty()) {
                if (config.debugEnabled()) {
                    log.info("Nothing found from " + minTs + " period till " + (minTs+timeSpan));
                }
                
                minTs = minTs + timeSpan;
                
            } else {
                
                minTs = (long)samplesList.get(samplesList.size()-1).getCollectedTimestamp() + 1;
                if (lastTsUpdated != minTs) {

                    if (config.debugEnabled()) {
                        log.info("Adding " + samplesList.size() + " samples for period till " + minTs);
                    }

                    // Send collected samples to the remote server
                    if (samplePersister.addSamples(samplesList)) {
                        history.saveEvent(new HistoryEventContainer(getPersisterMarker(channel), "" + minTs));
                    } else {
                        throw new PersisterException("");
                    }
                
                    lastTsUpdated = minTs;
                } else {
                    
                    if (config.debugEnabled()) {
                        log.info("Sample with timestamp " + minTs + " already added. Skipping.");
                    }
                    
                    minTs++;
                }
            }
        } while (minTs < maxTs);
    }
}
