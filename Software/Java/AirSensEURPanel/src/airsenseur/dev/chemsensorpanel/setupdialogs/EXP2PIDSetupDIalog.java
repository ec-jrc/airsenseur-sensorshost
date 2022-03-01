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
public class EXP2PIDSetupDIalog extends SensorSetupDialog {
    
    private final String sensorName = "PID Control";
    
    private final int coolerChannel;
    private final int heaterChannel;

    /**
     * Creates new form GenericSensorSetupDIalog
     * @param sensorId
     * @param coolerChannel
     * @param heaterChannel
     * @param parent
     * @param modal
     */
    public EXP2PIDSetupDIalog(int sensorId,
                                int coolerChannel, int heaterChannel,
                                MainApplicationFrame parent, boolean modal) {
        super(parent, modal, sensorId);
                
        initComponents();
        
        this.coolerChannel = coolerChannel;
        this.heaterChannel = heaterChannel;
        
        iIRAndAvgPanel.setBoardId(boardId);
        iIRAndAvgPanel.setChannelId(sensorId);
        iIRAndAvgPanel.disableIIRSection();
        
        PIDRegisterRW.setBoardId(boardId);
        PIDRegisterRW.setChannelId(sensorId);
        PIDRegisterRW.setMaxValue(65535);
        
        // Channel is enabled by default
        jCheckBoxChannelsEnabled.setSelected(true);
    }

    @Override
    public void setBoardId(int boardId) {
        super.setBoardId(boardId); 
        
        iIRAndAvgPanel.setBoardId(boardId);
        PIDRegisterRW.setBoardId(boardId);
    }

    @Override
    public void setShieldProtocolLayer(ShieldProtocolLayer shieldProtocolLayer) {
        super.setShieldProtocolLayer(shieldProtocolLayer);
        
        iIRAndAvgPanel.setShieldProtocolLayer(shieldProtocolLayer);
        PIDRegisterRW.setShieldProtocolLayer(shieldProtocolLayer);
    }
    
    /**
     * Update the board with the information set on this setup panel
     * @throws airsenseur.dev.exceptions.SensorBusException
     */
    @Override
    public void storeToBoard() throws SensorBusException {
        iIRAndAvgPanel.storeToBoard();
        PIDRegisterRW.storeToBoard();
        
        boolean lplEnabled = jCheckBoxChannelsEnabled.isSelected();
        shieldProtocolLayer.renderWriteChannelEnable(boardId, coolerChannel, lplEnabled);
        shieldProtocolLayer.renderWriteChannelEnable(boardId, heaterChannel, lplEnabled);
        
        if (shieldProtocolLayer != null) {
            shieldProtocolLayer.renderSavePresetWithName(boardId, sensorId, this.sensorName);
        }
    }    

    @Override
    public void readFromBoard() throws SensorBusException {

        iIRAndAvgPanel.readFromBoard();
        PIDRegisterRW.readFromBoard();
        
        shieldProtocolLayer.renderReadChannelEnable(boardId, coolerChannel);
    }

    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        iIRAndAvgPanel.evaluateRxMessage(rxMessage);
        PIDRegisterRW.evaluateRxMessage(rxMessage);
                
        // Channel enabled
        Boolean lplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, coolerChannel);
        if (lplEnabled != null) {
            jCheckBoxChannelsEnabled.setSelected(lplEnabled);
        }
    }
    
    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        iIRAndAvgPanel.onDataMessageFromConfiguration(rxMessage);
        PIDRegisterRW.onDataMessageFromConfiguration(rxMessage);
               
        // Channel enabled
        Boolean lplEnabled = shieldProtocolLayer.evalReadChannelEnable(rxMessage, boardId, coolerChannel);
        if (lplEnabled != null) {
            jCheckBoxChannelsEnabled.setSelected(lplEnabled);
        }           
    }
    
    @Override
    public void onSensorPresetDatabaseChanged() {
    }

    @Override
    public void setConnected(boolean connected) {
        super.setConnected(connected); 
        
        PIDRegisterRW.setConnected(connected);
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
        jCheckBoxChannelsEnabled = new javax.swing.JCheckBox();
        PIDRegisterRW = new airsenseur.dev.chemsensorpanel.widgets.FreeRegisterRWPanel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(sensorName);
        setResizable(false);

        jCheckBoxChannelsEnabled.setText("Channels Enabled");
        jCheckBoxChannelsEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxChannelsEnabledActionPerformed(evt);
            }
        });

        jLabel1.setText("Read/Write to a specific register into PID sensor.");

        jLabel3.setText("Don't do it unless you know what you're doing.");

        jLabel5.setText("Address: 0x0000 to 0x0002 -> P,I,D coefficients");

        jLabel7.setText("Address: 0x0006 -> Write 0xAA to confirm and apply");

        jLabel6.setText("Address: 0x0003 -> Multiplier, common to the PID coeffs.");

        jLabel8.setText("Address: 0x0004 -> Min heating % threshold (in 1/100)");

        jLabel9.setText("Address: 0x0005 -> Min cooling % threshold (in 1/100)");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxChannelsEnabled)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3)
                            .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)
                            .addComponent(jLabel7)
                            .addComponent(jLabel6)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(PIDRegisterRW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel9)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBoxChannelsEnabled)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(PIDRegisterRW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxChannelsEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxChannelsEnabledActionPerformed

    }//GEN-LAST:event_jCheckBoxChannelsEnabledActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.widgets.FreeRegisterRWPanel PIDRegisterRW;
    private airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel iIRAndAvgPanel;
    private javax.swing.JCheckBox jCheckBoxChannelsEnabled;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator1;
    // End of variables declaration//GEN-END:variables

}
