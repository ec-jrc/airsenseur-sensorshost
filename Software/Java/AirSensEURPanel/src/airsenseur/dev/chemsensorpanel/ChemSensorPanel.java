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

import airsenseur.dev.helpers.FileConfiguration;
import airsenseur.dev.chemsensorpanel.comm.ChemSensorCommHandler;
import airsenseur.dev.comm.CommProtocolHelper;
import airsenseur.dev.chemsensorpanel.comm.ConnectionDialog;
import airsenseur.dev.comm.ChemSensorBoard;
import airsenseur.dev.comm.CommProtocolHelper.DataMessage;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author marco
 */
public class ChemSensorPanel extends javax.swing.JFrame implements WindowListener {
    
    private final int CHEM_SENSOR_CHANNEL_ID_1  = 0;
    private final int CHEM_SENSOR_CHANNEL_ID_2  = 1;
    private final int CHEM_SENSOR_CHANNEL_ID_3  = 2;
    private final int CHEM_SENSOR_CHANNEL_ID_4  = 3;
    private final int PRESS_SENSOR_CHANNEL_ID = 4;
    private final int TEMP_SENSOR_CHANNEL_ID = 5;
    private final int HUM_SENSOR_CHANNEL_ID = 6;
    
    // The handler of a physical chem sensor board connected through a serial port
    private final ChemSensorBoard chemSensorBoard = new ChemSensorCommHandler(this);
    
    // The panel for managing chem sensor board connected through a host via ethernet
    private final RemoteBoardUpdateDialog remoteBoardDialog = new RemoteBoardUpdateDialog(this, false);
    
    // The panel for editing sensor's database
    private final SensorPresetManagerDialog sensorPresetManagerDialog = new SensorPresetManagerDialog(this, false);
    
    // The chemical sensors setup panels
    private final List<SensorSetupDialog> sensorSetupDialogs = new ArrayList<>();
    
    // We need to understand if the board has been connected or not
    private boolean connected = false;
    
    // Timer to periodically update the sample diagrams
    private final Timer refreshTimer = new Timer(1000, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            onRefreshTimer();
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
    
    private final List<SampleLogger> sampleLoggerPanels = new ArrayList<>();
    
    /**
     * Creates new form ChemSensorPanel
     */
    public ChemSensorPanel() {
        
        // Generate the sensor setup dialogs
        sensorSetupDialogs.add(new ChemSensorSetupDialog(this, false, CHEM_SENSOR_CHANNEL_ID_1));
        sensorSetupDialogs.add(new ChemSensorSetupDialog(this, false, CHEM_SENSOR_CHANNEL_ID_2));
        sensorSetupDialogs.add(new ChemSensorSetupDialog(this, false, CHEM_SENSOR_CHANNEL_ID_3));
        sensorSetupDialogs.add(new ChemSensorSetupDialog(this, false, CHEM_SENSOR_CHANNEL_ID_4));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog("Pressure Sensor", PRESS_SENSOR_CHANNEL_ID, this, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog("Temperature Sensor", TEMP_SENSOR_CHANNEL_ID, this, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog("Humidity Sensor", HUM_SENSOR_CHANNEL_ID, this, false));
        
        initComponents();
        
        // Populate links between menu items and windows
        menuFromWindow.put(sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_1), jMenuItemChemSetup1);
        menuFromWindow.put(sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_2), jMenuItemChemSetup2);
        menuFromWindow.put(sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_3), jMenuItemChemSetup3);
        menuFromWindow.put(sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_4), jMenuItemChemSetup4);
        menuFromWindow.put(sensorSetupDialogs.get(PRESS_SENSOR_CHANNEL_ID), jMenuItemPressSetup);
        menuFromWindow.put(sensorSetupDialogs.get(TEMP_SENSOR_CHANNEL_ID), jMenuItemTempSetup);
        menuFromWindow.put(sensorSetupDialogs.get(HUM_SENSOR_CHANNEL_ID), jMenuItemHumSetup);
        menuFromWindow.put(remoteBoardDialog, jMenuItemUploadConfig);
        menuFromWindow.put(sensorPresetManagerDialog, jMenuItemSensorDBEdit);
        
        // Add myself as a window listener so I'll be able to update the
        // menu status even if the user is closing a panel from the related window button
        for (SensorSetupDialog dialog:sensorSetupDialogs) {
            dialog.addWindowListener(this);
        }
        remoteBoardDialog.addWindowListener(this);
        sensorPresetManagerDialog.addWindowListener(this);
        
        // Setup a callback for preset database change propagation events
        sensorPresetManagerDialog.registerCallBack(new SensorPresetManagerDialog.CallBack() {

            @Override
            public void onDatabaseChanged() {
                sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_1).onSensorPresetDatabaseChanged();
                sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_2).onSensorPresetDatabaseChanged();
                sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_3).onSensorPresetDatabaseChanged();
                sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_4).onSensorPresetDatabaseChanged();
            }
        });
        
        // Aggregate sample loggers so it's more easy to handle them
        sampleLoggerPanels.add(sampleLogger0);
        sampleLoggerPanels.add(sampleLogger1);
        sampleLoggerPanels.add(sampleLogger2);
        sampleLoggerPanels.add(sampleLogger3);
        
        // Initialize the Chemical sample loggers properties
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).setLoggerProperties("Chem S" + (n+1), 0, 65535, 10);
            sampleLoggerPanels.get(n).setSensorId(n);
            sampleLoggerPanels.get(n).setDataProcessing(SampleLogger.unsignedConvertion);
            sampleLoggerPanels.get(n).setLogger(logger);
        }
        
        // Initialize other sample loggers
        sampleLogger4.setLoggerProperties("Pressure", 0, 1200, 10);
        sampleLogger4.setSensorId(PRESS_SENSOR_CHANNEL_ID);
        sampleLogger4.setLogger(logger);
        sampleLogger4.setDataProcessing(new SampleLogger.DataProcessing() {
            @Override
            public double processSample(double sample) {
                return (sample / 48.0);
            }
        });
        sampleLoggerPanels.add(sampleLogger4);
        
        sampleLogger5.setLoggerProperties("Temperature", 0, 65535, 10);
        sampleLogger5.setSensorId(TEMP_SENSOR_CHANNEL_ID);
        sampleLogger5.setLogger(logger);
        sampleLoggerPanels.add(sampleLogger5);        
        
        sampleLogger6.setLoggerProperties("Humidity", 0, 65535, 10);
        sampleLogger6.setSensorId(HUM_SENSOR_CHANNEL_ID);
        sampleLogger6.setLogger(logger);
        sampleLoggerPanels.add(sampleLogger6);
        
        // Define the default data sampling for humidity and pressure
        jCBpthRevision.setSelectedIndex(0);
        selectPTHRevision();

        // Start the refresh timer
        refreshTimer.setRepeats(true);
        refreshTimer.setDelay(1000);
        refreshTimer.start();
        
        // Initialize the configuration file chooser
        fChooser.setFileFilter(new FileNameExtensionFilter("AirSensEUR Configuration Files (*.asc)", "asc"));
        fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fChooser.setMultiSelectionEnabled(false);        
    }
    
    private void onRefreshTimer() {
        
        if (!connected) {
            return;
        }
        
        // Ask for a sample
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).readFromBoard();
        }
        
        // Update the free memory label
        CommProtocolHelper.instance().renderGetFreeMemory();
        chemSensorBoard.writeBufferToBoard();
        
        // Get Pressure sensor name
        CommProtocolHelper.instance().renderSensorInquiry(PRESS_SENSOR_CHANNEL_ID); 
    }
    
    public void onDataReceived() {
        
        DataMessage rxMessage;
        while ((rxMessage = CommProtocolHelper.instance().getNextRxDataMessage()) != null) {
            
            // Evaluate my needs
            Integer freeMem = CommProtocolHelper.instance().evalFreeMemory(rxMessage);
            if (freeMem != null) {
                jLabelFreeMem.setText(freeMem.toString() + " bytes");
            }

            // Check for PTH version (Rev < 1.4 have BMP180, Rev >= 1.4 have BMP280 pressure sensors)
            String setupName = CommProtocolHelper.instance().evalSensorInquiry(rxMessage, PRESS_SENSOR_CHANNEL_ID);
            if ((setupName != null) && !setupName.isEmpty()) {
                if (setupName.contains("180")) {
                    jCBpthRevision.setSelectedIndex(0);
                } else if (setupName.contains("280")) {
                    jCBpthRevision.setSelectedIndex(1);
                }
            }
            
            // Loop on each panel and propagate this message
            for(SensorSetupDialog setupDialog : sensorSetupDialogs) {
                setupDialog.evaluateRxMessage(rxMessage);
            }
            
            // Update the sample loggers
            for (int n = 0; n < sampleLoggerPanels.size(); n++) {
                sampleLoggerPanels.get(n).evaluateRxMessage(rxMessage);
            }

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
        jLabel1 = new javax.swing.JLabel();
        jLabelFreeMem = new javax.swing.JLabel();
        sampleLogger0 = new airsenseur.dev.chemsensorpanel.LineGraphSampleLoggerPanel();
        sampleLogger1 = new airsenseur.dev.chemsensorpanel.LineGraphSampleLoggerPanel();
        sampleLogger2 = new airsenseur.dev.chemsensorpanel.LineGraphSampleLoggerPanel();
        sampleLogger3 = new airsenseur.dev.chemsensorpanel.LineGraphSampleLoggerPanel();
        sampleLogger4 = new airsenseur.dev.chemsensorpanel.TextBasedSampleLoggerPanel();
        sampleLogger5 = new airsenseur.dev.chemsensorpanel.TextBasedSampleLoggerPanel();
        sampleLogger6 = new airsenseur.dev.chemsensorpanel.TextBasedSampleLoggerPanel();
        jCBpthRevision = new javax.swing.JComboBox();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemSaveConfig = new javax.swing.JMenuItem();
        jMenuItemReadConfig = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenuItemUploadConfig = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        jMenuItemAbout = new javax.swing.JMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuBoard = new javax.swing.JMenu();
        jMenuItemConnect = new javax.swing.JMenuItem();
        jMenuItemDisconnect = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuItemReadFromBoard = new javax.swing.JMenuItem();
        jMenuItemWriteToBoard = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemStartSampling = new javax.swing.JMenuItem();
        jMenuItemStopSampling = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jMenuItemChemSetup1 = new javax.swing.JMenuItem();
        jMenuItemChemSetup2 = new javax.swing.JMenuItem();
        jMenuItemChemSetup3 = new javax.swing.JMenuItem();
        jMenuItemChemSetup4 = new javax.swing.JMenuItem();
        jMenuItemPressSetup = new javax.swing.JMenuItem();
        jMenuItemTempSetup = new javax.swing.JMenuItem();
        jMenuItemHumSetup = new javax.swing.JMenuItem();
        jMenuSensorsDB = new javax.swing.JMenu();
        jMenuItemSensorDBEdit = new javax.swing.JMenuItem();

        jMenuItem1.setText("jMenuItem1");

        jMenuItem2.setText("jMenuItem2");

        jMenuItem3.setText("jMenuItem3");

        jMenuItem4.setText("jMenuItem4");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("ChemSensorBoard Panel");
        setLocationByPlatform(true);

        jLabel1.setText("Available RAM on the sensor board:");

        jLabelFreeMem.setText("--");

        jCBpthRevision.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PTH Revision < 1.4", "PTH Revision >= 1.4" }));
        jCBpthRevision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBpthRevisionActionPerformed(evt);
            }
        });

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

        jMenuItemUploadConfig.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        jMenuItemUploadConfig.setText("Upload Config");
        jMenuItemUploadConfig.setToolTipText("Upload configuration file to a remote Shield");
        jMenuItemUploadConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemUploadConfigActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemUploadConfig);
        jMenuFile.add(jSeparator4);

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

        jMenuBoard.setText("Local Board");

        jMenuItemConnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/airsenseur/dev/chemsensorpanel/icons/connect.png"))); // NOI18N
        jMenuItemConnect.setText("Connect via Serial Line");
        jMenuItemConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemConnect);

        jMenuItemDisconnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/airsenseur/dev/chemsensorpanel/icons/disconnect.png"))); // NOI18N
        jMenuItemDisconnect.setText("Disconnect");
        jMenuItemDisconnect.setEnabled(false);
        jMenuItemDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDisconnectActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemDisconnect);
        jMenuBoard.add(jSeparator2);

        jMenuItemReadFromBoard.setText("Read From Board");
        jMenuItemReadFromBoard.setEnabled(false);
        jMenuItemReadFromBoard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemReadFromBoardActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemReadFromBoard);

        jMenuItemWriteToBoard.setText("Write To Board");
        jMenuItemWriteToBoard.setEnabled(false);
        jMenuItemWriteToBoard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemWriteToBoardActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemWriteToBoard);
        jMenuBoard.add(jSeparator1);

        jMenuItemStartSampling.setText("Start sampling");
        jMenuItemStartSampling.setEnabled(false);
        jMenuItemStartSampling.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartSamplingActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemStartSampling);

        jMenuItemStopSampling.setText("Stop sampling");
        jMenuItemStopSampling.setEnabled(false);
        jMenuItemStopSampling.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStopSamplingActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemStopSampling);
        jMenuBoard.add(jSeparator3);

        jMenuItemChemSetup1.setText("ChemSensor 1 Setup");
        jMenuItemChemSetup1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChemSetup1ActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemChemSetup1);

        jMenuItemChemSetup2.setText("ChemSensor 2 Setup");
        jMenuItemChemSetup2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChemSetup2ActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemChemSetup2);

        jMenuItemChemSetup3.setText("ChemSensor 3 Setup");
        jMenuItemChemSetup3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChemSetup3ActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemChemSetup3);

        jMenuItemChemSetup4.setText("ChemSensor 4 Setup");
        jMenuItemChemSetup4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChemSetup4ActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemChemSetup4);

        jMenuItemPressSetup.setText("Pressure Sensor Setup");
        jMenuItemPressSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPressSetupActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemPressSetup);

        jMenuItemTempSetup.setText("Temperature Sensor Setup");
        jMenuItemTempSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTempSetupActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemTempSetup);

        jMenuItemHumSetup.setText("Humidity Sensor Setup");
        jMenuItemHumSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHumSetupActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemHumSetup);

        jMenuBar.add(jMenuBoard);

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
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(sampleLogger0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(sampleLogger2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleLogger1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLogger3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(sampleLogger6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jCBpthRevision, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(sampleLogger4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(sampleLogger5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabelFreeMem, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sampleLogger4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(sampleLogger5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(sampleLogger6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel1)
                                .addComponent(jLabelFreeMem))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(sampleLogger0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(sampleLogger1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(sampleLogger2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sampleLogger3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCBpthRevision, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConnectActionPerformed
        
        ConnectionDialog connectionDialog = new ConnectionDialog(this, true);
        connectionDialog.init(chemSensorBoard);
        connectionDialog.setVisible(true);
        
        setConnected(connectionDialog.isConnected());
    }//GEN-LAST:event_jMenuItemConnectActionPerformed

    private void jMenuItemDisconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDisconnectActionPerformed
        
        chemSensorBoard.disConnectFromBoard();
        setConnected(false);
    }//GEN-LAST:event_jMenuItemDisconnectActionPerformed

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed

        if (!connected) {
            System.exit(0);
        } else {
            
            if (javax.swing.JOptionPane.showConfirmDialog(this, 
                    "Are you sure to disconnect from the board and exit?", 
                    "Warning", javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION) {
                
                chemSensorBoard.disConnectFromBoard();
                System.exit(0);
            }
        }
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItemChemSetup2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChemSetup2ActionPerformed
        updateMenuItemVisibilityForDialog(sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_2));
    }//GEN-LAST:event_jMenuItemChemSetup2ActionPerformed

    private void jMenuItemChemSetup1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChemSetup1ActionPerformed
        updateMenuItemVisibilityForDialog(sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_1));
    }//GEN-LAST:event_jMenuItemChemSetup1ActionPerformed

    private void jMenuItemChemSetup3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChemSetup3ActionPerformed
        updateMenuItemVisibilityForDialog(sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_3));
    }//GEN-LAST:event_jMenuItemChemSetup3ActionPerformed

    private void jMenuItemChemSetup4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChemSetup4ActionPerformed
        updateMenuItemVisibilityForDialog(sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_4));
    }//GEN-LAST:event_jMenuItemChemSetup4ActionPerformed

    private void jMenuItemPressSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPressSetupActionPerformed
        updateMenuItemVisibilityForDialog(sensorSetupDialogs.get(PRESS_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemPressSetupActionPerformed

    private void jMenuItemTempSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTempSetupActionPerformed
        updateMenuItemVisibilityForDialog(sensorSetupDialogs.get(TEMP_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemTempSetupActionPerformed

    private void jMenuItemHumSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemHumSetupActionPerformed
        updateMenuItemVisibilityForDialog(sensorSetupDialogs.get(HUM_SENSOR_CHANNEL_ID));
    }//GEN-LAST:event_jMenuItemHumSetupActionPerformed

    private void jMenuItemWriteToBoardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemWriteToBoardActionPerformed
        
        if (connected) {
            
            // Generate the command list to be sent to be board
            for(SensorSetupDialog setupDialog : sensorSetupDialogs) {
                
                // Render the command for this dialog
                setupDialog.storeToBoard();
                
                // Dump the command list to the board
                chemSensorBoard.writeBufferToBoard();
                try {
                    // Wait some time
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ChemSensorPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }   
        }
    }//GEN-LAST:event_jMenuItemWriteToBoardActionPerformed

    private void jMenuItemReadFromBoardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemReadFromBoardActionPerformed
        
        if (connected) {
            
            // Generate the command list to be sent to the board
            for (SensorSetupDialog setupDialog : sensorSetupDialogs) {

                // Render commands for this dialog
                setupDialog.readFromBoard();
                
                // Dump the command list to the board
                chemSensorBoard.writeBufferToBoard();
                
                try {
                    // Wait some time
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ChemSensorPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }
    }//GEN-LAST:event_jMenuItemReadFromBoardActionPerformed

    private void jMenuItemStartSamplingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartSamplingActionPerformed
        
        if (connected) {
            
            // Start a new logger
            if (logger != null) {
                String fileName = "Samples";
                logger.openFile(fileName);
            }
            
            // Add the start sample command to the buffer
            CommProtocolHelper.instance().renderStartSample();
            
            // Force tx data to the board
            chemSensorBoard.writeBufferToBoard();
        }
    }//GEN-LAST:event_jMenuItemStartSamplingActionPerformed

    private void jMenuItemStopSamplingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStopSamplingActionPerformed
        
        if (connected) {
            
            // Close the log file, if any
            if (logger != null) {
                logger.closeFile();
            }
            
            // Add the stop sample command to the buffer
            CommProtocolHelper.instance().renderStopSample();
            
            // Force tx data to tbe board
            chemSensorBoard.writeBufferToBoard();
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
        configFile.appendCommands(getCurrentConfiguration(false));
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
        DataMessage configurationMessage;
        while ((configurationMessage = configFile.getNextCommand()) != null) {
            
            // The configuration file is a container of commands set to the
            // board in order to properly set it up. When reading, we need to 
            // convert the "set" commands stored in the configuration file like if
            // the board was "reading" their status. By performing this convertion,
            // the whole java panel set can update with the same procedure used when 
            // receiving messages from the board.
            configurationMessage.toAnswerCommandString();
            
            // Loop on each panel and propagate this message
            for(SensorSetupDialog setupDialog : sensorSetupDialogs) {
                setupDialog.evaluateRxMessage(configurationMessage);
            }
        }
        
        configFile.closeFile();
        
        // Enable time refresh, if required
        connected = connectedStatus;
    }//GEN-LAST:event_jMenuItemReadConfigActionPerformed

    private void jMenuItemUploadConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemUploadConfigActionPerformed
        updateMenuItemVisibilityForDialog(remoteBoardDialog);
    }//GEN-LAST:event_jMenuItemUploadConfigActionPerformed

    private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAboutActionPerformed
        
        AboutDialog about = new AboutDialog(this, true);
        about.setVisible(true);
    }//GEN-LAST:event_jMenuItemAboutActionPerformed

    private void jMenuItemSensorDBEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSensorDBEditActionPerformed
        updateMenuItemVisibilityForDialog(sensorPresetManagerDialog);
    }//GEN-LAST:event_jMenuItemSensorDBEditActionPerformed

    private void jCBpthRevisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBpthRevisionActionPerformed
        selectPTHRevision();
    }

    private void selectPTHRevision() {
        
        int selected = jCBpthRevision.getSelectedIndex();
        SampleLogger.DataProcessing temperature = SampleLogger.sht31TemperatureDataProcessing;
        SampleLogger.DataProcessing pressure = SampleLogger.sht31PressureDataProcessing;
        if (selected == 0) {
            temperature = SampleLogger.ur100CDTemperatureDataProcessing;
            pressure = SampleLogger.ur100CDPressureDataProcessing;
        }
        
        sampleLogger5.setDataProcessing(temperature);
        sampleLogger6.setDataProcessing(pressure);
    }//GEN-LAST:event_jCBpthRevisionActionPerformed

    protected List<DataMessage> getCurrentConfiguration(boolean forceRestartSampling) {
        
        List<DataMessage> currentConfiguration = new ArrayList<>();
        
        // Prevent timer interactions
        boolean connectedStatus = connected;
        connected = false;
        
        // Generate the command list to be sent to be board
        chemSensorBoard.getCurrentBuffer().clear();
        
        // Add the stop sample command to the buffer, if required
        if (forceRestartSampling) {
            CommProtocolHelper.instance().renderStopSample();
        }
        
        for(SensorSetupDialog setupDialog : sensorSetupDialogs) {

            // Render the command for this dialog
            setupDialog.storeToBoard();
        }
        
        // Add the start sample command to the buffer, if required
        if (forceRestartSampling) {
            CommProtocolHelper.instance().renderStartSample();
        }
        
        // Save to the configuration file
        currentConfiguration.addAll(chemSensorBoard.getCurrentBuffer());
        
        // Clear the buffer queue
        chemSensorBoard.getCurrentBuffer().clear();
        
        // Enable time refresh, if required
        connected = connectedStatus;
        
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
    private void setConnected(boolean connected) {
        
        this.connected = connected;
        
        jMenuItemConnect.setEnabled(!connected);
        jMenuItemDisconnect.setEnabled(connected);
        jMenuItemReadFromBoard.setEnabled(connected);
        jMenuItemWriteToBoard.setEnabled(connected);
        jMenuItemStartSampling.setEnabled(connected);
        jMenuItemStopSampling.setEnabled(connected);
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
            java.util.logging.Logger.getLogger(ChemSensorPanel.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChemSensorPanel().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jCBpthRevision;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelFreeMem;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenu jMenuBoard;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItemAbout;
    private javax.swing.JMenuItem jMenuItemChemSetup1;
    private javax.swing.JMenuItem jMenuItemChemSetup2;
    private javax.swing.JMenuItem jMenuItemChemSetup3;
    private javax.swing.JMenuItem jMenuItemChemSetup4;
    private javax.swing.JMenuItem jMenuItemConnect;
    private javax.swing.JMenuItem jMenuItemDisconnect;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemHumSetup;
    private javax.swing.JMenuItem jMenuItemPressSetup;
    private javax.swing.JMenuItem jMenuItemReadConfig;
    private javax.swing.JMenuItem jMenuItemReadFromBoard;
    private javax.swing.JMenuItem jMenuItemSaveConfig;
    private javax.swing.JMenuItem jMenuItemSensorDBEdit;
    private javax.swing.JMenuItem jMenuItemStartSampling;
    private javax.swing.JMenuItem jMenuItemStopSampling;
    private javax.swing.JMenuItem jMenuItemTempSetup;
    private javax.swing.JMenuItem jMenuItemUploadConfig;
    private javax.swing.JMenuItem jMenuItemWriteToBoard;
    private javax.swing.JMenu jMenuSensorsDB;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private airsenseur.dev.chemsensorpanel.LineGraphSampleLoggerPanel sampleLogger0;
    private airsenseur.dev.chemsensorpanel.LineGraphSampleLoggerPanel sampleLogger1;
    private airsenseur.dev.chemsensorpanel.LineGraphSampleLoggerPanel sampleLogger2;
    private airsenseur.dev.chemsensorpanel.LineGraphSampleLoggerPanel sampleLogger3;
    private airsenseur.dev.chemsensorpanel.TextBasedSampleLoggerPanel sampleLogger4;
    private airsenseur.dev.chemsensorpanel.TextBasedSampleLoggerPanel sampleLogger5;
    private airsenseur.dev.chemsensorpanel.TextBasedSampleLoggerPanel sampleLogger6;
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
