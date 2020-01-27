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
 *
 * @author marco
 */
public class HostSensorBatteryChargerStatus implements HostSensor {
    
    private final static int SAMPLING_PERIOD = 600000;  /* 600 seconds */
    
    private final static String BATTERYSTATUSFILE = "/sys/class/power_supply/ltc4156-charger-0/status";
    private Integer lastReadValue = null;
    private long lastSampleTimestamp = 0;
    
    private static final Logger LOG = LoggerFactory.getLogger(HostSensorBatteryChargerStatus.class);
    private boolean logEnabled = true;

    @Override
    public int getBoardId() {
        return HostSensorBoard.HOST_BOARD_ID;
    }

    @Override
    public int getChannel() {
        return HostSensorBoard.CHANNEL_BATTERY_L4156_STATUS;
    }

    @Override
    public String getName() {
        return "L4156STA";
    }

    @Override
    public String getSerial() {
        return "NA";
    }

    @Override
    public String getMeasurementUnits() {
        return "enum";
    }

    @Override
    public String getMathExpression() {
        return "x";
    }

    @Override
    public Integer getSamplingPeriod() {
        return SAMPLING_PERIOD;
    }

    @Override
    public int getValue() {
        if((System.currentTimeMillis() - lastSampleTimestamp) > SAMPLING_PERIOD) {

            lastSampleTimestamp = System.currentTimeMillis();
            lastReadValue = getBatteryChargeStatus();

            if (lastReadValue != null) {
                return lastReadValue;
            }
        }
                
        return 0;
    }

    @Override
    public int getTimestamp() {
        
        return (int) (lastSampleTimestamp / 1000);
    }

    /**
     * 
     * @return 0 -> Full, 1 -> Charging, 2 -> Discharging, or null 
    */
    private Integer getBatteryChargeStatus() {
        
        try {
            String contents = new String(Files.readAllBytes(Paths.get(BATTERYSTATUSFILE)));
            
            if (contents.startsWith("Full")) {
                return 0;
            }
            
            if (contents.startsWith("Charging")) {
                return 1;
            }
            
            if (contents.startsWith("Discharging")) {
                return 2;
            }
            
        } catch (IOException e) {
            if (logEnabled) {
                
                LOG.error("Input/Output exception when reading " + BATTERYSTATUSFILE);

                // Prevent flooding logs when no sensor is available on the board
                logEnabled = false;
            }
        }
        
        return null;
    }        

}
