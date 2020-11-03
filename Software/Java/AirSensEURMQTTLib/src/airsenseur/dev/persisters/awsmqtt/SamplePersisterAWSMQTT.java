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

package airsenseur.dev.persisters.awsmqtt;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SamplesPersister;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class SamplePersisterAWSMQTT extends AWSMQTTHelper implements SamplesPersister {
    
    public static class NameField {
        public final String placeholder;
        
        public NameField(String value) {
            placeholder = value;
        }
    }
    
    public static class DoubleField {
        public final double placeholder;
        
        public DoubleField(double value) {
            placeholder = value;
        }
    }
    
    private final AWSMQTTASEDevice aseDevice;
    private final ObjectMapper objectMapper;
    
    private final Logger log = LoggerFactory.getLogger(SamplePersisterAWSMQTT.class);
    
    public SamplePersisterAWSMQTT(String host, String topic, String clientID, String keyFileName, String certFileName, String hostFileName, String keyAlgorithm) throws PersisterException {
        super(host, clientID, keyFileName, certFileName, hostFileName, keyAlgorithm);
        
        this.aseDevice = new AWSMQTTASEDevice(clientID);
        
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    @Override
    public boolean startNewLog() throws PersisterException {
        log.info("Connecting to the remote endpoint");
        
        openConnection(aseDevice);
        
        return true;
    }

    @Override
    public void stop() {
        try {
            log.info("SamplePeristerMQTT stopped");
            closeConnection();
        } catch (PersisterException ex) {
            log.info("Error closing connection. Warns and continue.");
        }
    }

    /**
     * Push a single sample to MQTT server
     * @param sample
     * @return
     * @throws PersisterException 
     */
    @Override
    public boolean addSample(SampleDataContainer sample) throws PersisterException {
                
        Thing thing = new Thing();
        try {
            // Update thing with current values
            thing.state.desired.counter = sample.getChannel();
            
            // Stringify
            String jsonState = "{ \"timestamp\": " + sample.getCollectedTimestamp() + ", " + getJSONPayloadForSample(sample) + "}";
            
            // Update remote shadow status
            publishSample(jsonState);
            
        } catch (PersisterException ex) {
            log.info(ex.getMessage());
            
            return false;
        }
        
        return true;
    }

    /**
     * Push several samples to the MQTT server
     * @param samples
     * @return
     * @throws PersisterException 
     */
    @Override
    public boolean addSamples(List<SampleDataContainer> samples) throws PersisterException {
        
        if (samples == null) {
            throw new PersisterException(("Invalid parameter on addSamples"));
        }
        
        // Loop on samples and send all through MQTT
        for (SampleDataContainer sample:samples) {
            if (!addSample(sample)) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public String getPersisterMarker(int channel) {
        return HistoryEventContainer.EVENT_LATEST_AWSMQTT_SAMPLEPUSH_TS;
    }
    
    
    private String getJSONPayloadForSample(SampleDataContainer sample) throws PersisterException {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("\"state\":{");
        sb.append("\"reported\":{");
                
        int channel = sample.getChannel();
        sb.append(synthesizeTextJSONField("name", channel, sample.getName())).append(",");
        sb.append(synthesizeDoubleJSONField("sampleRawVal", channel, sample.getSampleVal())).append(",");
        sb.append(synthesizeDoubleJSONField("sampleEvaluatedVal", channel, sample.getSampleEvaluatedVal())).append(",");
        sb.append(synthesizeDoubleJSONField("boardTimeStamp", channel, sample.getTimeStamp())).append(",");
        sb.append(synthesizeGenericDoubleJSONField("gpsTimestamp", sample.getGpsTimestamp())).append(",");
        sb.append(synthesizeGenericDoubleJSONField("latitude", sample.getLatitude())).append(",");
        sb.append(synthesizeGenericDoubleJSONField("longitude", sample.getLongitude())).append(",");
        sb.append(synthesizeGenericDoubleJSONField("altitude", sample.getAltitude()));
        
        sb.append("}"); // end of reported
        sb.append("}"); // end of state

        return sb.toString();
    }
    
    private String synthesizeGenericDoubleJSONField(String fieldName, double value) throws PersisterException {

        DoubleField field = new DoubleField(value);
        
        String json = "";
        try {
            String jsonTemp = objectMapper.writeValueAsString(field);
            jsonTemp = jsonTemp.replace("placeholder", fieldName);
            
            json = jsonTemp.substring(1, jsonTemp.length()-1);
            
        } catch (JsonProcessingException ex) {
            throw new PersisterException(ex.getMessage());
        }
        
        return json;
    }
    
    private String synthesizeDoubleJSONField(String fieldName, int channel, double value) throws PersisterException {
        
        DoubleField field = new DoubleField(value);
        
        String json = "";
        try {
            String jsonTemp = objectMapper.writeValueAsString(field);
            json = replaceJsonField(jsonTemp, fieldName, channel);
            
        } catch (JsonProcessingException ex) {
            throw new PersisterException(ex.getMessage());
        }
        
        return json;
    }
    
    private String synthesizeTextJSONField(String fieldName, int channel, String value) throws PersisterException {
        
        NameField field = new NameField(value);
        String json = "";
        try {
            String jsonTemp = objectMapper.writeValueAsString(field);
            json = replaceJsonField(jsonTemp, fieldName, channel);
            
        } catch (JsonProcessingException ex) {
            throw new PersisterException(ex.getMessage());
        }
        
        return json;
    }
    
    private String replaceJsonField(String jsonTemp, String fieldName, int channel) {
        
        String fieldNameReplacement = String.format("%s_%03d", fieldName, channel);
        jsonTemp = jsonTemp.replace("placeholder", fieldNameReplacement);
        
        return jsonTemp.substring(1, jsonTemp.length()-1);
    }
}
