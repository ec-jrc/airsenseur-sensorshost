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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class IFLINKRequestWorkerThreadCurl extends IFLINKRequestWorkerThread {
    
    private final Logger log = LoggerFactory.getLogger(IFLINKRequestWorkerThreadCurl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IFLINKRequestWorkerThreadCurl(String apiUrl, int debugVerbose, String sensorID, String bearerToken, String json, SamplePersisterIFLINKBase parent) {
        super(apiUrl, debugVerbose, sensorID, bearerToken, json, parent);
    }
    
    @Override
    public boolean executeRequest(String apiUrl, int debugVerbose, String sensorID, String bearerToken, String json) {
        
        boolean result;
        
        List<String> parameters = new ArrayList<>();
        
        parameters.add("/usr/bin/curl");

        parameters.add("-H");
        parameters.add("Accept: application/json");
        parameters.add("-H");
        parameters.add("Authorization: Bearer " + bearerToken);
        parameters.add("-d");
        parameters.add(json);
        parameters.add(apiUrl);
        
        try {
            
            // Execute the external process then look at the results
            Process process = new ProcessBuilder(parameters).start();
            try {
                process.waitFor(getParent().getTimeout(), TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                return false;
            }
            
            int processResult = process.exitValue();
            if (processResult != 0) {
                log.error("Error starting the external process for iFLINK transaction: ErrorCode = " + processResult);
                return false;
            }
            
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            } 
            
            result = checkForErrorCodes(sb.toString());
            
        } catch (IOException ex) {
            log.error("Error executing the external process for iFLINK transaction");
            result = false;
        }
        
        return result;
    }
    
    private static class IFLINKResponse {
        public int sensorid;
        public String message;
        public int httpStatusCode;  
        
        @JsonProperty("http-status-code")
        public int getHttpStatusCode () {
            return httpStatusCode;
        }
        
        @JsonProperty("http-status-code")
        public void setHttpStatusCode(int statusCode) {
            httpStatusCode = statusCode;
        }
    }
    
    private boolean checkForErrorCodes(String response) {
        
        try {
            IFLINKResponse objResponse = objectMapper.readValue(response, IFLINKResponse.class);
            
            if (objResponse.httpStatusCode == 200) {
                return true;
            }
        } catch (IOException ex) {
            log.error("Invalid response received from the iFLINK server");
            if (getDebugVerbose() > 3) {
                log.error(response);
            }
            return false;
        }
        
        return true;
    }
    
}
