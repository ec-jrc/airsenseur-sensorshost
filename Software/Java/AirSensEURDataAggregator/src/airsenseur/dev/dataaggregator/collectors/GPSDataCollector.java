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

package airsenseur.dev.dataaggregator.collectors;

import de.taimos.gpsd4java.backend.GPSdEndpoint;
import de.taimos.gpsd4java.backend.ResultParser;
import de.taimos.gpsd4java.types.PollObject;
import java.io.IOException;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read GPS data from a JSON based service
 * @author marcos
 */
public class GPSDataCollector {
    
    private GPSdEndpoint ep;
    private PollObject pollResult;
    private boolean connected = false;
    private static final Logger log = LoggerFactory.getLogger(GPSDataCollector.class);    
    
    /**
     * Try to connect to a remote GPS JSON service
     * @param host
     * @param port
     * @return: true if success
     */
    public boolean connect(String host, int port) {
        
        try {
            
            ep = new GPSdEndpoint(host, port, new ResultParser());
            
            ep.start();
            ep.watch(true, false);
            
            connected = true;
            
        } catch (IOException | JSONException ex) {
            log.error("Impossible to connect to " + host + ":" + port);
            return false;
        }
        
        return true;
    }
    
    
    /** 
     * Disconnect from the remote server
     */
    public void disconnect() {
        if (ep != null) {
            ep.stop();
            connected = false;
        }
    }
    
    
    /**
     * Retrieve information from the remote GPS
     * Info are stored on the pollResult object
     * @return: true if success, false otherwise
     */
    public boolean poll() {
        
        if (!connected) {
            return false;
        }
        
        try {
            pollResult = ep.poll();
        } catch (IOException ex) {
            log.error(ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * Return true if connected and a valid fix has been found
     * @return 
     */
    public boolean isFixed() {
        return (connected && (pollResult != null) && (!pollResult.getFixes().isEmpty()));
    }
    
    /**
     * Retrieve the last available timestamp from the 1st satellite
     * @return 
     */
    public double getLastTimeStamp() {
        if (connected && (pollResult != null) && (!pollResult.getFixes().isEmpty())) {
            return pollResult.getFixes().get(0).getTimestamp();
        }
        
        return 0.0;
    }
    
    /**
     * Retrieve the last available latitude info from the 1st satellite
     * @return 
     */
    public double getLastLatitude() {
        if (connected && (pollResult != null) && (!pollResult.getFixes().isEmpty())) {
            return pollResult.getFixes().get(0).getLatitude();
        }
        
        return 0.0;
    }
    
    /**
     * Retrieve the last available latitude info from the 1st satellite
     * @return 
     */
    public double getLastLongitude() {
        if (connected && (pollResult != null) && (!pollResult.getFixes().isEmpty())) {
            return pollResult.getFixes().get(0).getLongitude();
        }
        
        return 0.0;
    }
    
    /**
     * Retrieve the last available latitude info from the 1st satellite
     * @return 
     */
    public double getLastAltitude() {
        if (connected && (pollResult != null) && (!pollResult.getFixes().isEmpty())) {
            return pollResult.getFixes().get(0).getAltitude();
        }
        
        return 0.0;
    }
}
