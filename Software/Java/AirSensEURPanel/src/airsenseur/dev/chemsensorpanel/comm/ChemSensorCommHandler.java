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

package airsenseur.dev.chemsensorpanel.comm;

import airsenseur.dev.chemsensorpanel.ChemSensorPanel;
import airsenseur.dev.comm.ChemSensorBoard;
import airsenseur.dev.comm.CommProtocolHelper;
import airsenseur.dev.comm.SerialPortHelper;
import airsenseur.dev.exceptions.ChemSensorBoardException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import purejavacomm.CommPortIdentifier;

/**
 *
 * @author marco
 */
public class ChemSensorCommHandler implements ChemSensorBoard {
    
    private final int DEFAULT_BAUDRATE = 9600;
    
    private final SerialPortHelper serialPortHelper = new SerialPortHelper();
    private ChemSensorPanel parent;
    
    // Data receiver callback implementation
    private final SerialPortHelper.SerialReceiverParser rxParser = new SerialPortHelper.SerialReceiverParser() {

        @Override
        public void onDataReceived(InputStream inputStream) {

            byte[] readBuffer = new byte[128];

            try {
                int numBytes = inputStream.read(readBuffer);

                System.out.println("Rx: " + readBuffer);

                    for (int i = 0; i < numBytes; i++) {
                        boolean newPacketReady = CommProtocolHelper.instance().onRxCharReceived(readBuffer[i]);
                            if ((parent != null) && newPacketReady) {
                                parent.onDataReceived();
                            }
                    }
                } catch (IOException ex) {
                }
            }
    };
    
    public ChemSensorCommHandler(ChemSensorPanel parent) {
        this.parent = parent;
    }
    
    // Standard constructor is forbidden because we need a valid parent for the data callback
    private ChemSensorCommHandler() {
    }

    @Override
    public void connectToBoard(CommPortIdentifier serialPort) throws ChemSensorBoardException {
        try {
            serialPortHelper.openPort(serialPort, DEFAULT_BAUDRATE, rxParser);
        } catch (Exception e) {
            throw new ChemSensorBoardException(e.getMessage());
        }
    }

    @Override
    public void disConnectFromBoard() {
        
        serialPortHelper.closePort();
    }
    

    @Override
    public synchronized void writeBufferToBoard() {
        
        // Retrieve the current buffer command list
        List<CommProtocolHelper.DataMessage> cmdList = CommProtocolHelper.instance().getCurrentCommandList();
        
        for (CommProtocolHelper.DataMessage command : cmdList) {
            try {
                
                serialPortHelper.writeString(command.getCommandString());  
                System.out.println(command.getCommandString());
                
            } catch (IOException e) {
            }
        }
        
        // Clear the command list
        cmdList.clear();
    }

    @Override
    public List<CommProtocolHelper.DataMessage> getCurrentBuffer() {
        return CommProtocolHelper.instance().getCurrentCommandList();
    }
}
