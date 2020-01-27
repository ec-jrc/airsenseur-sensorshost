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

package airsenseur.dev.chemsensorhost.json;

import airsenseur.dev.chemsensorhost.engine.ChemSensorHostEngine;
import airsenseur.dev.chemsensorhost.exceptions.JSONServerException;
import airsenseur.dev.json.ChemSensorService;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.StreamServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author marco
 */
public class JSONServer {
    
    private final ChemSensorServiceImpl sensorService = new ChemSensorServiceImpl();
    private final JsonRpcServer jsonRpcServer = new JsonRpcServer(sensorService, ChemSensorService.class);
    private StreamServer streamServer = null;
    
    public boolean init(ChemSensorHostEngine sensorHost, String address, int port) throws JSONServerException {
        
        try {
            int maxThreads = 10;
            int backlog = 10;
            InetAddress bindAddress = InetAddress.getByName(address);
            
            sensorService.setSensorHost(sensorHost);
            streamServer = new StreamServer(jsonRpcServer, maxThreads, port, backlog, bindAddress);
            streamServer.start();
            
        } catch (UnknownHostException ex) {
            throw new JSONServerException(ex.getMessage());
        } catch (IOException ex) {
            throw new JSONServerException(ex.getMessage());
        }
        
        return true;
    }
    
    public void stop() {
        if (streamServer != null) {
            try {
                streamServer.stop();
            } catch (InterruptedException ex) {
            }
        }
    }
}
