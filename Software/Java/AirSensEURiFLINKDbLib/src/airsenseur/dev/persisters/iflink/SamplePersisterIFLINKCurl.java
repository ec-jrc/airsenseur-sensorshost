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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class SamplePersisterIFLINKCurl extends SamplePersisterIFLINKBase {
        
    private final Logger log = LoggerFactory.getLogger(SamplePersisterIFLINKCurl.class);

    public SamplePersisterIFLINKCurl(String host, String endpoint, String sensorID, String bearerToken, List<String> sensorsList, String datePath, String inboundSensorID, boolean useHTTPS, boolean updatePosition, int numThreads, int aggregationFactor, int timeout, int debugVerbose) throws PersisterException {
        super(host, endpoint, sensorID, bearerToken, sensorsList, datePath, inboundSensorID, useHTTPS, updatePosition, numThreads, aggregationFactor, timeout, debugVerbose);
        
        log.info("Updating IFLink with curl engine");
    }
    
    @Override
    public boolean startNewLog() throws PersisterException {
        return true;
    }


    @Override
    Runnable getNewIFLINKRequestWorkerThread(String targetUrl, int debugVerbose, String sensorID, String bearerToken, String json, SamplePersisterIFLINKBase parent) {
        
        Runnable worker = new IFLINKRequestWorkerThreadCurl(targetUrl, 
                                                            debugVerbose, 
                                                            sensorID, 
                                                            bearerToken, 
                                                            json,
                                                            this);
        
        return worker;   
    }
}
