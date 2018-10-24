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

package airsenseur.dev.comm;

import airsenseur.dev.helpers.TaskScheduler;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base abstract class implementing common properties for TransporLogic with AppDataMessage pump
 * @author marco
 */
public abstract class TransportLogicBaseImpl extends TaskScheduler implements TransportLogic {
    
    private final AppDataMessageQueue rxDataQueue;
    private final AppDataMessageQueue txDataQueue;
    private final SensorBus parent;
    private final CommChannel commChannel;
    private final Logger log = LoggerFactory.getLogger(TransportLogicBaseImpl.class);
    
    public TransportLogicBaseImpl(AppDataMessageQueue rxDataQueue, AppDataMessageQueue txDataQueue, SensorBus parent, CommChannel commChannel) {
        this.rxDataQueue = rxDataQueue;
        this.txDataQueue = txDataQueue;
        this.parent = parent;
        this.commChannel = commChannel;
    }
    
    // From Task Skeduler
    @Override
    public void taskMain() {
        
        log.debug("Starting task " + getTaskName());
        
        try {
            // Spill out messages coming from the application layer and send to the serial line
            while (!isShutdown()) {
                
                // Take the message
                AppDataMessage message = getTxDataQueue().take();
                
                // Send through communication channel
                commChannel.writeMessage(toSerialBusFormat(message));
            }
        } catch (InterruptedException ex) {
        } catch (IOException io) {
            log.debug("Exception occurred in  " + getTaskName());
            if (getParent() != null) {
                getParent().reConnectToBus();
            }
        }
        
        log.debug("Terminating task " + getTaskName());
    }

    // From TaskScheduler
    @Override
    public String getTaskName() {
        return "TransportLogic-AppToBus";
    }
    
    // From TransportLogic
    @Override
    public void onDataReceived(InputStream inputStream) throws InterruptedException {
        
        byte[] readBuffer = new byte[256];
        try {
            int numBytes = inputStream.read(readBuffer);
            for (int i = 0; i < numBytes; i++) {
                AppDataMessage dataMessage = onRxCharReceived(readBuffer[i]);
                if (dataMessage != null) {
                    getRxDataQueue().put(dataMessage);
                }
            }
        } catch (IOException | StringIndexOutOfBoundsException ex) {
            if (getParent() != null) {
                getParent().reConnectToBus();
            }
        }
    }
        
    /**
     * (Optional)
     * @return the protocol version 
     */
    abstract protected char getProtocolVersion();

    /**
     * @return the rxDataQueue
     */
    public AppDataMessageQueue getRxDataQueue() {
        return rxDataQueue;
    }

    /**
     * @return the txDataQueue
     */
    public AppDataMessageQueue getTxDataQueue() {
        return txDataQueue;
    }

    /**
     * @return the parent
     */
    public SensorBus getParent() {
        return parent;
    }
}
