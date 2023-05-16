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
public class K96SensorSetupDIalog extends SensorSetupDialog {
    
    private final String sensorName = "K96";
    private final int sensorLPLChannel;
    private final int sensorSPLChannel;
    private final int sensorMPLChannel;
    private final int sensorPressureChannel;
    private final int sensorNTCChannel;
    private final int sensorRH0Channel;
    private final int sensorUFLPLChannel;
    private final int sensorUFMPLChannel;
    private final int sensorUFSPLChannel;
    private final int sensorErrorChannel;
    private final int sensorUFLPLErrorChannel;
    private final int sensorUFSPLErrorChanel;
    private final int sensorUFMPLErrorChannel;

    /**
     * Creates new form GenericSensorSetupDIalog
     * @param sensorId
     * @param sensorLPLChannel
     * @param sensorSPLChannel
     * @param sensorMPLChannel
     * @param sensorPressureChannel
     * @param sensorNTCChannel
     * @param sensorRH0Channel
     * @param sensorUFMPLChannel
     * @param sensorUFSPLChannel
     * @param sensorUFLPLChannel
     * @param sensorErrorChannel
     * @param sensorUFLPLErrorChannel
     * @param sensorUFSPLErrorChanel
     * @param sensorUFMPLErrorChannel
     * @param parent
     * @param modal
     */
    public K96SensorSetupDIalog(int sensorId,
                                int sensorLPLChannel, 
                                int sensorSPLChannel, int sensorMPLChannel, 
                                int sensorPressureChannel, int sensorNTCChannel, 
                                int sensorRH0Channel, int sensorUFLPLChannel,
                                int sensorUFSPLChannel, int sensorUFMPLChannel,
                                int sensorErrorChannel, int sensorUFLPLErrorChannel,
                                int sensorUFSPLErrorChanel, int sensorUFMPLErrorChannel,
                                MainApplicationFrame parent, boolean modal) {
        super(parent, modal, sensorId);
        
        this.sensorLPLChannel = sensorLPLChannel;
        this.sensorSPLChannel = sensorSPLChannel;
        this.sensorMPLChannel = sensorMPLChannel;
        this.sensorPressureChannel = sensorPressureChannel;
        this.sensorNTCChannel = sensorNTCChannel;
        this.sensorRH0Channel = sensorRH0Channel;
        this.sensorUFLPLChannel = sensorUFLPLChannel;
        this.sensorUFMPLChannel = sensorUFMPLChannel;
        this.sensorUFSPLChannel = sensorUFSPLChannel;
        this.sensorErrorChannel = sensorErrorChannel;
        this.sensorUFLPLErrorChannel = sensorUFLPLErrorChannel;
        this.sensorUFSPLErrorChanel = sensorUFSPLErrorChanel;
        this.sensorUFMPLErrorChannel = sensorUFMPLErrorChannel;
        
        initComponents();
        
        iIRAndAvgPanel.setBoardId(boardId);
        iIRAndAvgPanel.setChannelId(sensorId);
        iIRAndAvgPanel.disableIIRSection();
        
        k96RegisterRW.setBoardId(boardId);
        k96RegisterRW.setChannelId(sensorId);
        
        // Channel is enabled by default
        jCheckBoxLPLEnabled.setSelected(true);
        jCheckBoxMPLEnabled.setSelected(true);
        jCheckBoxSPLEnabled.setSelected(true);
        jCheckBoxNTCEnabled.setSelected(true);
        jCheckBoxPressureEnabled.setSelected(true);
        jCheckBoxTHEnabled.setSelected(true);
        jCheckBoxUFLPLEnabled.setSelected(true);
        jCheckBoxUFMPLEnabled.setSelected(true);
        jCheckBoxUFSPLEnabled.setSelected(true);
        jCheckBoxDebugChEnabled.setSelected(true);
    }

    @Override
    public void setBoardId(int boardId) {
        super.setBoardId(boardId); 
        
        iIRAndAvgPanel.setBoardId(boardId);
        k96RegisterRW.setBoardId(boardId);
    }

    @Override
    public void setShieldProtocolLayer(ShieldProtocolLayer shieldProtocolLayer) {
        super.setShieldProtocolLayer(shieldProtocolLayer);
        
        iIRAndAvgPanel.setShieldProtocolLayer(shieldProtocolLayer);
        k96RegisterRW.setShieldProtocolLayer(shieldProtocolLayer);
    }
    
    /**
     * Update the board with the information set on this setup panel
     * @throws airsenseur.dev.exceptions.SensorBusException
     */
    @Override
    public void storeToBoard() throws SensorBusException {
        iIRAndAvgPanel.storeToBoard();
        k96RegisterRW.storeToBoard();
        
        boolean lplEnabled = jCheckBoxLPLEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorLPLChannel, lplEnabled);
        
        boolean mplEnabled = jCheckBoxMPLEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorMPLChannel, mplEnabled);

        boolean splEnabled = jCheckBoxSPLEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorSPLChannel, splEnabled);
        
        boolean uflplEnabled = jCheckBoxUFLPLEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorUFLPLChannel, uflplEnabled);
        
        boolean ufmplEnabled = jCheckBoxUFMPLEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorUFMPLChannel, ufmplEnabled);
        
        boolean ufsplEnabled = jCheckBoxUFSPLEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorUFSPLChannel, ufsplEnabled);
        
        boolean pressureEnabled = jCheckBoxPressureEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorPressureChannel, pressureEnabled);
        
        boolean thEnabled = jCheckBoxTHEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorRH0Channel, thEnabled);
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorRH0Channel+1, thEnabled);
        
        boolean ntcEnabled = jCheckBoxNTCEnabled.isSelected();
        for (int channel = sensorNTCChannel; channel < (sensorNTCChannel+3); channel++) {
            shieldProtocolLayer.renderWriteChannelEnable(boardId, channel, ntcEnabled);
        }
        
        boolean debugEnabled = jCheckBoxDebugChEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorErrorChannel, debugEnabled);
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorUFLPLErrorChannel, debugEnabled);
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorUFMPLErrorChannel, debugEnabled);
        shieldProtocolLayer.renderWriteChannelEnable(boardId, sensorUFSPLErrorChanel, debugEnabled);
        
        if (shieldProtocolLayer != null) {
            shieldProtocolLayer.renderSavePresetWithName(boardId, sensorId, this.sensorName);
        }
    }    

    @Override
    public void readFromBoard() throws SensorBusException {

        iIRAndAvgPanel.readFromBoard();
        k96RegisterRW.readFromBoard();
        
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorId);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorLPLChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorMPLChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorSPLChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorPressureChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorRH0Channel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorNTCChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorUFLPLChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorUFMPLChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorUFSPLChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorErrorChannel);   
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorUFLPLErrorChannel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorUFSPLErrorChanel);
        shieldProtocolLayer.renderReadChannelEnable(boardId, sensorUFMPLErrorChannel);
    }

    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        iIRAndAvgPanel.evaluateRxMessage(rxMessage);
        k96RegisterRW.evaluateRxMessage(rxMessage);
                
        // Channel enabled
        Boolean lplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorLPLChannel);
        if (lplEnabled != null) {
            jCheckBoxLPLEnabled.setSelected(lplEnabled);
            return;
        }
        Boolean splEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorSPLChannel);
        if (splEnabled != null) {
            jCheckBoxSPLEnabled.setSelected(splEnabled);
            return;
        }   
        Boolean mplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorMPLChannel);
        if (mplEnabled != null) {
            jCheckBoxMPLEnabled.setSelected(mplEnabled);
            return;
        }   
        Boolean pressureEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorPressureChannel);
        if (pressureEnabled != null) {
            jCheckBoxPressureEnabled.setSelected(pressureEnabled);
            return;
        }   
        Boolean rh0Enabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorRH0Channel);
        if (rh0Enabled != null) {
            jCheckBoxTHEnabled.setSelected(rh0Enabled);
            return;
        }   
        Boolean ntcEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorNTCChannel);
        if (ntcEnabled != null) {
            jCheckBoxNTCEnabled.setSelected(ntcEnabled);
            return;
        }   
        Boolean uflplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorUFLPLChannel);
        if (uflplEnabled != null) {
            jCheckBoxUFLPLEnabled.setSelected(uflplEnabled);
            return;
        }
        Boolean ufmplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorUFMPLChannel);
        if (ufmplEnabled != null) {
            jCheckBoxUFMPLEnabled.setSelected(ufmplEnabled);
            return;
        }
        Boolean uflslEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorUFSPLChannel);
        if (uflslEnabled != null) {
            jCheckBoxUFSPLEnabled.setSelected(uflslEnabled);
            return;
        }
        Boolean debugEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorErrorChannel);
        if (debugEnabled != null) {
            jCheckBoxDebugChEnabled.setSelected(debugEnabled);
        }   
    }
    
    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        iIRAndAvgPanel.onDataMessageFromConfiguration(rxMessage);
        k96RegisterRW.onDataMessageFromConfiguration(rxMessage);
               
        // Channel enabled
        Boolean lplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorLPLChannel);
        if (lplEnabled != null) {
            jCheckBoxLPLEnabled.setSelected(lplEnabled);
            return;
        }
        Boolean splEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorSPLChannel);
        if (splEnabled != null) {
            jCheckBoxSPLEnabled.setSelected(splEnabled);
            return;            
        }   
        Boolean mplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorMPLChannel);
        if (mplEnabled != null) {
            jCheckBoxMPLEnabled.setSelected(mplEnabled);
            return;            
        }   
        Boolean pressureEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorPressureChannel);
        if (pressureEnabled != null) {
            jCheckBoxPressureEnabled.setSelected(pressureEnabled);
            return;            
        }   
        Boolean rh0Enabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorRH0Channel);
        if (rh0Enabled != null) {
            jCheckBoxTHEnabled.setSelected(rh0Enabled);
            return;            
        }   
        Boolean ntcEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorNTCChannel);
        if (ntcEnabled != null) {
            jCheckBoxNTCEnabled.setSelected(ntcEnabled);
            return;            
        }   
        Boolean uflplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorUFLPLChannel);
        if (uflplEnabled != null) {
            jCheckBoxUFLPLEnabled.setSelected(uflplEnabled);
            return;
        }
        Boolean ufmplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorUFMPLChannel);
        if (ufmplEnabled != null) {
            jCheckBoxUFMPLEnabled.setSelected(ufmplEnabled);
            return;
        }
        Boolean uflslEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorUFSPLChannel);
        if (uflslEnabled != null) {
            jCheckBoxUFSPLEnabled.setSelected(uflslEnabled);
            return;
        }        
        Boolean debugEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, sensorErrorChannel);
        if (debugEnabled != null) {
            jCheckBoxDebugChEnabled.setSelected(debugEnabled);
        }           
    }
    
    @Override
    public void onSensorPresetDatabaseChanged() {
    }

    @Override
    public void setConnected(boolean connected) {
        super.setConnected(connected); 
        
        k96RegisterRW.setConnected(connected);
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
        jCheckBoxLPLEnabled = new javax.swing.JCheckBox();
        jCheckBoxSPLEnabled = new javax.swing.JCheckBox();
        jCheckBoxMPLEnabled = new javax.swing.JCheckBox();
        jCheckBoxPressureEnabled = new javax.swing.JCheckBox();
        jCheckBoxTHEnabled = new javax.swing.JCheckBox();
        jCheckBoxDebugChEnabled = new javax.swing.JCheckBox();
        jCheckBoxNTCEnabled = new javax.swing.JCheckBox();
        jCheckBoxUFLPLEnabled = new javax.swing.JCheckBox();
        jCheckBoxUFSPLEnabled = new javax.swing.JCheckBox();
        jCheckBoxUFMPLEnabled = new javax.swing.JCheckBox();
        k96RegisterRW = new airsenseur.dev.chemsensorpanel.widgets.FreeRegisterRWPanel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(sensorName);
        setResizable(false);

        jCheckBoxLPLEnabled.setText("LPL Channel Enabled");
        jCheckBoxLPLEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxLPLEnabledActionPerformed(evt);
            }
        });

        jCheckBoxSPLEnabled.setText("SPL Channel Enabled");

        jCheckBoxMPLEnabled.setText("MPL Channel Enabled");

        jCheckBoxPressureEnabled.setText("Pressure Ch. Enabled");
        jCheckBoxPressureEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPressureEnabledActionPerformed(evt);
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

        jCheckBoxNTCEnabled.setText("NTC Chs. Enabled");

        jCheckBoxUFLPLEnabled.setText("UFLPL IR Channel Enabled");

        jCheckBoxUFSPLEnabled.setText("UFSPL IR Channel Enabled");

        jCheckBoxUFMPLEnabled.setText("UFMPL IR Channel Enabled");

        jLabel1.setText("Read/Write to a specific register into the K96 sensor.");

        jLabel2.setText("NOTE: The shield needs to be connected and sampling with");

        jLabel3.setText("Don't do it unless you know what you're doing.");

        jLabel4.setText("sample rate prescaler greater than 10 ticks for better results");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator1))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jCheckBoxUFMPLEnabled)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jCheckBoxSPLEnabled)
                                            .addComponent(jCheckBoxLPLEnabled)
                                            .addComponent(jCheckBoxMPLEnabled)
                                            .addComponent(jCheckBoxUFLPLEnabled))
                                        .addGap(29, 29, 29)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jCheckBoxPressureEnabled)
                                            .addComponent(jCheckBoxDebugChEnabled)
                                            .addComponent(jCheckBoxTHEnabled)
                                            .addComponent(jCheckBoxNTCEnabled)))))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel2))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel3))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel4))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(31, 31, 31)
                                .addComponent(k96RegisterRW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBoxUFSPLEnabled)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBoxLPLEnabled, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jCheckBoxTHEnabled))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBoxNTCEnabled)
                    .addComponent(jCheckBoxSPLEnabled, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBoxPressureEnabled)
                    .addComponent(jCheckBoxMPLEnabled))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxDebugChEnabled)
                    .addComponent(jCheckBoxUFLPLEnabled))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxUFSPLEnabled)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxUFMPLEnabled)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(177, 177, 177)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(k96RegisterRW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxLPLEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxLPLEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxLPLEnabledActionPerformed

    private void jCheckBoxPressureEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPressureEnabledActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxPressureEnabledActionPerformed

    private void jCheckBoxTHEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxTHEnabledActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxTHEnabledActionPerformed

    private void jCheckBoxDebugChEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDebugChEnabledActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxDebugChEnabledActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel iIRAndAvgPanel;
    private javax.swing.JCheckBox jCheckBoxDebugChEnabled;
    private javax.swing.JCheckBox jCheckBoxLPLEnabled;
    private javax.swing.JCheckBox jCheckBoxMPLEnabled;
    private javax.swing.JCheckBox jCheckBoxNTCEnabled;
    private javax.swing.JCheckBox jCheckBoxPressureEnabled;
    private javax.swing.JCheckBox jCheckBoxSPLEnabled;
    private javax.swing.JCheckBox jCheckBoxTHEnabled;
    private javax.swing.JCheckBox jCheckBoxUFLPLEnabled;
    private javax.swing.JCheckBox jCheckBoxUFMPLEnabled;
    private javax.swing.JCheckBox jCheckBoxUFSPLEnabled;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JSeparator jSeparator1;
    private airsenseur.dev.chemsensorpanel.widgets.FreeRegisterRWPanel k96RegisterRW;
    // End of variables declaration//GEN-END:variables

}
