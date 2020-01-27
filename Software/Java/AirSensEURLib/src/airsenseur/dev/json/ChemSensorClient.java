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

package airsenseur.dev.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read sensors data from a JSON remote service
 * @author marcos
 */
public class ChemSensorClient {
    
    private String hostname = "localhost";
    private int port = 8000;
    private Socket socket;
    private ChemSensorService service;
    private static final Logger log = LoggerFactory.getLogger(ChemSensorClient.class);
    
    public boolean connect(String hostname, int port) {
        
        this.hostname = hostname;
        this.port = port;
        
        JsonRpcClient client = new JsonRpcClient(new ObjectMapper());
        try {

            socket = new Socket(InetAddress.getByName(this.hostname), this.port);
            service = ProxyUtil.createClientProxy(ChemSensorService.class.getClassLoader(), ChemSensorService.class, client, socket);

        } catch (UnknownHostException ex) {
            log.error("Host " + hostname + " not found when connecting to retrieve sensors data");
            closeSocket();
            return false;
        } catch (IOException ex) {
            log.error("Input/Output exception when connecting to sensors data server at " + hostname + ":" + port);
            closeSocket();
            return false;
        }
        
        return true;
    }
    
    public void disconnect() {
        closeSocket();
    }
    
    public HostStatus getHostStatus() {
        
        if (service == null) {
            return null;
        }
        
        try {
            return service.getHostStatus();
        } catch (UndeclaredThrowableException ex) {
            return null;
        }
    }
    
    public List<BoardInfo> getSensorBoardsInfo() {
        if (service == null) {
            return null;
        }
        
        try {
            return service.getSensorBoardsInfo();
        } catch (UndeclaredThrowableException ex) {
            return null;
        }
    }
    
    public SensorConfig getSensorConfig(int sensorId) {
        if (service == null) {
            return null;
        }
        
        try {
            return service.getSensorConfig(sensorId);
        } catch (UndeclaredThrowableException ex) {
            return null;
        }
    }
       
    public SampleData getLastSample(int channel) {

        if (service == null) {
            return null;
        }

        try {
            return service.getLastSample(channel);
        } catch (UndeclaredThrowableException ex) {
            return null;
        }
    }
    
    public boolean startSampling() {
        
        if (service == null) {
            return false;
        }
        
        try {
            return service.startSampling();
        } catch (UndeclaredThrowableException ex) {
            return false;
        }
    }
    
    public boolean stopSampling() {
        
        if (service == null) {
            return false;
        }
        
        try {
            return service.stopSampling();
        } catch (UndeclaredThrowableException ex) {
            return false;
        }
    }
    
    public List<RawCommand> sendRawData(List<RawCommand> rawData) {
        
        if (service == null) {
            return null;
        }
        
        try {
            return service.sendRawData(rawData);
        } catch (UndeclaredThrowableException ex) {
            return null;
        }
    }
    
    public Integer getNumSensors() {
        if (service == null) {
            return null;
        }
        
        try {
            return service.getNumSensors();
        } catch (UndeclaredThrowableException ex) {
            return null;
        }
    }
    
    
    public void takeOwnership() {
        if (service != null) {
            try {
                service.takeOwnership();
            } catch (UndeclaredThrowableException ex) {
            }
        }
    }
    
    public void releaseOwnership() {
        if (service != null) {
            try {
                service.releaseOnwnership();
            } catch (UndeclaredThrowableException ex) {
            }
        }
    }
    
    private void closeSocket() {
        
        try {
            if (socket != null) {
                socket.close();
                
                socket = null;
                service = null;
            }
        } catch (IOException ex) {
        }
    }
}
