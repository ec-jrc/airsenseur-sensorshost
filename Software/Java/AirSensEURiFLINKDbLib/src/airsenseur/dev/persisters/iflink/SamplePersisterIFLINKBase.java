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

package airsenseur.dev.persisters.iflink;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SamplesPersister;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public abstract class SamplePersisterIFLINKBase implements SamplesPersister {
        
    private final String url;
    private final boolean updatePosition;
    private final String sensorID;
    private final String bearerToken;
    private final int timeout;
    private final int numThreads;
    private final int debugVerbose;
    private final String inboundSensorID;
    private final String datePath;
    private final int aggregationFactor;
    
    final ObjectMapper mapper = new ObjectMapper();
    
    private final Map<String,String> sensorsMapTemplate;    // Key: sensor name; value = "NaN". Used for fast JSON generation
    
    private final Logger log = LoggerFactory.getLogger(SamplePersisterIFLINKBase.class);
        
    private ExecutorService executor;
    private boolean sendProcessResult;

    
    public SamplePersisterIFLINKBase(String host, 
                                        String endpoint,
                                        String sensorID, 
                                        String bearerToken, 
                                        List<String> sensorsList, 
                                        String datePath, 
                                        String inboundSensorID, 
                                        boolean useHTTPS, 
                                        boolean updatePosition, 
                                        int numThreads, 
                                        int aggregationFactor, 
                                        int timeout, 
                                        int debugVerbose) throws PersisterException {
                
        this.updatePosition = updatePosition;
        this.url = ((useHTTPS)? "https://" : "http://") + host + endpoint;
        
        this.timeout = timeout;
        this.bearerToken = bearerToken;
        this.sensorID = sensorID;
        this.inboundSensorID = inboundSensorID;
        this.datePath = datePath;
        this.debugVerbose = debugVerbose;
        this.numThreads = numThreads;
        this.aggregationFactor = aggregationFactor;
        
        sensorsMapTemplate = new HashMap();
        sensorsList.forEach((sensorName) -> {
            sensorsMapTemplate.put(sensorName.trim().toLowerCase(), "NaN");
        });
        
        executor = Executors.newFixedThreadPool(numThreads);
        sendProcessResult = false;
    }
    
    @Override
    public void stop() {

        // Force shutdown of request threads
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
        int retry = 0;
        while (!executor.isTerminated() && (retry < 30)) { 
            try { 
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            retry++;
        } 
    }    

    @Override
    public boolean addSample(SampleDataContainer sample) throws PersisterException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public boolean addSamples(List<SampleDataContainer> samples) throws PersisterException {
        
        // Initialize the result for the whole dataset
        sendProcessResult = true;        
        
        // Loop on each samples, filter out unneeded channels, aggregate by timestamp if possible
        Map<Long, List<SampleDataContainer>> samplesByTimestamp = new HashMap<>();
        samples.forEach((sample) -> {
            String channelName = sample.getName().toLowerCase();
            
            // Filter out unneeded channels
            if (sensorsMapTemplate.containsKey(channelName)) {
                
                // aggregate by timestamp
                long unixTimeStamp = sample.getCollectedTimestamp() / (1000 * aggregationFactor);
                List<SampleDataContainer> sampleArray = samplesByTimestamp.get(unixTimeStamp);
                if(sampleArray == null) {
                    sampleArray = new ArrayList<>();
                    samplesByTimestamp.put(unixTimeStamp, sampleArray);
                }
                sampleArray.add(sample);
            } 
        });
        
        log.info("Aggregated " + samples.size() + " samples in " + samplesByTimestamp.keySet().size() + " queries");
        
        // Initialize the multithread executor
        executor = Executors.newFixedThreadPool(getNumThreads());
        
        // Loop on time aggregated filtered samples and generate JSON for iFLINK API
        for (long aggregatedTimeStamp:samplesByTimestamp.keySet()) {
            
            // Clone the map template to be populated with the values of all samples with this timestamp
            Map<String,String> valuedSensorsMap = new HashMap<>(sensorsMapTemplate);
            
            // Loop on samples with the same timestamp
            double longitude = 0.0f, latitude = 0.0f, elevation = 0.0f;
            for (SampleDataContainer sample:samplesByTimestamp.get(aggregatedTimeStamp)) {
                
                valuedSensorsMap.put(sample.getName().toLowerCase(), new Float(sample.getSampleEvaluatedVal()).toString());
                
                latitude = safeLonLatElev(sample.getLatitude());
                longitude = safeLonLatElev(sample.getLongitude());
                elevation = safeLonLatElev(sample.getAltitude());
            }
            
            // Add the inbound Sensor ID field
            valuedSensorsMap.put("sensorid", inboundSensorID);
            
            // JSONIZE the map by following the iFLINK requirements
            long timeStamp = aggregatedTimeStamp * aggregationFactor;
            String json = buildJSON_For_iFLINK(valuedSensorsMap, timeStamp);
            
            // Generate the target URL
            String targetUrl = synthesizeUrl(longitude, latitude, elevation);
            
            // Send to the remote server
            Runnable worker = getNewIFLINKRequestWorkerThread(targetUrl, debugVerbose, sensorID, bearerToken, json, this);
            executor.execute(worker);
            
            // If something goes wrong with current working posts, 
            // let's stop to send any other data
            if (!sendProcessResult) {
                break;
            }
        }
        
        // Wait for all threads to terminate
        executor.shutdown();
        int retry = 0;
        int maxRetry = samplesByTimestamp.size() * getTimeout() * 2;     // This is a reasonable maximum timeout
                                                                    // because we can't wait forever
        while (!executor.isTerminated() && (retry < maxRetry)) { 
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                retry = maxRetry+1;
            }
            retry++;
        }
        
        // We expect here to have all the threads terminated.
        // So the result is consistent with what happened with the remote server
        return sendProcessResult;
    }

    private static double safeLonLatElev(double unsafeVal) {
        return (unsafeVal > 0.0f)? unsafeVal : 0.0f;
    }

    @Override
    public String getPersisterMarker(int channel) {
        return HistoryEventContainer.EVENT_LATEST_IFLINK_SAMPLEPUSH_TS;
    }
    
    // Request for a worker thread generator, specific for the technology used to send data
    abstract Runnable getNewIFLINKRequestWorkerThread(String targetUrl, int debugVerbose, String sensorID, String bearerToken, String json, SamplePersisterIFLINKBase parent);
    
    // This is used by the working threads to callback the result
    // of the POST when terminated.
    public void setResult(boolean result) {
        sendProcessResult = sendProcessResult&result;
    }
    
    
    // Synthesize the JSON to be send over the wire
    private String buildJSON_For_iFLINK(Map<String,String> sensorValues, Long timeStamp) throws PersisterException {
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        sensorValues.forEach((sensorName, sensorValue) ->{
            sb.append("\"").append(sensorName).append("\": \"").append(sensorValue).append("\",");
        });
        
        // Append timestamp
        sb.append("\"").append(datePath).append("\": ").append(timeStamp.toString());
        
        sb.append("}");
        
        String json = sb.toString();
        
        // Check for JSON validity
        try {
            mapper.readTree(json); 
        } catch (IOException ex) {
            throw new PersisterException("Invalid JSON generated when sending to iFLINK server " + ex.getMessage());
        }
        
        return json;
    }
    
    
    // Synthesize the URL based on iFLINK protocol
    private String synthesizeUrl(double longitude, double latitude, double elevation) {
        
        // Generate the final URL. This is different if updatePosition is required by configuration
        String position = "";
        if (updatePosition) {
            
            position =  "/" + String.format("%.8f", longitude) + "/" + String.format("%.8f", latitude) + "/" + String.format("%.8f", elevation);
        }
        
        String target = url + "/" + sensorID + position + "/inbound";
        if (debugVerbose > 3) {
            log.info("Calling " + target);
        }
        
        return target;
    }

    /**
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * @return the numThreads
     */
    public int getNumThreads() {
        return numThreads;
    }
    
}
