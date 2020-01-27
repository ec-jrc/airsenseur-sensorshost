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
import airsenseur.dev.helpers.CodecHelper;
import airsenseur.dev.helpers.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic Communication Protocol Helper hiding implementation details about 
 * peer to peer or bus communication between host and shields
 * @author marco
 */
public class ShieldProtocolLayer {
    
    public static final int COMMPROTOCOL_COMMANDID_LENGTH = 1;
    public static final int COMMPROTOCOL_CHANNEL_LENGTH = 2;
    public static final int COMMPROTOCOL_COMMAND_CHANNEL_PATTERNLENGTH = COMMPROTOCOL_COMMANDID_LENGTH + COMMPROTOCOL_CHANNEL_LENGTH;
    
    
    private static final char COMMPROTOCOL_SENSOR_INQUIRY = 'I';
    private static final char COMMPROTOCOL_ECHO = 'E';
    private static final char COMMPROTOCOL_SAMPLE_ENABLE = 'S';
    private static final char COMMPROTOCOL_SAMPLE_DISABLE = 'X';
    private static final char COMMPROTOCOL_SET_SAMPLEPRESC = 'P';
    private static final char COMMPROTOCOL_GET_SAMPLEPRESC = 'Q';
    private static final char COMMPROTOCOL_SET_SAMPLEPOSTS = 'O';
    private static final char COMMPROTOCOL_GET_SAMPLEPOSTS = 'N';
    private static final char COMMPROTOCOL_SET_SAMPLEDECIM = 'D';
    private static final char COMMPROTOCOL_GET_SAMPLEDECIM = 'F';
    private static final char COMMPROTOCOL_SET_IIRDENOMVALUES = 'A';
    private static final char COMMPROTOCOL_GET_IIRDENOMVALUES = 'B';
    private static final char COMMPROTOCOL_LASTSAMPLE = 'G';
    private static final char COMMPROTOCOL_LASTSAMPLE_HRES = 'Y';
    private static final char COMMPROTOCOL_FREEMEMORY = 'M';
    private static final char COMMPROTOCOL_LOADPRESET = 'L';
    private static final char COMMPROTOCOL_SAVEPRESET = 'W';
    private static final char COMMPROTOCOL_WRITE_AFE_REG = 'R';
    private static final char COMMPROTOCOL_READ_AFE_REG = 'T';
    private static final char COMMPROTOCOL_WRITE_DAC_REG = 'C';
    private static final char COMMPROTOCOL_READ_DAC_REG = 'H';
    private static final char COMMPROTOCOL_WRITE_SSERIAL = 'J';
    private static final char COMMPROTOCOL_READ_SSERIAL = 'K';
    private static final char COMMPROTOCOL_WRITE_BOARDSERIAL = 'U';
    private static final char COMMPROTOCOL_READ_BOARDSERIAL = 'V';
    private static final char COMMPROTOCOL_READ_FWVERSION = 'Z';
    private static final char COMMPROTOCOL_READ_SAMPLEPERIOD = 'a';
    private static final char COMMPROTOCOL_READ_UNITS = 'b';
    private static final char COMMPROTOCOL_READ_BOARDTYPE = 'c';
    private static final char COMMPROTOCOL_WRITE_CHANENABLE = 'd';
    private static final char COMMPROTOCOL_READ_CHANENABLE = 'e';
    
    private static final Map<String,String> fromRequestToAnswerIdentifiers = new HashMap<String, String>() {{
        put(String.valueOf(COMMPROTOCOL_SET_SAMPLEPRESC), String.valueOf(COMMPROTOCOL_GET_SAMPLEPRESC));
        put(String.valueOf(COMMPROTOCOL_SET_SAMPLEPOSTS), String.valueOf(COMMPROTOCOL_GET_SAMPLEPOSTS));
        put(String.valueOf(COMMPROTOCOL_SET_SAMPLEDECIM), String.valueOf(COMMPROTOCOL_GET_SAMPLEDECIM));
        put(String.valueOf(COMMPROTOCOL_SET_IIRDENOMVALUES), String.valueOf(COMMPROTOCOL_GET_IIRDENOMVALUES));
        put(String.valueOf(COMMPROTOCOL_WRITE_AFE_REG), String.valueOf(COMMPROTOCOL_READ_AFE_REG));
        put(String.valueOf(COMMPROTOCOL_WRITE_DAC_REG), String.valueOf(COMMPROTOCOL_READ_DAC_REG));
        put(String.valueOf(COMMPROTOCOL_SAVEPRESET), String.valueOf(COMMPROTOCOL_SENSOR_INQUIRY));
        put(String.valueOf(COMMPROTOCOL_WRITE_SSERIAL), String.valueOf(COMMPROTOCOL_READ_SSERIAL));
        put(String.valueOf(COMMPROTOCOL_WRITE_BOARDSERIAL), String.valueOf(COMMPROTOCOL_READ_BOARDSERIAL)); 
        put(String.valueOf(COMMPROTOCOL_WRITE_CHANENABLE), String.valueOf(COMMPROTOCOL_READ_CHANENABLE));
    }};
    
    private static final List<String> boardTypesString = new ArrayList<String>() {{
        add("Unknown");
        add("Chemical Shield R3.x"); 
        add("Exp1Shield R1.x");
        add("HostBoard R2.x");
    }};
    
    private final SensorBus sensorBus;
    
    public ShieldProtocolLayer(SensorBus sensorBus) {
        this.sensorBus = sensorBus;
    }
    
    public void renderLMP9100RegSetup(int boardId, int channelId, int tiaReg, int refReg, int modeReg) throws SensorBusException {
        
        StringBuilder sb = new StringBuilder(10);
        sb.append(COMMPROTOCOL_WRITE_AFE_REG);
        sb.append(CodecHelper.encodeValue((char)channelId));
        sb.append(CodecHelper.encodeValue((char)tiaReg));
        sb.append(CodecHelper.encodeValue((char)refReg));
        sb.append(CodecHelper.encodeValue((char)modeReg));
        
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, sb.toString(), "LMP9100 Register Setup for channel " + channelId));
    }
    
    public void renderSavePresetWithName(int boardId, int channelId, String name) throws SensorBusException {
        StringBuilder sb = new StringBuilder(3+(2*name.length()));
        sb.append(COMMPROTOCOL_SAVEPRESET);
        sb.append(CodecHelper.encodeValue((char)channelId));
        sb.append(CodecHelper.encodeString(name));
        
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, sb.toString(), "Write Preset Name for channel " + channelId));
    }
    
    public void renderLMP9100ReadSetup(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_READ_AFE_REG), 
                                                    "LMP9100 Read configuration registers for channel " + channelId));
    }
    
    public void renderDAC5694RegSetup(int boardId, int channelId, int subChannel, int value, int gain) throws SensorBusException {
        StringBuilder sb = new StringBuilder(12);
        sb.append(COMMPROTOCOL_WRITE_DAC_REG);
        sb.append(CodecHelper.encodeValue((char)channelId));
        sb.append(CodecHelper.encodeValue((char)subChannel));
        sb.append(CodecHelper.encodeValue((short)value));
        sb.append(CodecHelper.encodeValue((char)gain));
        
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, sb.toString(), "DAC5694R Register Setup for channel " + channelId + " subchannel " + subChannel));
    }
    
    public void renderDAC5694ReadSetup(int boardId, int channelId, char subChannel) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_READ_DAC_REG, subChannel),
                                                    "DAC5694R Read configuration registers for channel " + channelId + " subChannel "+ subChannel));
    }
    
    public synchronized void renderSamplerPrescaler(int boardId, int channelId, int prescaler) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_SET_SAMPLEPRESC, (char)prescaler), 
                                                        "Set Prescaler for channel " + channelId));
    }
    
    public void renderSamplerPrescalerRead(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_GET_SAMPLEPRESC),
                                                        "Read Prescaler for channel " + channelId));
    }
    
    public void renderSamplerPostscaler(int boardId, int channelId, int postscaler) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_SET_SAMPLEPOSTS, (char)postscaler), 
                                                        "Set Postscaler for channel " + channelId));
    }
    
    public void renderSamplerPostscalerRead(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_GET_SAMPLEPOSTS),
                                                        "Read Postscaler for channel  " + channelId));
    }
    
    public void renderSamplerDecimation(int boardId, int channelId, int decimation) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_SET_SAMPLEDECIM, (char)decimation), 
                                                        "Set Decimation for channel " + channelId));
    }
    
    public void renderSamplerDecimationRead(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_GET_SAMPLEDECIM),
                                                        "Read Decimation for channel " + channelId));
    }
    
    public void renderSamplerIIRDenom(int boardId, int channelID, int denom1, int denom2) throws SensorBusException {
        
        List<Integer> parameters = new ArrayList<>();
        parameters.add(denom1);
        parameters.add(denom2);
        
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelID, COMMPROTOCOL_SET_IIRDENOMVALUES, parameters),
                                                        "Set IIR Parameters for channel " + channelID));
    }
    
    public void renderSamplerIIRDenomRead(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_GET_IIRDENOMVALUES),
                                                        "Read IIR parameters for channel " + channelId));
    }
    
    public void renderWriteChannelEnable(int boardId, int channelID, boolean enabled) throws SensorBusException {
        
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelID, COMMPROTOCOL_WRITE_CHANENABLE, (char)((enabled)? 1:0)), 
                                                        "Write channel enabled " + channelID));
    }
    
    public void renderReadChannelEnable(int boardId, int channelID) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelID, COMMPROTOCOL_READ_CHANENABLE),
                                                        "Read channel enabled " + channelID));
    }
    
    public void renderStartSample(int boardId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderGenericCmd(COMMPROTOCOL_SAMPLE_ENABLE), "Start Sampling"));
    }
    
    public void renderStopSample(int boardId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderGenericCmd(COMMPROTOCOL_SAMPLE_DISABLE), "Stop Sampling"));
    }
    
    public void renderGetFreeMemory(int boardId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderGenericCmd(COMMPROTOCOL_FREEMEMORY), "Get Free Memory"));
    }
    
    public void renderSensorInquiry(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_SENSOR_INQUIRY), "Inquiry sensor channel " + channelId));
    }
    
    public void renderGetLastSample(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_LASTSAMPLE), "Get Last Sample for channel " + channelId));
    }
    
    public void renderGetLastSampleHRes(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_LASTSAMPLE_HRES), "Get Last HiRes Sample for channel " + channelId));
    }
    
    public void renderSaveSensorSerialNumber(int boardId, int channelId, String serialNumber) throws SensorBusException {
        StringBuilder sb = new StringBuilder(3+(2*serialNumber.length()));
        sb.append(COMMPROTOCOL_WRITE_SSERIAL);
        sb.append(CodecHelper.encodeValue((char)channelId));
        sb.append(CodecHelper.encodeString(serialNumber));
        
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, sb.toString(), "Write Serial Number for channel " + channelId));
    }
    
    public void renderReadSensorSerialNumber(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_READ_SSERIAL), "Read serial number for channel " + channelId));
    }

    public void renderSaveBoardSerialNumber(int boardId, String serialNumber) throws SensorBusException {
        StringBuilder sb = new StringBuilder(3*(2*serialNumber.length()));
        sb.append(COMMPROTOCOL_WRITE_BOARDSERIAL);
        sb.append(CodecHelper.encodeString(serialNumber));
        
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, sb.toString(), "Write board serial number"));
    }
    
    public void renderReadBoardSerialNumber(int boardId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderGenericCmd(COMMPROTOCOL_READ_BOARDSERIAL), "Read board serial number"));
    }
    
    public void renderReadFirmwareVersion(int boardId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderGenericCmd(COMMPROTOCOL_READ_FWVERSION), "Read board firmware version"));
    }
    
    public void renderReadSamplePeriod(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_READ_SAMPLEPERIOD), "Read sample period for channel " + channelId));
    }
    
    public void renderReadUnits(int boardId, int channelId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderChannelCmd(channelId, COMMPROTOCOL_READ_UNITS), "Read measurement units for channel " + channelId));
    }
    
    public void renderReadBoardType(int boardId) throws SensorBusException {
        sensorBus.writeMessageToBus(new AppDataMessage(boardId, renderGenericCmd(COMMPROTOCOL_READ_BOARDTYPE), "Read board type"));
    }
    
    public void renderRawData(AppDataMessage dataMessage) throws SensorBusException {
        sensorBus.writeMessageToBus(dataMessage);
    }
    
    // Convert a possible "request" command string to an "answer" command string
    // This is useful for reading configuration file items in the 
    // host configuration panel
    public boolean toAnswerCommandString(AppDataMessage dataMessage) {
        if (dataMessage.getCommandString().length() > 2) {

            String commandId = dataMessage.getCommandString().substring(0, 1);
            String answerId = fromRequestToAnswerIdentifiers.get(commandId);
            if (answerId != null) {
                String commandString = dataMessage.getCommandString().replaceFirst(commandId, answerId);
                dataMessage.setCommandString(commandString);
                return true;
            }
        }

        return false;
    }
    
    // Convert the board type in a human readable format
    public static String getBoardTypeString(int boardTypeID) {
        if (boardTypeID >= boardTypesString.size()) {
            return "Unknown";
        }
        
        return boardTypesString.get(boardTypeID);
    }
    

    // Returns an integer if the rxMessage matches the required command; null otherwise
    public Integer evalFreeMemory(AppDataMessage rxMessage, int boardId) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_FREEMEMORY)) {
            return null;
        }
        
        return CodecHelper.decodeShortAt(rxMessage.getCommandString(), 1);
    }
    
    // Returns an integer identifying the prescaler on the selected channel if the rxMessage matches the required command and channel
    public Integer evalPrescalerInquiry(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_GET_SAMPLEPRESC)) {
            return null;
        }
        
        Integer rxChan = CodecHelper.decodeCharAt(rxMessage.getCommandString(), 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        return CodecHelper.decodeCharAt(rxMessage.getCommandString(), 3);
    }
    
    // Returns an integer identifying the postscaler on the selected channel if the rxMessage matches the required command and channel
    public Integer evalPostScalerInquiry(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_GET_SAMPLEPOSTS)) {
            return null;
        }
        
        Integer rxChan = CodecHelper.decodeCharAt(rxMessage.getCommandString(), 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        return CodecHelper.decodeCharAt(rxMessage.getCommandString(), 3);
    }
    
    // Returns an integer identifying the decimation on the selected channel if the rxMessage matches the required command and channel
    public Integer evalDecimationInquiry(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_GET_SAMPLEDECIM)) {
            return null;
        }
        
        Integer rxChan = CodecHelper.decodeCharAt(rxMessage.getCommandString(), 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        return CodecHelper.decodeCharAt(rxMessage.getCommandString(), 3);
    }
    
    // Returns a list of two IIR parameters if the rxMessage matches the required command and channel
    public List<Integer> evalIIRDenomInquiry(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_GET_IIRDENOMVALUES)) {
            return null;
        }

        Integer rxChan = CodecHelper.decodeCharAt(rxMessage.getCommandString(), 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }

        String rxMsg = rxMessage.getCommandString();
        Integer denom1 = CodecHelper.decodeCharAt(rxMsg, 3);
        Integer denom2 = CodecHelper.decodeCharAt(rxMsg, 5);
        
        if ((denom1 == null) || (denom2 == null))
            return null;
            
        List<Integer> result = new ArrayList<>();
        result.add(denom1);
        result.add(denom2);
        
        return result;
    }
    
    // Returns a string containing the name of the sensor preset, if the rxMessage matches the required command and channel
    public String evalSensorInquiry(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_SENSOR_INQUIRY)) {
            return null;
        }
        
        Integer rxChan = CodecHelper.decodeCharAt(rxMessage.getCommandString(), 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        String sensorName = CodecHelper.decodeStringAt(rxMessage.getCommandString(), 3);
        return sensorName;
    } 
    
    // Returns a list of integers containing the tia, ref and mode values if the rxMessage matches the command and channel
    public List<Integer> evalAFERegistersInquiry(AppDataMessage rxMessage, int boardId, int channel) {

        if (!rxMessage.matches(boardId, COMMPROTOCOL_READ_AFE_REG)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = CodecHelper.decodeCharAt(commandString, 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        Integer tia = CodecHelper.decodeCharAt(commandString, 3);
        Integer ref = CodecHelper.decodeCharAt(commandString, 5);
        Integer mode = CodecHelper.decodeCharAt(commandString, 7);
        
        List<Integer> result = new ArrayList<>();
        if ((tia != null) && (ref != null) && (mode != null)) {
            result.add(tia);
            result.add(ref);
            result.add(mode);
        }

        return result;
    }
    
    // Returns a list of integers containing the DAC sunbhannel, channel value, gain
    public List<Integer> evalDACRegistersInquiry(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_READ_DAC_REG)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = CodecHelper.decodeCharAt(commandString, 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }

        Integer rxSubChan = CodecHelper.decodeCharAt(commandString, 3);
        Integer rxVal = CodecHelper.decodeShortAt(commandString, 5);
        Integer rxGain = CodecHelper.decodeCharAt(commandString, 9);
        
        List<Integer> result = new ArrayList<>();
        if ((rxSubChan != null) && (rxVal != null) && (rxGain != null)) {
            result.add(rxSubChan);
            result.add(rxVal);
            result.add(rxGain);
        }
        
        return result;
    }
    
    public List<Integer> evalLastSampleInquiry(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_LASTSAMPLE)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = CodecHelper.decodeCharAt(commandString, 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        Integer sample = CodecHelper.decodeShortAt(commandString, 3);
        if (sample == null)
            return null;
                
        Integer timestamp = CodecHelper.decodeIntAt(commandString, 7);
        if (timestamp == null)
            return null;
        
        List<Integer> result = new ArrayList<>();
        result.add(sample);
        result.add(timestamp);
        
        return result;
    }
    
    public Pair<Integer, Float> evalLastSampleHResInquiry(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_LASTSAMPLE_HRES)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = CodecHelper.decodeCharAt(commandString, 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        Float sample = CodecHelper.decodeFloatAt(commandString, 3);
        if (sample == null)
            return null;
        
        Integer timestamp = CodecHelper.decodeIntAt(commandString, 11);
        if (timestamp == null) 
            return null;
        
        Pair<Integer, Float> result = new Pair<>(timestamp, sample);
        
        return result;
    }
    
    // Returns a string if the rxMessage matches the required command; null otherwise
    public String evalReadBoardSerialNumber(AppDataMessage rxMessage, int boardId) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_READ_BOARDSERIAL)) {
            return null;
        }
        
        return CodecHelper.decodeStringAt(rxMessage.getCommandString(), 1);
    }
    
    // Returns a string if the rxMessage matches the required command; null otherwise
    public String evalReadFirmwareVersion(AppDataMessage rxMessage, int boardId) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_READ_FWVERSION)) {
            return null;
        }
        
        return CodecHelper.decodeStringAt(rxMessage.getCommandString(), 1);
    }
    
    
    // Returns a string if the rxMessage matches the required command and channel; null otherwise
    public String evalReadSensorSerialNumber(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_READ_SSERIAL)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = CodecHelper.decodeCharAt(commandString, 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        return CodecHelper.decodeStringAt(rxMessage.getCommandString(), 3);
    }
    
    // Returns an Integer if the rxMessage matches the required command and channel; null otherwise
    public Integer evalReadSamplePeriod(AppDataMessage rxMessage, int boardId, int channel) {
        
        if (!rxMessage.matches(boardId, COMMPROTOCOL_READ_SAMPLEPERIOD)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = CodecHelper.decodeCharAt(commandString, 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        return CodecHelper.decodeIntAt(commandString, 3);
    }
    
    // Returns a string if the rxMessage matches the required command and channel; null otherwise
    public String evalReadUnits(AppDataMessage rxMessage, int boardId, int channel) {

        if (!rxMessage.matches(boardId, COMMPROTOCOL_READ_UNITS)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = CodecHelper.decodeCharAt(commandString, 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        return CodecHelper.decodeStringAt(commandString, 3);
    }
    
    // Returns a list of integer if the rxMessage matched the required command; null otherwise
    // The first integer is the board type ID
    // The second integer is the numnber of channels supported by the board itself
    public List<Integer> evalReadBoardType(AppDataMessage rxMessage, int boardId) {
        if(!rxMessage.matches(boardId, COMMPROTOCOL_READ_BOARDTYPE)) {
            return null;
        }
        
        List<Integer> result = new ArrayList<>();
        String commandString = rxMessage.getCommandString();
        Integer boardType = CodecHelper.decodeShortAt(commandString, 1);
        if (boardType == null) {
            return null;
        }
        result.add(boardType);
        
        Integer numChannels = CodecHelper.decodeShortAt(commandString, 5);
        if (numChannels == null) {
            return null;
        }
        
        result.add(numChannels);
        
        return result;
    }
    
    // Returns a boolean representing the status of specified channel; null if boardId,channel does not match
    public Boolean evalReadChannelEnable(AppDataMessage rxMessage, int boardId, int channel) {
        if (!rxMessage.matches(boardId, COMMPROTOCOL_READ_CHANENABLE)) {
            return null;
        }
        
        String commandString = rxMessage.getCommandString();
        Integer rxChan = CodecHelper.decodeCharAt(commandString, 1);
        if ((rxChan == null) || (rxChan != channel)) {
            return null;
        }
        
        Integer value = CodecHelper.decodeCharAt(commandString, 3);
        if (value == null) {
            return null;
        }
        
        return (value.byteValue() != 0)? Boolean.TRUE : Boolean.FALSE;
    }
       
    private String renderGenericCmd(char command) {
        
        return String.valueOf(command);
    }
    
    private String renderChannelCmd(int channelId, char command) {
        
        StringBuilder sb = new StringBuilder(3);
        sb.append(command);
        sb.append(CodecHelper.encodeValue((char)channelId));
        
        return sb.toString();
    }
    
    private String renderChannelCmd(int channelId, char command, char parameter) {
        
        StringBuilder sb = new StringBuilder(5);
        sb.append(command);
        sb.append(CodecHelper.encodeValue((char)channelId));
        sb.append(CodecHelper.encodeValue(parameter));
        
        return sb.toString();
    }
    
    private String renderChannelCmd(int channelId, char command, List<Integer> parameters) {

        StringBuilder sb = new StringBuilder(5+(2*parameters.size()));
        sb.append(command);
        sb.append(CodecHelper.encodeValue((char)channelId));
        for (int n = 0; n < parameters.size(); n++) {
            sb.append(CodecHelper.encodeValue((char)parameters.get(n).intValue()));
        }
        
        return sb.toString();
    }
}
