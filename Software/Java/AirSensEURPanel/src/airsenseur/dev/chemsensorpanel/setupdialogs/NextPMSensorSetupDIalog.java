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
import airsenseur.dev.chemsensorpanel.widgets.JOverridableTextField;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.exceptions.SensorBusException;

/**
 *
 * @author marco
 */
public class NextPMSensorSetupDIalog extends SensorSetupDialog {
    
    private final String sensorName = "NEXTPM";
    private final int pmChanNumber;
    private final int sensorPcs0Channel;
    private final int pcsChanNumber;
    private final int temperatureChannel;
    private final int humidityChannel;
    private final int stateChannel;
    private final int totalChanNum;

    /**
     * Creates new form GenericSensorSetupDIalog
     * @param sensorId: 1st PM sampler channel
     * @param pmChanNumber: Number of available PM channels
     * @param sensorPcs0Channel: First channel of Pcs0 sampler 
     * @param pcsChanNumber: Number of bins in the histogram
     * @param temperatureChannel: Typical size value channel
     * @param parent
     * @param modal
     */
    public NextPMSensorSetupDIalog(int sensorId, int pmChanNumber, int sensorPcs0Channel, 
                                    int pcsChanNumber, int temperatureChannel, int humidityChannel, int stateChannel,
                                    MainApplicationFrame parent, boolean modal) {
        super(parent, modal, sensorId);
        
        // NOTE: sensorID points to the 1st PM channel but is NOT the 1st channel for this sensor
        // The 1st channel for this sensor is represented by sensorPcs0Channel
        
        this.pmChanNumber = pmChanNumber;
        this.sensorPcs0Channel = sensorPcs0Channel;
        this.pcsChanNumber = pcsChanNumber;
        this.temperatureChannel = temperatureChannel;
        this.humidityChannel = humidityChannel;
        this.stateChannel = stateChannel;
        this.totalChanNum = pmChanNumber + pcsChanNumber + 3; // PM + PCS + Temperature + Humidity + status
                
        initComponents();
        
        iIRAndAvgPanel.setBoardId(boardId);
        iIRAndAvgPanel.setChannelId(sensorPcs0Channel);
        iIRAndAvgPanel.disablePrescaler();
        iIRAndAvgPanel.disableIIRSection();
        
        jTxtSensorSerialNumber.setEnabled(true);
        
        // Channels are enabled by default
        jCheckBoxPMEnabled.setSelected(true);
        jCheckBoxPcsEnabled.setSelected(true);
        jCheckBoxTemperatureEnabled.setSelected(true);
        jCheckBoxHumidityEnabled.setSelected(true);
        jCheckBoxStateEnabled.setSelected(true);
    }

    @Override
    public void setBoardId(int boardId) {
        super.setBoardId(boardId); 
        
        iIRAndAvgPanel.setBoardId(boardId);
    }

    @Override
    public void setShieldProtocolLayer(ShieldProtocolLayer shieldProtocolLayer) {
        super.setShieldProtocolLayer(shieldProtocolLayer);
        
        iIRAndAvgPanel.setShieldProtocolLayer(shieldProtocolLayer);
    }
    
    /**
     * Update the board with the information set on this setup panel
     * @throws airsenseur.dev.exceptions.SensorBusException
     */
    @Override
    public void storeToBoard() throws SensorBusException {
        iIRAndAvgPanel.storeToBoard();

        boolean binsEnabled = jCheckBoxPcsEnabled.isSelected();
        for (int channel = sensorPcs0Channel; channel < (sensorPcs0Channel + pcsChanNumber); channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, binsEnabled);
        }
        boolean pmEnabled = jCheckBoxPMEnabled.isSelected();
        for (int channel = sensorId; channel < (sensorId + pmChanNumber); channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, pmEnabled);
        }
        boolean temperatureEnabled = jCheckBoxTemperatureEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, temperatureChannel, temperatureEnabled);

        boolean humidityEnabled = jCheckBoxHumidityEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, humidityChannel, humidityEnabled);
        
        boolean stateEnabled = jCheckBoxStateEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, stateChannel, stateEnabled);
        
        if (shieldProtocolLayer != null) {
            shieldProtocolLayer.renderSavePresetWithName(boardId, sensorPcs0Channel, this.sensorName);
        }
        
        shieldProtocolLayer.renderSaveSensorSerialNumber(boardId, sensorPcs0Channel, jTxtSensorSerialNumber.getText());
    }    

    @Override
    public void readFromBoard() throws SensorBusException {

        iIRAndAvgPanel.readFromBoard();
        
        shieldProtocolLayer.renderReadSensorSerialNumber(boardId, sensorPcs0Channel);
        
        for (int channel = sensorPcs0Channel; channel < (sensorPcs0Channel + totalChanNum); channel++) {
            shieldProtocolLayer.renderReadChannelEnable(boardId, channel);
        }
    }

    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        iIRAndAvgPanel.evaluateRxMessage(rxMessage);
        
        // Sensor Serial Number
        String serialNumber = shieldProtocolLayer.evalReadSensorSerialNumber(rxMessage, boardId, sensorPcs0Channel);
        if ((serialNumber != null) && !serialNumber.isEmpty()) {
            jTxtSensorSerialNumber.setText(serialNumber);
        }
                
        // Channel enabled
        Boolean binsEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorPcs0Channel);
        if (binsEnabled != null) {
            jCheckBoxPcsEnabled.setSelected(binsEnabled);
        }
        Boolean pmEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorId);
        if (pmEnabled != null) {
            jCheckBoxPMEnabled.setSelected(pmEnabled);
        }
        Boolean temperatureChannelEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, temperatureChannel);
        if (temperatureChannelEnabled != null) {
            jCheckBoxTemperatureEnabled.setSelected(temperatureChannelEnabled);
        }
        Boolean humidityChannelEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, humidityChannel);
        if (humidityChannelEnabled != null) {
            jCheckBoxHumidityEnabled.setSelected(humidityChannelEnabled);
        }
        Boolean stateChannelEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, stateChannel);
        if (stateChannelEnabled != null) {
            jCheckBoxStateEnabled.setSelected(stateChannelEnabled);
        }
        
    }
    
    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        iIRAndAvgPanel.onDataMessageFromConfiguration(rxMessage);
        
        // Sensor Serial Number
        String serialNumber = shieldProtocolLayer.evalReadSensorSerialNumber(rxMessage, boardId, sensorPcs0Channel);
        if ((serialNumber != null) && !serialNumber.isEmpty()) {
            ((JOverridableTextField)jTxtSensorSerialNumber).setTextOverride(serialNumber);
        }
        
        // Channel enabled
        Boolean pmEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorId);
        if (pmEnabled != null) {
            jCheckBoxPMEnabled.setSelected(pmEnabled);
        }
        Boolean pcsEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorPcs0Channel);
        if (pcsEnabled != null) {
            jCheckBoxPcsEnabled.setSelected(pcsEnabled);
        }   
        Boolean temperatureEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, temperatureChannel);
        if (temperatureEnabled != null) {
            jCheckBoxTemperatureEnabled.setSelected(temperatureEnabled);
        }
        Boolean humidityEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, humidityChannel);
        if (humidityEnabled != null) {
            jCheckBoxHumidityEnabled.setSelected(humidityEnabled);
        }
        Boolean stateEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, stateChannel);
        if (stateEnabled != null) {
            jCheckBoxStateEnabled.setSelected(stateEnabled);
        }
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
        jCheckBoxPMEnabled = new javax.swing.JCheckBox();
        jCheckBoxPcsEnabled = new javax.swing.JCheckBox();
        jCheckBoxTemperatureEnabled = new javax.swing.JCheckBox();
        jCheckBoxHumidityEnabled = new javax.swing.JCheckBox();
        jCheckBoxStateEnabled = new javax.swing.JCheckBox();
        jLabel12 = new javax.swing.JLabel();
        jTxtSensorSerialNumber = new JOverridableTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(sensorName);
        setResizable(false);

        jCheckBoxPMEnabled.setText("PM Channels Enabled");
        jCheckBoxPMEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPMEnabledActionPerformed(evt);
            }
        });

        jCheckBoxPcsEnabled.setText("pcs Channels Enabled");
        jCheckBoxPcsEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPcsEnabledActionPerformed(evt);
            }
        });

        jCheckBoxTemperatureEnabled.setText("Temperature Channel Enabled");
        jCheckBoxTemperatureEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxTemperatureEnabledActionPerformed(evt);
            }
        });

        jCheckBoxHumidityEnabled.setText("Humidity Channel Enabled");
        jCheckBoxHumidityEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxHumidityEnabledActionPerformed(evt);
            }
        });

        jCheckBoxStateEnabled.setText("Status Channel Enabled");
        jCheckBoxStateEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxStateEnabledActionPerformed(evt);
            }
        });

        jLabel12.setText("Serial Number:");

        jTxtSensorSerialNumber.setText("no serial");
        jTxtSensorSerialNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTxtSensorSerialNumberActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(10, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jCheckBoxPcsEnabled)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxTemperatureEnabled))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                    .addGap(8, 8, 8)
                                    .addComponent(jLabel12)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jTxtSensorSerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                    .addComponent(jCheckBoxPMEnabled)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jCheckBoxStateEnabled)
                                        .addComponent(jCheckBoxHumidityEnabled)))))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxPcsEnabled)
                    .addComponent(jCheckBoxTemperatureEnabled))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxPMEnabled)
                    .addComponent(jCheckBoxHumidityEnabled))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxStateEnabled)
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jTxtSensorSerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxPMEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPMEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxPMEnabledActionPerformed

    private void jCheckBoxPcsEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPcsEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxPcsEnabledActionPerformed

    private void jCheckBoxTemperatureEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxTemperatureEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxTemperatureEnabledActionPerformed

    private void jCheckBoxHumidityEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxHumidityEnabledActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxHumidityEnabledActionPerformed

    private void jTxtSensorSerialNumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTxtSensorSerialNumberActionPerformed

    }//GEN-LAST:event_jTxtSensorSerialNumberActionPerformed

    private void jCheckBoxStateEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxStateEnabledActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxStateEnabledActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel iIRAndAvgPanel;
    private javax.swing.JCheckBox jCheckBoxHumidityEnabled;
    private javax.swing.JCheckBox jCheckBoxPMEnabled;
    private javax.swing.JCheckBox jCheckBoxPcsEnabled;
    private javax.swing.JCheckBox jCheckBoxStateEnabled;
    private javax.swing.JCheckBox jCheckBoxTemperatureEnabled;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JTextField jTxtSensorSerialNumber;
    // End of variables declaration//GEN-END:variables

}
