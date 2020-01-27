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

package airsenseur.dev.chemsensorhost.sensors;

/**
 * Holds the information for shields connected to the bus
 * @author marco
 */
public class SensorBoardInfo {
    
    private int boardId;
    private int numOfChannels;
    private int boardType;
    
    private final FieldConfig<String> serial = new FieldConfig<>("");
    private final FieldConfig<String> firmware = new FieldConfig<>("");

    public SensorBoardInfo() {
    }
    
    public SensorBoardInfo(int boardId, int boardType, int numOfChannels) {
        this.boardId = boardId;
        this.numOfChannels = numOfChannels;
        this.boardType = boardType;
    }
    
    /**
     * @return the boardId
     */
    public int getBoardId() {
        return boardId;
    }

    /**
     * @return the serial
     */
    public FieldConfig<String> getSerial() {
        return serial;
    }

    /**
     * @return the firmware
     */
    public FieldConfig<String>  getFirmware() {
        return firmware;
    }

    /**
     * @return the numOfChannels
     */
    public int getNumOfChannels() {
        return numOfChannels;
    }

    /**
     * @return the boardType
     */
    public int getBoardType() {
        return boardType;
    }
}
