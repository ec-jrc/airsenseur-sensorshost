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

package airsenseur.dev.chemsensorhost;

import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.exceptions.SensorBusException;
import airsenseur.dev.json.RawCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Communications within SensorBus are asynchronous. 
 * When an external application wants to read parameters stored on SensorBus 
 * connected boards, a logic to match requests and answers is needed.
 * This is what RawCommandQueue implements.
 * @author marco
 */
public class RawCommandQueue {
    
    private static class MessageContainer {
        
        private AppDataMessage dataMessage = null;
        private boolean acknowledged = false;
        
        public void setDataMessage(AppDataMessage dataMessage) {
            this.dataMessage = dataMessage;
            acknowledged = false;
        }
        
        public AppDataMessage getDataMessage() {
            return dataMessage;
        }
        
        public void setAcknowledged() {
            acknowledged = true;
            this.dataMessage = null;
        }
        
        public boolean isAcknowledged() {
            return acknowledged;
        }
    }

    private final Semaphore mutex = new Semaphore(1);    
    private final ShieldProtocolLayer protocolHelper;
    private final MessageContainer messageContainer = new MessageContainer();
    
    public RawCommandQueue(ShieldProtocolLayer protocolHelper) {
        this.protocolHelper = protocolHelper;
    }
    
    public List<RawCommand> sendCommandList(List<RawCommand> list) throws InterruptedException, SensorBusException {
        
        List<RawCommand> result = new ArrayList<>();
        
        // Wait if previous list has not completed
        mutex.tryAcquire(10, TimeUnit.SECONDS);
        
        // Loop on all messages to be sent
        for (RawCommand command:list) {
            AppDataMessage dataMessage = new AppDataMessage(command.boardId, command.commandString, command.commandString);
            
            // Send current message and wait for answer
            synchronized(messageContainer) {
                messageContainer.setDataMessage(dataMessage);
                protocolHelper.renderRawData(dataMessage);
                
                // Wait for acknowledge
                long curTime = System.currentTimeMillis();
                while (!messageContainer.isAcknowledged() && (System.currentTimeMillis() - curTime) < 1000) {
                    messageContainer.wait(100);
                }
                
                // If message has been acknowledged, generate a result
                if (messageContainer.isAcknowledged()) {
                    RawCommand rawCommand = new RawCommand(dataMessage.getBoardId(), dataMessage.getCommandString(), dataMessage.getCommandComment());
                    result.add(rawCommand);
                }
            }
        }
        
        // Release mutex 
        mutex.release();
        
        return result;
    }    
    
    
    // Handle messages coming from the SensorBus
    public boolean onNewMessageReady(AppDataMessage dataMessage) {
        
        boolean result = false;
        
        AppDataMessage pivot;
        synchronized(messageContainer) {
            
            pivot = messageContainer.getDataMessage();
            if ((pivot != null) && pivot.compareDataMessages(dataMessage)) {
                
                pivot.setCommandString(dataMessage.getCommandString());
                messageContainer.setAcknowledged();
                messageContainer.notify();

                result = true;
            }
        }
        
        return result;
    }
}
