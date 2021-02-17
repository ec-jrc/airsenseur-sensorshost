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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;
import purejavacomm.UnsupportedCommOperationException;

/**
 *
 * @author marco
 */
public class SerialCommPort {
    
    private final String applicationName;
    private SerialPort serialPort = null;
    private InputStream inputStream = null;
    protected OutputStream outputStream = null;
    
    private final ChannelDataConsumer rxDataConsumer;
    private final SerialReceiverListener rxListener = new SerialReceiverListener();
    
    public interface ChannelDataConsumer {

        void onDataReceived(InputStream inputStream) throws InterruptedException;
    }
    
    private class SerialReceiverListener implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent event) {
            
            if ((event != null) && (event.getEventType() == SerialPortEvent.DATA_AVAILABLE)) {
                                
                try {
                    if ((inputStream != null) && (inputStream.available() != 0)) {
                        try {
                            if (rxDataConsumer != null) {
                                rxDataConsumer.onDataReceived(inputStream);
                            }
                        } catch (InterruptedException ex) {
                        }
                    }
                } catch (IOException e) {
                }
            }
            
        }
    }  
    
    
    public SerialCommPort(String applicatioName, ChannelDataConsumer rxDataConsumer) {
        
        this.applicationName = applicatioName;
        this.rxDataConsumer = rxDataConsumer;
    }
    
    public boolean open(String name, int rate) throws GenericException {
        
        // Search the port enumerator into the available ports and try to open it when found
        try {
            Enumeration ports = CommPortIdentifier.getPortIdentifiers();
            while (ports.hasMoreElements()) {

                CommPortIdentifier port = (CommPortIdentifier)ports.nextElement();
                if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                    if (port.getName().compareToIgnoreCase(name) == 0) {

                        serialPort = (SerialPort) port.open(applicationName, 2000);
                        inputStream = serialPort.getInputStream();
                        outputStream = serialPort.getOutputStream();
                        serialPort.addEventListener(this.rxListener);
                        serialPort.notifyOnDataAvailable(true);
                        serialPort.setSerialPortParams(rate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                        
                        return true;
                    }
                }
            }
        } catch (UnsupportedCommOperationException | PortInUseException | TooManyListenersException | IOException e) {
            
            String message = e.getMessage();
            
            if ((message == null) && (e instanceof PortInUseException)) {
                message = "port already in use";
            }
            throw new GenericException(message);
        }
        
        return false;
    }
    
    
    public boolean close() {
        
        try {
            if (serialPort != null) {
                
                serialPort.notifyOnDataAvailable(false);
                serialPort.removeEventListener();
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
    
    boolean write(String message) throws GenericException {
        try {
            outputStream.write(message.getBytes());
            
        } catch (IOException ex) {
            return false;
        }
        
        return true;
    }
}
