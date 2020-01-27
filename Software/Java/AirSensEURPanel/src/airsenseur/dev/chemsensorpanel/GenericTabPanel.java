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


package airsenseur.dev.chemsensorpanel;

import airsenseur.dev.chemsensorpanel.helpers.FileLogger;
import airsenseur.dev.chemsensorpanel.helpers.HostConfigWriter;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.exceptions.SensorBusException;
import java.util.List;

/**
 * The GenericTabPanel is the contents of tabs present in the main application frame.
 * Each tab is associated to a board typology connected to the SensorBus.
 * Each GenericTabPanel can have ancillary SensorSetupDialog(s) that should be registered
 * to the main application frame
 * @author marco
 */
public abstract class GenericTabPanel extends javax.swing.JPanel {
    
    protected FileLogger logger;
    protected ShieldProtocolLayer shieldProtocolLayer;
    
    // This is used only by the graphical composition tool in the Netbeans IDE
    public GenericTabPanel() {
    }
    
    public GenericTabPanel(ShieldProtocolLayer shieldProtocolLayer, FileLogger fileLogger) {
        this.logger = fileLogger;
        this.shieldProtocolLayer = shieldProtocolLayer;
    }
    
    // Mish gui actions
    public abstract void onRefreshTimer() throws SensorBusException;
    public abstract int getSelectedBoardId();
    public abstract boolean getIsEnabled();
    public abstract void setConnected(boolean connected);
    
    // Actions with remote board
    public abstract void onDataReceived(AppDataMessage rxMessage);
    
    public abstract void storeToBoard() throws SensorBusException;
    public abstract void readFromBoard() throws SensorBusException;
    
    public abstract void startSample() throws SensorBusException;
    public abstract void stopSample() throws SensorBusException;
    
    // External configuration management
    public abstract void onDataMessageFromConfiguration(AppDataMessage configurationMessage);
    public abstract void onGetCurrentConfiguation(boolean forceRestartSampling) throws SensorBusException;
    
    // Board device ID
    public abstract String getBoardSerialNumber() throws SensorBusException;
    
    // Preset management
    public abstract void onDatabaseChanged();
    
    // Sensor Setup Dialog management
    public abstract List<SensorSetupDialog> getSensorSetupDialogs();
    
    // Host Configuration file management
    public abstract void collectHostConfigurationInformation(HostConfigWriter hostConfigWriter);
}
