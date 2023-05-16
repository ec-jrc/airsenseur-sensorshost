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

package airsenseur.dev.chemsensorpanel.setupdialogs;

import airsenseur.dev.chemsensorpanel.MainApplicationFrame;
import airsenseur.dev.chemsensorpanel.SensorSetupDialog;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.exceptions.SensorBusException;

/**
 *
 * @author marco
 */
public class ADT7470SetupDialog extends SensorSetupDialog {
    
    private final String sensorName = "ADT7470";
    private final int startTempChanNumber;
    private final int startFanChanNumber;
    private final int NUM_OF_TEMP_SENSORS = 3;
    private final int NUM_OF_FANS = 3;

    /**
     * Creates new form GenericSensorSetupDIalog
     * @param sensorId
     * @param startTempChanNumber: the 1st channel number for temperature
     * @param startFanChanNumber: the 1st channel number for fan
     * @param tempChamberChannelNumber
     * @param extFanChanNumber
     * @param intFanChanNumber
     * @param airCircFanChanNumber
     * @param parent
     * @param modal
     */
    public ADT7470SetupDialog(int sensorId, 
                                int startTempChanNumber, 
                                int startFanChanNumber,
                                int tempChamberChannelNumber,
                                int extFanChanNumber,
                                int intFanChanNumber,
                                int airCircFanChanNumber,
                                MainApplicationFrame parent, boolean modal) {
        super(parent, modal, sensorId);
        
        this.startTempChanNumber = startTempChanNumber;
        this.startFanChanNumber = startFanChanNumber;

        initComponents();
        
        // Start with significative labels
        panelInternalTemperature.setLabel("T Chamber:");
        panelAirCircFan.setLabel("Circ. Fan");
        panelIntHtrFan.setLabel("Int Fan");
        panelExtFan.setLabel("Ext Fan");
        
        // Initialize units for the cursor panels
        panelInternalTemperature.setUnit("C");
        panelAirCircFan.setUnit("%");
        panelIntHtrFan.setUnit("%");
        panelExtFan.setUnit("%");
        
        // Minimum and maximum temperature setpoints
        panelInternalTemperature.setMinimum(5);
        panelInternalTemperature.setMaximum(45);
        panelInternalTemperature.setTickSpacing(10);
        
        // Suggested internal chamber temperature
        panelInternalTemperature.setValue(40);
        
        // Initialize panels with board and channel information
        panelInternalTemperature.setBoardId(boardId);
        panelInternalTemperature.setChannelId(tempChamberChannelNumber);
        
        panelAirCircFan.setBoardId(boardId);
        panelAirCircFan.setChannelId(airCircFanChanNumber);
        panelAirCircFan.setTickSpacing(100);
        
        panelIntHtrFan.setBoardId(boardId);
        panelIntHtrFan.setChannelId(intFanChanNumber);
        panelIntHtrFan.setTickSpacing(100);
        
        panelExtFan.setBoardId(boardId);
        panelExtFan.setChannelId(extFanChanNumber);
        panelExtFan.setTickSpacing(100);
        
        iIRAndAvgPanel.setBoardId(boardId);
        iIRAndAvgPanel.setChannelId(sensorId);
        iIRAndAvgPanel.disablePrescaler();
        iIRAndAvgPanel.disableIIRSection();
        
        // Channels are enabled by default
        jCheckFansEnabled.setSelected(true);
        jCheckBoxTemperatureEnabled.setSelected(true);
    }

    @Override
    public void setBoardId(int boardId) {
        super.setBoardId(boardId); 
        
        panelInternalTemperature.setBoardId(boardId);
        panelAirCircFan.setBoardId(boardId);
        panelIntHtrFan.setBoardId(boardId);
        panelExtFan.setBoardId(boardId);        
        iIRAndAvgPanel.setBoardId(boardId);
    }

    @Override
    public void setShieldProtocolLayer(ShieldProtocolLayer shieldProtocolLayer) {
        super.setShieldProtocolLayer(shieldProtocolLayer);
        
        panelInternalTemperature.setShieldProtocolLayer(shieldProtocolLayer);
        panelAirCircFan.setShieldProtocolLayer(shieldProtocolLayer);
        panelIntHtrFan.setShieldProtocolLayer(shieldProtocolLayer);
        panelExtFan.setShieldProtocolLayer(shieldProtocolLayer);        
        iIRAndAvgPanel.setShieldProtocolLayer(shieldProtocolLayer);
    }
    
    /**
     * Update the board with the information set on this setup panel
     * @throws airsenseur.dev.exceptions.SensorBusException
     */
    @Override
    public void storeToBoard() throws SensorBusException {
        
        panelInternalTemperature.storeToBoard();
        panelAirCircFan.storeToBoard();
        panelIntHtrFan.storeToBoard();
        panelExtFan.storeToBoard();        
        iIRAndAvgPanel.storeToBoard();
        
        boolean fansEnabled = jCheckFansEnabled.isSelected();
        for (int channel = startFanChanNumber; channel < startFanChanNumber + NUM_OF_FANS; channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, fansEnabled);
        }
        
        boolean tempEnabled = jCheckBoxTemperatureEnabled.isSelected();
        for (int channel = startTempChanNumber; channel < startTempChanNumber + NUM_OF_TEMP_SENSORS; channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, tempEnabled);
        }
        
        // Store the configuration into the board EEPROM (through a preset save)
        if (shieldProtocolLayer != null) {
            shieldProtocolLayer.renderSavePresetWithName(boardId, sensorId, this.sensorName);
        }
    }    

    @Override
    public void readFromBoard() throws SensorBusException {
        
        panelInternalTemperature.readFromBoard();
        panelAirCircFan.readFromBoard();
        panelIntHtrFan.readFromBoard();
        panelExtFan.readFromBoard();        

        iIRAndAvgPanel.readFromBoard();
        
        for (int channel = sensorId; channel < (sensorId + NUM_OF_TEMP_SENSORS + NUM_OF_FANS); channel++) {
            shieldProtocolLayer.renderReadChannelEnable(boardId, channel);
        }
    }

    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        panelInternalTemperature.evaluateRxMessage(rxMessage);
        panelAirCircFan.evaluateRxMessage(rxMessage);
        panelIntHtrFan.evaluateRxMessage(rxMessage);
        panelExtFan.evaluateRxMessage(rxMessage);        
        
        iIRAndAvgPanel.evaluateRxMessage(rxMessage);
        
        // Channel enabled
        Boolean fansEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, startFanChanNumber);
        if (fansEnabled != null) {
            jCheckFansEnabled.setSelected(fansEnabled);
        }
        
        Boolean tempEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, startTempChanNumber);
        if (tempEnabled != null) {
            jCheckBoxTemperatureEnabled.setSelected(tempEnabled);
        }
    }
    
    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        evaluateRxMessage(rxMessage);
    }
    
    @Override
    public void onSensorPresetDatabaseChanged() {
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        iIRAndAvgPanel = new airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel();
        jCheckFansEnabled = new javax.swing.JCheckBox();
        jCheckBoxTemperatureEnabled = new javax.swing.JCheckBox();
        panelInternalTemperature = new airsenseur.dev.chemsensorpanel.widgets.PresetCursorPanel();
        panelExtFan = new airsenseur.dev.chemsensorpanel.widgets.PresetCursorPanel();
        panelIntHtrFan = new airsenseur.dev.chemsensorpanel.widgets.PresetCursorPanel();
        panelAirCircFan = new airsenseur.dev.chemsensorpanel.widgets.PresetCursorPanel();
        jLabelSetpoints = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(sensorName);
        setResizable(false);

        jCheckFansEnabled.setText("Fan Channels Enabled");
        jCheckFansEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckFansEnabledActionPerformed(evt);
            }
        });

        jCheckBoxTemperatureEnabled.setText("Temperature Channels Enabled");
        jCheckBoxTemperatureEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxTemperatureEnabledActionPerformed(evt);
            }
        });

        jLabelSetpoints.setText("Setpoints:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(69, 69, 69)
                        .addComponent(jSeparator2))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jCheckFansEnabled)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxTemperatureEnabled))
                            .addComponent(jLabelSetpoints)
                            .addComponent(panelInternalTemperature, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelExtFan, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelIntHtrFan, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelAirCircFan, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckFansEnabled)
                    .addComponent(jCheckBoxTemperatureEnabled))
                .addGap(4, 4, 4)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(jLabelSetpoints)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelInternalTemperature, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelExtFan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelIntHtrFan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelAirCircFan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckFansEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckFansEnabledActionPerformed

    }//GEN-LAST:event_jCheckFansEnabledActionPerformed

    private void jCheckBoxTemperatureEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxTemperatureEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxTemperatureEnabledActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel iIRAndAvgPanel;
    private javax.swing.JCheckBox jCheckBoxTemperatureEnabled;
    private javax.swing.JCheckBox jCheckFansEnabled;
    private javax.swing.JLabel jLabelSetpoints;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private airsenseur.dev.chemsensorpanel.widgets.PresetCursorPanel panelAirCircFan;
    private airsenseur.dev.chemsensorpanel.widgets.PresetCursorPanel panelExtFan;
    private airsenseur.dev.chemsensorpanel.widgets.PresetCursorPanel panelIntHtrFan;
    private airsenseur.dev.chemsensorpanel.widgets.PresetCursorPanel panelInternalTemperature;
    // End of variables declaration//GEN-END:variables

}
