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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author marco
 */
public class CommProtocolHelper {

    public static final String COMMPROTOCOL_HEADER = "{";
    public static final String COMMPROTOCOL_TRAILER = "}";
    public static final int COMMPROTOCOL_BUFFER_LENGTH = 32;
    
    public static final String COMMPROTOCOL_SENSOR_INQUIRY = "I";
    public static final String COMMPROTOCOL_ECHO = "E";
    public static final String COMMPROTOCOL_SAMPLE_ENABLE = "S";
    public static final String COMMPROTOCOL_SAMPLE_DISABLE = "X";
    public static final String COMMPROTOCOL_SET_SAMPLEPRESC = "P";
    public static final String COMMPROTOCOL_GET_SAMPLEPRESC = "Q";
    public static final String COMMPROTOCOL_SET_SAMPLEPOSTS = "O";
    public static final String COMMPROTOCOL_GET_SAMPLEPOSTS = "N";
    public static final String COMMPROTOCOL_SET_SAMPLEDECIM = "D";
    public static final String COMMPROTOCOL_GET_SAMPLEDECIM = "F";
    public static final String COMMPROTOCOL_SET_IIRDENOMVALUES = "A";
    public static final String COMMPROTOCOL_GET_IIRDENOMVALUES = "B";
    public static final String COMMPROTOCOL_LASTSAMPLE = "G";
    public static final String COMMPROTOCOL_FREEMEMORY = "M";
    public static final String COMMPROTOCOL_LOADPRESET = "L";
    public static final String COMMPROTOCOL_SAVEPRESET = "W";
    public static final String COMMPROTOCOL_WRITE_AFE_REG = "R";
    public static final String COMMPROTOCOL_READ_AFE_REG = "T";
    public static final String COMMPROTOCOL_WRITE_DAC_REG = "C";
    public static final String COMMPROTOCOL_READ_DAC_REG = "H";
    
    
    public static class DataMessage {
        
        private String commandString = "";
        private String commandComment = "";
        
        private DataMessage() {
        }
        
        public DataMessage(String command) {
            this.commandString = command;
        }
        
        public DataMessage(String command, String comment) {   
            this.commandString = command;
            this.commandComment = comment;
        }
        
        public DataMessage clone() {
            return new DataMessage(commandString, commandComment);
        }
        
        public String getCommandComment() {
            return commandComment;
        }

        public String getCommandString() {
            return commandString;
        }
        
        public boolean matches(String commandId) {
            if (commandString.length() > 2) {
                return (commandId.compareTo(commandString.substring(1, 2)) == 0);
            } 
            return false;
        }
        
        // Convert a possible "request" command string to an "answer" command string
        // This is useful for reading configuration file items in the 
        // host configuration panel
        public boolean toAnswerCommandString() {
            if (commandString.length() > 2) {
                
                String commandId = commandString.substring(1, 2);
                String answerId = fromRequestToAnswerIdentifiers.get(commandId);
                if (answerId != null) {
                    commandString = commandString.replaceFirst(commandId, answerId);
                    return true;
                }
            }
            
            return false;
        }
    }
    
    private enum rxStatuses {
        IDLE, HEADER_FOUND
    }
    
    private final List<DataMessage> toBoard = new ArrayList<>();
    private final LinkedList<DataMessage> fromBoard = new LinkedList<>();
    private String incomingBuffer = "";
    private rxStatuses rxStatus = rxStatuses.IDLE;
    private static final Map<String,String> fromRequestToAnswerIdentifiers = new HashMap() {{
        put(COMMPROTOCOL_SET_SAMPLEPRESC, COMMPROTOCOL_GET_SAMPLEPRESC);
        put(COMMPROTOCOL_SET_SAMPLEPOSTS, COMMPROTOCOL_GET_SAMPLEPOSTS);
        put(COMMPROTOCOL_SET_SAMPLEDECIM, COMMPROTOCOL_GET_SAMPLEDECIM);
        put(COMMPROTOCOL_SET_IIRDENOMVALUES, COMMPROTOCOL_GET_IIRDENOMVALUES);
        put(COMMPROTOCOL_WRITE_AFE_REG, COMMPROTOCOL_READ_AFE_REG);
        put(COMMPROTOCOL_WRITE_DAC_REG, COMMPROTOCOL_READ_DAC_REG);
        put(COMMPROTOCOL_SAVEPRESET, COMMPROTOCOL_SENSOR_INQUIRY);
    }};
    
    private static final CommProtocolHelper singleton = new CommProtocolHelper();
    
    private CommProtocolHelper() {
    }
    
    public static CommProtocolHelper instance() {
        return singleton;
    }
    
    public synchronized List<DataMessage> getCurrentCommandList() {
        return toBoard;
    }
    
    public synchronized void clearCurrentCommandList() {
        toBoard.clear();
    }
    
    public synchronized List<DataMessage> getSafeCurrentCommandList() {
        
        List<DataMessage> safeList = new ArrayList<>();
        
        for (DataMessage dataMessage:toBoard) {
            
            safeList.add(dataMessage.clone());
        }
        
        return safeList;
    }
    
    public synchronized void renderLMP9100RegSetup(int channelId, int tiaReg, int refReg, int modeReg) {
        
        StringBuilder sb = new StringBuilder(beginCommandString(COMMPROTOCOL_WRITE_AFE_REG));
        sb.append(encodeValue((char)channelId));
        sb.append(encodeValue((char)tiaReg));
        sb.append(encodeValue((char)refReg));
        sb.append(encodeValue((char)modeReg));
        sb.append(terminateCommandString());
        
        toBoard.add(new DataMessage(sb.toString(), "LMP9100 Register Setup for channel " + channelId));
    }
    
    public synchronized void renderSavePresetWithName(int channelId, String name) {
        StringBuilder sb = new StringBuilder(beginCommandString(COMMPROTOCOL_SAVEPRESET));
        sb.append(encodeValue((char)channelId));
        sb.append(encodeString(name));
        sb.append(terminateCommandString());
        
        toBoard.add(new DataMessage(sb.toString(), "Write Preset Name for channel " + channelId));
    }
    
    public synchronized void renderLMP9100ReadSetup(int channelId) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_READ_AFE_REG), 
                                                    "LMP9100 Read configuration registers for channel " + channelId));
    }
    
    public synchronized void renderDAC5694RegSetup(int channelId, int subChannel, int value, int gain) {
        StringBuilder sb = new StringBuilder(beginCommandString(COMMPROTOCOL_WRITE_DAC_REG));
        sb.append(encodeValue((char)channelId));
        sb.append(encodeValue((char)subChannel));
        sb.append(encodeValue((short)value));
        sb.append(encodeValue((char)gain));
        sb.append(terminateCommandString());
        
        toBoard.add(new DataMessage(sb.toString(), "DAC5694R Register Setup for channel " + channelId + " subchannel " + subChannel));
    }
    
    public synchronized void renderDAC5694ReadSetup(int channelId, char subChannel) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_READ_DAC_REG, subChannel),
                                                    "DAC5694R Read configuration registers for channel " + channelId + " subChannel "+ subChannel));
    }
    
    public synchronized void renderSamplerPrescaler(int channelId, int prescaler) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_SET_SAMPLEPRESC, (char)prescaler), 
                                                        "Set Prescaler for channel " + channelId));
    }
    
    public synchronized void renderSamplerPrescalerRead(int channelId) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_GET_SAMPLEPRESC),
                                                        "Read Prescaler for channel " + channelId));
    }
    
    public synchronized void renderSamplerPostscaler(int channelId, int postscaler) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_SET_SAMPLEPOSTS, (char)postscaler), 
                                                        "Set Postscaler for channel " + channelId));
    }
    
    public synchronized void renderSamplerPostscalerRead(int channelId) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_GET_SAMPLEPOSTS),
                                                        "Read Postscaler for channel  " + channelId));
    }
    
    public synchronized void renderSamplerDecimation(int channelId, int decimation) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_SET_SAMPLEDECIM, (char)decimation), 
                                                        "Set Decimation for channel " + channelId));
    }
    
    public synchronized void renderSamplerDecimationRead(int channelId) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_GET_SAMPLEDECIM),
                                                        "Read Decimation for channel " + channelId));
    }
    
    public synchronized void renderSamplerIIRDenom(int channelID, int denom1, int denom2) {
        
        List<Integer> parameters = new ArrayList<>();
        parameters.add(denom1);
        parameters.add(denom2);
        
        toBoard.add(new DataMessage(renderChannelCmd(channelID, COMMPROTOCOL_SET_IIRDENOMVALUES, parameters),
                                                        "Set IIR Parameters for channel " + channelID));
    }
    
    public synchronized void renderSamplerIIRDenomRead(int channelId) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_GET_IIRDENOMVALUES),
                                                        "Read IIR parameters for channel " + channelId));
    }
    
    public synchronized void renderStartSample() {
        toBoard.add(new DataMessage(renderGenericCmd(COMMPROTOCOL_SAMPLE_ENABLE), "Start Sampling"));
    }
    
    public synchronized void renderStopSample() {
        toBoard.add(new DataMessage(renderGenericCmd(COMMPROTOCOL_SAMPLE_DISABLE), "Stop Sampling"));
    }
    
    public synchronized void renderGetFreeMemory() {
        toBoard.add(new DataMessage(renderGenericCmd(COMMPROTOCOL_FREEMEMORY), "Get Free Memory"));
    }
    
    public synchronized void renderSensorInquiry(int channelId) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_SENSOR_INQUIRY), "Inquiry sensor channel " + channelId));
    }
    
    public synchronized void renderGetLastSample(int channelId) {
        toBoard.add(new DataMessage(renderChannelCmd(channelId, COMMPROTOCOL_LASTSAMPLE), "Get Last Sample for channel " + channelId));
    }
    
    public synchronized void renderRawData(DataMessage dataMessage) {
        toBoard.add(dataMessage);
    }
    
    // This is the main rx state machine. It aggregates all incoming
    // data and populates the fromBoard list as a FIFO buffer containing all the
    // received messages
    synchronized public boolean onRxCharReceived(byte value) {
        
        String pivotChar = String.valueOf((char)value);
        switch(rxStatus) {
            case IDLE: {
                // Searching for an header
                if (pivotChar.compareToIgnoreCase(COMMPROTOCOL_HEADER) == 0) {
                    incomingBuffer = COMMPROTOCOL_HEADER;
                    rxStatus = rxStatuses.HEADER_FOUND;
                }
            }
            break;
                
            case HEADER_FOUND: {
                // Searching for a trailer
                if (pivotChar.compareToIgnoreCase(COMMPROTOCOL_TRAILER) == 0) {
                    
                    // Found. Generate a new DataMessage and place it into the FIFO
                    DataMessage rxPacket = new DataMessage(incomingBuffer + pivotChar);
                    fromBoard.add(rxPacket);
                    return true;
                    
                } else if (pivotChar.compareToIgnoreCase(COMMPROTOCOL_HEADER) == 0) {
                   // ... but we found an header again...
                   // Discard all.
                   incomingBuffer = COMMPROTOCOL_HEADER;
                } else if (incomingBuffer.length() == COMMPROTOCOL_BUFFER_LENGTH) {
                   // We did not found any trailer and
                   // the buffer is empty. Discard all
                   rxStatus = rxStatuses.IDLE;
                } else {
                    
                    // Collecting payload
                    incomingBuffer = incomingBuffer + pivotChar;
                }
            }
            break;
        }
        
        return false;
    }
    
    // Returns a datamessage from the rx queue, if any.
    // otherwise returns null.
    synchronized public DataMessage getNextRxDataMessage() {
        
        if (fromBoard.size() == 0) {
            return null;
        }
        
        return fromBoard.removeFirst();
    }

    // Returns an integer if the rxMessage matches the required command; null otherwise
    public Integer evalFreeMemory(DataMessage rxMessage) {
        
        if (!rxMessage.matches(COMMPROTOCOL_FREEMEMORY)) {
            return null;
        }
        
        return decodeShortAt(rxMessage.getCommandString(), 2);
    }
    
    // Returns an integer identifying the prescaler on the selected channel if the rxMessage matches the required command and channel
    public Integer evalPrescalerInquiry(DataMessage rxMessage, int channel) {
        
        if (!rxMessage.matches(COMMPROTOCOL_GET_SAMPLEPRESC)) {
            return null;
        }
        
        Integer rxChan = decodeCharAt(rxMessage.getCommandString(), 2);
        if ((rxChan == null) || (rxChan.intValue() != channel)) {
            return null;
        }
        
        return decodeCharAt(rxMessage.getCommandString(), 4);
    }
    
    // Returns an integer identifying the postscaler on the selected channel if the rxMessage matches the required command and channel
    public Integer evalPostScalerInquiry(DataMessage rxMessage, int channel) {
        
        if (!rxMessage.matches(COMMPROTOCOL_GET_SAMPLEPOSTS)) {
            return null;
        }
        
        Integer rxChan = decodeCharAt(rxMessage.getCommandString(), 2);
        if ((rxChan == null) || (rxChan.intValue() != channel)) {
            return null;
        }
        
        return decodeCharAt(rxMessage.getCommandString(), 4);
    }
    
    // Returns an integer identifying the decimation on the selected channel if the rxMessage matches the required command and channel
    public Integer evalDecimationInquiry(DataMessage rxMessage, int channel) {
        
        if (!rxMessage.matches(COMMPROTOCOL_GET_SAMPLEDECIM)) {
            return null;
        }
        
        Integer rxChan = decodeCharAt(rxMessage.getCommandString(), 2);
        if ((rxChan == null) || (rxChan.intValue() != channel)) {
            return null;
        }
        
        return decodeCharAt(rxMessage.getCommandString(), 4);
    }
    
    // Returns a list of two IIR parameters if the rxMessage matches the required command and channel
    public List<Integer> evalIIRDenomInquiry(DataMessage rxMessage, int channel) {
        
        if (!rxMessage.matches(COMMPROTOCOL_GET_IIRDENOMVALUES)) {
            return null;
        }

        Integer rxChan = decodeCharAt(rxMessage.getCommandString(), 2);
        if ((rxChan == null) || (rxChan.intValue() != channel)) {
            return null;
        }

        String rxMsg = rxMessage.getCommandString();
        Integer denom1 = decodeCharAt(rxMsg, 4);
        Integer denom2 = decodeCharAt(rxMsg, 6);
        
        if ((denom1 == null) || (denom2 == null))
            return null;
            
        List<Integer> result = new ArrayList<>();
        result.add(denom1);
        result.add(denom2);
        
        return result;
    }
    
    // Returns a string containing the name of the sensor preset, if the rxMessage matches the required command and channel
    public String evalSensorInquiry(DataMessage rxMessage, int channel) {
        
        if (!rxMessage.matches(COMMPROTOCOL_SENSOR_INQUIRY)) {
            return null;
        }
        
        Integer rxChan = decodeCharAt(rxMessage.getCommandString(), 2);
        if ((rxChan == null) || (rxChan.intValue() != channel)) {
            return null;
        }
        
        String sensorName = decodeStringAt(rxMessage.getCommandString(), 4);
        return sensorName;
    } 
    
    // Returns a list of integers containing the tia, ref and mode values if the rxMessage matches the command and channel
    public List<Integer> evalAFERegistersInquiry(DataMessage rxMessage, int channel) {

        if (!rxMessage.matches(COMMPROTOCOL_READ_AFE_REG)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = decodeCharAt(commandString, 2);
        if ((rxChan == null) || (rxChan.intValue() != channel)) {
            return null;
        }
        
        Integer tia = decodeCharAt(commandString, 4);
        Integer ref = decodeCharAt(commandString, 6);
        Integer mode = decodeCharAt(commandString, 8);
        
        List<Integer> result = new ArrayList<>();
        if ((tia != null) && (ref != null) && (mode != null)) {
            result.add(tia);
            result.add(ref);
            result.add(mode);
        }

        return result;
    }
    
    // Returns a list of integers containing the DAC sunbhannel, channel value, gain
    public List<Integer> evalDACRegistersInquiry(DataMessage rxMessage, int channel) {
        
        if (!rxMessage.matches(COMMPROTOCOL_READ_DAC_REG)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = decodeCharAt(commandString, 2);
        if ((rxChan == null) || (rxChan.intValue() != channel)) {
            return null;
        }

        Integer rxSubChan = decodeCharAt(commandString, 4);
        Integer rxVal = decodeShortAt(commandString, 6);
        Integer rxGain = decodeCharAt(commandString, 10);
        
        List<Integer> result = new ArrayList<>();
        if ((rxSubChan != null) && (rxVal != null) && (rxGain != null)) {
            result.add(rxSubChan);
            result.add(rxVal);
            result.add(rxGain);
        }
        
        return result;
    }
    
    public List<Integer> evalLastSampleInquiry(DataMessage rxMessage, int channel) {
        
        if (!rxMessage.matches(COMMPROTOCOL_LASTSAMPLE)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = decodeCharAt(commandString, 2);
        if ((rxChan == null) || (rxChan.intValue() != channel)) {
            return null;
        }
        
        Integer sample = decodeShortAt(commandString, 4);
        if (sample == null)
            return null;
                
        Integer timestamp = decodeIntAt(commandString, 8);
        if (timestamp == null)
            return null;
        
        List<Integer> result = new ArrayList<>();
        result.add(sample);
        result.add(timestamp);
        
        return result;
    }
       
    private String renderGenericCmd(String command) {
        
        StringBuilder sb = new StringBuilder(beginCommandString(command));
        sb.append(terminateCommandString());
        
        return sb.toString();
    }
    
    private String renderChannelCmd(int channelId, String command) {
        
        StringBuilder sb = new StringBuilder(beginCommandString(command));
        sb.append(encodeValue((char)channelId));
        sb.append(terminateCommandString());
        
        return sb.toString();
    }
    
    private String renderChannelCmd(int channelId, String command, char parameter) {
        
        StringBuilder sb = new StringBuilder(beginCommandString(command));
        sb.append(encodeValue((char)channelId));
        sb.append(encodeValue(parameter));
        sb.append(terminateCommandString());
        
        return sb.toString();
    }
    
    private String renderChannelCmd(int channelId, String command, List<Integer> parameters) {

        StringBuilder sb = new StringBuilder(beginCommandString(command));
        sb.append(encodeValue((char)channelId));
        for (int n = 0; n < parameters.size(); n++) {
            sb.append(encodeValue((char)parameters.get(n).intValue()));
        }
        sb.append(terminateCommandString());
        
        return sb.toString();
    }
    
    private String encodeValue(char value) {
        
        String result = String.format("%02X", (byte)value);
        return result;
    }
    
    private String encodeValue(short value) {
        String result = String.format("%04X", (short)value);
        return result;
    }
    
    private String encodeString(String value) {
        
        StringBuilder sb = new StringBuilder();
        
        for (int n = 0; n < value.length(); n++) {
            sb.append(encodeValue(value.charAt(n)));
        }
        
        sb.append(encodeValue((char)0x00));
        
        return sb.toString();
    }
    
    private Integer decodeCharAt(String buffer, int start) {
        
        try {
            String val = buffer.substring(start, start+2);
            return Integer.valueOf(val, 16);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }
    
    private Integer decodeShortAt(String buffer, int start) {
        
        try {
            String val = buffer.substring(start, start+4);
            return Integer.valueOf(val, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Integer decodeIntAt(String buffer, int start) {
        try {
            String val = buffer.substring(start, start+8);
            return Integer.valueOf(val, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private String decodeStringAt(String buffer, int start) {
        
        Integer rxChar;
        StringBuilder sb = new StringBuilder();
        do {
            rxChar = decodeCharAt(buffer, start);
            if (rxChar != null) {
                sb.append((char)rxChar.byteValue());
            }
            start = start+2;
        } while ((rxChar != null) && (rxChar != '\0') && (start < buffer.length()));
        
        return sb.toString();
    }
    
    private String beginCommandString(String commandId) {
        return "{" + commandId;
    }
    
    private String terminateCommandString() {
        return "}";
    }
}
