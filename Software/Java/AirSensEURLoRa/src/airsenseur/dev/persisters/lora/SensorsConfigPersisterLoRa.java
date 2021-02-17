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

package airsenseur.dev.persisters.lora;

import airsenseur.dev.exceptions.GenericException;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.json.SensorConfig;
import airsenseur.dev.persisters.SensorsConfigPersister;
import airsenseur.dev.persisters.lora.helpers.LoraDeviceComm;
import java.util.List;

/**
 *
 * @author marco
 */
public class SensorsConfigPersisterLoRa implements SensorsConfigPersister {
    
    private final LoraDeviceComm loraDevice;
    
    private static class message extends BasicLoRaMessage {
        
        public message(int fPort) {
            super(fPort, false);
        }
        
        public void append(SensorConfig sensor) {

            super.append((short)sensor.sensorId);
            super.append((char)sensor.boardId);
            super.append(sensor.name);
            super.append(sensor.serial);
            super.append(sensor.measurementUnits);
        }
        
        // Returns the extimated size of ASCII encoded message for the specified board
        public int extimateSize(SensorConfig sensor) {
            return extimateEncodedStringSize(sensor.name) + 
                    extimateEncodedStringSize(sensor.serial) +
                    extimateEncodedStringSize(sensor.measurementUnits) + 6;
        }
        
    }
    
    public SensorsConfigPersisterLoRa(LoraDeviceComm loraDevice) {
        this.loraDevice = loraDevice;
    }

    @Override
    public boolean addSensorsConfig(List<SensorConfig> sensorsConfig) throws PersisterException {
        
        // Loop on sensors and send LoRa messages aggregating boards where possible
        message currentMessage = new message(LoraDeviceComm.LORA_DEFAULT_SENSORSINFO_PORT);        
        currentMessage.clear();
        try {
            for (SensorConfig sensor:sensorsConfig) {
                
                // Skip disabled sensors
                if (!sensor.enabled) {
                    continue;
                }
                
                int size = currentMessage.extimateSize(sensor);
                
                // It's impossible to send this information because it's more than
                // the allowed LoRa packet length. Discard it.
                if (size > loraDevice.getPacketLength()) {
                    continue;
                }

                // If we don't have enought space to append info to current message
                // send partial message
                if ((currentMessage.size() + size) > loraDevice.getPacketLength()) {
                    
                    if (!flushCurrentMessage(currentMessage))
                        return false;
                } else {

                    // Otherwise, append this board info to current message
                    currentMessage.append(sensor);
                }
            }
            
            // Flush partial accumulated messages
            return flushCurrentMessage(currentMessage);
            
        } catch (GenericException e) {
            return false;
        }
    }

    @Override
    public String getPersisterMarker(int channel) {
        return HistoryEventContainer.EVENT_LATEST_LORA_SENSORCONFIGPUSH_TS;
    }
    
    private boolean flushCurrentMessage(message currentMessage) throws GenericException {
        
        // Nothing to do with this message
        if (!currentMessage.dirty) {
            return true;
            
        }
        
        // Flush current message
        if (!loraDevice.sendPayload(currentMessage)) {
            return false;
        }
        
        // Prepare next buffer
        currentMessage.clear();
        
        return true;
    }    
}
