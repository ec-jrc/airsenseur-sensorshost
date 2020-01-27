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

package airsenseur.dev.chemsensorpanel.comm;

import airsenseur.dev.chemsensorpanel.AirSensEURPanel;
import airsenseur.dev.comm.CommChannelFactory;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.SensorBus.SensorBusMessageConsumer;
import airsenseur.dev.comm.SensorBusBase;
import airsenseur.dev.comm.TransportLogicFactory;
import airsenseur.dev.exceptions.SensorBusException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class SensorBusCommunicationHandler extends SensorBusBase implements SensorBusMessageConsumer {
    
    private AirSensEURPanel parent;
    private List<AppDataMessage> externalBuffer = null;
    private final Logger log = LoggerFactory.getLogger(SensorBusCommunicationHandler.class);
    
    public SensorBusCommunicationHandler(AirSensEURPanel parent) {
        this.parent = parent;
    }
    
    // Standard constructor is forbidden because we need a valid parent for the data callback
    private SensorBusCommunicationHandler() {
    }
    
    public void connectToSerialLine(String portId, boolean usePointToMultipoint) throws SensorBusException {
        
        // Inizialize with the proper communication channel and transport logic
        init(this, CommChannelFactory.commChannelType.SERIAL, 
                usePointToMultipoint? TransportLogicFactory.transportLogicType.POINT_TO_MULTIPOINT : TransportLogicFactory.transportLogicType.POINT_TO_POINT);
        
        connectToBus(portId);
    }
    
    public void connectToNetworkedHost(String address) throws SensorBusException {
        
        // Initialize with the proper communication channel and transport logic
        init(this, CommChannelFactory.commChannelType.JSON_RPC, TransportLogicFactory.transportLogicType.FLAT);
        
        connectToBus(address);
        takeBusOwnership();
    }

    @Override
    public void disConnectFromBus() {
        
        try {
            releaseBusOwnership();
        } catch (SensorBusException ex) {
        }
        
        super.disConnectFromBus(); 
    }
    
    
    // From SensorBusMessageConsumer
    // Data receiver callback implementation
    @Override
    public void onNewMessageReady(AppDataMessage message) {
        
        // Debug
        log.debug("R> [" + message.getBoardId() + "] (" + message.getCommandString() + ")" );
        
        if (parent != null) {
            parent.onDataReceived(message);
        }
    }

    // From Sensor Bus
    // This SensorBus implementation allows to dump all DataMessages sent to the 
    // bus on an external file. This is used to generate a configuration file that can
    // be used to reload configurations from external persistence
    @Override
    public void writeMessageToBus(AppDataMessage message) throws SensorBusException {
        
        log.debug("T> [" + message.getBoardId() + "] (" + message.getCommandString() + ")" );
        
        if (externalBuffer != null) {
            externalBuffer.add(message);
        } else {
            super.writeMessageToBus(message); 
        }
    }

    // Start and stop dumping sent messages to an external buffer
    public void startDumpingToBuffer(List<AppDataMessage> buffer) {
        this.externalBuffer = buffer;
    }
    
    public void stopDumpingToBuffer() {
        this.externalBuffer = null;
    }
}
