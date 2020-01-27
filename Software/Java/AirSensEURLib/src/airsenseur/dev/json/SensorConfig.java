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

package airsenseur.dev.json;

/**
 * Sensor channel configuration propagated through JSON interface
 * @author marco
 */
public class SensorConfig {
    
    public String name = "";
    public String serial = "";
    public String measurementUnits = "";
    public int sensorId = 0;
    public long samplingPeriod = 0;
    public long startSamplingTimestamp = 0;
    public boolean enabled = true;
        
    
    public SensorConfig() {
    }
    
    public SensorConfig(int sensorId) {
        this.sensorId = sensorId;
    }
    
    public SensorConfig(String name, String serial, String measurementUnits, int sensorId, long samplingPeriod, long startSamplingTimestamp, boolean enabled) {
        this.name = name;
        this.serial = serial;
        this.measurementUnits = measurementUnits;
        this.sensorId = sensorId;
        this.samplingPeriod = samplingPeriod;
        this.startSamplingTimestamp = startSamplingTimestamp;
        this.enabled = enabled;
    }
}

