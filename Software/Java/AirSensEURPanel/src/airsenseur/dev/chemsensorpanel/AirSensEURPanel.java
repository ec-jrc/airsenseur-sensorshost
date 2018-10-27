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
import airsenseur.dev.helpers.FileConfiguration;
import airsenseur.dev.chemsensorpanel.comm.SensorBusCommunicationHandler;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.chemsensorpanel.comm.SerialConnectionDialog;
import airsenseur.dev.chemsensorpanel.exceptions.ChemSensorPanelException;
import airsenseur.dev.chemsensorpanel.helpers.HostConfigWriter;
import airsenseur.dev.comm.AppDataMessage;
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
    
    // We need to understand if the board has been connected or not
    private boolean connected = false;
    
    // We need to understand if the board has been connected through serial line or network
    private boolean networked = false;
    
    // We need to knwow if we're using Point to Point or SensorBusEnabled adapters
    private final boolean useMultiPoint = true;
    
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
    private final FileConfiguration configFile = new FileConfiguration();
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
        tabPanelsList.add(oPCN2SensorPanel);
        tabPanelsList.add(expShield1Panel);
        
        // Populate links between menu items and windows
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_1), jMenuItemChemSetup1);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_2), jMenuItemChemSetup2);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_3), jMenuItemChemSetup3);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.CHEM_SENSOR_CHANNEL_ID_4), jMenuItemChemSetup4);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.PRESS_SENSOR_CHANNEL_ID), jMenuItemPressSetup);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.TEMP_SENSOR_CHANNEL_ID), jMenuItemTempSetup);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.HUM_SENSOR_CHANNEL_ID), jMenuItemHumSetup);
        menuFromWindow.put(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.GENERIC_INFO_CHANNEL_ID), jMenuChemShieldInfo);

        menuFromWindow.put(oPCN2SensorPanel.getSensorSetupDialogs().get(OPCN2ShieldPanel.OPCN2_SETUP_DIALOG_OPC), jMenuItemOPCN2Setup);
        menuFromWindow.put(oPCN2SensorPanel.getSensorSetupDialogs().get(OPCN2ShieldPanel.OPCN2_SETUP_DIALOG_MOX), jMenuItemMOXSetup);

        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_RD200M), jMenuItemRD200MSetup);
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_D300), jMenuItemD300Setup);
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_PMS5003), jMenuItemPMS5003Setup);
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_DIALOG_OPCN3), jMenuItemOPCN3Setup);
        menuFromWindow.put(expShield1Panel.getSensorSetupDialogs().get(ExpShield1Panel.EXPSHIELD1_SETUP_GENERIC_INFO), jMenuItemExpShieldInfo);
        
        menuFromWindow.put(sensorPresetManagerDialog, jMenuItemSensorDBEdit);
        
        // Add myself as a window listener so I'll be able to update the
        // menu status even if the user is closing a panel from the related window button
        for (SensorSetupDialog dialog:chemSensorPanel.getSensorSetupDialogs()) {
            dialog.addWindowListener(this);
        }
        for (SensorSetupDialog dialog:oPCN2SensorPanel.getSensorSetupDialogs()) {
            dialog.addWindowListener(this);
        }
        for (SensorSetupDialog dialog:expShield1Panel.getSensorSetupDialogs()) {
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
        fChooser.setFileFilter(new FileNameExtensionFilter("AirSensEUR Configuration Files (*.asc)", "asc"));
        fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fChooser.setMultiSelectionEnabled(false);     
    }
    
    private void onRefreshTimer() {
        
        if (!connected) {
            return;
        }
        
        for (GenericTabPanel panel:tabPanelsList) {
            panel.onRefreshTimer();
        }
    }
    
    private void onRefreshOwnershipTimer() {
        
        if (!connected) {
            return;
        }
        
        sensorBusCommunicationHandler.takeBusOwnership();
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
        jTabbedPane1 = new javax.swing.JTabbedPane();
        chemSensorPanel = new airsenseur.dev.chemsensorpanel.ChemShieldPanel(this, shieldProtocolLayer, logger)
        ;
        oPCN2SensorPanel = new airsenseur.dev.chemsensorpanel.OPCN2ShieldPanel(this, shieldProtocolLayer, logger);
        expShield1Panel = new airsenseur.dev.chemsensorpanel.ExpShield1Panel(this, shieldProtocolLayer, logger);
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
        jMenuItemTempSetup = new javax.swing.JMenuItem();
        jMenuItemHumSetup = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        jMenuChemShieldInfo = new javax.swing.JMenuItem();
        jMenuOPCN2 = new javax.swing.JMenu();
        jMenuItemOPCN2Setup = new javax.swing.JMenuItem();
        jMenuItemMOXSetup = new javax.swing.JMenuItem();
        jMenuExpShield1 = new javax.swing.JMenu();
        jMenuItemRD200MSetup = new javax.swing.JMenuItem();
        jMenuItemD300Setup = new javax.swing.JMenuItem();
        jMenuItemPMS5003Setup = new javax.swing.JMenuItem();
        jMenuItemOPCN3Setup = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        jMenuItemExpShieldInfo = new javax.swing.JMenuItem();
        jMenuSensorsDB = new javax.swing.JMenu();
        jMenuItemSensorDBEdit = new javax.swing.JMenuItem();

        jMenuItem1.setText("jMenuItem1");

        jMenuItem2.setText("jMenuItem2");

        jMenuItem3.setText("jMenuItem3");

        jMenuItem4.setText("jMenuItem4");

        jMenuItem5.setText("jMenuItem5");

        jMenuItem6.setText("jMenuItem6");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("AirSensEUR Panel");
        setLocationByPlatform(true);

        jTabbedPane1.addTab("Chemical Sensor Shield", chemSensorPanel);
        jTabbedPane1.addTab("OPC-N2 Sensor Shield", oPCN2SensorPanel);
        jTabbedPane1.addTab("ExpShield1", expShield1Panel);

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

        jMenuItemTempSetup.setText("Temperature Sensor Setup");
        jMenuItemTempSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTempSetupActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemTempSetup);

        jMenuItemHumSetup.setText("Humidity Sensor Setup");
        jMenuItemHumSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHumSetupActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuItemHumSetup);
        jMenuChemSensors.add(jSeparator6);

        jMenuChemShieldInfo.setText("Generic Information");
        jMenuChemShieldInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuChemShieldInfoActionPerformed(evt);
            }
        });
        jMenuChemSensors.add(jMenuChemShieldInfo);

        jMenuBar.add(jMenuChemSensors);

        jMenuOPCN2.setText("OPC-N2 Shield");

        jMenuItemOPCN2Setup.setText("OPC-N2 Setup");
        jMenuItemOPCN2Setup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOPCN2SetupActionPerformed(evt);
            }
        });
        jMenuOPCN2.add(jMenuItemOPCN2Setup);

        jMenuItemMOXSetup.setText("MOX Setup");
        jMenuItemMOXSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMOXSetupActionPerformed(evt);
            }
        });
        jMenuOPCN2.add(jMenuItemMOXSetup);

        jMenuBar.add(jMenuOPCN2);

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
        jMenuExpShield1.add(jSeparator4);

        jMenuItemExpShieldInfo.setText("Generic Information");
        jMenuItemExpShieldInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExpShieldInfoActionPerformed(evt);
            }
        });
        jMenuExpShield1.add(jMenuItemExpShieldInfo);

        jMenuBar.add(jMenuExpShield1);

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
        
        SerialConnectionDialog connectionDialog = new SerialConnectionDialog(this, true);
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

    private void jMenuItemTempSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTempSetupActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.TEMP_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemTempSetupActionPerformed

    private void jMenuItemHumSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemHumSetupActionPerformed
        updateMenuItemVisibilityForDialog(chemSensorPanel.getSensorSetupDialogs().get(ChemShieldPanel.HUM_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemHumSetupActionPerformed

    private void jMenuItemWriteToBoardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemWriteToBoardActionPerformed
        
        if (connected) {
            refreshTimer.stop();
            
            for (GenericTabPanel panel:tabPanelsList) {
                panel.storeToBoard();
            }
            
            refreshTimer.start();
        }
    }//GEN-LAST:event_jMenuItemWriteToBoardActionPerformed

    private void jMenuItemReadFromBoardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemReadFromBoardActionPerformed
        
        if (connected) {
            refreshTimer.stop();
            
            for (GenericTabPanel panel:tabPanelsList) {
                panel.readFromBoard();
            }
            
            refreshTimer.start();
        }
    }//GEN-LAST:event_jMenuItemReadFromBoardActionPerformed

    private void jMenuItemStartSamplingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartSamplingActionPerformed
        
        if (connected) {
            
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
        }
    }//GEN-LAST:event_jMenuItemStartSamplingActionPerformed

    private void jMenuItemStopSamplingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStopSamplingActionPerformed
        
        if (connected) {
            
            for (GenericTabPanel panel:tabPanelsList) {
                panel.stopSample();
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
            
            configFile.generateBoardSession();
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
        
        // Read the configuration file
        configFile.openFile(selectedFile, true);
        for (int tabSession = 0; tabSession < tabPanelsList.size(); tabSession++) {
            
            AppDataMessage configurationMessage;
            while ((configurationMessage = configFile.getNextCommand(tabSession)) != null) {

                // The configuration file is a container of commands set to the
                // board in order to properly set it up. When reading, we need to 
                // convert the "set" commands stored in the configuration file like if
                // the board was "reading" their status. By performing this convertion,
                // the whole java panel set can update with the same procedure used when 
                // receiving messages from the board.
                shieldProtocolLayer.toAnswerCommandString(configurationMessage);
                
                // Propagate to the proper tab
                tabPanelsList.get(tabSession).onDataMessageFromConfiguration(configurationMessage);
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

    private void jMenuItemOPCN2SetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOPCN2SetupActionPerformed
        updateMenuItemVisibilityForDialog(oPCN2SensorPanel.getSensorSetupDialogs().get(OPCN2ShieldPanel.OPCN2_SETUP_DIALOG_OPC));
    }//GEN-LAST:event_jMenuItemOPCN2SetupActionPerformed

    private void jMenuItemMOXSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemMOXSetupActionPerformed
        updateMenuItemVisibilityForDialog(oPCN2SensorPanel.getSensorSetupDialogs().get(OPCN2ShieldPanel.OPCN2_SETUP_DIALOG_MOX));
    }//GEN-LAST:event_jMenuItemMOXSetupActionPerformed

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

    protected List<AppDataMessage> getCurrentConfiguration(int tabPanelIndex, boolean forceRestartSampling) {
        
        List<AppDataMessage> currentConfiguration = new ArrayList<>();
        
        if (tabPanelIndex < tabPanelsList.size()) {
        
            // Prevent timer interactions
            boolean connectedStatus = connected;
            connected = false;

            // Start dumping to buffer
            sensorBusCommunicationHandler.startDumpingToBuffer(currentConfiguration);

            // Propagate to the selected panel
            tabPanelsList.get(tabPanelIndex).onGetCurrentConfiguation(forceRestartSampling);

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
        this.networked = networked;
        
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
    private airsenseur.dev.chemsensorpanel.ExpShield1Panel expShield1Panel;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenu jMenuChemSensors;
    private javax.swing.JMenuItem jMenuChemShieldInfo;
    private javax.swing.JMenu jMenuExpShield1;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItemAbout;
    private javax.swing.JMenuItem jMenuItemChemSetup1;
    private javax.swing.JMenuItem jMenuItemChemSetup2;
    private javax.swing.JMenuItem jMenuItemChemSetup3;
    private javax.swing.JMenuItem jMenuItemChemSetup4;
    private javax.swing.JMenuItem jMenuItemConnectNetwork;
    private javax.swing.JMenuItem jMenuItemConnectSerial;
    private javax.swing.JMenuItem jMenuItemD300Setup;
    private javax.swing.JMenuItem jMenuItemDisconnect;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemExpShieldInfo;
    private javax.swing.JMenuItem jMenuItemHumSetup;
    private javax.swing.JMenuItem jMenuItemMOXSetup;
    private javax.swing.JMenuItem jMenuItemOPCN2Setup;
    private javax.swing.JMenuItem jMenuItemOPCN3Setup;
    private javax.swing.JMenuItem jMenuItemPMS5003Setup;
    private javax.swing.JMenuItem jMenuItemPressSetup;
    private javax.swing.JMenuItem jMenuItemRD200MSetup;
    private javax.swing.JMenuItem jMenuItemReadConfig;
    private javax.swing.JMenuItem jMenuItemReadFromBoard;
    private javax.swing.JMenuItem jMenuItemSaveConfig;
    private javax.swing.JMenuItem jMenuItemSaveSensorProperties;
    private javax.swing.JMenuItem jMenuItemSensorDBEdit;
    private javax.swing.JMenuItem jMenuItemStartSampling;
    private javax.swing.JMenuItem jMenuItemStopSampling;
    private javax.swing.JMenuItem jMenuItemTempSetup;
    private javax.swing.JMenuItem jMenuItemWriteToBoard;
    private javax.swing.JMenu jMenuOPCN2;
    private javax.swing.JMenu jMenuSensorBus;
    private javax.swing.JMenu jMenuSensorsDB;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private airsenseur.dev.chemsensorpanel.OPCN2ShieldPanel oPCN2SensorPanel;
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