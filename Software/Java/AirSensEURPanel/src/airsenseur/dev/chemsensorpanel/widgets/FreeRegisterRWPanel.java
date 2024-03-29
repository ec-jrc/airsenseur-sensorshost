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

package airsenseur.dev.chemsensorpanel.widgets;

import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.exceptions.SensorBusException;
import airsenseur.dev.helpers.Pair;

/**
 *
 * @author marco
 */
public class FreeRegisterRWPanel extends SensorBusInteractingPanel {
    
    private int maxValue;
    
    /**
     * Creates new form GenericCursorPanel
     */
    public FreeRegisterRWPanel() {
        
        this.maxValue = 255;
        initComponents();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sliderDataModel = new airsenseur.dev.chemsensorpanel.dataModel.SliderDataModel();
        jLabel = new javax.swing.JLabel();
        jTextAddress = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jTextValue = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextResult = new javax.swing.JTextField();
        jButtonWrite = new javax.swing.JButton();
        jButtonRead = new javax.swing.JButton();

        jLabel.setText("Address (Hex):");

        jTextAddress.setText("0");
        jTextAddress.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextAddressKeyTyped(evt);
            }
        });

        jLabel1.setText("Value (Hex):");

        jTextValue.setText("0");
        jTextValue.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextValueKeyTyped(evt);
            }
        });

        jLabel2.setText("Response (Hex):");

        jTextResult.setEditable(false);

        jButtonWrite.setText("Write");
        jButtonWrite.setEnabled(false);
        jButtonWrite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteActionPerformed(evt);
            }
        });

        jButtonRead.setText("Read");
        jButtonRead.setEnabled(false);
        jButtonRead.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(33, 33, 33)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jTextAddress, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                            .addComponent(jTextValue, javax.swing.GroupLayout.Alignment.LEADING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonWrite)
                            .addComponent(jButtonRead)))
                    .addComponent(jTextResult))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel)
                    .addComponent(jTextAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonRead))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonWrite))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextResult, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonWriteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteActionPerformed

        // Clear the result text area
        jTextResult.setText("");

        Integer iVal, iAdd;
        try {
            
            iAdd = checkAddressValidity();
            iVal = checkValueValidity();
            
        } catch (NumberFormatException ex) {
            
            // Error reading the address or the value
            jTextResult.setText(ex.getMessage());
            
            return;
        }
        
        try {
            // Send the data to the remote shield
            shieldProtocolLayer.renderWriteGenericRegister(boardId, channelId, iAdd, iVal);
        } catch (SensorBusException ex) {
            
            jTextResult.setText("Error sending command. Is the board connected?");
        }
    }//GEN-LAST:event_jButtonWriteActionPerformed

    private Integer checkAddressValidity() throws NumberFormatException {
        
        // Retrieve the address
        String address = jTextAddress.getText();
        
        
        Integer iAdd = Integer.parseInt(address, 16);
        
        if ((iAdd > 65535) || (iAdd < 0)) {
            throw new NumberFormatException("Address out of range (0, 65535)");
        }
        
        return iAdd;
    }
    
    private Integer checkValueValidity() throws NumberFormatException {
        
        // Retrieve the value
        String value = jTextValue.getText();
        
        Integer iVal = Integer.parseInt(value, 16);
        
        if ((iVal < 0) || (iVal > maxValue)) {
            throw new NumberFormatException("Value out of range (0, " + maxValue + ")");
        }
        
        return iVal;
    }
    
    private void jTextAddressKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextAddressKeyTyped

        // Clear the result text area
        jTextResult.setText("");

        evaluateInputChar(evt);
    }//GEN-LAST:event_jTextAddressKeyTyped

    private void jTextValueKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextValueKeyTyped

        // Clear the result text area
        jTextResult.setText("");
        
        evaluateInputChar(evt);
    }//GEN-LAST:event_jTextValueKeyTyped

    private void jButtonReadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadActionPerformed

        // Clear the result text area
        jTextResult.setText("");

        Integer iAdd;
        try {
            
            iAdd = checkAddressValidity();
            
        } catch (NumberFormatException ex) {
            
            // Error reading the address or the value
            jTextResult.setText(ex.getMessage());
            
            return;
        }
        
        try {
            // Send the data to the remote shield
            shieldProtocolLayer.renderReadGenericRegister(boardId, channelId, iAdd);
        } catch (SensorBusException ex) {
            
            jTextResult.setText("Error sending command. Is the board connected?");
        }
    }//GEN-LAST:event_jButtonReadActionPerformed

    private void evaluateInputChar(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
                
        if (!Character.isDigit(c)) {
            if ( !(((c >= 'a') && (c <= 'f')) || ((c >= 'A') && (c <='F'))) ) {
                evt.consume();
            }
        }        
    }
    
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }
    
    @Override
    public void storeToBoard() throws SensorBusException {
        
        // Nothing to di with this widget. Actions are performed by the internal button
    }

    @Override
    public void readFromBoard() throws SensorBusException {
        
        // Nothing to di with this widget. Actions are performed by the internal button
    }

    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        // Evaluate results
        String result = shieldProtocolLayer.evalWriteGenericRegister(rxMessage, boardId, channelId);
        
        if (result != null) {
            jTextResult.setText(result);
            return;
        }
        
        Pair<Integer, Integer> results = shieldProtocolLayer.evalReadGenericRegister(rxMessage, boardId, channelId);
        if (result != null) {
            jTextResult.setText(String.format("%2X",results.second));
        }
    }

    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        // Nothing to di with this widget. Actions are performed by the internal button
    }
    
    @Override
    public void setConnected(boolean connected) {
        jButtonRead.setEnabled(connected);
        jButtonWrite.setEnabled(connected);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonRead;
    private javax.swing.JButton jButtonWrite;
    private javax.swing.JLabel jLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField jTextAddress;
    private javax.swing.JTextField jTextResult;
    private javax.swing.JTextField jTextValue;
    private airsenseur.dev.chemsensorpanel.dataModel.SliderDataModel sliderDataModel;
    // End of variables declaration//GEN-END:variables
}
