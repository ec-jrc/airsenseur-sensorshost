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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class SamplePersisterIFLINKEmbedded extends SamplePersisterIFLINKBase {
        
    private final boolean useHTTPS;
    
    private final Logger log = LoggerFactory.getLogger(SamplePersisterIFLINKEmbedded.class);
        
    private CloseableHttpClient httpClient;

    public SamplePersisterIFLINKEmbedded(String host, String endpoint, String sensorID, String bearerToken, List<String> sensorsList, String datePath, String inboundSensorID, boolean useHTTPS, boolean updatePosition, int numThreads, int aggregationFactor, int timeout, int debugVerbose) throws PersisterException {
        super(host, endpoint, sensorID, bearerToken, sensorsList, datePath, inboundSensorID, useHTTPS, updatePosition, numThreads, aggregationFactor, timeout, debugVerbose);

        this.useHTTPS = useHTTPS;
        
        log.info("Updating IFLink with embedded HTTPS engine");
    }

    @Override
    public boolean startNewLog() throws PersisterException {
        
        try {
            int timeout = getTimeout();
            int numThreads = getNumThreads();
            
            // Initialize proper HTTP client
            RequestConfig config = RequestConfig.custom()
                  .setConnectTimeout(timeout * 1000).setDecompressionEnabled(false)
                  .setConnectionRequestTimeout(timeout * 1000)
                  .setSocketTimeout(timeout * 1000).build();
            
            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
            connManager.setMaxTotal(numThreads*10);
            
            if (useHTTPS) {
                SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                        .build();
                SSLConnectionSocketFactory sslsf = new IFLINKSSLConnectionSocketFactory(sslContext);
                httpClient = HttpClients.custom().setConnectionManager(connManager).addInterceptorFirst(new IFLINKResponseInterceptor()).setDefaultRequestConfig(config).setSSLSocketFactory(sslsf).build();
                
            } else {
                httpClient = HttpClients.custom().setConnectionManager(connManager).addInterceptorFirst(new IFLINKResponseInterceptor()).setDefaultRequestConfig(config).build();
            }        
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Error pushing data to iFLINK server: Chipher algorithm not implemented: " + e.getMessage());
            return false;
        } catch (KeyStoreException e) {
            log.error("Error pushing data to iFLINK server: SSL Keystore exception: " + e.getMessage());
            return false;
        } catch (KeyManagementException e) {
            log.error("Error pushing data to iFLINK server: SSL Key Management Exception: " + e.getMessage());
            return false;
        }       
        
        return true;
    }

    @Override
    public void stop() {
        super.stop();
        
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
    }

    

    @Override
    Runnable getNewIFLINKRequestWorkerThread(String targetUrl, int debugVerbose, String sensorID, String bearerToken, String json, SamplePersisterIFLINKBase parent) {
        
        Runnable worker = new IFLINKRequestWorkerThreadEmbedded(httpClient, 
                                                                targetUrl, 
                                                                debugVerbose, 
                                                                sensorID, 
                                                                bearerToken, 
                                                                json,
                                                                this);
        
        return worker;
        
    }
    
    
    // iFLINK server seems not sending the required content length header.
    // We need to add it or the HTTP client implementation will raise a 
    // ProtocolException even if the answer is correct
    private final class IFLINKResponseInterceptor implements HttpResponseInterceptor {

        @Override
        public void process(HttpResponse hr, HttpContext hc) throws HttpException, IOException {

            HttpEntity entity = hr.getEntity();
            
            if (!hr.containsHeader(HttpHeaders.CONTENT_LENGTH)) {

                long contentLength = 0;     // We don't need to have the real
                                            // size but we're tring to evaluate here

                InputStream content = entity.getContent();
                if (content.markSupported()) {
                    content.mark(1024);
                    
                    ByteArrayOutputStream result = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = content.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }

                    contentLength = result.toString("UTF-8").length();
                    
                    content.reset();
                }
                
                hr.addHeader(HttpHeaders.CONTENT_LENGTH, "" + contentLength);
            }
        }
        
    }
}
