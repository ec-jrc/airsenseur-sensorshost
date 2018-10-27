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

package airsenseur.dev.chemsensorpanel.comm;

import airsenseur.dev.chemsensorpanel.*;
import airsenseur.dev.comm.SensorBus;
import airsenseur.dev.exceptions.SensorBusException;

/**
 *
 * @author marco
 */
public class RemoteConnectionDialog extends javax.swing.JDialog {
    
    private SensorBusCommunicationHandler sensorBoard = null;
    private boolean connected = false;
    
    /**
     * Creates new form RemoteBoardDialog
     * @param parent
     * @param modal
     */
    public RemoteConnectionDialog(AirSensEURPanel parent, boolean modal) {
        super(parent, modal);
        initComponents();
        
        jStatusLabel.setText("Select an address then press Connect");
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
        jTextBoardHost = new javax.swing.JTextField();
        jButtonConnect = new javax.swing.JButton();
        jStatusLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Network Connection");
        setResizable(false);

        jLabel1.setText("Remote board address:");

        jTextBoardHost.setText("192.168.100.1");

        jButtonConnect.setText("Connect");
        jButtonConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConnectActionPerformed(evt);
            }
        });

        jStatusLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jStatusLabel.setText("?");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jStatusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButtonConnect)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextBoardHost, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 28, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextBoardHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonConnect)
                    .addComponent(jStatusLabel))
                .addContainerGap(33, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonConnectActionPerformed
        
        if(sensorBoard != null) {
            
            try {
                sensorBoard.connectToNetworkedHost(jTextBoardHost.getText());
            } catch (SensorBusException e) {
                String message = e.getErrorMessage();
                if (message == null) {
                    message = "Unknonwn exception";
                }
                jStatusLabel.setText(message);
                sensorBoard.disConnectFromBus();
                return;
            }
            
            connected = true;
            jStatusLabel.setText("Success");
            jButtonConnect.setEnabled(false);            
        }
    }//GEN-LAST:event_jButtonConnectActionPerformed

    public void init(SensorBusCommunicationHandler sensorBoard) {
        
        this.sensorBoard = sensorBoard;
        this.connected = false;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        
        jButtonConnect.setEnabled(true);
        jStatusLabel.setText("");
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonConnect;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jStatusLabel;
    private javax.swing.JTextField jTextBoardHost;
    // End of variables declaration//GEN-END:variables
}