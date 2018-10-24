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
import airsenseur.dev.chemsensorpanel.helpers.HostConfigSensorProperties;
import airsenseur.dev.chemsensorpanel.helpers.HostConfigWriter;
import airsenseur.dev.chemsensorpanel.setupdialogs.GenericSensorSetupDIalog;
import airsenseur.dev.chemsensorpanel.setupdialogs.ChemSensorSetupDialog;
import airsenseur.dev.chemsensorpanel.setupdialogs.GenericBoardInfoDialog;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.ShieldProtocolLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author marco
 */
public class ChemShieldPanel extends GenericTabPanel {
    
    public final static int CHEM_SENSOR_CHANNEL_ID_1  = 0;
    public final static int CHEM_SENSOR_CHANNEL_ID_2  = CHEM_SENSOR_CHANNEL_ID_1 + 1;
    public final static int CHEM_SENSOR_CHANNEL_ID_3  = CHEM_SENSOR_CHANNEL_ID_2 + 1;
    public final static int CHEM_SENSOR_CHANNEL_ID_4  = CHEM_SENSOR_CHANNEL_ID_3 + 1;
    public final static int PRESS_SENSOR_CHANNEL_ID = CHEM_SENSOR_CHANNEL_ID_4  +1;
    public final static int TEMP_SENSOR_CHANNEL_ID = PRESS_SENSOR_CHANNEL_ID + 1;
    public final static int HUM_SENSOR_CHANNEL_ID = TEMP_SENSOR_CHANNEL_ID + 1;    
    public final static int GENERIC_INFO_CHANNEL_ID = HUM_SENSOR_CHANNEL_ID + 1;
    
    private final static String CHEM_SENSOR_CHANNEL_MATH_EXPRESSION = "if(x>32767,x-32768,x+32768)";
    private final static String UR100CD_TEMP_CHANNEL_MATH_EXPRESSION = "((x/16384)*165)-40.0";
    private final static String SHT31_TEMP_CHANNEL_MATH_EXPRESSION = "((x/65535*175) - 45.0)";
    private final static String UR100CD_PRESS_CHANNEL_MATH_EXPRESSION = "x/48.0";
    private final static String SHT31_PRESS_CHANNEL_MATH_EXPRESSION = "(x/65535 * 100.0)";
    private final static String HUMIDITY_CHANNEL_MATH_EXPRESSION = "(x/16384)*100.0";
    
    private final static String TEMP_SENSOR_NAME = "Temp";
    private final static String PRESS_SENSOR_NAME = "Press";
    private final static String HUM_SENSOR_NAME = "Humid";
    
    
    private int selectedBoardId = 0;
    private boolean boardEnabled = false;
    private String boardSerialNumber = "";
    
    // The chemical sensors setup panels
    private final List<SensorSetupDialog> sensorSetupDialogs = new ArrayList<>();
    
    private final List<SampleLogger> sampleLoggerPanels = new ArrayList<>();
    
    // This is used only by the graphical composition tool in the Netbeans IDE
    public ChemShieldPanel() {
        initComponents();
    }

    /**
     * Creates new form ChemSensorPanel
     * @param parent
     * @param shieldProtocolLayer
     * @param logger
     */
    public ChemShieldPanel(MainApplicationFrame parent, ShieldProtocolLayer shieldProtocolLayer, FileLogger logger) {
        super(shieldProtocolLayer, logger);
                
        // Generate the sensor setup dialogs
        sensorSetupDialogs.add(new ChemSensorSetupDialog(parent, false, CHEM_SENSOR_CHANNEL_ID_1));
        sensorSetupDialogs.add(new ChemSensorSetupDialog(parent, false, CHEM_SENSOR_CHANNEL_ID_2));
        sensorSetupDialogs.add(new ChemSensorSetupDialog(parent, false, CHEM_SENSOR_CHANNEL_ID_3));
        sensorSetupDialogs.add(new ChemSensorSetupDialog(parent, false, CHEM_SENSOR_CHANNEL_ID_4));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog(PRESS_SENSOR_NAME, PRESS_SENSOR_CHANNEL_ID, false, parent, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog(TEMP_SENSOR_NAME, TEMP_SENSOR_CHANNEL_ID, false, parent, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog(HUM_SENSOR_NAME, HUM_SENSOR_CHANNEL_ID, false, parent, false));
        sensorSetupDialogs.add(new GenericBoardInfoDialog(parent, false, "Chemical Shield Generic Info"));
        
        initComponents();
        
        // Aggregate sample loggers so it's more easy to handle them
        sampleLoggerPanels.add(sampleLogger0);
        sampleLoggerPanels.add(sampleLogger1);
        sampleLoggerPanels.add(sampleLogger2);
        sampleLoggerPanels.add(sampleLogger3);
        sampleLoggerPanels.add(sampleLogger4);
        sampleLoggerPanels.add(sampleLogger5);
        sampleLoggerPanels.add(sampleLogger6);
        
        // Initialize all loggers with common properties
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).setLoggerProperties("Chem S" + (n+1), 0, 65535, 10);
            sampleLoggerPanels.get(n).setDataProcessing(SampleLogger.unsignedConvertion);
            sampleLoggerPanels.get(n).setLogger(logger);
            sampleLoggerPanels.get(n).setSensorId(n);
            sampleLoggerPanels.get(n).setBoardId(selectedBoardId);
            sampleLoggerPanels.get(n).setShieldProtocolLayer(shieldProtocolLayer);            
        }

        // Initialize al sensorSetupDialogs
        int sensorId = CHEM_SENSOR_CHANNEL_ID_1;
        for (SensorSetupDialog dialog:sensorSetupDialogs) {
            dialog.setShieldProtocolLayer(shieldProtocolLayer);
            dialog.setChannelId(sensorId);
            dialog.setBoardId(selectedBoardId);
            sensorId++;
        }
        
        // Update some logger with specific properties
        sampleLogger4.setLoggerProperties(PRESS_SENSOR_NAME, 0, 1200, 10);
        sampleLogger4.setSensorId(PRESS_SENSOR_CHANNEL_ID);
        sampleLogger4.setDataProcessing(new SampleLogger.DataProcessing() {
            @Override
            public double processSample(double sample) {
                return (sample / 48.0);
            }
        });
        
        sampleLogger5.setLoggerProperties(TEMP_SENSOR_NAME, 0, 65535, 10);
        sampleLogger5.setSensorId(TEMP_SENSOR_CHANNEL_ID);
        
        sampleLogger6.setLoggerProperties(HUM_SENSOR_NAME, 0, 65535, 10);
        sampleLogger6.setSensorId(HUM_SENSOR_CHANNEL_ID);       
        
        // Define the default data sampling for humidity and pressure
        jCBpthRevision.setSelectedIndex(0);
        selectPTHRevision();
        
        // Board ID handling
        onBoardIDChanged();
    }

    @Override
    public List<SensorSetupDialog> getSensorSetupDialogs() {
        return sensorSetupDialogs;
    }
    
    @Override
    public void onRefreshTimer() {
        if (!boardEnabled) {
            return;
        }        
        
        // Ask for a sample
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).readFromBoard();
        }
        
        // Update the free memory label
        shieldProtocolLayer.renderGetFreeMemory(selectedBoardId);
        
        // Get Pressure sensor name
        shieldProtocolLayer.renderSensorInquiry(selectedBoardId, PRESS_SENSOR_CHANNEL_ID); 
        
        // Get the board sensor name
        shieldProtocolLayer.renderReadBoardSerialNumber(selectedBoardId);
    }

    @Override
    public int getSelectedBoardId() {
        return selectedBoardId;
    }

    @Override
    public boolean getIsEnabled() {
        return boardEnabled;
    }

    @Override
    public void setConnected(boolean connected) {
        jCBBoardId.setEnabled(!connected);
    }
    
    @Override
    public void onDataReceived(AppDataMessage rxMessage) {
        
        if (!boardEnabled) {
            return;
        }        
        
        // Evaluate my needs
        Integer freeMem = shieldProtocolLayer.evalFreeMemory(rxMessage, selectedBoardId);
        if (freeMem != null) {
            jLabelFreeMem.setText(freeMem.toString() + " bytes");
        }
        
        String serialBoard = shieldProtocolLayer.evalReadBoardSerialNumber(rxMessage, selectedBoardId);
        if (serialBoard != null) {
            boardSerialNumber = serialBoard;
            jLabelBoardSerialNumber.setText(serialBoard);
        }

        // Check for PTH version (Rev < 1.4 have BMP180, Rev >= 1.4 have BMP280 pressure sensors)
        String setupName = shieldProtocolLayer.evalSensorInquiry(rxMessage, selectedBoardId, PRESS_SENSOR_CHANNEL_ID);
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

    @Override
    public void storeToBoard() {
        if (!boardEnabled) {
            return;
        }        
        
        // Generate the command list to be sent to be board
        for(SensorSetupDialog setupDialog : sensorSetupDialogs) {

            // Render the command for this dialog
            setupDialog.storeToBoard();

            try {
                // Wait some time
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(AirSensEURPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }   
    }

    @Override
    public void readFromBoard() {
        if (!boardEnabled) {
            return;
        }        
        
        // Generate the command list to be sent to the board
        for (SensorSetupDialog setupDialog : sensorSetupDialogs) {

            // Render commands for this dialog
            setupDialog.readFromBoard();

            try {
                // Wait some time
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(AirSensEURPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void startSample() {
        if (!boardEnabled) {
            return;
        }
        
        // Add the start sample command to the buffer
        shieldProtocolLayer.renderStartSample(selectedBoardId);
    }

    @Override
    public void stopSample() {
        if (!boardEnabled) {
            return;
        }
        
        // Add the stop sample command to the buffer
        shieldProtocolLayer.renderStopSample(selectedBoardId);
    }

    @Override
    public void onDataMessageFromConfiguration(AppDataMessage configurationMessage) {
        
        // Target the configuration message to the selected board Id
        AppDataMessage targetConfigurationMessage = new AppDataMessage(selectedBoardId, configurationMessage.getCommandString(), configurationMessage.getCommandComment());
        
        // Loop on each panel and propagate this message
        for(SensorSetupDialog setupDialog : sensorSetupDialogs) {
            setupDialog.onDataMessageFromConfiguration(targetConfigurationMessage);
        }
    }

    @Override
    public void onGetCurrentConfiguation(boolean forceRestartSampling) {
        
        // Add the stop sample command to the buffer, if required
        if (forceRestartSampling) {
            shieldProtocolLayer.renderStopSample(selectedBoardId);
        }
        
        for(SensorSetupDialog setupDialog : sensorSetupDialogs) {

            // Render the command for this dialog
            setupDialog.storeToBoard();
        }
        
        // Add the start sample command to the buffer, if required
        if (forceRestartSampling) {
            shieldProtocolLayer.renderStartSample(selectedBoardId);
        }
    }
    
    @Override
    public String getBoardSerialNumber() {
        return boardSerialNumber;
    }
    
    @Override
    public void onDatabaseChanged() {
        
        sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_1).onSensorPresetDatabaseChanged();
        sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_2).onSensorPresetDatabaseChanged();
        sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_3).onSensorPresetDatabaseChanged();
        sensorSetupDialogs.get(CHEM_SENSOR_CHANNEL_ID_4).onSensorPresetDatabaseChanged();
    }
   
    @Override
    public void collectHostConfigurationInformation(HostConfigWriter hostConfigWriter) {
        
        // Start with chemical sensors channels
        for (int n = CHEM_SENSOR_CHANNEL_ID_1; n <= CHEM_SENSOR_CHANNEL_ID_4; n++) {
            HostConfigSensorProperties sensorProperties = hostConfigWriter.addNewSensor();
            
            sensorProperties.setSensorBoardId(selectedBoardId);
            sensorProperties.setSensorChannel(n);
            sensorProperties.setSensorHiRes(false);
            sensorProperties.setSensorExpression(CHEM_SENSOR_CHANNEL_MATH_EXPRESSION);
        }
        
        // Pressure
        int selected = jCBpthRevision.getSelectedIndex();
        HostConfigSensorProperties sensorProperties = hostConfigWriter.addNewSensor();
        sensorProperties.setSensorName(PRESS_SENSOR_NAME);
        sensorProperties.setSensorBoardId(selectedBoardId);
        sensorProperties.setSensorChannel(PRESS_SENSOR_CHANNEL_ID);
        sensorProperties.setSensorHiRes(false);
        sensorProperties.setSensorExpression((selected==0)? UR100CD_PRESS_CHANNEL_MATH_EXPRESSION : SHT31_PRESS_CHANNEL_MATH_EXPRESSION);
        
        
        // Temperature 
        sensorProperties = hostConfigWriter.addNewSensor();
        sensorProperties.setSensorName(TEMP_SENSOR_NAME);
        sensorProperties.setSensorBoardId(selectedBoardId);
        sensorProperties.setSensorChannel(TEMP_SENSOR_CHANNEL_ID);
        sensorProperties.setSensorHiRes(false);
        sensorProperties.setSensorExpression((selected==0)? UR100CD_TEMP_CHANNEL_MATH_EXPRESSION : SHT31_TEMP_CHANNEL_MATH_EXPRESSION);
        

        // Humidity
        sensorProperties = hostConfigWriter.addNewSensor();
        sensorProperties.setSensorName(HUM_SENSOR_NAME);
        sensorProperties.setSensorBoardId(selectedBoardId);
        sensorProperties.setSensorBoardId(HUM_SENSOR_CHANNEL_ID);
        sensorProperties.setSensorHiRes(false);
        sensorProperties.setSensorExpression(HUMIDITY_CHANNEL_MATH_EXPRESSION);
    }
    

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sampleLogger0 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        sampleLogger1 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        sampleLogger2 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        sampleLogger3 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        sampleLogger4 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        sampleLogger5 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        sampleLogger6 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        jCBpthRevision = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabelFreeMem = new javax.swing.JLabel();
        jCBBoardId = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabelBoardSerialNumber = new javax.swing.JLabel();

        jCBpthRevision.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PTH Revision < 1.4", "PTH Revision >= 1.4" }));
        jCBpthRevision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBpthRevisionActionPerformed(evt);
            }
        });

        jLabel1.setText("Available board RAM:");

        jLabelFreeMem.setText("--");

        jCBBoardId.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Not Connected", "ID 0", "ID 1", "ID 2", "ID 3", "ID 4", "ID 5", "ID 6", "ID 7", "ID 8", "ID 9", "ID 10", "ID 11", "ID 12", "ID 13", "ID 14" }));
        jCBBoardId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBBoardIdActionPerformed(evt);
            }
        });

        jLabel2.setText("Board ID:");

        jLabel3.setText("Board serial number:");

        jLabelBoardSerialNumber.setText("--");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLogger0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLogger2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLogger1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLogger3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sampleLogger4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sampleLogger5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sampleLogger6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCBpthRevision, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelFreeMem, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelBoardSerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(143, 143, 143))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabelFreeMem)
                    .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabelBoardSerialNumber))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sampleLogger4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(sampleLogger5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(sampleLogger6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCBpthRevision, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleLogger1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLogger0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleLogger2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLogger3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

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

    private void jCBBoardIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBBoardIdActionPerformed
        onBoardIDChanged();
    }//GEN-LAST:event_jCBBoardIdActionPerformed


    private void onBoardIDChanged() {
        int selected = jCBBoardId.getSelectedIndex();
        if (selected == 0) {
            boardEnabled = false;
            selectedBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
        } else {
            boardEnabled = true;
            selectedBoardId = selected - 1;
        }
        
        // Update loggers
        for (SampleLogger tempLogger:sampleLoggerPanels) {
            tempLogger.setBoardId(selectedBoardId);
        }
        
        // Update setup dialogs
        for (SensorSetupDialog setupDialog:sensorSetupDialogs) {
            setupDialog.setBoardId(selectedBoardId);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jCBBoardId;
    private javax.swing.JComboBox jCBpthRevision;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabelBoardSerialNumber;
    private javax.swing.JLabel jLabelFreeMem;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLogger0;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLogger1;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLogger2;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLogger3;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLogger4;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLogger5;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLogger6;
    // End of variables declaration//GEN-END:variables

}
