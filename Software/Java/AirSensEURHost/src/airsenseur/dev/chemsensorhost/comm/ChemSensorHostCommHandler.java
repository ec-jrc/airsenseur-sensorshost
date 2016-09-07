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

package airsenseur.dev.chemsensorhost.comm;

import airsenseur.dev.chemsensorhost.Configuration;
import airsenseur.dev.comm.ChemSensorBoard;
import airsenseur.dev.comm.CommProtocolHelper;
import airsenseur.dev.comm.SerialPortHelper;
import airsenseur.dev.exceptions.ChemSensorBoardException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import purejavacomm.CommPortIdentifier;

/**
 *
 * @author marcos
 */
public class ChemSensorHostCommHandler implements ChemSensorBoard {
    
    public static interface ChemSensorMessageHandler {
        
        void onNewPacketReady();
    }
    
    private final Logger log = LoggerFactory.getLogger(ChemSensorHostCommHandler.class);
    
    private final int DEFAULT_BAUDRATE = 9600;
    
    private final SerialPortHelper serialPortHelper = new SerialPortHelper();
    private final ChemSensorMessageHandler messageHandler;
    
    private CommPortIdentifier serialPortIdentifier = null;
    private boolean connected = false;
    
    // Data receiver callback implementation
    private final SerialPortHelper.SerialReceiverParser rxParser; 
    
    public ChemSensorHostCommHandler(ChemSensorMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        this.rxParser = new SerialPortHelper.SerialReceiverParser() {

            @Override
            public void onDataReceived(InputStream inputStream) {
                
                byte[] readBuffer = new byte[256];
                
                try {
                    int numBytes = inputStream.read(readBuffer);
                    
                    // Dump received data
                    if (Configuration.getConfig().debugVerbose() > Configuration.DEBUG_VERBOSE_DUMP_SERIAL) {
                        log.trace("Rx: " + new String(readBuffer, "UTF-8"));
                    }
                    
                    for (int i = 0; i < numBytes; i++) {
                        boolean newPacketReady = CommProtocolHelper.instance().onRxCharReceived(readBuffer[i]);
                        if (newPacketReady) {
                            ChemSensorHostCommHandler.this.messageHandler.onNewPacketReady();
                        }
                    }
                } catch (IOException | StringIndexOutOfBoundsException ex) {
                    log.error("Errors found on received data.");
                    reConnectToBoard();
                }
            }
    };

    }
    
    public void connectToBoard(String serialPort) throws ChemSensorBoardException {
        
        serialPortHelper.refreshSerialPortList();
        CommPortIdentifier[] commPorts = serialPortHelper.enumerateSerialPortNames();
        if (commPorts == null) {
            throw new ChemSensorBoardException("No serial ports available");
        }
        
        for (CommPortIdentifier commPort:commPorts) {
            String portName = commPort.getName();
            if (portName.compareToIgnoreCase(serialPort) == 0) {
                connectToBoard(commPort);

                return;
            }
        }
        
        throw new ChemSensorBoardException("Seial port " + serialPort + " not found/available. Board not connected.");
    }
    
    public boolean reConnectToBoard() {
        
        log.info("Trying to reconnect to the serial port.");
        disConnectFromBoard();
        try {
            connectToBoard(serialPortIdentifier);
        } catch (ChemSensorBoardException ex) {
            
            log.error("Error when trying to reconnect to the serial port");
            return false;
        }
        
        return true;
    }

    @Override
    public void connectToBoard(CommPortIdentifier serialPort) throws ChemSensorBoardException {
        
        connected = false;
        serialPortIdentifier = serialPort;        
        
        try {
            serialPortHelper.openPort(serialPort, DEFAULT_BAUDRATE, rxParser);
            log.info("Successfully connected to the serial port " + serialPort.getName());
        } catch (Exception e) {
            throw new ChemSensorBoardException(e.getMessage());
        }
        
        connected = true;
    }

    @Override
    public void disConnectFromBoard() {
        
        serialPortHelper.closePort();
        connected = false;
    }
    

    @Override
    public synchronized void writeBufferToBoard() {
        
        // If not connected, try to reconnect
        if (!connected) {
            log.info("Write data to an unconnected serial line.");
            reConnectToBoard();
        }
        
        // Retrieve the current buffer command list
        List<CommProtocolHelper.DataMessage> cmdList = CommProtocolHelper.instance().getSafeCurrentCommandList();
        
        for (CommProtocolHelper.DataMessage command : cmdList) {
            try {
                
                serialPortHelper.writeString(command.getCommandString());
                
                // Dump sent data
                if (Configuration.getConfig().debugVerbose() > Configuration.DEBUG_VERBOSE_DUMP_SERIAL) {
                    log.trace("Tx: " + command.getCommandString() + " (" + command.getCommandComment() + ")");
                }
                
            } catch (IOException | IllegalStateException e) {
            }
        }
        
        // Clear the command list
        CommProtocolHelper.instance().clearCurrentCommandList();
    }

    @Override
    public List<CommProtocolHelper.DataMessage> getCurrentBuffer() {
        return CommProtocolHelper.instance().getCurrentCommandList();
    }
}
