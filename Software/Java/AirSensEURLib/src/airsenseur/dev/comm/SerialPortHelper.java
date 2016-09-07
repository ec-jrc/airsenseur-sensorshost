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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import purejavacomm.*;

/**
 * @author marco
 * Handle low level open/close actions for handling one serial port at time
 */
public class SerialPortHelper {
    
    private ArrayList<CommPortIdentifier> portsList = new ArrayList<>();

    private SerialPort serialPort = null;    
    private InputStream inputStream = null;
    protected OutputStream outputStream = null;
    
    private SerialReceiverParser rxDataParser; 
    private SerialReceiverListener rxListener;

    private String appName = "UnknownAppName";
    
    public static interface SerialReceiverParser {
        
        void onDataReceived(InputStream inputStream);
    }
    
    private class SerialReceiverListener implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent event) {
            
            if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                                
                try {
                    if (inputStream.available() != 0) {
                        rxDataParser.onDataReceived(inputStream);
                    }
                } catch (IOException e) {
                }
            }
            
        }
    }
            
    public ArrayList<CommPortIdentifier> refreshSerialPortList() {
        
        portsList = new ArrayList<>();

        Enumeration ports = CommPortIdentifier.getPortIdentifiers();
        while (ports.hasMoreElements()) {
            
            CommPortIdentifier port = (CommPortIdentifier)ports.nextElement();

            if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                portsList.add(port);
            }
            
        }
                
        return portsList;
    }
    
    public CommPortIdentifier[] enumerateSerialPortNames() {
        
        return portsList.toArray(new CommPortIdentifier[0]);
    }
    
    public boolean openPort(CommPortIdentifier portId, int baudrate, SerialReceiverParser rxDataParser ) throws Exception {
        
        boolean result = false;
        
        if (portId != null) {
            
            closePort();
            
            try {
                
                this.rxDataParser = rxDataParser;
                this.rxListener = new SerialReceiverListener();
                
                serialPort = (SerialPort) portId.open(appName, 2000);
                inputStream = serialPort.getInputStream();
                outputStream = serialPort.getOutputStream();
                serialPort.addEventListener(this.rxListener);
                serialPort.notifyOnDataAvailable(true);
                serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                
                result = true;

            } catch (UnsupportedCommOperationException | PortInUseException | TooManyListenersException | IOException e) {
                throw new Exception(e);

            }
        }
        
        return result;
    }
    
    public boolean closePort() {
        
        try {
            if (serialPort != null) {
                
                serialPort.notifyOnDataAvailable(false);
                serialPort.removeEventListener();
            }
                        
            if (rxListener != null) {
                rxListener = null;
            }
                
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            if (serialPort != null) {
                serialPort.close();
                serialPort = null;
            }
            
        } catch (IOException e) {
            
        }
        
        return true;
    }
    
    public void writeString(String data) throws IOException {
        
        if (outputStream != null) {
            outputStream.write(data.getBytes());
        }
    }
    
    public void setAppName(String appName) {
        this.appName = appName;
    }
}
