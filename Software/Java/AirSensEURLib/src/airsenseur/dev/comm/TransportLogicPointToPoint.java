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

/**
 * Implements peer to peer communication between host and shield
 * @author marco
 */
public class TransportLogicPointToPoint extends TransportLogicBaseImpl {
    
    private final int DEFAULT_BAUDRATE = 9600;
    
    private static final char COMMPROTOCOL_HEADER = '{';
    private static final char COMMPROTOCOL_TRAILER = '}';
    private static final int COMMPROTOCOL_BUFFER_LENGTH = 48;

    private enum rxStatuses {
        IDLE, HEADER_FOUND
    }

    private rxStatuses rxStatus = rxStatuses.IDLE;
    private final StringBuilder incomingBuffer = new StringBuilder(COMMPROTOCOL_BUFFER_LENGTH);
    
    public TransportLogicPointToPoint(AppDataMessageQueue rxDataQueue, AppDataMessageQueue txDataQueue, SensorBus parent, CommChannel commChannel) {
        super(rxDataQueue, txDataQueue, parent, commChannel);
    }
    
    // From TransportLogic
    @Override
    public int getBaudrate() {
        return DEFAULT_BAUDRATE;
    }
    
    // From TransportLogic
    // Extract dataMessages from the incoming stream
    @Override
    public AppDataMessage onRxCharReceived(byte value) {
        
        char pivotChar = (char)value;
        switch(rxStatus) {
            case IDLE: {
                // Searching for an header
                if (pivotChar == COMMPROTOCOL_HEADER) {
                    incomingBuffer.setLength(0);
                    rxStatus = rxStatuses.HEADER_FOUND;
                }
            }
            break;
                
            case HEADER_FOUND: {
                // Searching for a trailer
                if (pivotChar == COMMPROTOCOL_TRAILER) {
                    
                    // Found. Generate a new AppDataMessage and place it into the FIFO
                    rxStatus = rxStatuses.IDLE;
                    AppDataMessage rxPacket = new AppDataMessage(incomingBuffer.toString());
                    return rxPacket;
                    
                } else if (pivotChar == COMMPROTOCOL_HEADER) {
                   // ... but we found an header again...
                   // Discard all.
                   rxStatus = rxStatuses.IDLE;
                } else if (incomingBuffer.length() == COMMPROTOCOL_BUFFER_LENGTH) {
                   // We did not found any trailer and
                   // the buffer is empty. Discard all
                   rxStatus = rxStatuses.IDLE;
                } else {
                    // Collecting payload
                    incomingBuffer.append(pivotChar);
                }
            }
            break;
        }
        
        return null;
    }
    
    // From TransportLogic
    @Override
    public void onDataReceived(CommChannelDataMessage message) throws InterruptedException {
        throw new UnsupportedOperationException("onDataReceived it's not supposed to be available on TransportLogicPointToPoint");
    }
    
    // In a point to point implementation, AppDataMessages are 
    // directly sent to the other side 
    @Override
    public CommChannelDataMessage toSerialBusFormat(AppDataMessage dataMessage) {
        return new CommChannelDataMessage(COMMPROTOCOL_HEADER + dataMessage.getCommandString() + COMMPROTOCOL_TRAILER);
    }
    
    @Override
    // From TransportLogicBaseImpl
    protected char getProtocolVersion() {
        return 0x00;
    }
}
