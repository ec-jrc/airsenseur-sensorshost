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
import airsenseur.dev.chemsensorpanel.comm.RemoteConnectionDialog;
import airsenseur.dev.chemsensorpanel.comm.SensorBusCommunicationHandler;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.chemsensorpanel.comm.SerialConnectionDialog;
import airsenseur.dev.chemsensorpanel.exceptions.ChemSensorPanelException;
import airsenseur.dev.chemsensorpanel.helpers.HostConfigWriter;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.exceptions.SensorBusException;
import airsenseur.dev.helpers.FileConfigurationTypedSessions;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author marco
 */
public class AirSensEURPanel extends MainApplicationFrame implements WindowListener {
    
    // Some common static definitions
    private final static int REFRESH_TIMER_FOR_NETWORKED_DEVICES = 8000;
    private final static int REFRESH_TIMER_FOR_SERIAL_DEVICES = 1000;
    private final static int DEFAULT_DEBUG_VERSION_IN_SENSORPROPERTIES = 3;

    // The handler of a physical bus connecting sensor boards
    private final SensorBusCommunicationHandler sensorBusCommunicationHandler = new SensorBusCommunicationHandler(this);
    
    // The handler of a high level shield protocol engine 
    private final ShieldProtocolLayer shieldProtocolLayer = new ShieldProtocolLayer(sensorBusCommunicationHandler);
        
    // The panel for editing sensor's database
    private final SensorPresetManagerDialog sensorPresetManagerDialog = new SensorPresetManagerDialog(this, false);
    
    // Main tab panel list
    private final List<GenericTabPanel> tabPanelsList = new ArrayList<>();
    
    // TypedSessionsID in the configuration file, expected for each tab. 
    // NOTE: The array order NEEDs to match the tabPanelsList order
    private final Integer tabSessions[] = { ShieldProtocolLayer.CHEM_SHIELD_R3X_TYPE_ID,
                                            ShieldProtocolLayer.ENV1_SHIELD_R1X_TYPE_ID,
                                            ShieldProtocolLayer.EXP1_SHIELD_R1X_TYPE_ID, 
                                            ShieldProtocolLayer.EXP2_SHIELD_R1X_TYPE_ID
                                            };
    
    // We need to understand if the board has been connected or not
    private boolean connected = false;
        
    // We need to knwow if we're using Point to Point or SensorBusEnabled adapters
    private final boolean useMultiPoint = true;
    
    // Don't flood with error messages
    private boolean showingErrorDialog = false;
    
    // Timer to periodically update the sample diagrams
    private final Timer refreshTimer = new Timer(REFRESH_TIMER_FOR_NETWORKED_DEVICES, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            onRefreshTimer();
        }
    });
    
    // Timer to periodically take ownership (usefull on network connected devices)
    private final Timer refreshOwnership = new Timer(40000, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            onRefreshOwnershipTimer();
        }
    });
    
    // The file logger
    private final FileLogger logger = new FileLogger();
    
    // Configuration file and file chooser
    private final FileConfigurationTypedSessions configFile = new FileConfigurationTypedSessions();
    private final JFileChooser fChooser = new JFileChooser();
    
    // HashMap to retrieve menu items from window handlers
    private final Map<Window, JMenuItem> menuFromWindow = new HashMap<>();
    
    // Other irrelevant stuffs
    private final javax.swing.ImageIcon tickMark = new javax.swing.ImageIcon(getClass().getResource("/airsenseur/dev/chemsensorpanel/icons/accept.png"));
        
    /**
     * Creates new form ChemShieldPanel
     */
    public AirSensEURPanel() {
        
        initComponents();
        
        // Add genericTabPanels in a list for an easy access
        tabPanelsList.add(chemSensorPanel);
        tabPanelsList.add(envShield1Panel);
        tabPanelsList.add(expShield1Panel);
        tabPanelsList.add(expShield2Panel);
        
        // Populate links between menu items and windows
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_1), jMenuItemChemSetup1);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_2), jMenuItemChemSetup2);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_3), jMenuItemChemSetup3);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_4), jMenuItemChemSetup4);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.PRESS_SENSOR_CHANNEL_ID), jMenuItemPressSetup);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.TEMP_EXT_SENSOR_CHANNEL_ID), jMenuItemExtTempSetup);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.HUM_EXT_SENSOR_CHANNEL_ID), jMenuItemExtHumSetup);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.TEMP_INT_SENSOR_CHANNEL_ID), jMenuItemIntTempSetup);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.HUM_INT_SENSOR_CHANNEL_ID), jMenuItemIntHumSetup);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.GENERIC_INFO_CHANNEL_ID), jMenuChemShieldInfo);

        menuFromWindow.put(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.ENVSHIELD1_SETUP_DIALOG_AUDIOFAST), jMenuItemAudioFast);
        menuFromWindow.put(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.ENVSHIELD1_SETUP_DIALOG_AUDIOSLOW), jMenuItemAudioSlow);
        menuFromWindow.put(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.ENVSHIELD1_SETUP_TEMPERATURE_HUMIDITY), jMenuItemTemperatureHum);
        menuFromWindow.put(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.ENVSHIELD1_SETUP_OPT3001), jMenuItemOPT3001);
        menuFromWindow.put(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.GENERIC_INFO_CHANNEL_ID), jMenuEnvShieldInfo);
       
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_RD200M), jMenuItemRD200MSetup);
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_D300), jMenuItemD300Setup);
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_PMS5003), jMenuItemPMS5003Setup);
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_OPCN3), jMenuItemOPCN3Setup);
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_SPS30), jMenuItemSPS30Setup);
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_GENERIC_INFO), jMenuItemExpShieldInfo);
        
        menuFromWindow.put(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_D300), jMenuItemD300_Exp2Setup);        
        menuFromWindow.put(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_SHT31I), jMenuItemSHT31I_Exp2);        
        menuFromWindow.put(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_SHT31E), jMenuItemSHT31E_Exp2);        
        menuFromWindow.put(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_INTAD), jMenuItemAnFbk_Exp2);        
        menuFromWindow.put(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_PID), jMenuItemPID_HC_Exp2);        
        menuFromWindow.put(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_ADT7470), jMenuItemTAndFan_Exp2);        
        menuFromWindow.put(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_K96), jMenuItemK96_Exp2);        
        menuFromWindow.put(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_GENERIC_INFO), jMenuItemExpShield2Info);        
        
        menuFromWindow.put(sensorPresetManagerDialog, jMenuItemSensorDBEdit);
        
        // Add myself as a window listener so I'll be able to update the
        // menu status even if the user is closing a panel from the related window button
        for (SensorSetupDialog dialog:chemSensorPanel.getSensorSetupDialogs()) {
            dialog.addWindowListener(this);
        }
        for (SensorSetupDialog dialog:envShield1Panel.getSensorSetupDialogs()) {
            dialog.addWindowListener(this);
        }
        for (SensorSetupDialog dialog:expShield1Panel.getSensorSetupDialogs()) {
            dialog.addWindowListener(this);
        }
        for (SensorSetupDialog dialog:expShield2Panel.getSensorSetupDialogs()) {
            dialog.addWindowListener(this);
        }
        sensorPresetManagerDialog.addWindowListener(this);
                
        // Setup a callback for preset database change propagation events
        sensorPresetManagerDialog.registerCallBack(new SensorPresetManagerDialog.CallBack() {

            @Override
            public void onDatabaseChanged() {
                for (GenericTabPanel panel:tabPanelsList) {
                    panel.onDatabaseChanged();
                }
            }
        });
        
        // Start the refresh timer
        refreshTimer.setRepeats(true);
        refreshTimer.setInitialDelay(REFRESH_TIMER_FOR_NETWORKED_DEVICES);
        refreshTimer.setDelay(REFRESH_TIMER_FOR_NETWORKED_DEVICES);
        refreshTimer.start();
        
        // Start the refresh ownership timer
        refreshOwnership.setRepeats(true);
        refreshOwnership.start();
        
        // Initialize the configuration file chooser
        File workingDirectory = new File(System.getProperty("user.dir"));
        fChooser.setCurrentDirectory(workingDirectory);
        fChooser.setFileFilter(new FileNameExtensionFilter("AirSensEUR Configuration Files (*.asc)", "asc"));
        fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fChooser.setMultiSelectionEnabled(false);     
    }
    
    private void onRefreshTimer() {
        
        if (!connected) {
            return;
        }
        try {
            for (GenericTabPanel panel:tabPanelsList) {
                panel.onRefreshTimer();
            }
        } catch (SensorBusException ex) {
            signalErrorAndDisconnect("Communication with remote board was lost.", ex);            
        }
    }
    
    private void onRefreshOwnershipTimer() {
        
        if (!connected) {
            return;
        }
        try {
            sensorBusCommunicationHandler.takeBusOwnership();
        } catch (SensorBusException ex) {
            
            signalErrorAndDisconnect("Communication with remote board was lost.", ex);
       }
    }
    
    private void signalErrorAndDisconnect(String error, Exception ex) {
        
        sensorBusCommunicationHandler.disConnectFromBus();
        setConnected(false, false);

        if (!showingErrorDialog) {
            showingErrorDialog = true;
            if (ex != null) {
                error += ex.getMessage();
            }
            JOptionPane.showMessageDialog(rootPane, error, "Connection Error", JOptionPane.ERROR_MESSAGE);
            showingErrorDialog = false;
        }
    }
    
    public void onDataReceived(AppDataMessage rxMessage) {

        for (GenericTabPanel panel:tabPanelsList) {
            panel.onDataReceived(rxMessage);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        chemSensorPanel = new airsenseur.dev.chemsensorpanel.ChemShieldPanel(this, shieldProtocolLayer, logger)
        ;
        expShield1Panel = new airsenseur.dev.chemsensorpanel.ExpShield1Panel(this, shieldProtocolLayer, logger);
        expShield2Panel = new airsenseur.dev.chemsensorpanel.ExpShield2Panel(this, shieldProtocolLayer, logger);
        envShield1Panel = new airsenseur.dev.chemsensorpanel.EnvShield1Panel(this, shieldProtocolLayer, logger);
        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemSaveConfig = new javax.swing.JMenuItem();
        jMenuItemReadConfig = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenuItemSaveSensorProperties = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jMenuItemAbout = new javax.swing.JMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuSensorBus = new javax.swing.JMenu();
        jMenuItemConnectSerial = new javax.swing.JMenuItem();
        jMenuItemConnectNetwork = new javax.swing.JMenuItem();
        jMenuItemDisconnect = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuItemReadFromBoard = new javax.swing.JMenuItem();
        jMenuItemWriteToBoard = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemStartSampling = new javax.swing.JMenuItem();
        jMenuItemStopSampling = new javax.swing.JMenuItem();
        jMenuChemSensors = new javax.swing.JMenu();
        jMenuItemChemSetup1 = new javax.swing.JMenuItem();
        jMenuItemChemSetup2 = new javax.swing.JMenuItem();
        jMenuItemChemSetup3 = new javax.swing.JMenuItem();
        jMenuItemChemSetup4 = new javax.swing.JMenuItem();
        jMenuItemPressSetup = new javax.swing.JMenuItem();
        jMenuItemExtTempSetup = new javax.swing.JMenuItem();
        jMenuItemExtHumSetup = new javax.swing.JMenuItem();
        jMenuItemIntTempSetup = new javax.swing.JMenuItem();
        jMenuItemIntHumSetup = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        jMenuChemShieldInfo = new javax.swing.JMenuItem();
        jMenuExpShield1 = new javax.swing.JMenu();
        jMenuItemRD200MSetup = new javax.swing.JMenuItem();
        jMenuItemD300Setup = new javax.swing.JMenuItem();
        jMenuItemPMS5003Setup = new javax.swing.JMenuItem();
        jMenuItemOPCN3Setup = new javax.swing.JMenuItem();
        jMenuItemSPS30Setup = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        jMenuItemExpShieldInfo = new javax.swing.JMenuItem();
        jMenuExpShield2 = new javax.swing.JMenu();
        jMenuItemSHT31I_Exp2 = new javax.swing.JMenuItem();
        jMenuItemSHT31E_Exp2 = new javax.swing.JMenuItem();
        jMenuItemAnFbk_Exp2 = new javax.swing.JMenuItem();
        jMenuItemPID_HC_Exp2 = new javax.swing.JMenuItem();
        jMenuItemTAndFan_Exp2 = new javax.swing.JMenuItem();
        jMenuItemD300_Exp2Setup = new javax.swing.JMenuItem();
        jMenuItemK96_Exp2 = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        jMenuItemExpShield2Info = new javax.swing.JMenuItem();
        jMenuEnvShield1 = new javax.swing.JMenu();
        jMenuItemAudioFast = new javax.swing.JMenuItem();
        jMenuItemAudioSlow = new javax.swing.JMenuItem();
        jMenuItemTemperatureHum = new javax.swing.JMenuItem();
        jMenuItemOPT3001 = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        jMenuEnvShieldInfo = new javax.swing.JMenuItem();
        jMenuSensorsDB = new javax.swing.JMenu();
        jMenuItemSensorDBEdit = new javax.swing.JMenuItem();

        jMenuItem1.setText("jMenuItem1");

        jMenuItem2.setText("jMenuItem2");

        jMenuItem3.setText("jMenuItem3");

        jMenuItem4.setText("jMenuItem4");

        jMenuItem5.setText("jMenuItem5");

        jMenuItem6.setText("jMenuItem6");

        jMenuItem7.setText("jMenuItem7");

        jMenuItem8.setText("jMenuItem8");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("AirSensEUR Panel");
        setLocationByPlatform(true);

        jTabbedPane1.addTab("Chemical Sensor Shield", chemSensorPanel);
        jTabbedPane1.addTab("ExpShield1", expShield1Panel);
        jTabbedPane1.addTab("ExpShield2", expShield2Panel);
        jTabbedPane1.addTab("EnvShield1", envShield1Panel);

        jMenuFile.setText("File");

        jMenuItemSaveConfig.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        jMenuItemSaveConfig.setText("Save Config");
        jMenuItemSaveConfig.setToolTipText("Save configuration to file");
        jMenuItemSaveConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveConfigActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveConfig);

        jMenuItemReadConfig.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        jMenuItemReadConfig.setText("Read Config");
        jMenuItemReadConfig.setToolTipText("Read configuration from file");
        jMenuItemReadConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemReadConfigActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemReadConfig);
        jMenuFile.add(jSeparator5);

        jMenuItemSaveSensorProperties.setText("Save sensor.properties");
        jMenuItemSaveSensorProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveSensorPropertiesActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveSensorProperties);
        jMenuFile.add(jSeparator3);

        jMenuItemAbout.setText("About");
        jMenuItemAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAboutActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemAbout);

        jMenuItemExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        jMenuItemExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/airsenseur/dev/chemsensorpanel/icons/door_out.png"))); // NOI18N
        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExit);

        jMenuBar.add(jMenuFile);

        jMenuSensorBus.setText("Sensor Bus");

        jMenuItemConnectSerial.setIcon(new javax.swing.ImageIcon(getClass().getResource("/airsenseur/dev/chemsensorpanel/icons/connect.png"))); // NOI18N
        jMenuItemConnectSerial.setText("Connect via Serial Line");
        jMenuItemConnectSerial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectSerialActionPerformed(evt);
            }
        });
        jMenuSensorBus.add(jMenuItemConnectSerial);

        jMenuItemConnectNetwork.setIcon(new javax.swing.ImageIcon(getClass().getResource("/airsenseur/dev/chemsensorpanel/icons/connect.png"))); // NOI18N
        jMenuItemConnectNetwork.setText("Connect via Network");
        jMenuItemConnectNetwork.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectNetworkActionPerformed(evt);
            }
        });
        jMenuSensorBus.add(jMenuItemConnectNetwork);

        jMenuItemDisconnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/airsenseur/dev/chemsensorpanel/icons/disconnect.png"))); // NOI18N
        jMenuItemDisconnect.setText("Disconnect");
        jMenuItemDisconnect.setEnabled(false);
        jMenuItemDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDisconnectActionPerformed(evt);
            }
        });
        jMenuSensorBus.add(jMenuItemDisconnect);
        jMenuSensorBus.add(jSeparator2);

        jMenuItemReadFromBoard.setText("Read From Board");
        jMenuItemReadFromBoard.setEnabled(false);
        jMenuItemReadFromBoard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemReadFromBoardActionPerformed(evt);
            }
        });
        jMenuSensorBus.add(jMenuItemReadFromBoard);

        jMenuItemWriteToBoard.setText("Write To Board");
        jMenuItemWriteToBoard.setEnabled(false);
        jMenuItemWriteToBoard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemWriteToBoardActionPerformed(evt);
            }
        });
        jMenuSensorBus.add(jMenuItemWriteToBoard);
        jMenuSensorBus.add(jSeparator1);

        jMenuItemStartSampling.setText("Start sampling");
        jMenuItemStartSampling.setEnabled(false);
        jMenuItemStartSampling.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartSamplingActionPerformed(evt);
            }
        });
        jMenuSensorBus.add(jMenuItemStartSampling);

        jMenuItemStopSampling.setText("Stop sampling");
        jMenuItemStopSampling.setEnabled(false);
        jMenuItemStopSampling.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStopSamplingActionPerformed(evt);
            }
        });
        jMenuSensorBus.add(jMenuItemStopSampling);

        jMenuBar.add(jMenuSensorBus);

        jMenuChemSensors.setText("Chemical Shield");

        jMenuItemChemSetup1.setText("ChemSensor 1 Setup");
        jMenuItemChemSetup1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChemSetup1ActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemChemSetup1);

        jMenuItemChemSetup2.setText("ChemSensor 2 Setup");
        jMenuItemChemSetup2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChemSetup2ActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemChemSetup2);

        jMenuItemChemSetup3.setText("ChemSensor 3 Setup");
        jMenuItemChemSetup3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChemSetup3ActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemChemSetup3);

        jMenuItemChemSetup4.setText("ChemSensor 4 Setup");
        jMenuItemChemSetup4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChemSetup4ActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemChemSetup4);

        jMenuItemPressSetup.setText("Pressure Sensor Setup");
        jMenuItemPressSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPressSetupActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemPressSetup);

        jMenuItemExtTempSetup.setText("External Temperature Sensor Setup");
        jMenuItemExtTempSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExtTempSetupActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemExtTempSetup);

        jMenuItemExtHumSetup.setText("External Humidity Sensor Setup");
        jMenuItemExtHumSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExtHumSetupActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemExtHumSetup);

        jMenuItemIntTempSetup.setText("Internal Temperature Sensor Setup");
        jMenuItemIntTempSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemIntTempSetupActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemIntTempSetup);

        jMenuItemIntHumSetup.setText("Internal Humidity Sensor Setup");
        jMenuItemIntHumSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemIntHumSetupActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemIntHumSetup);
        jMenuChemSensors.add(jSeparator6);

        jMenuChemShieldInfo.setText("Generic Information");
        jMenuChemShieldInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuChemShieldInfoActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuChemShieldInfo);

        jMenuBar.add(jMenuChemSensors);

        jMenuExpShield1.setText("ExpShield1");

        jMenuItemRD200MSetup.setText("RD200M");
        jMenuItemRD200MSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRD200MSetupActionPerformed(evt);
            }
        });
        jMenuExpShield1.add(jMenuItemRD200MSetup);

        jMenuItemD300Setup.setText("D300");
        jMenuItemD300Setup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemD300SetupActionPerformed(evt);
            }
        });
        jMenuExpShield1.add(jMenuItemD300Setup);

        jMenuItemPMS5003Setup.setText("PMS5003");
        jMenuItemPMS5003Setup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPMS5003SetupActionPerformed(evt);
            }
        });
        jMenuExpShield1.add(jMenuItemPMS5003Setup);

        jMenuItemOPCN3Setup.setText("OPC-N3");
        jMenuItemOPCN3Setup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOPCN3SetupActionPerformed(evt);
            }
        });
        jMenuExpShield1.add(jMenuItemOPCN3Setup);

        jMenuItemSPS30Setup.setText("SPS30");
        jMenuItemSPS30Setup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSPS30SetupActionPerformed(evt);
            }
        });
        jMenuExpShield1.add(jMenuItemSPS30Setup);
        jMenuExpShield1.add(jSeparator4);

        jMenuItemExpShieldInfo.setText("Generic Information");
        jMenuItemExpShieldInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExpShieldInfoActionPerformed(evt);
            }
        });
        jMenuExpShield1.add(jMenuItemExpShieldInfo);

        jMenuBar.add(jMenuExpShield1);

        jMenuExpShield2.setText("ExpShield2");

        jMenuItemSHT31I_Exp2.setText("Int Temp/Umid");
        jMenuItemSHT31I_Exp2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSHT31I_Exp2ActionPerformed(evt);
            }
        });
        jMenuExpShield2.add(jMenuItemSHT31I_Exp2);

        jMenuItemSHT31E_Exp2.setText("Ext Temp/Umid");
        jMenuItemSHT31E_Exp2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSHT31E_Exp2ActionPerformed(evt);
            }
        });
        jMenuExpShield2.add(jMenuItemSHT31E_Exp2);

        jMenuItemAnFbk_Exp2.setText("Analog Fbk");
        jMenuItemAnFbk_Exp2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAnFbk_Exp2ActionPerformed(evt);
            }
        });
        jMenuExpShield2.add(jMenuItemAnFbk_Exp2);

        jMenuItemPID_HC_Exp2.setText("PID Heater/Cooler");
        jMenuItemPID_HC_Exp2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPID_HC_Exp2ActionPerformed(evt);
            }
        });
        jMenuExpShield2.add(jMenuItemPID_HC_Exp2);

        jMenuItemTAndFan_Exp2.setText("Temp/Fans");
        jMenuItemTAndFan_Exp2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTAndFan_Exp2ActionPerformed(evt);
            }
        });
        jMenuExpShield2.add(jMenuItemTAndFan_Exp2);

        jMenuItemD300_Exp2Setup.setText("D300");
        jMenuItemD300_Exp2Setup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemD300_Exp2SetupActionPerformed(evt);
            }
        });
        jMenuExpShield2.add(jMenuItemD300_Exp2Setup);

        jMenuItemK96_Exp2.setText("K96");
        jMenuItemK96_Exp2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemK96_Exp2ActionPerformed(evt);
            }
        });
        jMenuExpShield2.add(jMenuItemK96_Exp2);
        jMenuExpShield2.add(jSeparator8);

        jMenuItemExpShield2Info.setText("Generic Information");
        jMenuItemExpShield2Info.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExpShield2InfoActionPerformed(evt);
            }
        });
        jMenuExpShield2.add(jMenuItemExpShield2Info);

        jMenuBar.add(jMenuExpShield2);

        jMenuEnvShield1.setText("EnvShield1");

        jMenuItemAudioFast.setText("Audio Fast");
        jMenuItemAudioFast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAudioFastActionPerformed(evt);
            }
        });
        jMenuEnvShield1.add(jMenuItemAudioFast);

        jMenuItemAudioSlow.setText("Audio Slow");
        jMenuItemAudioSlow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAudioSlowActionPerformed(evt);
            }
        });
        jMenuEnvShield1.add(jMenuItemAudioSlow);

        jMenuItemTemperatureHum.setText("Temp/Humid");
        jMenuItemTemperatureHum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTemperatureHumActionPerformed(evt);
            }
        });
        jMenuEnvShield1.add(jMenuItemTemperatureHum);

        jMenuItemOPT3001.setText("Light (OPT3001)");
        jMenuItemOPT3001.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOPT3001ActionPerformed(evt);
            }
        });
        jMenuEnvShield1.add(jMenuItemOPT3001);
        jMenuEnvShield1.add(jSeparator7);

        jMenuEnvShieldInfo.setText("Generic Information");
        jMenuEnvShieldInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuEnvShieldInfoActionPerformed(evt);
            }
        });
        jMenuEnvShield1.add(jMenuEnvShieldInfo);

        jMenuBar.add(jMenuEnvShield1);

        jMenuSensorsDB.setText("Database");

        jMenuItemSensorDBEdit.setText("Sensors Database Editor");
        jMenuItemSensorDBEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSensorDBEditActionPerformed(evt);
            }
        });
        jMenuSensorsDB.add(jMenuItemSensorDBEdit);

        jMenuBar.add(jMenuSensorsDB);

        setJMenuBar(jMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemConnectSerialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConnectSerialActionPerformed
        
        // Counts how many boards are enabled by the ID dropdown 
        int boardsEnabled = 0;
        for (GenericTabPanel panel : tabPanelsList) {
            if (panel.getIsEnabled()) {
                boardsEnabled++;
            }
        }

        SerialConnectionDialog connectionDialog = new SerialConnectionDialog(this, boardsEnabled, true);
        connectionDialog.init(sensorBusCommunicationHandler);
        connectionDialog.setVisible(true);
        
        setConnected(connectionDialog.isConnected(), false);
    }//GEN-LAST:event_jMenuItemConnectSerialActionPerformed

    private void jMenuItemDisconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDisconnectActionPerformed
        
        sensorBusCommunicationHandler.disConnectFromBus();
        setConnected(false, false);
    }//GEN-LAST:event_jMenuItemDisconnectActionPerformed

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed

        if (!connected) {
            System.exit(0);
        } else {
            
            if (javax.swing.JOptionPane.showConfirmDialog(this, 
                    "Are you sure to disconnect from the board and exit?", 
                    "Warning", javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION) {
                
                sensorBusCommunicationHandler.disConnectFromBus();
                System.exit(0);
            }
        }
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItemChemSetup2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChemSetup2ActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_2));
    }//GEN-LAST:event_jMenuItemChemSetup2ActionPerformed

    private void jMenuItemChemSetup1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChemSetup1ActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_1));
    }//GEN-LAST:event_jMenuItemChemSetup1ActionPerformed

    private void jMenuItemChemSetup3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChemSetup3ActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_3));
    }//GEN-LAST:event_jMenuItemChemSetup3ActionPerformed

    private void jMenuItemChemSetup4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChemSetup4ActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_4));
    }//GEN-LAST:event_jMenuItemChemSetup4ActionPerformed

    private void jMenuItemPressSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPressSetupActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.PRESS_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemPressSetupActionPerformed

    private void jMenuItemExtTempSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExtTempSetupActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.TEMP_EXT_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemExtTempSetupActionPerformed

    private void jMenuItemExtHumSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExtHumSetupActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.HUM_EXT_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemExtHumSetupActionPerformed

    private void jMenuItemWriteToBoardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemWriteToBoardActionPerformed
        
        if (connected) {
            refreshTimer.stop();
            
            try {            
                for (GenericTabPanel panel:tabPanelsList) {
                        panel.storeToBoard();
                }
            } catch (SensorBusException ex) {
                signalErrorAndDisconnect("Communication with remote board was lost.", ex);
            }
            
            refreshTimer.start();
        }
    }//GEN-LAST:event_jMenuItemWriteToBoardActionPerformed

    private void jMenuItemReadFromBoardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemReadFromBoardActionPerformed
        
        if (connected) {
            refreshTimer.stop();
            
            try {
                for (GenericTabPanel panel:tabPanelsList) {
                    panel.readFromBoard();
                }
            } catch (SensorBusException ex) {
                signalErrorAndDisconnect("Communication with remote board was lost.", ex);
            }
            
            refreshTimer.start();
        }
    }//GEN-LAST:event_jMenuItemReadFromBoardActionPerformed

    private void jMenuItemStartSamplingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartSamplingActionPerformed
        
        if (connected) {

            try {
                // The filename will contain all known board IDs and serial numbers
                StringBuilder boardIDs = new StringBuilder();
                boardIDs.append("Samples");
                for (GenericTabPanel panel:tabPanelsList) {

                    if (panel.getIsEnabled()) {
                        String serialBoard = panel.getBoardSerialNumber();
                        String boardId = "" + panel.getSelectedBoardId();

                        if (serialBoard.isEmpty()) {
                            serialBoard = "NA";
                        }

                        boardIDs.append("_(").append(boardId).append("_").append(serialBoard).append(")");
                    }
                }            

                // Start a new logger
                if (logger != null) {
                    String fileName = boardIDs.toString();
                    logger.openFile(fileName);
                }

                for (GenericTabPanel panel:tabPanelsList) {
                    panel.startSample();
                }
                
            } catch (SensorBusException ex) {
                signalErrorAndDisconnect("Communication with remote board was lost.", ex);                
            }
        }
    }//GEN-LAST:event_jMenuItemStartSamplingActionPerformed

    private void jMenuItemStopSamplingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStopSamplingActionPerformed
        
        if (connected) {

            try {
                for (GenericTabPanel panel:tabPanelsList) {
                    panel.stopSample();
                }
            } catch (SensorBusException ex) {
                signalErrorAndDisconnect("Communication with remote board was lost.", ex);
            }
            
            // Close the log file, if any
            if (logger != null) {
                logger.closeFile();
            }
        }
    }//GEN-LAST:event_jMenuItemStopSamplingActionPerformed

    private void jMenuItemSaveConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveConfigActionPerformed

        // Retrieve the file name
        File selectedFile;
        int returnVal = fChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fChooser.getSelectedFile();
        } else {
            return;
        }
        
        if ((selectedFile == null) || (selectedFile.isDirectory())) {
            return;
        }
        
        // Force the extension, if any
        if(!selectedFile.getAbsolutePath().toLowerCase().endsWith(".asc") ) {
            selectedFile = new File(selectedFile.getAbsolutePath() + ".asc");        
        }
        
        // Save to the configuration file
        configFile.openFile(selectedFile, false);
        for (int tabIndex = 0; tabIndex < tabPanelsList.size(); tabIndex++) {
            
            configFile.generateBoardSession(tabSessions[tabIndex]);
            configFile.appendCommands(getCurrentConfiguration(tabIndex, false));
        }

        configFile.closeFile();
    }//GEN-LAST:event_jMenuItemSaveConfigActionPerformed

    private void jMenuItemReadConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemReadConfigActionPerformed

        // Retrieve the file name
        File selectedFile;
        int returnVal = fChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fChooser.getSelectedFile();
        } else {
            return;
        }
        
        if ((selectedFile == null) || (selectedFile.isDirectory())) {
            return;
        }
        
        // Prevent timer interactions
        boolean connectedStatus = connected;
        connected = false;
        
        // Read the typedSession format configuration file
        configFile.openFile(selectedFile, true);        
        for (int tab = 0; tab < tabPanelsList.size(); tab++) {
            
            AppDataMessage configurationMessage;
            Integer tabSession = tabSessions[tab];
            while ((configurationMessage = configFile.getNextCommand(tabSession)) != null) {

                // The configuration file is a container of commands set to the
                // board in order to properly set it up. When reading, we need to 
                // convert the "set" commands stored in the configuration file like if
                // the board was "reading" their status. By performing this convertion,
                // the whole java panel set can update with the same procedure used when 
                // receiving messages from the board.
                shieldProtocolLayer.toAnswerCommandString(configurationMessage);
                
                // Propagate to the proper tab
                tabPanelsList.get(tab).onDataMessageFromConfiguration(configurationMessage);
            }
        }
        
        configFile.closeFile();
        
        // Enable time refresh, if required
        connected = connectedStatus;
    }//GEN-LAST:event_jMenuItemReadConfigActionPerformed

    private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAboutActionPerformed
        
        AboutDialog about = new AboutDialog(this, true);
        about.setVisible(true);
    }//GEN-LAST:event_jMenuItemAboutActionPerformed

    private void jMenuItemSensorDBEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSensorDBEditActionPerformed
        updateMenuItemVisibilityForDialog(sensorPresetManagerDialog);
    }//GEN-LAST:event_jMenuItemSensorDBEditActionPerformed

    private void jMenuItemConnectNetworkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConnectNetworkActionPerformed
        RemoteConnectionDialog connectionDialog = new RemoteConnectionDialog(this, true);
        connectionDialog.init(sensorBusCommunicationHandler);
        connectionDialog.setVisible(true);
        
        setConnected(connectionDialog.isConnected(), true);
    }//GEN-LAST:event_jMenuItemConnectNetworkActionPerformed

    private void jMenuItemSaveSensorPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveSensorPropertiesActionPerformed
        // Retrieve the file name
        File selectedFile;
        JFileChooser fChooserSensorProperties = new JFileChooser();
        fChooserSensorProperties.setFileFilter(new FileNameExtensionFilter("AirSensEUR Host Configuration Files (*.properties)", "properties"));
        fChooserSensorProperties.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fChooserSensorProperties.setMultiSelectionEnabled(false);     

        int returnVal = fChooserSensorProperties.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fChooserSensorProperties.getSelectedFile();
        } else {
            return;
        }
        
        if ((selectedFile == null) || (selectedFile.isDirectory())) {
            return;
        }
        
        // Force the extension, if any
        if(!selectedFile.getAbsolutePath().toLowerCase().endsWith(".properties") ) {
            selectedFile = new File(selectedFile.getAbsolutePath() + ".properties");        
        }
        
        // Collect information
        HostConfigWriter hostConfigWriter = new HostConfigWriter();
        
        hostConfigWriter.setDebugVerbose(DEFAULT_DEBUG_VERSION_IN_SENSORPROPERTIES);
        hostConfigWriter.setUseBusProtocol(useMultiPoint);
        
        for (GenericTabPanel tabPanel:tabPanelsList) {
            if (tabPanel.getIsEnabled()) {
                tabPanel.collectHostConfigurationInformation(hostConfigWriter);
            }
        }
        
        // Save the file
        try {
            hostConfigWriter.generateConfigFile(selectedFile);
        } catch (ChemSensorPanelException ex) {
            JOptionPane.showMessageDialog(rootPane, "Error generating the properties file");
        } 
        
        JOptionPane.showMessageDialog(rootPane, "File saved");
    }//GEN-LAST:event_jMenuItemSaveSensorPropertiesActionPerformed

    private void jMenuChemShieldInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuChemShieldInfoActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.GENERIC_INFO_CHANNEL_ID));
    }//GEN-LAST:event_jMenuChemShieldInfoActionPerformed

    private void jMenuItemPMS5003SetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPMS5003SetupActionPerformed
        updateMenuItemVisibilityForDialog(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_PMS5003));
    }//GEN-LAST:event_jMenuItemPMS5003SetupActionPerformed

    private void jMenuItemRD200MSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRD200MSetupActionPerformed
        updateMenuItemVisibilityForDialog(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_RD200M));
    }//GEN-LAST:event_jMenuItemRD200MSetupActionPerformed

    private void jMenuItemD300SetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemD300SetupActionPerformed
        updateMenuItemVisibilityForDialog(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_D300));
    }//GEN-LAST:event_jMenuItemD300SetupActionPerformed

    private void jMenuItemOPCN3SetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOPCN3SetupActionPerformed
        updateMenuItemVisibilityForDialog(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_OPCN3));
    }//GEN-LAST:event_jMenuItemOPCN3SetupActionPerformed

    private void jMenuItemExpShieldInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExpShieldInfoActionPerformed
        updateMenuItemVisibilityForDialog(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_GENERIC_INFO));
    }//GEN-LAST:event_jMenuItemExpShieldInfoActionPerformed

    private void jMenuItemIntTempSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemIntTempSetupActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.TEMP_INT_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemIntTempSetupActionPerformed

    private void jMenuItemIntHumSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemIntHumSetupActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.HUM_INT_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemIntHumSetupActionPerformed

    private void jMenuItemAudioFastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAudioFastActionPerformed
        updateMenuItemVisibilityForDialog(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.ENVSHIELD1_SETUP_DIALOG_AUDIOFAST));
    }//GEN-LAST:event_jMenuItemAudioFastActionPerformed

    private void jMenuItemAudioSlowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAudioSlowActionPerformed
        updateMenuItemVisibilityForDialog(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.ENVSHIELD1_SETUP_DIALOG_AUDIOSLOW));
    }//GEN-LAST:event_jMenuItemAudioSlowActionPerformed

    private void jMenuItemTemperatureHumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTemperatureHumActionPerformed
        updateMenuItemVisibilityForDialog(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.ENVSHIELD1_SETUP_TEMPERATURE_HUMIDITY));
    }//GEN-LAST:event_jMenuItemTemperatureHumActionPerformed

    private void jMenuItemOPT3001ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOPT3001ActionPerformed
        updateMenuItemVisibilityForDialog(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.ENVSHIELD1_SETUP_OPT3001));
    }//GEN-LAST:event_jMenuItemOPT3001ActionPerformed

    private void jMenuEnvShieldInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuEnvShieldInfoActionPerformed
        updateMenuItemVisibilityForDialog(envShield1Panel.getSensorSetupDialogs().get(EnvShield1Panel.GENERIC_INFO_CHANNEL_ID));
    }//GEN-LAST:event_jMenuEnvShieldInfoActionPerformed

    private void jMenuItemSPS30SetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSPS30SetupActionPerformed
        updateMenuItemVisibilityForDialog(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_SPS30));
    }//GEN-LAST:event_jMenuItemSPS30SetupActionPerformed

    private void jMenuItemD300_Exp2SetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemD300_Exp2SetupActionPerformed
        updateMenuItemVisibilityForDialog(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_D300));
    }//GEN-LAST:event_jMenuItemD300_Exp2SetupActionPerformed

    private void jMenuItemSHT31I_Exp2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSHT31I_Exp2ActionPerformed
        updateMenuItemVisibilityForDialog(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_SHT31I));
    }//GEN-LAST:event_jMenuItemSHT31I_Exp2ActionPerformed

    private void jMenuItemSHT31E_Exp2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSHT31E_Exp2ActionPerformed
        updateMenuItemVisibilityForDialog(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_SHT31E));
    }//GEN-LAST:event_jMenuItemSHT31E_Exp2ActionPerformed

    private void jMenuItemAnFbk_Exp2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAnFbk_Exp2ActionPerformed
        updateMenuItemVisibilityForDialog(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_INTAD));
    }//GEN-LAST:event_jMenuItemAnFbk_Exp2ActionPerformed

    private void jMenuItemPID_HC_Exp2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPID_HC_Exp2ActionPerformed
        updateMenuItemVisibilityForDialog(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_PID));
    }//GEN-LAST:event_jMenuItemPID_HC_Exp2ActionPerformed

    private void jMenuItemTAndFan_Exp2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTAndFan_Exp2ActionPerformed
        updateMenuItemVisibilityForDialog(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_ADT7470));
    }//GEN-LAST:event_jMenuItemTAndFan_Exp2ActionPerformed

    private void jMenuItemK96_Exp2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemK96_Exp2ActionPerformed
        updateMenuItemVisibilityForDialog(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_DIALOG_K96));
    }//GEN-LAST:event_jMenuItemK96_Exp2ActionPerformed

    private void jMenuItemExpShield2InfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExpShield2InfoActionPerformed
        updateMenuItemVisibilityForDialog(expShield2Panel.getSensorSetupDialogs().get(ExpShield2Panel.EXPSHIELD2_SETUP_GENERIC_INFO));
    }//GEN-LAST:event_jMenuItemExpShield2InfoActionPerformed

    protected List<AppDataMessage> getCurrentConfiguration(int tabPanelIndex, boolean forceRestartSampling) {
        
        List<AppDataMessage> currentConfiguration = new ArrayList<>();
        
        if (tabPanelIndex < tabPanelsList.size()) {
        
            // Prevent timer interactions
            boolean connectedStatus = connected;
            connected = false;

            // Start dumping to buffer
            sensorBusCommunicationHandler.startDumpingToBuffer(currentConfiguration);

            try {
                // Propagate to the selected panel
                tabPanelsList.get(tabPanelIndex).onGetCurrentConfiguation(forceRestartSampling);
            } catch (SensorBusException ex) {
            }

            // Restore normal communication
            sensorBusCommunicationHandler.stopDumpingToBuffer();

            // Enable time refresh, if required
            connected = connectedStatus;
        }
        
        return currentConfiguration;
    }
            
    private void updateMenuItemVisibilityForDialog(Window window) {
        
        // Search associated menu item, if any
        JMenuItem menuItem = menuFromWindow.get(window);
        if (menuItem != null) {
            
            boolean visible = !window.isVisible();
            window.setVisible(visible);
            if (visible) {
                menuItem.setIcon(tickMark);
            } else {
                menuItem.setIcon(null);
            }
        }
    }
    
    // Handle menu and other internal statuses when connected/disconnected to the board
    private void setConnected(boolean connected, boolean networked) {
        
        this.connected = connected;
        
        jMenuItemConnectSerial.setEnabled(!connected);
        jMenuItemConnectNetwork.setEnabled(!connected);
        jMenuItemDisconnect.setEnabled(connected);
        jMenuItemReadFromBoard.setEnabled(connected);
        jMenuItemWriteToBoard.setEnabled(connected);
        jMenuItemStartSampling.setEnabled(connected);
        jMenuItemStopSampling.setEnabled(connected);
        
        for (GenericTabPanel panel:tabPanelsList) {
            panel.setConnected(connected);
        }
        
        // Update refresh timer accordingly
        if (networked) {
            refreshTimer.setDelay(REFRESH_TIMER_FOR_NETWORKED_DEVICES);
        } else {
            refreshTimer.setDelay(REFRESH_TIMER_FOR_SERIAL_DEVICES);
        }
        
        refreshTimer.restart();        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AirSensEURPanel.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AirSensEURPanel().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.ChemShieldPanel chemSensorPanel;
    private airsenseur.dev.chemsensorpanel.EnvShield1Panel envShield1Panel;
    private airsenseur.dev.chemsensorpanel.ExpShield1Panel expShield1Panel;
    private airsenseur.dev.chemsensorpanel.ExpShield2Panel expShield2Panel;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenu jMenuChemSensors;
    private javax.swing.JMenuItem jMenuChemShieldInfo;
    private javax.swing.JMenu jMenuEnvShield1;
    private javax.swing.JMenuItem jMenuEnvShieldInfo;
    private javax.swing.JMenu jMenuExpShield1;
    private javax.swing.JMenu jMenuExpShield2;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItemAbout;
    private javax.swing.JMenuItem jMenuItemAnFbk_Exp2;
    private javax.swing.JMenuItem jMenuItemAudioFast;
    private javax.swing.JMenuItem jMenuItemAudioSlow;
    private javax.swing.JMenuItem jMenuItemChemSetup1;
    private javax.swing.JMenuItem jMenuItemChemSetup2;
    private javax.swing.JMenuItem jMenuItemChemSetup3;
    private javax.swing.JMenuItem jMenuItemChemSetup4;
    private javax.swing.JMenuItem jMenuItemConnectNetwork;
    private javax.swing.JMenuItem jMenuItemConnectSerial;
    private javax.swing.JMenuItem jMenuItemD300Setup;
    private javax.swing.JMenuItem jMenuItemD300_Exp2Setup;
    private javax.swing.JMenuItem jMenuItemDisconnect;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemExpShield2Info;
    private javax.swing.JMenuItem jMenuItemExpShieldInfo;
    private javax.swing.JMenuItem jMenuItemExtHumSetup;
    private javax.swing.JMenuItem jMenuItemExtTempSetup;
    private javax.swing.JMenuItem jMenuItemIntHumSetup;
    private javax.swing.JMenuItem jMenuItemIntTempSetup;
    private javax.swing.JMenuItem jMenuItemK96_Exp2;
    private javax.swing.JMenuItem jMenuItemOPCN3Setup;
    private javax.swing.JMenuItem jMenuItemOPT3001;
    private javax.swing.JMenuItem jMenuItemPID_HC_Exp2;
    private javax.swing.JMenuItem jMenuItemPMS5003Setup;
    private javax.swing.JMenuItem jMenuItemPressSetup;
    private javax.swing.JMenuItem jMenuItemRD200MSetup;
    private javax.swing.JMenuItem jMenuItemReadConfig;
    private javax.swing.JMenuItem jMenuItemReadFromBoard;
    private javax.swing.JMenuItem jMenuItemSHT31E_Exp2;
    private javax.swing.JMenuItem jMenuItemSHT31I_Exp2;
    private javax.swing.JMenuItem jMenuItemSPS30Setup;
    private javax.swing.JMenuItem jMenuItemSaveConfig;
    private javax.swing.JMenuItem jMenuItemSaveSensorProperties;
    private javax.swing.JMenuItem jMenuItemSensorDBEdit;
    private javax.swing.JMenuItem jMenuItemStartSampling;
    private javax.swing.JMenuItem jMenuItemStopSampling;
    private javax.swing.JMenuItem jMenuItemTAndFan_Exp2;
    private javax.swing.JMenuItem jMenuItemTemperatureHum;
    private javax.swing.JMenuItem jMenuItemWriteToBoard;
    private javax.swing.JMenu jMenuSensorBus;
    private javax.swing.JMenu jMenuSensorsDB;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables

    
    // WindowListener implementation
    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        updateMenuItemVisibilityForDialog(e.getWindow());
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }
    // End of WindowListener Implementation
}
