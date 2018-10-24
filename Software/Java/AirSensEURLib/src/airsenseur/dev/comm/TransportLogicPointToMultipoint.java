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

import airsenseur.dev.helpers.CodecHelper;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Implements point to multi-point connections between host and multiple shields
 * connected to a SensorBus
 * @author marco
 */
public class TransportLogicPointToMultipoint extends TransportLogicBaseImpl {
    
    private final int DEFAULT_BAUDRATE = 38400;
    
    private final static char COMMPROTOCOL_PTM_HOST_HEADER = '[';
    private final static char COMMPROTOCOL_PTM_HOST_TRAILER = ']';
    
    private final static char COMMPROTOCOL_PTM_VERSION = '0';
    
    private final static char COMMPROTOCOL_PTM_SLAVE_HEADER = '(';
    private final static char COMMPROTOCOL_PTM_SLAVE_TRAILER = ')';
    
    private static final int COMMPROTOCOL_BUFFER_LENGTH = 256;

    private enum rxStatuses {
        IDLE, HEADER_FOUND, VERSION_FOUND, BOARDID1_FOUND, BOARDID_FOUND,
    }
    
    private rxStatuses rxStatus = rxStatuses.IDLE;
    private int incomingBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
    
    private final StringBuilder incomingBuffer = new StringBuilder(COMMPROTOCOL_BUFFER_LENGTH);
    private final StringBuilder outcomingBuffer = new StringBuilder(COMMPROTOCOL_BUFFER_LENGTH);
    private final char[] boardIdBuffer = { 0x00, 0x00 };
    private final Semaphore semaphore = new Semaphore(1);

    public TransportLogicPointToMultipoint(AppDataMessageQueue rxDataQueue, AppDataMessageQueue txDataQueue, SensorBus parent, CommChannel commChannel) {
        super(rxDataQueue, txDataQueue, parent, commChannel);
    }

    // From TransportLogic
    @Override
    public int getBaudrate() {
        return DEFAULT_BAUDRATE;
    }

    // From TransportLogic
    @Override
    public AppDataMessage onRxCharReceived(byte value) {
        
        char pivotChar = (char)value;
        
        // Perform operations based on current status
        switch (rxStatus) {
            case IDLE: {
                // Searching for an header
                if (pivotChar == COMMPROTOCOL_PTM_SLAVE_HEADER) {
                    incomingBuffer.setLength(0);
                    incomingBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
                    rxStatus = rxStatuses.HEADER_FOUND;
                }
            }
            break;
                
            case HEADER_FOUND: {
                // We expect a compatible protocol version                                    
                if (pivotChar == getProtocolVersion()) {
                    rxStatus = rxStatuses.VERSION_FOUND;
                } else if (pivotChar == COMMPROTOCOL_PTM_SLAVE_HEADER) {
                   // ... but we found an header again... so I should stay on this state.
                   // This is obviously a communication error, but we prefere to parse the incoming
                   // packet instead of discard it by entering inthe IDLE status.
                    rxStatus = rxStatuses.HEADER_FOUND;
                } else {
                    // ... but we found something we cannot handle...
                    rxStatus = rxStatuses.IDLE;
                }
            }   
            break;
                
            case VERSION_FOUND: {
                // We expect an hex digit, msb of boardId
                if (isValidDigit(pivotChar)) {
                    // Here we expect an epmty buffer
                    boardIdBuffer[0] = pivotChar;
                    rxStatus = rxStatuses.BOARDID1_FOUND;
                } else {
                    rxStatus = rxStatuses.IDLE;
                }
            }
            break;
                
            case BOARDID1_FOUND: {
                // We expect an hex digit, lsb of boardId
                if (isValidDigit(pivotChar)) {
                    
                    boardIdBuffer[1] = pivotChar;
                    Integer id = CodecHelper.decodeChars(boardIdBuffer);
                    if (id != null) {
                        incomingBoardId = id;
                        rxStatus = rxStatuses.BOARDID_FOUND;
                    } else {
                        rxStatus = rxStatuses.IDLE;
                    }
                } else {
                    rxStatus = rxStatuses.IDLE;
                }
            }
            break;
                
            case BOARDID_FOUND: {
                // Searching for a trailer
                if (pivotChar == COMMPROTOCOL_PTM_SLAVE_TRAILER) {
                    
                    // End of frame found. Signal to the sender engine that the bus is not busy anymore
                    semaphore.drainPermits();
                    semaphore.release();
                    
                    // Generate a datamessage with incoming string
                    rxStatus = rxStatuses.IDLE;
                    return new AppDataMessage(incomingBoardId, incomingBuffer.toString());
                    
                } else if (pivotChar == COMMPROTOCOL_PTM_SLAVE_HEADER) {
                    // ... We found an header again... discard all
                    rxStatus = rxStatuses.IDLE;
                } else {
                    
                    // Handle buffer overflows
                    if (incomingBuffer.length() == COMMPROTOCOL_BUFFER_LENGTH) {
                        rxStatus = rxStatuses.IDLE;
                    } else {
                    
                        // Collecting payload
                        incomingBuffer.append(pivotChar);
                    }
                }
            }
            break;
        }
        
        return null;
    }

    // From TransportLogic
    @Override
    public void onDataReceived(CommChannelDataMessage message) throws InterruptedException {
        throw new UnsupportedOperationException("onDataReceived it's not supposed to be available on TransportLogicPointToMultipoint");
    }

    // From TransportLogic
    @Override
    public CommChannelDataMessage toSerialBusFormat(AppDataMessage dataMessage) {
        
        // Before sending a new packet we must ensure that we're not 
        // waiting for an answer. The timeout prevents deadlocks if no answer
        // has been received from the other side of the bus
        try {
            boolean acquired = semaphore.tryAcquire(300, TimeUnit.MILLISECONDS);
            if (!acquired) {
                // Timeout occurred. This means there is nobody able to answer
                // to the previous packet
                // TBD: Notify parent or log this event
                SensorBus parent = getParent();
            }
        } catch (InterruptedException ex) {
        }
        
        // Generate the output string
        outcomingBuffer.setLength(0);
        outcomingBuffer.append(COMMPROTOCOL_PTM_HOST_HEADER);
        outcomingBuffer.append(getProtocolVersion());
        outcomingBuffer.append(CodecHelper.encodeValue((char)dataMessage.getBoardId()));
        outcomingBuffer.append(dataMessage.getCommandString());
        outcomingBuffer.append(COMMPROTOCOL_PTM_HOST_TRAILER);
        
        return new CommChannelDataMessage(dataMessage.getBoardId(), outcomingBuffer.toString());
    }

    @Override
    // From TransportLogicBaseImpl
    protected char getProtocolVersion() {
        return COMMPROTOCOL_PTM_VERSION;
    }
    
    private boolean isValidDigit(char pivot) {
        return (((pivot >= 'A') && (pivot <= 'F')) || 
                ((pivot >= '0') && (pivot <= '9')) ||
                ((pivot >= 'a') && (pivot <= 'f')) );
    }
    
}

/**
 * Point to Multi-point protocol basics:
 * 
 * From Host to Slaves:
 * [VXXPayload]
 * 
 * Where:
 * - The character "[" identifies the start of host frame
 * - V is a byte identifying the protocol version
 * - XX is the target board Id in hex format (0xFF is reserved)
 * - Payload starts at the 5th byte and reflects the contents of dataMessage command string
 * - The character "]" identifies the end of host frame
 * 
 * From Slaves to Host:
 * (VXXPayload)
 * 
 * Where:
 * - The character "(" identifies the start of slave frame
 * - V is a byte identifying the protocol version
 * - XX is the originating slave Id in hex format (0xFF is reserved)
 * - Payload starts at the 5th byte and reflects the contents of slave answer string
 * - The character ")" identifies the end of host frame
 * 
 * NOTES: 
 * 1: frames sent by the host are "echoed" back by the single wire bus so they should be skipped by the rx engine
 * 2: single wire bus is half-duplex by design and, for this reason, the host should wait for slave answer (or timeout) before transmitting next frame
 * 3: there is not any error detection/correction procedure implemented, so, transmitted and/or received frames can be lost
 */
