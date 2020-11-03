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

import airsenseur.dev.datapush.config.Configuration;
import airsenseur.dev.datapush.datacontainers.DataPushDataContainer;
import airsenseur.dev.datapush.dataprocessors.DataPushBoardInfoProcessor;
import airsenseur.dev.datapush.dataprocessors.DataPushProcessor;
import airsenseur.dev.datapush.dataprocessors.DataPushSamplesProcessor;
import airsenseur.dev.datapush.dataprocessors.DataPushSensorConfigProcessor;
import airsenseur.dev.exceptions.ConfigurationException;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.history.HistoryPersister;
import airsenseur.dev.history.sql.HistoryPersisterSQL;
import airsenseur.dev.persisters.SampleAndConfigurationLoader;
import airsenseur.dev.persisters.SampleLoader;
import airsenseur.dev.persisters.SamplesPersister;
import airsenseur.dev.persisters.awsmqtt.SamplePersisterAWSMQTT;
import airsenseur.dev.persisters.iflink.SamplePersisterIFLINKCurl;
import airsenseur.dev.persisters.iflink.SamplePersisterIFLINKEmbedded;
import airsenseur.dev.persisters.influxdb.BoardPersisterInfluxDB;
import airsenseur.dev.persisters.influxdb.SamplePersisterInfluxDB;
import airsenseur.dev.persisters.influxdb.SensorConfigPersisterInfluxDB;
import airsenseur.dev.persisters.mqtt.SamplePersisterMQTT;
import airsenseur.dev.persisters.sosdb.HistorySOSDB;
import airsenseur.dev.persisters.sosdb.SamplePersisterSOSDB;
import airsenseur.dev.persisters.sql.SensorConfigLoaderSQL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class AirSensEURDataPush {
    
    private static final String BOARD_INFO_DATASETNAME_POSTFIX = "_Boards";
    private static final String SENSOR_CONFIG_DATASETNAME_POSTFIX = "_Sensors";
    
    private static final Logger log = LoggerFactory.getLogger(AirSensEURDataPush.class);
    
    private HistoryPersister history;
    private Configuration config;
    private Configuration.workingMode workingMode;
    
    private DataPushProcessor dataPushBoardInfoProcessor;
    private DataPushProcessor dataPushSensorsConfigProcessor;
    private DataPushProcessor dataPushSampleProcessor;
    
    public void start(Configuration config) {
        
        this.config = config;
                
        // The sample loader from the database
        SampleAndConfigurationLoader sampleAndConfigurationLoader = new SensorConfigLoaderSQL(config.getPersisterPath());
        try {
            if (!sampleAndConfigurationLoader.openLog()) {
                throw new PersisterException("Impossible to open the database");
            }
        } catch (PersisterException ex) {
            log.error(ex.getErrorMessage() + " Terminating.");
            return;
        }
        
        // Retrieve the main working mode (SOS or InfluxDB)
        try {
            workingMode = config.getWorkingMode();
        } catch (ConfigurationException ex) {
            log.error(ex.getErrorMessage() + " Terminating.");
            return;
        }
        
        // Generate the main business objects
        List<Integer> channels = new ArrayList<>();
        SamplesPersister samplePersister;
        try {
            if (workingMode == workingMode.INFLUX) {
                samplePersister = initWM_InfluxDB(config, channels, sampleAndConfigurationLoader);

            } else if (workingMode == workingMode.MQTT) {
                samplePersister = initWM_MQTT(config, channels);

            } else if (workingMode == workingMode.AWS_MQTT) {
                samplePersister = initWM_AWSMQTT(config, channels);

            } else if (workingMode == workingMode.SOSDB) {
                samplePersister = initWM_SOSDB(config, channels);
                
            } else {
                samplePersister = initWM_iFLINK(config, channels);
                
            }   
        } catch (PersisterException ex) {
            log.error(ex.getErrorMessage() + " Terminating.");
            return;
        }
        
        // The Data Push Sample Processor is common to all working modes
        dataPushSampleProcessor = new DataPushSamplesProcessor(sampleAndConfigurationLoader, samplePersister);        
        
        // Start the history persister
        try {
            history.openLog(true);
        } catch (PersisterException ex) {
            log.error(ex.getErrorMessage() + " Terminating.");
            return;
        }
        
        // Start the sample persister
        try {
            samplePersister.startNewLog();
        } catch (PersisterException ex) {
            log.error(ex.getErrorMessage() + " Terminating.");
            return;
        }
        
        // Run the datapush process
        try {
            
            processBoardConfig();
            processSensorsConfig();
            for (int channel:channels) {
                processChannelSamples(channel);
            }
            
            
        } catch (PersisterException ex) {
            log.error(ex.getErrorMessage() + " Terminating.");
        } finally {
            
            // Close logger
            history.closeLog();
            samplePersister.stop();
            sampleAndConfigurationLoader.stop();
        }
    }


    private SamplesPersister initWM_InfluxDB(Configuration configuration, 
                                            List<Integer> channels, 
                                            SampleAndConfigurationLoader sampleAndConfigurationLoader) {
        
        SamplesPersister samplePersister;
        log.info("WorkingMode: INFLUX DB");
        
        // Initialize persisters
        BoardPersisterInfluxDB boardPersister = new BoardPersisterInfluxDB(configuration.getInfluxDbDataSetName() + BOARD_INFO_DATASETNAME_POSTFIX, 
                                                                            configuration.getInfluxDbHost(), 
                                                                            configuration.getInfluxDbPort(), 
                                                                            configuration.getInfluxDbName(), 
                                                                            configuration.getInfluxDbUsername(), 
                                                                            configuration.getInfluxDbPassword(), 
                                                                            configuration.getInfluxDbUseLineProtocol(), 
                                                                            configuration.getUseHTTPSProtocol(), 
                                                                            configuration.getConnectionTimeout());
        
        SensorConfigPersisterInfluxDB sensorConfigPersister = new SensorConfigPersisterInfluxDB(configuration.getInfluxDbDataSetName() + SENSOR_CONFIG_DATASETNAME_POSTFIX, 
                                                                                                configuration.getInfluxDbHost(), 
                                                                                                configuration.getInfluxDbPort(), 
                                                                                                configuration.getInfluxDbName(), 
                                                                                                configuration.getInfluxDbUsername(), 
                                                                                                configuration.getInfluxDbPassword(), 
                                                                                                configuration.getInfluxDbUseLineProtocol(), 
                                                                                                configuration.getUseHTTPSProtocol(), 
                                                                                                configuration.getConnectionTimeout());
        history = new HistoryPersisterSQL(configuration.getHistoryPath());
        samplePersister = new SamplePersisterInfluxDB(configuration.getInfluxDbDataSetName(), 
                                                        configuration.getInfluxDbHost(), 
                                                        configuration.getInfluxDbPort(), 
                                                        configuration.getInfluxDbName(), 
                                                        configuration.getInfluxDbUsername(), 
                                                        configuration.getInfluxDbPassword(), 
                                                        configuration.getInfluxDbUseLineProtocol(), 
                                                        configuration.getUseHTTPSProtocol(), 
                                                        configuration.getConnectionTimeout());
        channels.add(SampleLoader.CHANNEL_INVALID);
        
        
        // Initialize processors. If the user don't need to send registry information, avoid to create the related processors
        if (!configuration.getSkipRegistry()) {
            
            dataPushBoardInfoProcessor = new DataPushBoardInfoProcessor(sampleAndConfigurationLoader, boardPersister);
            dataPushSensorsConfigProcessor = new DataPushSensorConfigProcessor(sampleAndConfigurationLoader, sensorConfigPersister);
        }
        
        return samplePersister;
    }
    
    private SamplesPersister initWM_SOSDB(Configuration configuration, List<Integer> channels) throws PersisterException {
        
        SamplesPersister samplePersister;
        log.info("WorkingMode: 52°North SOS");
            
        // Initialize persisters
        history = new HistorySOSDB(configuration);
        samplePersister = new SamplePersisterSOSDB(configuration);
        for (int channel = 0; channel < config.getSensorsObservedProp().size(); channel++) {
            channels.add(channel);
        }
        
        return samplePersister;
    }   
    
    private SamplesPersister initWM_MQTT(Configuration configuration, List<Integer> channels) {
        
        SamplesPersister samplePersister;
        log.info("WorkingMode: MQTT");
        
        // Initialize persisters
        history = new HistoryPersisterSQL(configuration.getHistoryPath());
        samplePersister = new SamplePersisterMQTT(configuration.getMQTTHost(), 
                                                    configuration.getMQTTUsername(), 
                                                    configuration.getMQTTPassword(), 
                                                    configuration.getMQTTBaseTopic(), 
                                                    configuration.getMQTTUseSSL(), 
                                                    configuration.getConnectionTimeout(), 
                                                    configuration.getMQTTPort(), 
                                                    configuration.getMQTTQoS());
        
        channels.add(SampleLoader.CHANNEL_INVALID);
        
        return samplePersister;
    }
    
    private SamplesPersister initWM_AWSMQTT(Configuration configuration, List<Integer> channels) throws PersisterException {
        
        SamplesPersister samplePersister;
        log.info("WordingMode: AWS-MQTT-IOT");
        
        // Initialize persisters
        history = new HistoryPersisterSQL(config.getHistoryPath());
        samplePersister = new SamplePersisterAWSMQTT(configuration.getMQTTHost(), 
                                                        configuration.getMQTTBaseTopic(), 
                                                        configuration.getAWSIOClientID(), 
                                                        configuration.getAWSIOTKeyPath(), 
                                                        configuration.getAWSIOTCertPath(), 
                                                        configuration.getAWSIOTHostPath(),  
                                                        configuration.getAWSIOKeyAlgorithm());
        
        channels.add(SampleLoader.CHANNEL_INVALID);
        
        return samplePersister;
    }    
    
    
    private SamplesPersister initWM_iFLINK(Configuration configuration, List<Integer> channels)  throws PersisterException {

        SamplesPersister samplePersister;
        log.info("WorkingMode: iFLINK");
        
        history = new HistoryPersisterSQL(config.getHistoryPath());
        if (configuration.getIFLINKUseCurl()) {
            samplePersister = new SamplePersisterIFLINKCurl(configuration.getIFLINKHost(), 
                                                        configuration.getIFLINKEndpoint(),
                                                        configuration.getIFLINKSensorID(),
                                                        configuration.getIFLINKBearerToken(),
                                                        configuration.getIFLINKSensorsList(), 
                                                        configuration.getIFLINKDatePath(),
                                                        configuration.getIFLINKInboundSensorID(),
                                                        configuration.getUseHTTPSProtocol(),
                                                        configuration.getIFLINKUpdatePosition(),
                                                        configuration.getNumThreads(),
                                                        configuration.getAggregationFactor(),
                                                        configuration.getConnectionTimeout(),
                                                        configuration.debugVerbose());
        } else {
            samplePersister = new SamplePersisterIFLINKEmbedded(configuration.getIFLINKHost(), 
                                                            configuration.getIFLINKEndpoint(),
                                                            configuration.getIFLINKSensorID(),
                                                            configuration.getIFLINKBearerToken(),
                                                            configuration.getIFLINKSensorsList(), 
                                                            configuration.getIFLINKDatePath(),
                                                            configuration.getIFLINKInboundSensorID(),
                                                            configuration.getUseHTTPSProtocol(),
                                                            configuration.getIFLINKUpdatePosition(),
                                                            configuration.getNumThreads(),
                                                            configuration.getAggregationFactor(),
                                                            configuration.getConnectionTimeout(),
                                                            configuration.debugVerbose());
            
        }
        
        // Add all channels. Data will be filtered by means of the configured paths in the iFLINKSensorsList
        channels.add(SampleLoader.CHANNEL_INVALID);
        
        return samplePersister;        
    }
        
    private boolean processBoardConfig() throws PersisterException {
        if (dataPushBoardInfoProcessor == null) {
            return false;
        }
        
        MinMax minMaxTS = getMinMaxTsFromHistory(0, dataPushBoardInfoProcessor);
        processDataSetInTheRange(0, minMaxTS, dataPushBoardInfoProcessor);
        
        return true;
    }
    
    private boolean processSensorsConfig() throws PersisterException {
        if (dataPushSensorsConfigProcessor == null) {
            return false;
        }
        
        MinMax minMaxTS = getMinMaxTsFromHistory(0, dataPushSensorsConfigProcessor);
        processDataSetInTheRange(0, minMaxTS, dataPushSensorsConfigProcessor);
        
        return true;
    }
    
    private boolean processChannelSamples(int channel) throws NumberFormatException, PersisterException {
        
        if (dataPushSampleProcessor == null) {
            return false;
        }
        
        MinMax minMaxTs = getMinMaxTsFromHistory(channel, dataPushSampleProcessor);
        processDataSetInTheRange(channel, minMaxTs, dataPushSampleProcessor);
        
        return true;
    }
    
    private MinMax getMinMaxTsFromHistory(int channel, DataPushProcessor dataPushProcessor) throws NumberFormatException, PersisterException {
        
        // Retrieve the minimum and maximum timestamp present in the database for that channel
        // Retry several times before aborting. This way we will be able to 
        // handle table locks in a safer way
        int retry = 0;
        long minTs = 0;
        long maxTs = 0;
        boolean bValid = false;
        while (retry < config.getMaxDatabaseRetry()) {
            try {
                MinMax minMax = dataPushProcessor.getMinMaxTxForChannel(channel);
                minTs = minMax.getMin();
                maxTs = minMax.getMax();
                retry = config.getMaxDatabaseRetry();
                bValid = true;
                
            } catch (PersisterException ex) {
                log.info("Error retrieving minimum and maximum timestamp in the database. Database locked? Retrying.");
                retry++;

                try {
                    TimeUnit.MILLISECONDS.sleep(1333);
                } catch (InterruptedException ie){
                    retry = config.getMaxDatabaseRetry();
                }
            }
        }
        
        if (!bValid) {
            throw new PersisterException("Impossible to retrieve minimum and maximum timestamp in the database.");
        }
        
        long latestAddedTs = 0;
        HistoryEventContainer event = history.loadEvent(dataPushProcessor.getPersisterMarker(channel));
        if (event != null) {
            latestAddedTs = Long.valueOf(event.getEventValue());
        }
        
        // Take the maximum value between latest added and available ts
        minTs = (minTs < latestAddedTs)? latestAddedTs : minTs;
        maxTs = (maxTs > minTs)? maxTs : minTs;
        log.info("Skipping data older than " + minTs + " for channel " + channel);
        log.info("Running until " + maxTs + " is reached " + " for channel " + channel);
        
        return new MinMax(minTs, maxTs);
    }

    private void processDataSetInTheRange(int channel, MinMax minMaxTs, DataPushProcessor dataPushProcessor) throws PersisterException {
        
        // Loop on all available samples and store them to the remote server
        long minTs = minMaxTs.getMin();
        long maxTs = minMaxTs.getMax();
        long timeSpan = config.loadDataTimeSpan() * dataPushProcessor.getTimeSpanMultiplier();
        long lastTsUpdated = -1;
        DataPushDataContainer dataContainer = dataPushProcessor.clearDataContainer();
        do {
            // Retry several times before aborting. This way we will be able to 
            // handle table locks in a safer way
            int retry = 0;
            while (retry < config.getMaxDatabaseRetry()) {
                try {
                    dataContainer = dataPushProcessor.loadDataSetFromLocalPersistence(channel, new MinMax(minTs, minTs+timeSpan));
                    retry = config.getMaxDatabaseRetry();
                    
                } catch (PersisterException ex) {
                    log.info("Error retrieving dataset from database. Database locked? Retrying.");
                    retry++;
                    
                    try {
                        TimeUnit.MILLISECONDS.sleep(1333);
                    } catch (InterruptedException ie){
                        retry = config.getMaxDatabaseRetry();
                    }
                }
                
            }
            if (dataContainer.isEmpty()) {
                if (config.debugEnabled()) {
                    log.info("Nothing found from " + minTs + " period till " + (minTs+timeSpan));
                }
                
                minTs = minTs + timeSpan;
                
            } else {
                
                minTs = dataPushProcessor.getLatestTimestampInDataContainer(dataContainer);
                
                // Prevent stacking from malformed minTs
                if (minTs < 0) {
                    minTs = maxTs;
                } 
                
                if (lastTsUpdated != minTs) {

                    if (config.debugEnabled()) {
                        log.info("Adding " + dataContainer.size() + " samples for period till " + minTs);
                    }

                    // Send collected samples to the remote server
                    if (dataPushProcessor.sendDataToRemotePersistence(dataContainer)) {
                        history.saveEvent(new HistoryEventContainer(dataPushProcessor.getPersisterMarker(channel), "" + minTs));
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
