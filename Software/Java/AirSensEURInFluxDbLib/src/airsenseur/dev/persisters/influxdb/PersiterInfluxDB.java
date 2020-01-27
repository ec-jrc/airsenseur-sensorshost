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

package airsenseur.dev.persisters.influxdb;

import airsenseur.dev.exceptions.PersisterException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic InfluxDB persister
 * @author marco
 */
public class PersiterInfluxDB {
    
    private final String dataSetName;
    private final String url;
    private final String dbUser;
    private final String dbPassword;
    private final boolean useLineProtocol;
    private final boolean useSSL;
    private final int timeout;
    
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(PersiterInfluxDB.class);

    public PersiterInfluxDB(String dataSetName, String dbHost, int dbPort, String dbName, String dbUser, String dbPassword, boolean useLineProtocol, boolean useSSL, int timeout) {
        this.dataSetName = dataSetName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.useLineProtocol = useLineProtocol;
        this.useSSL = useSSL;
        this.timeout = timeout;
        
        String protocol = "http://";
        if (this.useSSL) {
            protocol = "https://";
        }
        
        if (!useLineProtocol) {
            url = "http://" + dbHost + ":" + dbPort + "/db/" + dbName + "/series?u=" + dbUser + "&p=" + dbPassword;
        } else {
            url = protocol + dbHost + ":" + dbPort + "/write?db=" + dbName + "&precision=ms";
        }
    }    
    
    protected boolean sendDataToInfluxDB(Series series) throws PersisterException {
        if (useLineProtocol) {
            return sendDataToInfluxDBThroughLineProtocol(series);
        }
        
        return sendDataToInfluxDBThroughJSON(series);
    }
    
    protected String getDataSetName() {
        return dataSetName;
    }
    
    private boolean sendDataToInfluxDBThroughJSON(Series series) throws PersisterException {
        
        // Serialize through JSON
        String jsonString;
        try {
        jsonString = mapper.writeValueAsString(series);
        } catch (JsonProcessingException ex) {
            throw new PersisterException(ex.getMessage());
        }
        
        boolean result = true;
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost postRequest = new HttpPost(url);
            StringEntity input = new StringEntity(jsonString);
            input.setContentType("text/plain");
            postRequest.setEntity(input);
            
            HttpResponse response = httpClient.execute(postRequest);
            if (response.getStatusLine().getStatusCode() != 200) {
                
                log.error("InfluxDB server error response code: " + response.getStatusLine().getStatusCode());
                log.error("IngluxDB server error: " + response.getStatusLine().getReasonPhrase());
                
                result = false;
            }
            
            httpClient.getConnectionManager().shutdown();
            
        } catch (IOException e) {
            log.error("Error pushing samples to InfluxDB: " + e.getMessage());
            return false;
        }
        
        return result;
    }    
    
    private boolean sendDataToInfluxDBThroughLineProtocol(Series series) throws PersisterException {
        
        // Serialize through line protocol
        String queryString = serializeToLineProtocol(series);
        
        boolean result = true;
        try {
            
            // Initialize proper HTTP client
            RequestConfig config = RequestConfig.custom()
                  .setConnectTimeout(timeout * 1000)
                  .setConnectionRequestTimeout(timeout * 1000)
                  .setSocketTimeout(timeout * 1000).build();
            CloseableHttpClient httpClient;
            if (useSSL) {
                SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                        .build();
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
                httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).setSSLSocketFactory(sslsf).build();
                
            } else {
                httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
            }
            
            HttpPost postRequest = new HttpPost(url);         
            StringEntity input = new StringEntity(queryString, ContentType.DEFAULT_BINARY);
            input.setContentType("text/plain");
            postRequest.setEntity(input);
            try {
                postRequest.addHeader(new BasicScheme(StandardCharsets.UTF_8).authenticate(new UsernamePasswordCredentials(dbUser, dbPassword) , postRequest, null));
            } catch (AuthenticationException ex) {
                log.error("InfluxDB basic authentication failed when creating BasicScheme");                    
                throw new PersisterException("Basic authentication failed");
            }
            
            HttpResponse response = httpClient.execute(postRequest);
            if (response.getStatusLine().getStatusCode() != 204) {
                
                log.error("InfluxDB server error response code: " + response.getStatusLine().getStatusCode());
                log.error("IngluxDB server error: " + response.getStatusLine().getReasonPhrase());
                
                result = false;
            }
            
            httpClient.getConnectionManager().shutdown();
            
        } catch (IOException e) {
            log.error("Error pushing data to InfluxDB: " + e.getMessage());
            return false;
        } catch (NoSuchAlgorithmException e) {
            log.error("Error pushing data to InfluxDB: Chipher algorithm not implemented: " + e.getMessage());
            return false;
        } catch (KeyStoreException e) {
            log.error("Error pushing data to InfluxDB: SSL Keystore exception: " + e.getMessage());
            return false;
        } catch (KeyManagementException e) {
            log.error("Error pushing data to InfluxDB: SSL Key Management Exception: " + e.getMessage());
            return false;
        }
        
        return result;
    }
    
    private String serializeToLineProtocol(Series series) throws PersisterException {
        
        StringBuilder sb = new StringBuilder();
        for (Serie serie:series) {
            sb.append(serie.toLineProtocol());
        }
        
        return sb.toString();
    }    
    
}
