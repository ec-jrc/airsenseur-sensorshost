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

package airsenseur.dev.persisters.lora;

import airsenseur.dev.exceptions.ConfigurationException;
import airsenseur.dev.exceptions.GenericException;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.helpers.FileRecipe;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SamplesPersister;
import airsenseur.dev.persisters.lora.helpers.LoraDeviceComm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class SamplePersisterLoRa  implements SamplesPersister {
    
    private final String endpoint;
    private final String appEUI;
    private final String appKey;
    private final String devEUI;
    private final FileRecipe recipe = new FileRecipe();
    private final int dataRate;
    private final int txPower;
    private final int aggregationFactor;
    
    private final LoraDeviceComm deviceComm;
    
    private static class message extends BasicLoRaMessage {
        
        public message(int fPort) {
            super(fPort, true);
        }
        
        public void clear(long timestamp, int boardTimeStamp) {
            super.clear();
            
            this.timeStamp = timestamp;
            this.boardTimeStamp = boardTimeStamp;

            super.append(timestamp);
            super.append(boardTimeStamp);
            super.dirty = false;
        }
        
        public long timeStamp;
        public int boardTimeStamp;
    }
    
    private final message currentSampleMessage = new message(LoraDeviceComm.LORA_DEFAULT_SAMPLES_PORT);
    
    private final Logger log = LoggerFactory.getLogger(SamplePersisterLoRa.class);    
    
    public SamplePersisterLoRa(String endpoint, String appEUI, String appKey, String devEUI, String recipeFile, int dataRate, int txPower, int maxRetry, 
                                int packetLength, int sleepTime, boolean disableADR, boolean forceUnconfirmed, int aggregationFactor, int debugVerbose) {
        this.endpoint = endpoint;
        this.appEUI = appEUI;
        this.appKey = appKey;
        this.devEUI = devEUI;
        this.dataRate = dataRate;
        this.txPower = txPower;
        this.aggregationFactor = aggregationFactor;
        
        deviceComm = new LoraDeviceComm(maxRetry, packetLength, sleepTime, disableADR, debugVerbose > 3);
        
        if (forceUnconfirmed) {
            currentSampleMessage.confirmed = false;
        }
        
        try {
            recipe.read(recipeFile);
        } catch (ConfigurationException ex) {
            log.error("LoRa recipe file not read: " + ex.getErrorMessage());
        }
    }
    
    public LoraDeviceComm getLoRaDeviceComm() { return deviceComm; };

    @Override
    public boolean startNewLog() throws PersisterException {
        
        try {
            // Try to open the LoRa device at the specified endpoint
            if (!deviceComm.open(endpoint)) {
                throw new PersisterException("Invalid endpoint " + endpoint + ". Impossibile to open LoRa dongle at this address.");
            }
            
            // Evaluate the recipe file, if any
            Iterator<String> recipeIt = recipe.getRecipe();
            while (recipeIt.hasNext()) {
                String command = recipeIt.next();
                if (!deviceComm.sendGenericCommand(command)) {
                    log.error("Error when sending recipe command to LoRa device: " + command);
                }
            }
            
            // Try to initialize the LoRa device
            if (!deviceComm.setTxPower(txPower)) {
                log.error("Impossible to set TxPower with value " + txPower);
            }
            if (!deviceComm.setDataRate(dataRate)) {
                log.error("Impossible to set Datarate with value " + dataRate);
            }
            if (!deviceComm.setAppEUI(appEUI)) {
                log.info("No appEUI specified. Continuing with default in the EEPROM dongle, if any");
            }
            if (!deviceComm.setAppKey(appKey)) {
                log.info("No appKey specified. Continuing with default in the EEPROM dongle, if any");
            }
            if (!deviceComm.setDeviceEUI(devEUI)) {
                log.info("No device EUI specified. Continuing with default in the EEPROM dongle, if any");
            }
            
            // Join OTAA
            if (!deviceComm.joinOTAA())
                throw new PersisterException("Impossible to connect to remote LoRa app");
            
            // Initialize the LoRa message buffer
            currentSampleMessage.clear();
            
        } catch (GenericException e) {
            throw new PersisterException(e.getMessage());
        }
        
        return true;
    }

    @Override
    public void stop() {
        
        // Close the LoRa device
        deviceComm.close();
    }

    @Override
    public boolean addSample(SampleDataContainer sample) throws PersisterException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public boolean addSamples(List<SampleDataContainer> samples) throws PersisterException {
        
        // Loop on each samples, aggregate by timestamp if possible
        Map<Long, List<SampleDataContainer>> samplesByTimestamp = new HashMap<>();
        samples.forEach((sample) -> {
            
            // aggregate by unix timestamp (converted to seconds)
            long unixTimeStamp = toAggregatedTimestamp(sample.getCollectedTimestamp());
            List<SampleDataContainer> sampleArray = samplesByTimestamp.get(unixTimeStamp);
            if(sampleArray == null) {
                sampleArray = new ArrayList<>();
                samplesByTimestamp.put(unixTimeStamp, sampleArray);
            }
            sampleArray.add(sample);
        });
        
        log.info("Aggregated " + samples.size() + " samples in " + samplesByTimestamp.keySet().size() + " queries");
        
        // Loop on time aggregated samples to calculate a single boardtimestamp to
        // use as global boardtimestamp for the message. We define the single
        // timestamp as the highest timestamp for the lowest channel in the aggregated timestamp
        // This loop searches also the lowest available GPS timestamp information to be sent before any sample values
        // to identify a unique position of the AirSensEUR unit in this chunk
        double longitude = 0.0f, latitude = 0.0f, elevation = 0.0f;
        long gpsTimestamp = Long.MIN_VALUE;
        Map<Long, Integer> boardTimestampByTimeStamp = new HashMap<>();
        for (long aggregatedTimeStamp:samplesByTimestamp.keySet()) {
            
            int lowestChannel = Integer.MAX_VALUE;
            for (SampleDataContainer sample:samplesByTimestamp.get(aggregatedTimeStamp)) {
                
                if ((latitude == 0.0f) && (sample.getLatitude() > 0.0f)) {
                    long javaTimeStamp = toJavaTimeStamp(aggregatedTimeStamp);
                    if (gpsTimestamp < javaTimeStamp) {
                        gpsTimestamp = javaTimeStamp;
                        
                        latitude = safeLonLatElev(sample.getLatitude());
                        longitude = safeLonLatElev(sample.getLongitude());
                        elevation = safeLonLatElev(sample.getAltitude());
                    }
                }
                
                if (sample.getChannel() < lowestChannel) {
                    lowestChannel = sample.getChannel();
                }
            }
            
            int highestTimeStamp = Integer.MIN_VALUE;
            for (SampleDataContainer sample:samplesByTimestamp.get(aggregatedTimeStamp)) {
                
                if ((sample.getChannel() == lowestChannel) && (sample.getTimeStamp() > highestTimeStamp)) {
                    highestTimeStamp = sample.getTimeStamp();
                }
            }
            
            boardTimestampByTimeStamp.put(aggregatedTimeStamp, highestTimeStamp);
        }
        
        try {
        
            // GPS information is set as a separate message, only one time each data chunk
            // and timestamped with the lowest timestamp in the chunk
            if (latitude != 0.0) {
                if (!sendGPSInformation((float)latitude, (float)longitude, (float)elevation, gpsTimestamp))
                    return false;
            }

            // Loop on time aggregated samples and send a message for each row
            // To compact the data to be sent, we suppose each sent packet contains the 
            // unix timestamp at the beginning of the packet, then a single boardTimeStamp common to all samples,
            // then pairs of samples information (channel, value) will follow until the max packet length is reach.
            for (long aggregatedTimeStamp:samplesByTimestamp.keySet()) {

                long javaTimeStamp = toJavaTimeStamp(aggregatedTimeStamp);
                int boardTimeStamp = boardTimestampByTimeStamp.get(aggregatedTimeStamp);

                // Start a new message with current timestamp
                currentSampleMessage.clear(javaTimeStamp, boardTimeStamp);

                // Loop on samples with the same timestamp and send through LoRa
                for (SampleDataContainer sample:samplesByTimestamp.get(aggregatedTimeStamp)) {

                    int channel = sample.getChannel();
                    double value = sample.getSampleEvaluatedVal();

                    // Append to the outcoming message and send a new message, if needed
                    if (!addSampleValueToMessage(channel, (float)value))
                        return false;

                }

                if (!flushCurrentMessage()) {
                    return false;
                }
            }
            
            return true;
            
        } catch (GenericException ex) {
            throw new PersisterException(ex.getMessage());
        }
    }

    @Override
    public String getPersisterMarker(int channel) {
        return HistoryEventContainer.EVENT_LATEST_LORA_SAMPLEPUSH_TS;
    }
    
    private static double safeLonLatElev(double unsafeVal) {
        return (unsafeVal > 0.0f)? unsafeVal : 0.0f;
    }
    
    private long toAggregatedTimestamp(long javaTimeStamp) {
        return (javaTimeStamp / (1000 * aggregationFactor));
    }
    
    private long toJavaTimeStamp(long aggregatedTimeStamp) {
        return (aggregatedTimeStamp * 1000 * aggregationFactor);
    }
                
    // Try to send the current message via confirmed LoRa messages, then wait for the required idle time
    // before proceeding with next processing.
    // Returns false if the radio channel is busy and/or is not possible to send the message
    private boolean flushCurrentMessage() throws GenericException {
        
        // Nothing to send. Exit.
        if (!currentSampleMessage.dirty) {
            return true;
        }
        
        return deviceComm.sendPayload(currentSampleMessage);
    }
    
    // Append a sample value into current message and flush
    // the message if required
    private boolean addSampleValueToMessage(int channel, float value) throws GenericException {
        
        // We need to flush current message because there is not enought space for
        // the next sample ?
        int currentSampleMessageLength = currentSampleMessage.size();
        int allowablePacketLength = deviceComm.getPacketLength();
        if ((currentSampleMessageLength + 5) > allowablePacketLength) {
            
            // Yes. 
            if (!flushCurrentMessage()) {
                return false;
            }
            
            // Start a new message
            currentSampleMessage.clear(currentSampleMessage.timeStamp, currentSampleMessage.boardTimeStamp);
        }
        
        currentSampleMessage.append((char)channel);
        currentSampleMessage.append(value);
        
        return true;
    }
    
    
    // GPS Info are formatted as follow:
    // 64 bit Unix timestamp
    // 32 bit float longitude
    // 32 bit float latitude
    // 32 bit float elevation
    private boolean sendGPSInformation(float longitude, float latitude, float elevation, long timestamp) throws GenericException {
        
        message gpsLoRaMessage = new message(LoraDeviceComm.LORA_DEFAULT_GPSINFO_PORT);
        
        gpsLoRaMessage.append(timestamp);
        gpsLoRaMessage.append(longitude);
        gpsLoRaMessage.append(latitude);
        gpsLoRaMessage.append(elevation);
        
        return deviceComm.sendPayload(gpsLoRaMessage);
    }
}
