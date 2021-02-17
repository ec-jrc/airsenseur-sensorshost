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
    
    private final static char COMMPROTOCOL_PTM_VERSION_ZERO = '0';
    private final static char COMMPROTOCOL_PTM_VERSION_ONE = '1';
    
    private final static char COMMPROTOCOL_PTM_SLAVE_HEADER = '(';
    private final static char COMMPROTOCOL_PTM_SLAVE_TRAILER = ')';
    
    private static final int COMMPROTOCOL_BUFFER_LENGTH = 256;
    
    private static final int DEFAULT_CRC_INITVALUE = 0xffffffff;
    private static final int DEFAULT_CRC_POLYNOMIAL = 0x04C11DB7;

    private enum rxStatuses {
        IDLE, HEADER_FOUND, VERSION_FOUND, BOARDID1_FOUND, BOARDID_FOUND,
    }
    
    private rxStatuses rxStatus = rxStatuses.IDLE;
    private int incomingBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
    
    private final StringBuilder incomingBuffer = new StringBuilder(COMMPROTOCOL_BUFFER_LENGTH);
    private final StringBuilder outcomingBuffer = new StringBuilder(COMMPROTOCOL_BUFFER_LENGTH);
    private final char[] boardIdBuffer = { 0x00, 0x00 };
    private final boolean useCRCWhenAvailable;
    private final Semaphore semaphore = new Semaphore(1);

    public TransportLogicPointToMultipoint(AppDataMessageQueue rxDataQueue, AppDataMessageQueue txDataQueue, SensorBus parent, CommChannel commChannel, boolean useCRCWhenAvailable) {
        super(rxDataQueue, txDataQueue, parent, commChannel);
        
        this.useCRCWhenAvailable = useCRCWhenAvailable;
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
                    Long id = CodecHelper.decodeChars(boardIdBuffer);
                    if (id != null) {
                        incomingBoardId = id.intValue();
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
                    
                    // End of frame found. Evaluate it.
                    boolean packetValid = true;
                    String incomingMessage = incomingBuffer.toString();
                    
                    // Check for CRC-32 on incoming packet to validate the command
                    if (useCRCWhenAvailable) {
                        
                        // CRC-32 is on the last 8 bytes. Evaluated it
                        String strRxCRC = incomingMessage.substring(incomingMessage.length()-8);
                        incomingMessage = incomingMessage.substring(0, incomingMessage.length()-8);                        
                        packetValid = checkCRC(incomingMessage, strRxCRC);
                    }
                    
                    if (packetValid) {
                        
                        // Signal to the sender engine that the bus is not busy anymore
                        semaphore.drainPermits();
                        semaphore.release();

                        // Generate a datamessage with incoming string
                        rxStatus = rxStatuses.IDLE;
                        return new AppDataMessage(incomingBoardId, incomingMessage);
                    }
                    
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
        return (useCRCWhenAvailable)? COMMPROTOCOL_PTM_VERSION_ONE : COMMPROTOCOL_PTM_VERSION_ZERO;
    }
    
    private boolean isValidDigit(char pivot) {
        return (((pivot >= 'A') && (pivot <= 'F')) || 
                ((pivot >= '0') && (pivot <= '9')) ||
                ((pivot >= 'a') && (pivot <= 'f')) );
    }
    
    private boolean checkCRC(String buffer, String rxCRC) {
        
        // Calculate the CRC for the incoming string
        byte bytes[] = buffer.getBytes();
        int countCRC = DEFAULT_CRC_INITVALUE;

        for(int j=0; j<bytes.length; j++) {
            
            countCRC ^= bytes[j] << 24;
            for (byte i = 0; i < 8; ++i)
            {
                if ( (countCRC & 0x80000000) != 0) {
                    
                    countCRC = (countCRC << 1) ^ DEFAULT_CRC_POLYNOMIAL;
                } else {
                    
                    countCRC <<= 1;
                }
            }
        }

        // Decode the CRC from ASCII format
        Long lRxCRC = CodecHelper.decodeLongAt(rxCRC, 0);
        
        // Compare and return
        return lRxCRC.intValue() == countCRC;
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
 * - V is a byte identifying the protocol version (0 or 1)
 * - XX is the originating slave Id in hex format (0xFF is reserved)
 * - Payload starts at the 5th byte and reflects the contents of slave answer string
 * - The character ")" identifies the end of host frame
 * 
 * NOTES: 
 * 1: frames sent by the host are "echoed" back by the single wire bus so they should be skipped by the rx engine
 * 2: single wire bus is half-duplex by design and, for this reason, the host should wait for slave answer (or timeout) before transmitting next frame
 * 3: there is not any error detection/correction procedure implemented, so, transmitted and/or received frames can be lost
 * 
 * Addendum to Protocol Version 1
 * A CRC has been added to the slave to master only packets. The CRC is calculated as CRC-32 format
 * and is appended to the payload, before the frame termination character.
 * The CRC-32 is ASCII encoded and is 8 bytes length. It's calculated from the bytes in the payload only. 
 * 
 * Examples:
 * [101G00](101G0000000000000086D06DD0)
 * [101G01](101G010000000000006E743BD5)
 * [101G02](101G020000000000005359DC6D)
 *  ^^^^^^  ^^^^^^^^^^^^^^^^^^^^^^^^^^
 *  ||||||  ||||||||||||||||||||||||||
 *  ||||||  ||||||||||||||||||++++++++- CRC-32 (from the payload bytes)
 *  ||||||  |||+++++++++++++++- Payload (answer)
 *  ||||||  |++- Board ID (answer)
 *  ||||||  +- Protocol version (answer)
 *  |||+++- Request payload
 *  |++- Board ID
 *  +- Protocol version 1
 */
