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

import java.io.IOException;
import java.util.Random;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class IFLINKRequestWorkerThreadEmbedded extends IFLINKRequestWorkerThread {
    
    private final Logger log = LoggerFactory.getLogger(IFLINKRequestWorkerThreadEmbedded.class);
    private final CloseableHttpClient httpClient;

    public IFLINKRequestWorkerThreadEmbedded(CloseableHttpClient httpClient, String apiUrl, int debugVerbose, String sensorID, String bearerToken, String json, SamplePersisterIFLINKBase parent) {
        super(apiUrl, debugVerbose, sensorID, bearerToken, json, parent);
        this.httpClient = httpClient;
    }
    
    @Override
    public boolean executeRequest(String apiUrl, int debugVerbose, String sensorID, String bearerToken, String json) {
        
        boolean result = true;
        
        try {

            HttpPost postRequest = new HttpPost(apiUrl);
            StringEntity input = new StringEntity(json, ContentType.APPLICATION_JSON);

            postRequest.addHeader("ASETransactionID", "TRN-" + ((new Random()).nextFloat()));
            postRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            postRequest.setEntity(input);

            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (response.getStatusLine().getStatusCode() != 200) {

                    log.error("iFLINK server error response code: " + response.getStatusLine().getStatusCode());
                    log.error("iFLINK server error: " + response.getStatusLine().getReasonPhrase());

                    result = false;
                }
                
                // This should'nt needed but we added for completeness
                response.close();
            }

        } catch (IOException e) {
            log.error("Error pushing data to iFLINK server: " + e.getMessage());
            return false;
        }         
        
        return result;
    }
    
}
