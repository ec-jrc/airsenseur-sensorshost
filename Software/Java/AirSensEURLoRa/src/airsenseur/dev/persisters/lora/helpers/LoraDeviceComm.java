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

package airsenseur.dev.persisters.lora.helpers;

import airsenseur.dev.exceptions.GenericException;
import airsenseur.dev.persisters.lora.BasicLoRaMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class LoraDeviceComm implements SerialCommPort.ChannelDataConsumer {
    
    // fPort IDs where messages are directed
    public final static int LORA_DEFAULT_SAMPLES_PORT = 1;
    public final static int LORA_DEFAULT_GPSINFO_PORT = 2;
    public final static int LORA_DEFAULT_BOARDSINFO_PORT = 3;
    public final static int LORA_DEFAULT_SENSORSINFO_PORT = 4;    
    
    // Some useful constants
    private final int NUM_OF_ACKDATA_TRIGGER_TOHIGHDR = 50; // Num of ack'd tx messages that may trigger a faster datarate
    private final int serialBaudrate = 57600;
    private final int timeoutMACRxData = 20; // In seconds
    private final int timeoutMACRxMultipleAnswers = 240;     // In seconds
    private final int maxRetry;
    private final int overridePacketLen;
    private final int overrideSleepTime;
    private final boolean disableADR;

    private final boolean logConversation;
    
    private final SerialCommPort serialPort = new SerialCommPort("AirSensEURDataPush_LoRa", this);
    private final ArrayBlockingQueue<DeviceMessage> rxQueue = new ArrayBlockingQueue<>(10);
    
    private final Logger log = LoggerFactory.getLogger(LoraDeviceComm.class);
    
    private final List<String> RX_OK = new ArrayList<String>() {{  add("ok"); }};
    private final List<String> RX_JOINOTAA = new ArrayList<String>() {{ add("ok"); add("accepted"); }};
    private final List<String> TX_CNF_MSG = new ArrayList<String>() {{ add("ok"); add("mac_tx_ok"); }};
    private final List<String> TX_UNCNF_MSG = new ArrayList<String>() {{ add("ok"); }};
    
    private final int waitTimeByDR[] = { 280, 158, 70, 68, 68, 38, 19, 19 };
    private final int packetLengthByDR[] = { 51, 51, 51, 115, 222, 222, 222, 222 };
    private int currentDataRate;        
    private int numOfAckDataMessages;
    
    private enum resultstatus {
        CMD_OK,
        CMD_KO,
        CMD_MAC_BUSY,
        CMD_MAC_ERR,
        CMD_MAC_INVALID_DATA_LENGTH,
        CMD_NO_FREE_CHANNELS
    }
    
    private static class DeviceMessage {
        
        public DeviceMessage(byte[] buffer, int length) {
            data = new String(buffer);
            data = data.substring(0, length);
        }
        
        public DeviceMessage(String data) {
            this.data = data.trim();
        }
        
        public String data;
    }        
    
    
    /**
     * Hides implementation communication details with MHCP LoRa serial dongle
     * 
     * @param maxRetry defines the number of retry to join and/or send data 
     * before raising an exception on error
     * @param overridePacketLen overrides the number of bytes allowed in the message packet
     * @param overrideSleepTime overrides the wait time after each tx message (in seconds)
     * @param disableADR disables ADR if true
     * @param logConversation if true, conversation between host and dongle is logged
     */
    public LoraDeviceComm(int maxRetry, int overridePacketLen, int overrideSleepTime, boolean disableADR, boolean logConversation) {
        this.maxRetry = maxRetry;
        this.overridePacketLen = overridePacketLen;
        this.overrideSleepTime = overrideSleepTime;
        this.logConversation = logConversation;
        this.currentDataRate = 0;
        this.disableADR = disableADR;
        this.numOfAckDataMessages = 0;
    }
    
    @Override
    // Data received from the serial port. Push a new message in the message queue
    public void onDataReceived(InputStream inputStream) throws InterruptedException {
        byte[] readBuffer = new byte[256];
        try {
            int numBytes = inputStream.read(readBuffer);
            if (numBytes > 0) {
                String[] rxData = (new String(readBuffer)).split("\r\n");
                for (String msg:rxData) {
                    DeviceMessage message = new DeviceMessage(msg);
                    if (!message.data.isEmpty()) {
                        rxQueue.put(message);
                    }
                }
            }
            
        } catch (IOException | StringIndexOutOfBoundsException ex) {
        }
        
    }
    
    public boolean open(String endpoint) throws GenericException {
        
        serialPort.close();

        return serialPort.open(endpoint, serialBaudrate);
    }
    
    public boolean close() {
        return serialPort.close();
    }
    
    public int getPacketLength() throws GenericException {
        
        // If packet length is overridden by configuration, use it.
        if (overridePacketLen != 0) {
            return overridePacketLen;
        }
                
        return packetLengthByDR[currentDataRate];
    }
    
    private int getCurrentSleepTime() {
        
        // If sleep time is overridden by configuration, use it.
        if (overrideSleepTime != 0) {
            return overrideSleepTime;
        }
        
        // Otherwise use a dynamic sleep time based on current Data Rate (it may vary due to ADR politics)
        return waitTimeByDR[currentDataRate];
    }
    
    private resultstatus getCurrentDataRate() throws GenericException {
        
        // We always start with an empty queue list of received messages
        rxQueue.clear();
        
        try {
            // Send command
            String message = "mac get dr";
            serialPort.write(message + "\r\n");
            logConversation(message);
            
            // Wait for answer
            DeviceMessage rxData = rxQueue.poll(timeoutMACRxData, TimeUnit.SECONDS);
            if (rxData != null) {
                
                logConversation(rxData.data);
                
                if (rxData.data.compareToIgnoreCase("invalid_param") == 0) {
                    return resultstatus.CMD_KO;
                }
                
                // Update current data rate
                currentDataRate = Integer.valueOf(rxData.data);
                if (currentDataRate >= packetLengthByDR.length) {
                    currentDataRate = 0;
                }
                
                return resultstatus.CMD_OK;
            }
            
            return resultstatus.CMD_KO;
            
        } catch (InterruptedException ex) {
            return resultstatus.CMD_KO;
        }
    }
    
    // send a MAC message and don't care about result (except if "invalid_param")
    private resultstatus sendCommand(String message) throws GenericException {
        
        // We always start with an empty queue list of reeived messages
        rxQueue.clear();
        
        try {
            // Send command
            serialPort.write(message + "\r\n");
            logConversation(message);
            
            // Wait for answer
            DeviceMessage rxData = rxQueue.poll(timeoutMACRxData, TimeUnit.SECONDS);
            if (rxData != null) {
                
                logConversation(rxData.data);
                return (rxData.data.compareToIgnoreCase("invalid_param") != 0)? resultstatus.CMD_OK : resultstatus.CMD_KO;
            }
            
            return resultstatus.CMD_KO;
            
        } catch (InterruptedException ex) {
            return resultstatus.CMD_KO;
        }
    }
    
    
    // send a MAC message and take care about the list of message we expect to receive
    private resultstatus sendCommand(String message, List<String> expectedAnswers) throws GenericException {
        
        // We always start with an empty queue list of reeived messages
        rxQueue.clear();
        
        try {
            // Send command
            serialPort.write(message + "\r\n");
            logConversation(message);
            
            // Wait for the answers
            for (String expectedAnswer:expectedAnswers) {
                DeviceMessage rxData = rxQueue.poll(timeoutMACRxMultipleAnswers, TimeUnit.SECONDS);
                
                if (rxData == null) {                    
                    // Timeout: something goes wrong. Return false
                    return resultstatus.CMD_KO;
                }
                
                logConversation(rxData.data);
                
                // Evaluate rxData
                if (rxData.data.compareToIgnoreCase("invalid_param") == 0) {
                    return resultstatus.CMD_KO;
                }
                
                // No Free channels
                if (rxData.data.compareToIgnoreCase("no_free_ch") == 0) {
                    return resultstatus.CMD_NO_FREE_CHANNELS;
                }
                
                // Mac busy
                if (rxData.data.compareToIgnoreCase("busy") == 0) {
                    return resultstatus.CMD_MAC_BUSY;
                }
                
                // Mac err
                if (rxData.data.compareToIgnoreCase("mac_err") == 0) {
                    return resultstatus.CMD_MAC_ERR;
                }
                
                // Invalid data length
                if (rxData.data.compareToIgnoreCase("invalid_data_len") == 0) {
                    return resultstatus.CMD_MAC_INVALID_DATA_LENGTH;
                }
                
                // Evaluate rxData with the expected answer
                if (rxData.data.compareToIgnoreCase(expectedAnswer) != 0) {
                    return resultstatus.CMD_KO;
                }
            }
            
            return resultstatus.CMD_OK;
        }
        catch (InterruptedException ex) {
            return resultstatus.CMD_KO;
        }
        
    }
    
    public boolean sendGenericCommand(String command) throws GenericException {
        
        return sendCommand(command) == resultstatus.CMD_OK;
    }
    
    public boolean setTxPower(int txPower) throws GenericException  {
        
        if (sendCommand("mac pause") != resultstatus.CMD_OK)
            return false;
        
        if (sendCommand("radio set pwr " + txPower, RX_OK) != resultstatus.CMD_OK)
            return false;
        
        return (sendCommand("mac resume", RX_OK) == resultstatus.CMD_OK);
    }
    
    public boolean setDataRate(int datarate) throws GenericException  {
        
        if (sendCommand("mac pause") != resultstatus.CMD_OK)
            throw new GenericException("Error setting devEUI");
        
        if (sendCommand("mac set dr " + datarate, RX_OK) != resultstatus.CMD_OK) {
            return false;
        }
        
        currentDataRate = datarate;        
        
        if (sendCommand("mac set adr on", RX_OK) != resultstatus.CMD_OK) {
            return false;
        }
        
        return sendCommand("mac resume", RX_OK) == resultstatus.CMD_OK;
    }
    
    // if no appEUI is specivied, we expect to continue with no errors
    // (the dongle appEUI is used in this case)
    // In all other cases it returns true or an exception if problems
    public boolean setAppEUI(String appEUI) throws GenericException {
        
        if (appEUI.isEmpty()) {
            return true;
        }
        
        if (sendCommand("mac pause") != resultstatus.CMD_OK)
            throw new GenericException("Error setting appEUI");

        if (sendCommand("mac set appeui " + appEUI, RX_OK) != resultstatus.CMD_OK)
            throw new GenericException("Error setting appEUI");
        
        // In case appEUI is set externally, clear the stored and application security keys
        if (sendCommand("mac set nwkskey 00000000000000000000000000000000", RX_OK) != resultstatus.CMD_OK)
            throw new GenericException("Error clearing network key");
        
        if (sendCommand("mac set appskey 00000000000000000000000000000000", RX_OK) != resultstatus.CMD_OK)
            throw new GenericException("Error clearing app security key");
        
        
        if (sendCommand("mac resume", RX_OK) != resultstatus.CMD_OK)
            throw new GenericException("Error setting appEUI");
        
        return true;
    }
    
    // If no appKey is speficied, we expect to continue with no errors
    // (the dongle appKey is used in this case)
    // In all other cases it returns true or an exception if problems
    public boolean setAppKey(String appKey) throws GenericException {
        if (appKey.isEmpty()) {
            return true;
        }
        
        if (sendCommand("mac pause") != resultstatus.CMD_OK)
            throw new GenericException("Error setting appKey");

        if (sendCommand("mac set appkey " + appKey, RX_OK) != resultstatus.CMD_OK)
            throw new GenericException("Error setting appKey");
        
        if (sendCommand("mac resume", RX_OK) != resultstatus.CMD_OK)
            throw new GenericException("Error setting appKey");
        
        return true;
    }
    
    // If no EUI is speficied, we expect to continue with no errors
    // (the dongle EUI is used in this case)
    // In all other cases it returns true or an exception if problems
    public boolean setDeviceEUI(String devEUI) throws GenericException  {
        if (devEUI.isEmpty()) {
            return true;
        }
        
        if (sendCommand("mac pause") != resultstatus.CMD_OK)
            throw new GenericException("Error setting devEUI");

        if (sendCommand("mac set deveui " + devEUI, RX_OK) != resultstatus.CMD_OK)
            throw new GenericException("Error setting devEUI");
        
        if (sendCommand("mac resume", RX_OK) != resultstatus.CMD_OK)
            throw new GenericException("Error setting devEUI");
        
        return true;
    }
    
    // Try to join with OTAA. Returns false if not able to do it after several retries
    public boolean joinOTAA() throws GenericException  {
        
        int retry = 0;
        while (retry < maxRetry) {
            if (sendCommand("mac join otaa", RX_JOINOTAA) == resultstatus.CMD_OK) {
                return true;
            }
            
            retry++;
        }
        
        return false;
    }
    
    public boolean sendPayload(BasicLoRaMessage msg) throws GenericException {
        
        int port = msg.fPort;
        String message = msg.buffer.toString();
        
        if (disableADR) {
            sendCommand("mac set adr off");
        }
        
        int retry = 0;
        int freeChannelRetry = 0;
        while ((retry < maxRetry) && (freeChannelRetry < 30)) {
            try {
            
                String cnf = msg.confirmed? "cnf " : "uncnf ";
                resultstatus result = sendCommand("mac tx " +  cnf + port + " " + message, (msg.confirmed)? TX_CNF_MSG : TX_UNCNF_MSG);
                if (null != result) switch (result) {
                    case CMD_OK:
                        
                        // Update the num of tx messages sent
                        numOfAckDataMessages++;
                        
                        // Wait before next message is allowed
                        int sleepTime = getCurrentSleepTime()*1000;
                        int maxPacketLength = getPacketLength();
                        sleepTime = (int)((float)sleepTime * (((float)msg.size()) / maxPacketLength));
                        log.info("Waiting " + sleepTime + " milliseconds.");
                        Thread.sleep(sleepTime);
                        
                        if (getCurrentDataRate() != resultstatus.CMD_OK) {
                            return false;
                        }
                        
                        // Try to increase the datarate after a series of
                        // successfull tx commands
                        if (!disableADR && (currentDataRate < 5) && (numOfAckDataMessages >= NUM_OF_ACKDATA_TRIGGER_TOHIGHDR)) {
                            log.info("Trying to increase the datarate");
                            numOfAckDataMessages = 0;
                            setDataRate(currentDataRate + 1);
                        }
                        
                        return true;
                        
                    case CMD_NO_FREE_CHANNELS:
                        log.info("No free channels available. Retrying in 20 seconds.");
                        Thread.sleep(20000);
                        retry--;
                        freeChannelRetry++;
                        getCurrentDataRate();
                        break;
                        
                    case CMD_MAC_BUSY:
                        log.info("Mac Busy. Retrying in 20 seconds.");
                        Thread.sleep(20000);
                        retry--;
                        freeChannelRetry++;
                        getCurrentDataRate();
                        break;
                        
                    case CMD_MAC_INVALID_DATA_LENGTH:
                        log.info("Mac invalid data length. Retrying in 20 seconds.");
                        Thread.sleep(20000);
                        
                        // Try to scale up the datarate on next tx
                        // We don't expect to have ADR that lower the datarate but we seen this behavour in the MHCP dongle
                        numOfAckDataMessages = 0;
                        getCurrentDataRate();
                        if (currentDataRate < 5) {
                            setDataRate(currentDataRate + 1);
                        }
                        
                        retry--;
                        break;
                        
                    case CMD_MAC_ERR:
                        log.info("Mac error. No ack received from concentrator?. Retrying in 20 seconds.");
                        numOfAckDataMessages = 0;
                        Thread.sleep(20000);
                        getCurrentDataRate();
                        retry--;
                        break;
                        
                    case CMD_KO:
                        log.info("Error transmitting packet.");
                        numOfAckDataMessages = 0;
                        return false;
                        
                    default:
                        break;
                }
                
                retry++;
            } catch (InterruptedException ex) {
                return false;
            }
        }
        
        return false;
    }
        
    private void logConversation(String message) {
        if (!logConversation)
            return;
        
        if (message != null) {
            log.info(message);
        }
    }
}
