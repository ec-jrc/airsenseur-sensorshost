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
package airsenseur.dev.chemsensorhost.sensors.hostsensors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data retrieval functions for the Host board
 * @author marco
 */
public class HostSensorBoard {
    
    // Host board Id
    public final static int HOST_BOARD_ID = 0xFF;
    
    // ChannelsID
    public final static int CHANNEL_BATTERY_L4156_STATUS = 0x00;
    public final static int CHANNEL_BATTERY_L2942_CURRENT_COUNTER = 0x01;
    public final static int CHANNEL_BATTERY_L2942_VOLTAGE = 0x02;
    public final static int CHANNEL_NUM_CHANNELS = CHANNEL_BATTERY_L2942_VOLTAGE + 1;
    
    private final static String DISTRO_VERSION_FILE = "/etc/as_distro";
    private final static String BOARD_SERIAL_FILE = "/sys/bus/i2c/drivers/at24/1-0050/eeprom";
    
    private final static int ERRORLOGMAXTHRESHOLD = 10;
    private static final Logger LOG = LoggerFactory.getLogger(HostSensorBoard.class);
    private int errorLogCounter = 0;
    
    

    public int getBoardId() { 
        return HOST_BOARD_ID;
    }
    
    public int numChannels() {
        return CHANNEL_NUM_CHANNELS;
    }
    
    public int boardType() {
        return 3;
    }
    
    public String getSerial() {
        
        try {
            byte contents[] = Files.readAllBytes(Paths.get(BOARD_SERIAL_FILE));
            
            if (contents.length > 256) {
                return "";
            }
            
            // Convert to HEX representation the latest 6 bytes
            StringBuilder sb = new StringBuilder();
            for (int n = contents.length - 6; n < contents.length; n++) {
                sb.append(String.format("%02X", contents[n]));
                if (n != (contents.length-1)) {
                    sb.append(":");
                }
            }
            
            return sb.toString();
            
        } catch (IOException e) {
            logError("No serial IC available");
        } catch (Exception e) {
            logError("Error reading serial IC");
        }
        
        return "";
    }
    
    public String getFirmware() {
        
        try {
            String contents = new String(Files.readAllBytes(Paths.get(DISTRO_VERSION_FILE)));
            
            if (contents.length() > 100) {
                contents = contents.substring(0, 100);
            }
            
            return contents;
        } catch (IOException e) {
            logError("No distro version file available");
        }
        
        return "";
    }

    private void logError(String error) {
        
        if (errorLogCounter < ERRORLOGMAXTHRESHOLD) {
            errorLogCounter++;
            LOG.error(error);
        }
    }
    
}
