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
 * Spare utilities to check safety in Json
 * @author marco
 */
public class JsonFieldsUtils {
    
    public static BoardInfo safeCheck(BoardInfo boardInfo) {

        if (boardInfo != null) {
            boardInfo.boardType = safeString(boardInfo.boardType);
            boardInfo.fwRevision = safeString(boardInfo.fwRevision);
            boardInfo.serial = safeString(boardInfo.serial);
        }
        
        return boardInfo;
    }
    
    public static SensorConfig safeCheck(SensorConfig sensorConfig) {
        
        if (sensorConfig != null) {
            sensorConfig.name = safeString(sensorConfig.name);
            sensorConfig.serial = safeString(sensorConfig.serial);
            sensorConfig.measurementUnits = safeString(sensorConfig.measurementUnits);
        }
        
        return sensorConfig;
    }
    
    public static SampleData safeCheck(SampleData sampleData) {
        
        if (sampleData != null) {
            sampleData.name = safeString(sampleData.name);
            sampleData.serial = safeString(sampleData.serial);
        }
        
        return sampleData;
    }
    
    private static String safeString(String input) {
        
        input = input.trim();
        input = input.replaceAll("\n", "");
        input = input.replaceAll("\r", "");
        input = input.replaceAll("[^\\p{ASCII}]", "");
        
        return input;
    }
}
