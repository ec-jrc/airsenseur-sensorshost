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
import airsenseur.dev.chemsensorpanel.setupdialogs.GenericBoardInfoDialog;
import airsenseur.dev.chemsensorpanel.setupdialogs.GenericSensorSetupDIalog;
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
public class EnvShield1Panel extends GenericTabPanel {
    
    public final static int AUDIOFAST_CHANNEL = 0;
    public final static int AUDIOFAST_NUM_OF_CHANNELS = 3;
    
    public final static int AUDIOSLOW_CHANNEL = AUDIOFAST_CHANNEL + AUDIOFAST_NUM_OF_CHANNELS;
    public final static int AUDIOSLOW_NUM_OF_CHANNELS = 3;
    
    public final static int TEMPERATURE_CHANNEL = AUDIOSLOW_CHANNEL + AUDIOFAST_NUM_OF_CHANNELS;
    public final static int HUMIDITY_CHANNEL = TEMPERATURE_CHANNEL + 1;
    
    public final static int LIGHT_OPT3001_CHANNEL = HUMIDITY_CHANNEL + 1;
        
    public final static int ENVSHIELD1_NUM_OF_CHANNELS = LIGHT_OPT3001_CHANNEL + 1;

    public final static int ENVSHIELD1_SETUP_DIALOG_AUDIOFAST = 0;
    public final static int ENVSHIELD1_SETUP_DIALOG_AUDIOSLOW = 1;
    public final static int ENVSHIELD1_SETUP_TEMPERATURE_HUMIDITY = 2;
    public final static int ENVSHIELD1_SETUP_OPT3001 = 3;
    public final static int GENERIC_INFO_CHANNEL_ID = ENVSHIELD1_SETUP_OPT3001 + 1;    
    
    private int selectedBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
    private boolean boardEnabled = false;
    
    private final static String DEFAULT_CHANNEL_MATH_EXPRESSION =  "x";
    private final static String AUDIOFAST_CHANNEL_MATH_EXPRESSION = "x/100";
    private final static String AUDIOSLOW_CHANNEL_MATH_EXPRESSION = "x/100";
    private final static String SHT31_TEMP_CHANNEL_MATH_EXPRESSION = "((x/65535*175) - 45.0)";
    private final static String SHT31_HUMIDITY_CHANNEL_MATH_EXPRESSION = "(x/65535)*100.0";
    private final static String OPT3001_LIGHT_MATH_EXPRESSION = "x*2";
    
    private final static String AUDIOFAST_SENSOR_NAME = "Audio Fast";
    private final static String AUDIOSLOW_SENSOR_NAME = "Audio Slow";
    private final static String TEMP_HUMID_PANEL_NAME = "Temperature/Humidity";
    private final static String TEMP_SENSOR_NAME = "Temperature";
    private final static String HUM_SENSOR_NAME = "Humidity";
    private final static String OPT3001_SENSOR_NAME = "Light";
    
    // The chemical sensors setup panels
    private final List<SensorSetupDialog> sensorSetupDialogs = new ArrayList<>();
    
    private final List<SampleLogger> sampleLoggerPanels = new ArrayList<>();
    
    private String boardSerialNumber = "";
    
    // This is used only by the graphical composition tool in the Netbeans IDE
    public EnvShield1Panel() {
        initComponents();
    }

    /**
     * Creates new form ChemSensorPanel
     * @param parent
     * @param shieldProtocolLayer
     * @param logger
     */
    public EnvShield1Panel(MainApplicationFrame parent, ShieldProtocolLayer shieldProtocolLayer, FileLogger logger) {
        super(shieldProtocolLayer, logger);
        
        // Generate the sensor setup dialogs
        sensorSetupDialogs.add(new GenericSensorSetupDIalog(AUDIOFAST_SENSOR_NAME, AUDIOFAST_CHANNEL, AUDIOFAST_NUM_OF_CHANNELS, true, true, true, parent, false));        
        sensorSetupDialogs.add(new GenericSensorSetupDIalog(AUDIOSLOW_SENSOR_NAME, AUDIOSLOW_CHANNEL, AUDIOSLOW_NUM_OF_CHANNELS, true, true, true, parent, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog(TEMP_HUMID_PANEL_NAME, TEMPERATURE_CHANNEL, 2,true, false, false, parent, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog(OPT3001_SENSOR_NAME, LIGHT_OPT3001_CHANNEL, 1, true, false, false, parent, false));
        sensorSetupDialogs.add(new GenericBoardInfoDialog(parent, false, "Environment Shield 1 Generic Info"));        
        
        initComponents();
        
        // Aggregate sample loggers so it's more easy to handle them
        sampleLoggerPanels.add(sampleLoggerAudioFast);
        sampleLoggerPanels.add(sampleLoggerAudioSlow);
        sampleLoggerPanels.add(sampleLoggerTemperature);
        sampleLoggerPanels.add(sampleLoggerHumidity);
        sampleLoggerPanels.add(sampleLoggerOPT3001);
        
        // Initialize all loggers with common properties
        sampleLoggerAudioFast.setLoggerProperties("AudioFast [dB] x 100", 0, -1, 10);
        sampleLoggerAudioFast.setSensorId(AUDIOFAST_CHANNEL);
        
        sampleLoggerAudioSlow.setLoggerProperties("AudioSlow [dB] x 100 [ppm]", 0, -1, 10);
        sampleLoggerAudioSlow.setSensorId(AUDIOSLOW_CHANNEL);
        
        sampleLoggerTemperature.setLoggerProperties(TEMP_SENSOR_NAME, 0, 65535, 10);
        sampleLoggerTemperature.setSensorId(TEMPERATURE_CHANNEL);
        sampleLoggerTemperature.setDataProcessing(SampleLogger.sht31TemperatureDataProcessing);
        
        sampleLoggerHumidity.setLoggerProperties(HUM_SENSOR_NAME, 0, 65535, 10);
        sampleLoggerHumidity.setSensorId(HUMIDITY_CHANNEL);  
        sampleLoggerHumidity.setDataProcessing(SampleLogger.sht31HumidityDataProcessing);
        
        sampleLoggerOPT3001.setLoggerProperties(OPT3001_SENSOR_NAME, 0, 90000, 0);
        sampleLoggerOPT3001.setSensorId(LIGHT_OPT3001_CHANNEL);
        sampleLoggerOPT3001.setDataProcessing(new SampleLogger.DataProcessing() {
            @Override
            public double processSample(double sample) {
                return sample * 2;
            }
        });
        
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).setLogger(logger);
            sampleLoggerPanels.get(n).setBoardId(selectedBoardId);
            sampleLoggerPanels.get(n).setShieldProtocolLayer(shieldProtocolLayer);            
        }

        // Initialize al sensorSetupDialogs
        for (SensorSetupDialog dialog:sensorSetupDialogs) {
            dialog.setShieldProtocolLayer(shieldProtocolLayer);
            dialog.setBoardId(selectedBoardId);
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
        
        // Propagate to the configuration dialogs, they may require this information
        for (SensorSetupDialog dialog : sensorSetupDialogs) {
            dialog.setConnected(connected);
        }
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
        
        String serialBoard = shieldProtocolLayer.evalReadBoardSerialNumber(rxMessage, selectedBoardId);
        if (serialBoard != null) {
            boardSerialNumber = serialBoard;
            jLabelBoardSerialNumber.setText(serialBoard);
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
        // Nothing to do with presets
    }
    
    @Override
    public void collectHostConfigurationInformation(HostConfigWriter hostConfigWriter) {
        
        // All channels in the OPCN2 Shields have the same behaviour. 
        // Channel name is read from the shield at startup
        for (int n = 0; n <= ENVSHIELD1_NUM_OF_CHANNELS; n++) {
            HostConfigSensorProperties sensorProperties = hostConfigWriter.addNewSensor();
            sensorProperties.setSensorBoardId(selectedBoardId);
            sensorProperties.setSensorChannel(n);
            
            if (n == AUDIOSLOW_CHANNEL) {
                sensorProperties.setSensorExpression(AUDIOSLOW_CHANNEL_MATH_EXPRESSION);
            } else if (n >= AUDIOFAST_CHANNEL) {
                sensorProperties.setSensorExpression(AUDIOFAST_CHANNEL_MATH_EXPRESSION);
            } else {
                sensorProperties.setSensorExpression(DEFAULT_CHANNEL_MATH_EXPRESSION);
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

        jCBBoardId = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        sampleLoggerAudioFast = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        sampleLoggerAudioSlow = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        jLabelBoardSerialNumber = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        sampleLoggerTemperature = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        sampleLoggerHumidity = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        sampleLoggerOPT3001 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();

        setPreferredSize(new java.awt.Dimension(858, 547));

        jCBBoardId.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Not Connected", "ID 0", "ID 1", "ID 2", "ID 3", "ID 4", "ID 5", "ID 6", "ID 7", "ID 8", "ID 9", "ID 10", "ID 11", "ID 12", "ID 13", "ID 14" }));
        jCBBoardId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBBoardIdActionPerformed(evt);
            }
        });

        jLabel2.setText("Board ID:");

        jLabelBoardSerialNumber.setText("--");

        jLabel3.setText("Board serial number:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sampleLoggerAudioFast, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleLoggerAudioSlow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleLoggerTemperature, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLoggerHumidity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLoggerOPT3001, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelBoardSerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(jLabelBoardSerialNumber))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(sampleLoggerAudioSlow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(sampleLoggerAudioFast, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sampleLoggerTemperature, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleLoggerHumidity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleLoggerOPT3001, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(217, Short.MAX_VALUE))
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
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabelBoardSerialNumber;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLoggerAudioFast;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLoggerAudioSlow;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLoggerHumidity;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLoggerOPT3001;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLoggerTemperature;
    // End of variables declaration//GEN-END:variables

}
