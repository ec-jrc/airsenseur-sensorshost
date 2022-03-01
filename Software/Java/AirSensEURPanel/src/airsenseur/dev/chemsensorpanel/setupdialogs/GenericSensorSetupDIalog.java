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
public class GenericSensorSetupDIalog extends SensorSetupDialog {
    
    private String sensorName = "Generic Sensor";
    private final boolean serialDisabled;
    private final int numChannels;

    /**
     * Creates new form GenericSensorSetupDIalog
     * @param sensorName
     * @param sensorId
     * @param numChannels: number of consecutive channels, if any. Used only for enable/disable channel checkbox
     * @param sensorSerialDisabled
     * @param prescalerDisabled
     * @param IIRDisabled
     * @param parent
     * @param modal
     */
    public GenericSensorSetupDIalog(String sensorName, int sensorId, int numChannels, boolean sensorSerialDisabled, boolean prescalerDisabled, boolean IIRDisabled, MainApplicationFrame parent, boolean modal) {
        super(parent, modal, sensorId);
        
        this.sensorName = sensorName;
        this.serialDisabled = sensorSerialDisabled;
        this.numChannels = numChannels;
        
        initComponents();
        
        iIRAndAvgPanel.setBoardId(boardId);
        iIRAndAvgPanel.setChannelId(sensorId);
        if (prescalerDisabled) {
            iIRAndAvgPanel.disablePrescaler();
        }
        if (IIRDisabled) {
            iIRAndAvgPanel.disableIIRSection();
        }
        if (serialDisabled) {
            jTxtSensorSerialNumber.setEnabled(false);
        }
        
        // Channel is enabled by default
        jCheckBoxChannelEnabled.setSelected(true);
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
        
        boolean channelsEnabled = jCheckBoxChannelEnabled.isSelected();
        for (int channel = sensorId; channel < sensorId + numChannels; channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, channelsEnabled);
        }
        
        if (shieldProtocolLayer != null) {
            shieldProtocolLayer.renderSavePresetWithName(boardId, sensorId, this.sensorName);
        }
        
        if (!serialDisabled) {
            shieldProtocolLayer.renderSaveSensorSerialNumber(boardId, sensorId, jTxtSensorSerialNumber.getText());
        }
    }    

    @Override
    public void readFromBoard() throws SensorBusException {

        iIRAndAvgPanel.readFromBoard();
        
        shieldProtocolLayer.renderReadSensorSerialNumber(boardId, sensorId);
        for (int channel = sensorId; channel < sensorId + numChannels; channel++) {
            shieldProtocolLayer.renderReadChannelEnable(boardId, channel);
        }
    }

    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        iIRAndAvgPanel.evaluateRxMessage(rxMessage);
        
        // Sensor Serial Number
        String serialNumber = shieldProtocolLayer.evalReadSensorSerialNumber(rxMessage, boardId, sensorId);
        if ((serialNumber != null) && !serialNumber.isEmpty()) {
            jTxtSensorSerialNumber.setText(serialNumber);
        }
        
        // Channel enabled
        Boolean chEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorId);
        if (chEnabled != null) {
            jCheckBoxChannelEnabled.setSelected(chEnabled);
        }
    }
    
    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        iIRAndAvgPanel.onDataMessageFromConfiguration(rxMessage);
        
        // Sensor Serial Number
        String serialNumber = shieldProtocolLayer.evalReadSensorSerialNumber(rxMessage, boardId, sensorId);
        if ((serialNumber != null) && !serialNumber.isEmpty()) {
            ((JOverridableTextField)jTxtSensorSerialNumber).setTextOverride(serialNumber);
        }
        
        // Channel enabled
        Boolean chEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorId);
        if (chEnabled != null) {
            jCheckBoxChannelEnabled.setSelected(chEnabled);
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
        jLabel12 = new javax.swing.JLabel();
        jTxtSensorSerialNumber = new JOverridableTextField();
        jCheckBoxChannelEnabled = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(sensorName);
        setResizable(false);

        jLabel12.setText("Serial Number:");

        jTxtSensorSerialNumber.setText("no serial");
        jTxtSensorSerialNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTxtSensorSerialNumberActionPerformed(evt);
            }
        });

        jCheckBoxChannelEnabled.setText("Channel Enabled");
        jCheckBoxChannelEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxChannelEnabledActionPerformed(evt);
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
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jCheckBoxChannelEnabled)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jTxtSensorSerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(65, 65, 65))))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(13, Short.MAX_VALUE)
                .addComponent(jCheckBoxChannelEnabled)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTxtSensorSerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTxtSensorSerialNumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTxtSensorSerialNumberActionPerformed

    }//GEN-LAST:event_jTxtSensorSerialNumberActionPerformed

    private void jCheckBoxChannelEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxChannelEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxChannelEnabledActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel iIRAndAvgPanel;
    private javax.swing.JCheckBox jCheckBoxChannelEnabled;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JTextField jTxtSensorSerialNumber;
    // End of variables declaration//GEN-END:variables

}
