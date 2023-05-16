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
public class SPS30SensorSetupDialog extends SensorSetupDialog {
    
    private final String sensorName = "SPS30";
    private final int pmChanNumber;
    private final int sensorBin0Channel;
    private final int binsChanNumber;
    private final int typicalSizeChannel;

    /**
     * Creates new form GenericSensorSetupDIalog
     * @param sensorId
     * @param pmChanNumber: Number of available PM channels
     * @param sensorBin0Channel: First channel of Bin0
     * @param binsChanNumber: Number of bins in the histogram
     * @param typicalSizeChannel: Typical size value channel
     * @param parent
     * @param modal
     */
    public SPS30SensorSetupDialog(int sensorId, int pmChanNumber, int sensorBin0Channel, int binsChanNumber, int typicalSizeChannel, MainApplicationFrame parent, boolean modal) {
        super(parent, modal, sensorId);
        
        this.pmChanNumber = pmChanNumber;
        this.sensorBin0Channel = sensorBin0Channel;
        this.binsChanNumber = binsChanNumber;
        this.typicalSizeChannel = typicalSizeChannel;
                
        initComponents();
        
        iIRAndAvgPanel.setBoardId(boardId);
        iIRAndAvgPanel.setChannelId(sensorId);
        iIRAndAvgPanel.disablePrescaler();
        iIRAndAvgPanel.disableIIRSection();
        
        // Channels are enabled by default
        jCheckBoxPMEnabled.setSelected(true);
        jCheckBoxBinsEnabled.setSelected(true);
        jCheckBoxTypSizeEnabled.setSelected(true);
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
        
        boolean pmEnabled = jCheckBoxPMEnabled.isSelected();
        for (int channel = sensorId; channel < (sensorId + pmChanNumber); channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, pmEnabled);
        }
        boolean binsEnabled = jCheckBoxBinsEnabled.isSelected();
        for (int channel = sensorBin0Channel; channel < (sensorBin0Channel + binsChanNumber); channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, binsEnabled);
        }
        boolean typSizeEnabled = jCheckBoxTypSizeEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, typicalSizeChannel, typSizeEnabled);
        
        if (shieldProtocolLayer != null) {
            shieldProtocolLayer.renderSavePresetWithName(boardId, sensorId, this.sensorName);
        }
    }    

    @Override
    public void readFromBoard() throws SensorBusException {

        iIRAndAvgPanel.readFromBoard();
        
        shieldProtocolLayer.renderReadSensorSerialNumber(boardId, sensorId);
        
        for (int channel = sensorId; channel < (sensorId + binsChanNumber + pmChanNumber); channel++) {
            shieldProtocolLayer.renderReadChannelEnable(boardId, channel);
        }
    }

    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        iIRAndAvgPanel.evaluateRxMessage(rxMessage);
                
        // Channel enabled
        Boolean pmEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorId);
        if (pmEnabled != null) {
            jCheckBoxPMEnabled.setSelected(pmEnabled);
        }
        Boolean binsEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorBin0Channel);
        if (binsEnabled != null) {
            jCheckBoxBinsEnabled.setSelected(binsEnabled);
        }
        Boolean typSizeEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, typicalSizeChannel);
        if (typSizeEnabled != null) {
            jCheckBoxTypSizeEnabled.setSelected(typSizeEnabled);
        }
    }
    
    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        iIRAndAvgPanel.onDataMessageFromConfiguration(rxMessage);
        
        // Channel enabled
        Boolean pmEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorId);
        if (pmEnabled != null) {
            jCheckBoxPMEnabled.setSelected(pmEnabled);
        }
        Boolean binsEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorBin0Channel);
        if (binsEnabled != null) {
            jCheckBoxBinsEnabled.setSelected(binsEnabled);
        }   
        Boolean typSizeEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, typicalSizeChannel);
        if (typSizeEnabled != null) {
            jCheckBoxTypSizeEnabled.setSelected(typSizeEnabled);
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
        jCheckBoxBinsEnabled = new javax.swing.JCheckBox();
        jCheckBoxTypSizeEnabled = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(sensorName);
        setResizable(false);

        jCheckBoxPMEnabled.setText("PM Channels Enabled");
        jCheckBoxPMEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPMEnabledActionPerformed(evt);
            }
        });

        jCheckBoxBinsEnabled.setText("Bins Channels Enabled");
        jCheckBoxBinsEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxBinsEnabledActionPerformed(evt);
            }
        });

        jCheckBoxTypSizeEnabled.setText("TypicalSize Channel Enabled");
        jCheckBoxTypSizeEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxTypSizeEnabledActionPerformed(evt);
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
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jCheckBoxPMEnabled))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jCheckBoxBinsEnabled)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxTypSizeEnabled)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxBinsEnabled)
                    .addComponent(jCheckBoxTypSizeEnabled))
                .addGap(3, 3, 3)
                .addComponent(jCheckBoxPMEnabled)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxPMEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPMEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxPMEnabledActionPerformed

    private void jCheckBoxBinsEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxBinsEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxBinsEnabledActionPerformed

    private void jCheckBoxTypSizeEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxTypSizeEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxTypSizeEnabledActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel iIRAndAvgPanel;
    private javax.swing.JCheckBox jCheckBoxBinsEnabled;
    private javax.swing.JCheckBox jCheckBoxPMEnabled;
    private javax.swing.JCheckBox jCheckBoxTypSizeEnabled;
    // End of variables declaration//GEN-END:variables

}
