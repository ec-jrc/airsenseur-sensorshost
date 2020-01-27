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

import airsenseur.dev.comm.AppDataMessage;

/**
 * Host configuration informations associated to a single channel/sensor.
 * @author marco
 */
public class HostConfigSensorProperties {
    
    protected String sensorName = "";
    protected String sensorExpression = "x";
    protected int sensorBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
    protected int sensorChannel = 0;

    /**
     * @param sensorName the sensorName to set
     */
    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    /**
     * @param sensorExpression the sensorExpression to set
     */
    public void setSensorExpression(String sensorExpression) {
        this.sensorExpression = sensorExpression;
    }

    /**
     * @param sensorBoardId the sensorBoardId to set
     */
    public void setSensorBoardId(int sensorBoardId) {
        this.sensorBoardId = sensorBoardId;
    }

    /**
     * @param sensorChannel the sensorChannel to set
     */
    public void setSensorChannel(int sensorChannel) {
        this.sensorChannel = sensorChannel;
    }
}
