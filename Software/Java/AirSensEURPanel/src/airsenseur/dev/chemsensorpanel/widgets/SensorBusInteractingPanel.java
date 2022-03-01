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

package airsenseur.dev.chemsensorpanel.widgets;

import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.exceptions.SensorBusException;

/**
 * Base class for all panels interacting with the sensor bus
 * @author marco
 */
public abstract class SensorBusInteractingPanel extends javax.swing.JPanel {
    
    protected ShieldProtocolLayer shieldProtocolLayer;
    protected int boardId = AppDataMessage.BOARD_ID_UNDEFINED;
    protected int channelId = 0;
    protected boolean connected = false;
    
    public void setShieldProtocolLayer(ShieldProtocolLayer shieldProtocolLayer) {
        this.shieldProtocolLayer = shieldProtocolLayer;
    }
    
    public void setBoardId(int boarId) {
        this.boardId = boarId;
    }
    
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
    
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    
    public abstract void storeToBoard() throws SensorBusException;
    public abstract void readFromBoard() throws SensorBusException;
    public abstract void evaluateRxMessage(AppDataMessage rxMessage);
    public abstract void onDataMessageFromConfiguration(AppDataMessage rxMessage);
}
