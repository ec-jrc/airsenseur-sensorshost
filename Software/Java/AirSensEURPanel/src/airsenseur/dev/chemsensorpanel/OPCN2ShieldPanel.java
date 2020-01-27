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
import airsenseur.dev.chemsensorpanel.setupdialogs.MOXSensorSetupDialog;
import airsenseur.dev.chemsensorpanel.setupdialogs.OPCN2SensorSetupDialog;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.exceptions.SensorBusException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author marco
 */
public class OPCN2ShieldPanel extends GenericTabPanel {

    public final static int HISTOGRAM_BINS_NUM = 16;
    public final static int PM_BINS_NUM = 3;
    public final static int AVERAGER_FOR_PM1  = HISTOGRAM_BINS_NUM;
    public final static int AVERAGER_FOR_PM25 = (AVERAGER_FOR_PM1+1);
    public final static int AVERAGER_FOR_PM10 = (AVERAGER_FOR_PM25+1);
    public final static int AVERAGER_FOR_TEMP = (AVERAGER_FOR_PM10+1);
    public final static int AVERAGER_FOR_VOLUME = (AVERAGER_FOR_TEMP+1);
    public final static int AVERAGER_FOR_MOX = (AVERAGER_FOR_VOLUME+1);

    public final static int OPCN2SENSOR_NUM_OF_CHANNELS = 21;
    public final static int OPCN2_CHANNEL_BIN1  = 0;
    public final static int OPCN2_CHANNEL_PM1 = OPCN2_CHANNEL_BIN1 + HISTOGRAM_BINS_NUM;
    public final static int MOX_CHANNEL = AVERAGER_FOR_MOX;
    public final static int MOX_NUM_OF_CHANNELS = 1;
    
    public final static int OPCN2_SETUP_DIALOG_OPC = 0;
    public final static int OPCN2_SETUP_DIALOG_MOX = 1;
    
    private final static String DEFAULT_CHANNEL_MATH_EXPRESSION =  "x/10000";
    
    private int selectedBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
    private boolean boardEnabled = false;
    private String boardSerialNumber = "";
    
    // The chemical sensors setup panels
    private final List<SensorSetupDialog> sensorSetupDialogs = new ArrayList<>();
    
    private final List<SampleLogger> sampleLoggerPanels = new ArrayList<>();
    
    // This is used only by the graphical composition tool in the Netbeans IDE
    public OPCN2ShieldPanel() {
        initComponents();
    }

    /**
     * Creates new form ChemSensorPanel
     * @param parent
     * @param shieldProtocolLayer
     * @param logger
     */
    public OPCN2ShieldPanel(MainApplicationFrame parent, ShieldProtocolLayer shieldProtocolLayer, FileLogger logger) {
        super(shieldProtocolLayer, logger);
        
        // Generate the sensor setup dialogs
        sensorSetupDialogs.add(new OPCN2SensorSetupDialog(parent, false, OPCN2_CHANNEL_BIN1, OPCN2SENSOR_NUM_OF_CHANNELS));
        sensorSetupDialogs.add(new MOXSensorSetupDialog(parent, false, MOX_CHANNEL, MOX_NUM_OF_CHANNELS));
        
        initComponents();
        
        // Aggregate sample loggers so it's more easy to handle them
        sampleLoggerPanels.add(sampleLogger0);
        sampleLoggerPanels.add(sampleLogger1);
        sampleLoggerPanels.add(sampleLogger2);
        sampleLoggerPanels.add(sampleLogger3);
        sampleLoggerPanels.add(sampleLogger4);
        
        // Initialize all loggers with common properties
        sampleLogger0.setLoggerProperties("OPCN2 - Bins [#/ml] x 1000)", 0, 5, HISTOGRAM_BINS_NUM);
        sampleLogger0.setSensorId(OPCN2_CHANNEL_BIN1);
        sampleLogger0.setDataProcessing(new SampleLogger.DataProcessing() {

            @Override
            public double processSample(double sample) {
                return sample / 10;
            }
        });
        
        sampleLogger1.setLoggerProperties("OPCN2 - PM1, 2.5, 10 [ug/m3]", 0, 5, PM_BINS_NUM);
        sampleLogger1.setSensorId(OPCN2_CHANNEL_PM1);
        sampleLogger1.setDataProcessing(SampleLogger.highResSampleBaseDefaultDataProcessing);
        
        // Update some logger with specific properties
        sampleLogger2.setLoggerProperties("Temperature [C]", 0, 65535, 10);
        sampleLogger2.setSensorId(AVERAGER_FOR_TEMP);
        sampleLogger2.setHighResolutionMode();
        sampleLogger2.setDataProcessing(SampleLogger.highResSampleBaseDefaultDataProcessing);
        
        sampleLogger3.setLoggerProperties("Volume [ml]", 0, -1, 10);
        sampleLogger3.setSensorId(AVERAGER_FOR_VOLUME);
        sampleLogger3.setHighResolutionMode();
        sampleLogger3.setDataProcessing(SampleLogger.highResSampleBaseDefaultDataProcessing);
        
        sampleLogger4.setLoggerProperties("MOX [ohm]", 0, -1, 10);
        sampleLogger4.setSensorId(AVERAGER_FOR_MOX);
        sampleLogger4.setHighResolutionMode();
        sampleLogger4.setDataProcessing(SampleLogger.highResSampleBaseDefaultDataProcessing);
        
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).setLogger(logger);
            sampleLoggerPanels.get(n).setBoardId(selectedBoardId);
            sampleLoggerPanels.get(n).setShieldProtocolLayer(shieldProtocolLayer);            
        }

        // Initialize al sensorSetupDialogs
        int sensorId = OPCN2_CHANNEL_BIN1;
        for (SensorSetupDialog dialog:sensorSetupDialogs) {
            dialog.setShieldProtocolLayer(shieldProtocolLayer);
            dialog.setChannelId(sensorId);
            dialog.setBoardId(selectedBoardId);
            sensorId++;
        }
        
        
        // Board ID handling
        onBoardIDChanged();
    }

    @Override
    public List<SensorSetupDialog> getSensorSetupDialogs() {
        return sensorSetupDialogs;
    }
    
    @Override
    public void onRefreshTimer() throws SensorBusException {
        if (!boardEnabled) {
            return;
        }        
        
        // Ask for a sample
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).readFromBoard();
        }
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
    public void storeToBoard() throws SensorBusException {
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
    public void readFromBoard() throws SensorBusException {
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
    public void startSample() throws SensorBusException {
        if (!boardEnabled) {
            return;
        }
        
        // Add the start sample command to the buffer
        shieldProtocolLayer.renderStartSample(selectedBoardId);
    }

    @Override
    public void stopSample() throws SensorBusException {
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
    public void onGetCurrentConfiguation(boolean forceRestartSampling) throws SensorBusException {
        
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
        
        sensorSetupDialogs.get(OPCN2_CHANNEL_BIN1).onSensorPresetDatabaseChanged();
    }
    
    @Override
    public void collectHostConfigurationInformation(HostConfigWriter hostConfigWriter) {
        
        // All channels in the OPCN2 Shields have the same behaviour. 
        // Channel name is read from the shield at startup
        for (int n = 0; n <= AVERAGER_FOR_MOX; n++) {
            HostConfigSensorProperties sensorProperties = hostConfigWriter.addNewSensor();
            sensorProperties.setSensorBoardId(selectedBoardId);
            sensorProperties.setSensorChannel(n);
            sensorProperties.setSensorExpression(DEFAULT_CHANNEL_MATH_EXPRESSION);
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

        jCBBoardId = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        sampleLogger0 = new airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel();
        sampleLogger1 = new airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel();
        sampleLogger2 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        sampleLogger3 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        sampleLogger4 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();

        jCBBoardId.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Not Connected", "ID 0", "ID 1", "ID 2", "ID 3", "ID 4", "ID 5", "ID 6", "ID 7", "ID 8", "ID 9", "ID 10", "ID 11", "ID 12", "ID 13", "ID 14" }));
        jCBBoardId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBBoardIdActionPerformed(evt);
            }
        });

        jLabel2.setText("Board ID:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleLogger0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLogger3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(sampleLogger1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sampleLogger2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(sampleLogger4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLogger0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLogger2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLogger1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLogger3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLogger4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

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
        for (SampleLogger sampleLogger:sampleLoggerPanels) {
            sampleLogger.setBoardId(selectedBoardId);
        }
        
        // Update setup dialogs
        for (SensorSetupDialog setupDialog:sensorSetupDialogs) {
            setupDialog.setBoardId(selectedBoardId);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jCBBoardId;
    private javax.swing.JLabel jLabel2;
    private airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel sampleLogger0;
    private airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel sampleLogger1;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLogger2;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLogger3;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLogger4;
    // End of variables declaration//GEN-END:variables

}
