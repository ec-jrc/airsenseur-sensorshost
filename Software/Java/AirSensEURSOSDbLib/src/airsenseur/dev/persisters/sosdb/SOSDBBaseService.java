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
package airsenseur.dev.persisters.sosdb;

import airsenseur.dev.exceptions.PersisterException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

/**
 * Base service for all SOSDB Json based services
 * @author marco
 */
public abstract class SOSDBBaseService {
    
    protected final String foiId;
    protected final String foiName;
    protected final ObjectMapper mapper = new ObjectMapper();
    protected final String offeringName;
    protected final List<String> sensorsObservedProp;
    protected final List<String> sensorsProcedure;
    protected final String url;    
    protected final int timeout;
    
    protected int lastStatusCode;    
    protected String lastReasonMessage;

    FileWriter dumpFile;
    
    public abstract Logger getLogger();

    public SOSDBBaseService(ConfigurationSOSDB configuration) throws PersisterException {
        
        // Initialize the base url
        StringBuilder sb = new StringBuilder("http://" + configuration.getSOSDBHost());
        if (configuration.getSOSDBPort() != 80) {
            sb.append(":").append(configuration.getSOSDBPort());
        }
        sb.append("/").append(configuration.getSOSDBEndpoint());
        url = sb.toString();
        timeout = configuration.getHTTPTimeout();

        // Initialize other parameters
        offeringName = configuration.getOfferingName();
        
        sensorsProcedure = configuration.getSensorsProcedure();
        sensorsObservedProp = configuration.getSensorsObservedProp();
        
        foiId = configuration.getFOIId();
        foiName = configuration.getFOIName();

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);     

        if (!configuration.debugDumpJSON().isEmpty()) {
            try {
                dumpFile = new FileWriter(configuration.debugDumpJSON());
            } catch (IOException ex) {
                throw new PersisterException("Error when opening JSON transaction file dump: " + configuration.debugDumpJSON());
            }
        }
    }
    
    /**
     * Send the input object to server and wait for the answer
     * The returned value is an instance of Class<?> resultFactory or null if fail.
     * When fail, the caller can look at the lastStatusCode and lastReasonMessage to properly
     * handle the problem. If null and lastStatusCode == 200 the problem has been occurred
     * on deserialization of data back from the server.
     * @param input: data to be sent to the server 
     * @param resultFactory: data type expected to the server
     * @return the object populated with the server answer or null if something went wrong
     * @throws PersisterException 
     */
    protected Object sendToServer(Object input, Class<?> resultFactory) throws PersisterException {
        
        // Serialize through JSON
        String jsonString;
        try {
            jsonString = mapper.writeValueAsString(input);
        } catch (JsonProcessingException ex) {
            throw new PersisterException(ex.getMessage());
        }
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .build();
        HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        try {
            HttpPost postRequest = new HttpPost(url);
            StringEntity inputEntity = new StringEntity(jsonString);
            inputEntity.setContentType("application/json");
            inputEntity.setContentEncoding("charset=UTF-8");
            postRequest.setEntity(inputEntity);
            
            HttpResponse response = httpClient.execute(postRequest);
            lastStatusCode = response.getStatusLine().getStatusCode();
            lastReasonMessage = "";
            if (lastStatusCode != 200) {
                
                lastReasonMessage = response.getStatusLine().getReasonPhrase();
                getLogger().error("SOSDB server error response code: " + lastStatusCode);
                getLogger().error("SOSDB server error: " + lastReasonMessage);
                
                dumpTransaction(jsonString, response);
                
                return null;
            } 

            // Try to deserialize the result
            try {
                InputStream inputStream = response.getEntity().getContent();
                return mapper.readValue(inputStream, resultFactory);
            } catch (JsonParseException | JsonMappingException ex) {
                return null;
            }
            
        } catch (IOException e) {
            throw new PersisterException(e.getMessage());
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private void dumpTransaction(String jsonString, HttpResponse response) throws UnsupportedOperationException {
        if (dumpFile != null) {
            try {
                dumpFile.write("S:\n");
                dumpFile.write(jsonString);
                dumpFile.write("R:\n");
                InputStream inputStream = response.getEntity().getContent();
                dumpFile.write(getStringFromInputStream(inputStream));
                dumpFile.write("\n");
                dumpFile.flush();
            } catch (IOException ex) {
            }
        }
    }

    private String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }

        return sb.toString();
    }
}
