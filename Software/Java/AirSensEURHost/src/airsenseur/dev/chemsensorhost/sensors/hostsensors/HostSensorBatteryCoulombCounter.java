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
public class HostSensorBatteryCoulombCounter implements HostSensor {
    
    private final static int SAMPLING_PERIOD = 600000;  /* 600 seconds */
    
    private final static String BATTERYCURRENT = "/sys/class/power_supply/ltc2942/charge_counter";
    private Integer lastReadValue = null;
    private long lastSampleTimestamp = 0;
    
    private final static int ERRORLOGMAXTHRESHOLD = 10;
    private static final Logger LOG = LoggerFactory.getLogger(HostSensorBatteryCoulombCounter.class);
    private int errorLogCounter = 0;
    

    @Override
    public int getBoardId() {
        return HostSensorBoard.HOST_BOARD_ID;
    }

    @Override
    public int getChannel() {
        return HostSensorBoard.CHANNEL_BATTERY_L2942_CURRENT_COUNTER;
    }

    @Override
    public String getName() {
        return "L2942CUR";
    }

    @Override
    public String getSerial() {
        return "NA";
    }

    @Override
    public String getMeasurementUnits() {
        return "mC";
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
            lastReadValue = getBatteryChargeCounter();

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
     * @return the Coulomb counter on the battery gauge IC or null
     */
    private Integer getBatteryChargeCounter() {
       
        try {
            
            String contents = new String(Files.readAllBytes(Paths.get(BATTERYCURRENT)));
            contents = contents.replace("\r", "").replace("\n", "").trim();
            
            return Integer.valueOf(contents);
            
        } catch (IOException e) {
            logError("Input/Output exception when reading " + BATTERYCURRENT);
        } catch (NumberFormatException e) {
            logError("Invalid value read from " + BATTERYCURRENT);
        }
        
        return null;
    }    
    
    private void logError(String error) {
        
        if (errorLogCounter < ERRORLOGMAXTHRESHOLD) {
            errorLogCounter++;
            LOG.error(error);
        }
    }
}
