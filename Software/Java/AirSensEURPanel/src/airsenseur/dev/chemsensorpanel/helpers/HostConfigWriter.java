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

package airsenseur.dev.chemsensorpanel.helpers;

import airsenseur.dev.chemsensorpanel.exceptions.ChemSensorPanelException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the configuration file to be uploaded on the remote AirSensEUR unit.
 * @author marco
 */
public class HostConfigWriter {

    private final String serialPort = "ttyS1";
    private final String jsonHostName = "0.0.0.0";
    private final int jsonPort = 8000;
    private final int pollTime = 3000;
    
    private boolean useBusProtocol;
    private int debugVerbose = 0;
    
    private final List<HostConfigSensorProperties> sensorsProperties = new ArrayList<>();
    
    /**
     * @param useBusProtocol the useBusProtocol to set
     */
    public void setUseBusProtocol(boolean useBusProtocol) {
        this.useBusProtocol = useBusProtocol;
    }

    /**
     * @param debugVerbose the debugVerbose to set
     */
    public void setDebugVerbose(int debugVerbose) {
        this.debugVerbose = debugVerbose;
    }
    
    /**
     * Add a new sensor on the sensors list
     * @return 
     */
    public HostConfigSensorProperties addNewSensor() {
        sensorsProperties.add(new HostConfigSensorProperties());
        return sensorsProperties.get(sensorsProperties.size()-1);
    }
    
    /**
     * Generates a new configuration file based on stored information
     * @param fileName 
     * @throws airsenseur.dev.chemsensorpanel.exceptions.ChemSensorPanelException 
     */
    public void generateConfigFile(String fileName) throws ChemSensorPanelException {
        
        File configFile = new File(fileName);
        generateConfigFile(configFile);
    }

    /**
     * Generates a new configuration file based on stored information
     * @param configFile 
     * @throws airsenseur.dev.chemsensorpanel.exceptions.ChemSensorPanelException 
     */
    public void generateConfigFile(File configFile) throws ChemSensorPanelException {
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.append(renderKeyVal("port", serialPort));

            // Debug
            writer.append(renderKeyVal("debug", debugVerbose));

            // json Host and port
            writer.append(renderKeyVal("jsonHostname", jsonHostName));
            writer.append(renderKeyVal("jsonPort", jsonPort));

            // Poll time
            writer.append(renderKeyVal("pollTime", pollTime));

            // BusProtocol
            writer.append(renderKeyVal("useBusProtocol", useBusProtocol));

            // Num of sensors
            writer.append(renderKeyVal("numSensors", sensorsProperties.size()));

            // Sensor Properties
            for (int n = 0; n < sensorsProperties.size(); n++) {
                HostConfigSensorProperties sensorProperty = sensorsProperties.get(n);

                // SensorName
                if (!sensorProperty.sensorName.isEmpty()) {
                    writer.append(renderKeyVal(renderKey("sensorname", n), sensorProperty.sensorName));
                }

                // Sensor Expression
                writer.append(renderKeyVal(renderKey("sensorexpression", n), sensorProperty.sensorExpression));

                // Sensor Board Id
                if (useBusProtocol) {
                    writer.append(renderKeyVal(renderKey("sensorboarid", n), sensorProperty.sensorBoardId));
                }

                // Sensor Channel
                writer.append(renderKeyVal(renderKey("sensorchannel", n), sensorProperty.sensorChannel));
            }
        } catch (IOException ex) {
            throw new ChemSensorPanelException(ex.getMessage());
        }
    }
    
    private String renderKey(String keyBase, int keyNumber) {
        return String.format("%s_%02d", keyBase, keyNumber);
    }
        
    private StringBuilder renderKeyVal(String key, String val) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(key).append("=").append(val).append("\n");
        
        return sb;
    }
    
    private StringBuilder renderKeyVal(String key, long val) {
        return renderKeyVal(key, Long.toString(val));
    }
    
    private StringBuilder renderKeyVal(String key, boolean val) {
        return renderKeyVal(key, val? "1":"0");
    }
}
