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

import airsenseur.dev.exceptions.SensorBusException;
import airsenseur.dev.helpers.TaskScheduler;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Low level handling of sensor bus.
 * It implements tx and rx queues and associated datapump threads
 * @author marco
 */
public abstract class SensorBusBase extends TaskScheduler implements SensorBus {
    
    private final static int dataMessageQueueSize = 256;
    
    private final AppDataMessageQueue rxDataQueue = new AppDataMessageQueue(dataMessageQueueSize);
    private final AppDataMessageQueue txDataQueue = new AppDataMessageQueue(dataMessageQueueSize);
    
    private SensorBusMessageConsumer messageConsumer = null; 
    private CommChannel commChannel = null;    
    private TransportLogicBaseImpl transportLogic = null;
    
    private String busIdentifier = null;
    private boolean connected = false;
    
    private final Logger log = LoggerFactory.getLogger(SensorBusBase.class);
    
    // From SensorBus
    @Override
    public void init(SensorBusMessageConsumer messageConsumer, CommChannelFactory.commChannelType commChannelType, TransportLogicFactory.transportLogicType type) throws SensorBusException {
        
        this.messageConsumer = messageConsumer;
        commChannel = CommChannelFactory.getInstance(commChannelType);
        transportLogic = TransportLogicFactory.getInstance(type, rxDataQueue, txDataQueue, this, commChannel);
    }

    // From SensorBus
    @Override
    public void connectToBus(String busIdentifier) throws SensorBusException {
        
        // Clear message queues
        rxDataQueue.clear();
        txDataQueue.clear();

        // Connect to the specified channel
        try {
            if (!commChannel.openPort(busIdentifier, transportLogic.getBaudrate(), transportLogic)) {
                throw new SensorBusException("Impossible to open the specified communication channel");
            }
        } catch (Exception e) {
            throw new SensorBusException(e.getMessage());
        }
        
        // Remember where I'm connected if I need to reconnect to
        this.busIdentifier = busIdentifier;
        connected = true;
        
        // Start the tx thread message pump
        transportLogic.startNow();
        
        // Start the rx thread message pump
        startNow();
    }

    // From SensorBus
    @Override
    public void disConnectFromBus() {
        
        // Stop the tx thread message pump
        transportLogic.stop();
        
        // Stop the rx thread message pump
        stop();
        
        // Clear message queues
        rxDataQueue.clear();
        txDataQueue.clear();

        // Close the communication channel
        commChannel.closePort();
        connected = false;
    }

    // From SensorBus
    @Override
    public boolean reConnectToBus() {
        disConnectFromBus();
        try {
            connectToBus(busIdentifier);
        } catch (SensorBusException ex) {
            return false;
        }
        
        return true;
    }

    @Override
    public void takeBusOwnership() throws SensorBusException {
        
        try {
            commChannel.takeOwnership();
        } catch (IOException ex) {
            throw new SensorBusException("Error taking remote unit ownership");
        }
    }

    @Override
    public void releaseBusOwnership()  throws SensorBusException {
        try {
            commChannel.releaseOwnership();
        } catch (IOException ex) {
            throw new SensorBusException(("Error releasing remote unit ownership"));
        }
    }

    @Override
    public void writeMessageToBus(AppDataMessage message) throws SensorBusException {
        
        // If not connected, try to reconnect
        if (!isConnected()) {
            reConnectToBus();
        }
        
        try {
            txDataQueue.put(message);
        } catch (InterruptedException ex) {
            log.debug("exception occurred in writeMessageToBus");
        }
    }
    
    // From TaskScheduler
    // Implements the message pump extracting data from the rxDataQueue and 
    // calling the AppDataMessage consumer callback
    @Override
    public void taskMain() {
        
        log.debug("Starting task " + getTaskName());
        
        try {
            while (!isShutdown()) {
                // Extract a message from the rx Data Message Queue
                AppDataMessage message = rxDataQueue.take();
                
                if (this.messageConsumer != null) {
                    messageConsumer.onNewMessageReady(message);
                }
            }
        } catch (InterruptedException ex) {
            log.debug("exception occurred in " + getTaskName());
        }
        
        log.debug("Terminating task " + getTaskName());
    }

    // From TaskScheduler
    @Override
    public String getTaskName() {
        return "SensorBusBase-BusToApp";
    }
    
    public boolean isConnected() {
        return connected;
    }
}
