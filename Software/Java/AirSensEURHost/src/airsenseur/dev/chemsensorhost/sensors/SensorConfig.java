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

import airsenseur.dev.comm.AppDataMessage;

/**
 * Information about configuration features of a generic sensor connected to the system
 * @author marco
 */
public class SensorConfig {
    
    private final FieldConfig<String> name = new FieldConfig<>("");
    private final FieldConfig<String> serial = new FieldConfig<>("");
    private final FieldConfig<String> measurementUnits = new FieldConfig<>("");
    private final FieldConfig<String> mathExpression = new FieldConfig<>("");    
    private final FieldConfig<Integer> samplingPeriod = new FieldConfig<>(0);
    private final FieldConfig<Boolean> enabled = new FieldConfig<>(Boolean.TRUE);

    private int boardId = AppDataMessage.BOARD_ID_UNDEFINED;
    private int channel = 0;
    
    public void init(int boardId, int channel) {
        this.boardId = boardId;
        this.channel = channel;
    }

    /**
     * @return the name
     */
    public FieldConfig<String> getName() {
        return name;
    }

    /**
     * @return the serial
     */
    public FieldConfig<String> getSerial() {
        return serial;
    }

    /**
     * @return the boardId
     */
    public int getBoardId() {
        return boardId;
    }

    /**
     * @return the channel
     */
    public int getChannel() {
        return channel;
    }

    /**
     * @return the samplingPeriod
     */
    public FieldConfig<Integer> getSamplingPeriod() {
        return samplingPeriod;
    }

    /**
     * @return the mathExpression
     */
    public FieldConfig<String> getMathExpression() {
        return mathExpression;
    }

    /**
     * @return the measurementUnits
     */
    public FieldConfig<String> getMeasurementUnits() {
        return measurementUnits;
    }

    /**
     * @return the enabled
     */
    public FieldConfig<Boolean> getEnabled() {
        return enabled;
    }
    
    
}
