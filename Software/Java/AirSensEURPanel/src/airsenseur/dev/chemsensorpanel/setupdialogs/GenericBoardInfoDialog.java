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
import airsenseur.dev.comm.AppDataMessage;

/**
 *
 * @author marco
 */
public class GenericBoardInfoDialog extends SensorSetupDialog {

    /**
     * Creates new form GenericBoardInfoDialog
     * @param parent
     * @param modal
     * @param title
     */
    public GenericBoardInfoDialog(MainApplicationFrame parent, boolean modal, String title) {
        super(parent, modal, 0);
        initComponents();
        
        jLabelDialogTitle.setText(title);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabelDialogTitle = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextBoardSN = new JOverridableTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabelFWRevision = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Firmware revision:");

        jLabelDialogTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelDialogTitle.setText("--");

        jLabel2.setText("Board serial number:");

        jTextBoardSN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextBoardSNActionPerformed(evt);
            }
        });

        jLabel3.setText("NOTE: do not change if you don't know what you're doing");

        jLabelFWRevision.setText("--");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelFWRevision, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextBoardSN, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabelDialogTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(jLabelDialogTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabelFWRevision))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextBoardSN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addGap(18, 18, 18))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextBoardSNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextBoardSNActionPerformed
    }//GEN-LAST:event_jTextBoardSNActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabelDialogTitle;
    private javax.swing.JLabel jLabelFWRevision;
    private javax.swing.JTextField jTextBoardSN;
    // End of variables declaration//GEN-END:variables

    
    @Override
    public void storeToBoard() {
        
        if (!jTextBoardSN.getText().isEmpty()) {
            shieldProtocolLayer.renderSaveBoardSerialNumber(boardId, jTextBoardSN.getText());
        }
    }

    @Override
    public void readFromBoard() {
        shieldProtocolLayer.renderReadBoardSerialNumber(boardId);
        shieldProtocolLayer.renderReadFirmwareVersion(boardId);
    }

    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        String serialBoard = shieldProtocolLayer.evalReadBoardSerialNumber(rxMessage, boardId);
        if (serialBoard != null) {
            jTextBoardSN.setText(serialBoard);
        }
        
        String firmwareVersion = shieldProtocolLayer.evalReadFirmwareVersion(rxMessage, boardId);
        if (firmwareVersion != null) {
            jLabelFWRevision.setText(firmwareVersion);
        }
    }
    
    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        String serialBoard = shieldProtocolLayer.evalReadBoardSerialNumber(rxMessage, boardId);
        if (serialBoard != null) {
            ((JOverridableTextField)jTextBoardSN).setTextOverride(serialBoard);
        }
    }

    @Override
    public void onSensorPresetDatabaseChanged() {
    }
}