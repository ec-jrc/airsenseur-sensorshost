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

package airsenseur.dev.persisters.influxdb;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.json.SensorConfig;

/**
 *
 * @author marco
 */
public class SensorConfigSerie extends Serie {
    
    // We use those offset to access to some specific tokens
    // when generating the line protocol for influxdb serialization.
    // Keep the offset synchronized with the columns contents when
    // changing this class implementation.
    private final static int TIME_TOKEN_OFFSET = 0;
    private final static int NAME_TOKEN_OFFSET = 1;
    private final static int SENSORID_TOKEN_OFFSET = 2;
    
    public SensorConfigSerie(String name) {
        super(name);
        
        getColumns().add("time");
        getColumns().add("name");
        getColumns().add("sensorid");
        getColumns().add("boardid");
        getColumns().add("serial");
        getColumns().add("units");
        getColumns().add("samplingperiod");
        getColumns().add("enabled");
    }    
    
    public void addSensorConfig(SensorConfig sensorConfig) {
        
        Point point = new Point();
        getPoints().add(point);
        
        point.add(sensorConfig.startSamplingTimestamp);
        point.add(sensorConfig.name);
        point.add(sensorConfig.sensorId);
        point.add(sensorConfig.boardId);
        point.add(encloseInQuotation(sensorConfig.serial));
        point.add(encloseInQuotation(sensorConfig.measurementUnits));
        point.add(sensorConfig.samplingPeriod);
        point.add(sensorConfig.enabled);
    }

    @Override
    public String toLineProtocol() throws PersisterException {
        
        // We expect to have time, name and chsensorId, serial to these specified 
        // offsets in the column list. If not, raise an error
        if (!getColumns().get(TIME_TOKEN_OFFSET).equalsIgnoreCase("time")) {
            throw new PersisterException("Invalid time token found in sensor config data serie. Please alert the development team");
        }
        if (!getColumns().get(NAME_TOKEN_OFFSET).equalsIgnoreCase("name")) {
            throw new PersisterException("Invalid name token found in sensor config data serie. Please alert the development team");
        }
        if (!getColumns().get(SENSORID_TOKEN_OFFSET).equalsIgnoreCase("sensorid")) {
            throw new PersisterException("Invalid sensorid token found in sensor config data serie. Please alert the development team");
        }
        
        StringBuilder sb = new StringBuilder();
        for (Point point:getPoints()) {
            
            // Get the time value
            Object timeVal = point.get(TIME_TOKEN_OFFSET);

            // Skip samples with invalid name. This should never happens but it's better to check
            String name = safeEscape((String)point.get(NAME_TOKEN_OFFSET));
            if (!name.isEmpty()) {

                // Measurement
                sb.append(getName()).append(",");

                // - Generate the tags string -
                // Tag: Name
                sb.append(getColumns().get(NAME_TOKEN_OFFSET)).append("=").append(name).append(",");
                
                // Tag: sensorid
                sb.append(getColumns().get(SENSORID_TOKEN_OFFSET)).append("=").append(point.get(SENSORID_TOKEN_OFFSET)).append(" ");
                
                // Values:
                for (int n = 0; n < getColumns().size(); n++) {

                    // Skip known tokens
                    if ((n == TIME_TOKEN_OFFSET) || (n == NAME_TOKEN_OFFSET) || (n == SENSORID_TOKEN_OFFSET)) {
                        continue;
                    }
                    
                    sb.append(getColumns().get(n)).append("=").append(point.get(n));
                    if (n != getColumns().size()-1) {
                        sb.append(",");
                    }
                }

                // Timestamp
                sb.append(" ").append(timeVal).append("\n");
            }
        }
        
        return sb.toString();
    }    
}
