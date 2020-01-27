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
public class OPCN3SensorSetupDIalog extends SensorSetupDialog {
    
    private final String sensorName = "OPC-N3";
    private final int pmChanNumber;
    private final int sensorBin0Channel;
    private final int binsChanNumber;
    private final int sensorTHChannel;
    private final int sensorDebugChannel;
    private final int debugChanNumber;

    /**
     * Creates new form GenericSensorSetupDIalog
     * @param sensorId
     * @param pmChanNumber
     * @param sensorBin0Channel
     * @param binsChanNumber
     * @param sensorTHChannel
     * @param sensorDebugChannel
     * @param debugChanNumber
     * @param parent
     * @param modal
     */
    public OPCN3SensorSetupDIalog(int sensorId, int pmChanNumber, 
                                int sensorBin0Channel, int binsChanNumber, 
                                int sensorTHChannel,
                                int sensorDebugChannel, int debugChanNumber,
                                MainApplicationFrame parent, boolean modal) {
        super(parent, modal, sensorId);
        
        this.pmChanNumber = pmChanNumber;
        this.sensorBin0Channel = sensorBin0Channel;
        this.binsChanNumber = binsChanNumber;
        this.sensorTHChannel = sensorTHChannel;
        this.sensorDebugChannel = sensorDebugChannel;
        this.debugChanNumber = debugChanNumber;
                
        initComponents();
        
        iIRAndAvgPanel.setBoardId(boardId);
        iIRAndAvgPanel.setChannelId(sensorBin0Channel);
        iIRAndAvgPanel.disableIIRSection();
        
        // Channel is enabled by default
        jCheckBoxPMEnabled.setSelected(true);
        jCheckBoxBinsEnabled.setSelected(true);
        jCheckBoxTHEnabled.setSelected(true);
        jCheckBoxDebugChEnabled.setSelected(true);
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
        
        boolean binsEnabled = jCheckBoxBinsEnabled.isSelected();
        for (int channel = sensorBin0Channel; channel < (sensorBin0Channel + binsChanNumber); channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, binsEnabled);
        }
        
        boolean pmEnabled = jCheckBoxPMEnabled.isSelected();
        for (int channel = sensorId; channel < (sensorId + pmChanNumber); channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, pmEnabled);
        }
        
        boolean thEnabled = jCheckBoxTHEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorTHChannel, thEnabled);
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorTHChannel+1, thEnabled);
        
        boolean debugEnabled = jCheckBoxDebugChEnabled.isSelected();
        for (int channel = sensorDebugChannel; channel < (sensorDebugChannel+debugChanNumber); channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, debugEnabled);
        }
        
        if (shieldProtocolLayer != null) {
            shieldProtocolLayer.renderSavePresetWithName(boardId, sensorId, this.sensorName);
        }
    }    

    @Override
    public void readFromBoard() throws SensorBusException {

        iIRAndAvgPanel.readFromBoard();
        
        shieldProtocolLayer.renderReadSensorSerialNumber(boardId, sensorId);
        
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorId);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorBin0Channel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorTHChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorDebugChannel);
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
        Boolean thEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorTHChannel);
        if (thEnabled != null) {
            jCheckBoxTHEnabled.setSelected(thEnabled);
        }   
        Boolean debugEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorDebugChannel);
        if (debugEnabled != null) {
            jCheckBoxDebugChEnabled.setSelected(debugEnabled);
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
        Boolean thEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorTHChannel);
        if (thEnabled != null) {
            jCheckBoxTHEnabled.setSelected(thEnabled);
        }   
        Boolean debugEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorDebugChannel);
        if (debugEnabled != null) {
            jCheckBoxDebugChEnabled.setSelected(debugEnabled);
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
        jCheckBoxTHEnabled = new javax.swing.JCheckBox();
        jCheckBoxDebugChEnabled = new javax.swing.JCheckBox();

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

        jCheckBoxTHEnabled.setText("Temperature/Humidity");
        jCheckBoxTHEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxTHEnabledActionPerformed(evt);
            }
        });

        jCheckBoxDebugChEnabled.setText("Debug Channels");
        jCheckBoxDebugChEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxDebugChEnabledActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxTHEnabled)
                            .addComponent(jCheckBoxPMEnabled))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxBinsEnabled)
                            .addComponent(jCheckBoxDebugChEnabled))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxPMEnabled)
                    .addComponent(jCheckBoxBinsEnabled))
                .addGap(3, 3, 3)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxTHEnabled)
                    .addComponent(jCheckBoxDebugChEnabled))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxPMEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPMEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxPMEnabledActionPerformed

    private void jCheckBoxBinsEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxBinsEnabledActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxBinsEnabledActionPerformed

    private void jCheckBoxTHEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxTHEnabledActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxTHEnabledActionPerformed

    private void jCheckBoxDebugChEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDebugChEnabledActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxDebugChEnabledActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel iIRAndAvgPanel;
    private javax.swing.JCheckBox jCheckBoxBinsEnabled;
    private javax.swing.JCheckBox jCheckBoxDebugChEnabled;
    private javax.swing.JCheckBox jCheckBoxPMEnabled;
    private javax.swing.JCheckBox jCheckBoxTHEnabled;
    // End of variables declaration//GEN-END:variables

}
