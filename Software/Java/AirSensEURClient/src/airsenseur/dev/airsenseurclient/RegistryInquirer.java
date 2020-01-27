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

package airsenseur.dev.airsenseurclient;

import airsenseur.dev.json.BoardInfo;
import airsenseur.dev.json.ChemSensorClient;
import airsenseur.dev.json.SensorConfig;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieve the remote board and sensor registry and write it to the specified file
 * @author marco
 */
public class RegistryInquirer {
    
    private final ChemSensorClient dataCollector;
    
    private static final Logger log = LoggerFactory.getLogger(RegistryInquirer.class);

    public RegistryInquirer(ChemSensorClient dataCollector) {
        this.dataCollector = dataCollector;
    }

    public boolean process(String outFileName) {
        
        // Retrieve the board info list
        log.info("Retrieving remote boards information");
        List<BoardInfo> boardsInfo = dataCollector.getSensorBoardsInfo();
        if (boardsInfo == null) {
            log.info("Error retrieving remote boards information");
            return false;
        }
        
        // Retrieve the sensor info list
        log.info("Retrieving remote sensors information");
        int numSensors = dataCollector.getNumSensors();
        List<SensorConfig> sensorsInfo = new ArrayList<>(numSensors);
        for (int i = 0; i < numSensors; i++) {
            SensorConfig sensorConfig = dataCollector.getSensorConfig(i);
            if (sensorConfig != null) {
                sensorsInfo.add(sensorConfig);
            } else {
                log.info("Error retrieving information for sensor " + i);
                return false;
            }
        }
        
        // Open the file for writing
        log.info("Writing collected information in the file " + outFileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFileName))) {

            dumpBoardsInfo(boardsInfo, writer);
            dumpSensorsInfo(sensorsInfo, writer);
            
        } catch (IOException ex) {
            log.info("Error writing remote boards information into local file.");
            return false;
        }
        
        return true;
    }
    
    private boolean dumpBoardsInfo(List<BoardInfo> boardsInfo, BufferedWriter writer) throws IOException {
        
        for (BoardInfo board : boardsInfo) {
            writer.append("" + board.boardId + ";");
            writer.append(board.boardType  + ";");
            writer.append("\"" +  board.serial + "\";");
            writer.append("\"" + board.fwRevision + "\"");
            writer.newLine();
        }
        
        writer.newLine();
        
        return true;
    }
    
    private boolean dumpSensorsInfo(List<SensorConfig> sensorsInfo, BufferedWriter writer) throws IOException {
        
        for (SensorConfig sensor : sensorsInfo) {
            writer.append("" + sensor.sensorId + ";");
            writer.append("\"" + sensor.name + "\";");
            writer.append("\"" + sensor.serial + "\";");
            writer.append("" + sensor.samplingPeriod + ";");
            writer.append("\"" + sensor.measurementUnits + "\";");
            if (sensor.enabled) {
                writer.append("\"enabled\"");
            } else {
                writer.append("\"disabled\"");
            }
            writer.newLine();
        }
        return true;
    }
}
