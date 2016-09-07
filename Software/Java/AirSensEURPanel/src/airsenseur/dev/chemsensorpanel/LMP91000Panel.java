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

package airsenseur.dev.chemsensorpanel;

import airsenseur.dev.chemsensorpanel.exceptions.PresetException;
import airsenseur.dev.chemsensorpanel.sensorsdb.PresetValue;
import airsenseur.dev.chemsensorpanel.sensorsdb.PresetValues;
import airsenseur.dev.comm.CommProtocolHelper;
import java.util.List;

/**
 *
 * @author marco
 */
public class LMP91000Panel extends PresetDrivenPanel {
    
    private double extVoltage = 2.5f;
    
    /**
     * Creates new form LMP91000Panel
     */
    public LMP91000Panel() {
        initComponents();
        updateInternalZero();
        updateBiasVoltage();
    }
    
    public void externalVoltageUpdated(double extVoltage) {
        
        this.extVoltage = extVoltage;
        
        updateBiasVoltage();
        updateInternalZero();
    }
        
    private void updateInternalZero() {
        
        boolean internal = jCBVSrc.getSelectedItem().toString().compareToIgnoreCase("Internal") == 0;
        String zeroPerc = jCBIntZ.getSelectedItem().toString().replaceAll("%", "");
        int zeroVal;
        if (zeroPerc.compareToIgnoreCase("bypass") == 0) {
            zeroVal = 0;
        } else {
            zeroVal = Integer.valueOf(zeroPerc).intValue();
        }
        
        double dbZeroVal = (internal)? 5.0:extVoltage;
        dbZeroVal = (dbZeroVal * zeroVal) / 100;
        
        String sZeroVal = String.format("%.3f", dbZeroVal);
        jTxtZeroVoltage.setText(sZeroVal);
    }
    
    private void updateBiasVoltage() {
        
        boolean internal = jCBVSrc.getSelectedItem().toString().compareToIgnoreCase("Internal") == 0;
        boolean negative = jCBBiasPol.getSelectedItem().toString().compareToIgnoreCase("Negative") == 0;
        double dbRefVal = ((internal)? 5.0:extVoltage) * ((negative)? -1.0:1.0);
        
        int biasPerc = Integer.valueOf(jCBBiasPerc.getSelectedItem().toString().replaceAll("%", "")).intValue();
        
        dbRefVal = (dbRefVal * biasPerc) / 100;
        
        String sBiasVal = String.format("%.3f", dbRefVal);
        jTxtBiasVoltage.setText(sBiasVal);
    }
    
    public String getSensorName() {
        return jTxtName.getText();
    }
    
    public void setSensorName(String sensorName) {
        
        if (sensorName == null) {
            sensorName = "";
        }
        
        jTxtName.setText(sensorName);
    }
    
    public void storeToBoard(int channelId) {
        
        int tiaRegVal = getTiaRegVal();
        int refRegVal  = getRefRegVal();
        int modeRegVal = getModeRegVal();
        
        CommProtocolHelper.instance().renderLMP9100RegSetup(channelId, tiaRegVal, refRegVal, modeRegVal);
    }

    
    public void readFromBoard(int channelId) {
        
        CommProtocolHelper.instance().renderLMP9100ReadSetup(channelId);
    }
    
    public void evaluateRxMessage(CommProtocolHelper.DataMessage rxMessage, int channelId) {
        
        CommProtocolHelper helper = CommProtocolHelper.instance();
        
        // Tia, Ref, mode registers
        List<Integer> registers;
        registers = helper.evalAFERegistersInquiry(rxMessage, channelId);
        if ((registers != null) && !registers.isEmpty()) {
            
            int tia = registers.get(0);
            int ref = registers.get(1);
            int mode = registers.get(2);
            
            setFromTiaRegVal(tia);
            setFromRefRegVal(ref);
            setFromModeRegVal(mode);
            
            refreshGuiItems();
        
            updateInternalZero();
            updateBiasVoltage();
        }
    }
    
    @Override
    public void loadPresetValues(PresetValues presetValues) throws PresetException {
        
        int tia = presetValues.get("91000_R").getValueAsInteger();
        int ref = presetValues.get("91000_T").getValueAsInteger();
        int mode = presetValues.get("91000_M").getValueAsInteger();
        
        setFromTiaRegVal(tia);
        setFromRefRegVal(ref);
        setFromModeRegVal(mode);
        
        jTxtName.setText(presetValues.get("91000_N").getValue());

        refreshGuiItems();

        updateInternalZero();
        updateBiasVoltage();
    }

    @Override
    public void savePresetValues(PresetValues presetValues) throws PresetException {
        
        String name = jTxtName.getText();
        if (name.trim().isEmpty()) {
            name = "no name";
        }
        presetValues.add(new PresetValue("91000_N", name));
        presetValues.add(new PresetValue("91000_R", getTiaRegVal()));
        presetValues.add(new PresetValue("91000_T", getRefRegVal()));
        presetValues.add(new PresetValue("91000_M", getModeRegVal()));
    }
    
    private void refreshGuiItems() {
        
        jCBBiasPerc.repaint();
        jCBBiasPol.repaint();
        jCBFetShort.repaint();
        jCBGain.repaint();
        jCBIntZ.repaint();
        jCBLoad.repaint();
        jCBVSrc.repaint();
        jCBMode.repaint();
    }

    private void setFromModeRegVal(int mode) {
        
        lMP9100_MODE_FetShort.setSelectedItem(mode);
        lMP9100_MODE.setSelectedItem(mode);
    }

    private void setFromRefRegVal(int ref) {
        
        lMP9100_REF_Bias.setSelectedItem(ref);
        lMP9100_REF_BiasSign.setSelectedItem(ref);
        lMP9100_REF_IntZ.setSelectedItem(ref);
        lMP9100_REF_Source.setSelectedItem(ref);
    }

    private void setFromTiaRegVal(int tia) {
        
        lMP9100TIAGain.setSelectedItem(tia);
        lMP9100_TIA_RLoad.setSelectedItem(tia);
    }
    
    private int getModeRegVal() {
        
        // Force the 3LEAD AMP CELL mode in this AFE
        int modeRegVal = lMP9100_MODE_FetShort.getRegisterSelectedValue() +
                        lMP9100_MODE.getRegisterSelectedValue();
        
        return modeRegVal;
    }

    private int getRefRegVal() {
        
        int refRegVal = lMP9100_REF_Bias.getRegisterSelectedValue() +
                        lMP9100_REF_BiasSign.getRegisterSelectedValue() +
                        lMP9100_REF_IntZ.getRegisterSelectedValue() +
                        lMP9100_REF_Source.getRegisterSelectedValue();
        
        return refRegVal;
    }
    
    private int getTiaRegVal() {
        
        int tiaRegVal = lMP9100TIAGain.getRegisterSelectedValue() + 
                        lMP9100_TIA_RLoad.getRegisterSelectedValue();
        
        return tiaRegVal;
    }    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lMP9100TIAGain = new airsenseur.dev.chemsensorpanel.dataModel.LMP9100_TIA_Gain();
        lMP9100_TIA_RLoad = new airsenseur.dev.chemsensorpanel.dataModel.LMP9100_TIA_RLoad();
        lMP9100_REF_Source = new airsenseur.dev.chemsensorpanel.dataModel.LMP9100_REF_Source();
        lMP9100_REF_IntZ = new airsenseur.dev.chemsensorpanel.dataModel.LMP9100_REF_IntZ();
        lMP9100_REF_BiasSign = new airsenseur.dev.chemsensorpanel.dataModel.LMP9100_REF_BiasSign();
        lMP9100_REF_Bias = new airsenseur.dev.chemsensorpanel.dataModel.LMP9100_REF_Bias();
        lMP9100_MODE_FetShort = new airsenseur.dev.chemsensorpanel.dataModel.LMP9100_MODE_FetShort();
        lMP9100_MODE = new airsenseur.dev.chemsensorpanel.dataModel.LMP9100_MODE();
        jCBGain = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jCBLoad = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jCBVSrc = new javax.swing.JComboBox();
        jCBIntZ = new javax.swing.JComboBox();
        jCBBiasPol = new javax.swing.JComboBox();
        jCBBiasPerc = new javax.swing.JComboBox();
        jCBFetShort = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jTxtZeroVoltage = new javax.swing.JTextField();
        jTxtBiasVoltage = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jTxtName = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jCBMode = new javax.swing.JComboBox();

        jCBGain.setModel(lMP9100TIAGain);

        jLabel1.setText("Gain:");

        jLabel2.setText("Load:");

        jCBLoad.setModel(lMP9100_TIA_RLoad);
        jCBLoad.setMinimumSize(new java.awt.Dimension(117, 27));

        jLabel3.setText("Reference Source:");

        jLabel4.setText("Internal Zero:");

        jLabel5.setText("Bias Polarity:");

        jLabel6.setText("Bias Percentage:");

        jLabel7.setText("Shorting FET:");

        jCBVSrc.setModel(lMP9100_REF_Source);
        jCBVSrc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBVSrcActionPerformed(evt);
            }
        });

        jCBIntZ.setModel(lMP9100_REF_IntZ);
        jCBIntZ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBIntZActionPerformed(evt);
            }
        });

        jCBBiasPol.setModel(lMP9100_REF_BiasSign);
        jCBBiasPol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBBiasPolActionPerformed(evt);
            }
        });

        jCBBiasPerc.setModel(lMP9100_REF_Bias);
        jCBBiasPerc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBBiasPercActionPerformed(evt);
            }
        });

        jCBFetShort.setModel(lMP9100_MODE_FetShort);

        jLabel8.setText("Selected Internal Zero Voltage:");

        jLabel9.setText("Selected Bias Voltage:");

        jTxtZeroVoltage.setEditable(false);
        jTxtZeroVoltage.setToolTipText("Shows the AFE Internal Zero voltage based on the parameters selected");

        jTxtBiasVoltage.setEditable(false);
        jTxtBiasVoltage.setToolTipText("Shows the AFE Bias Voltage applied to the sensor based on the selected parameters");

        jLabel10.setText("Sensor Name:");

        jTxtName.setText("no name");

        jLabel11.setText("Working Mode:");

        jCBMode.setModel(lMP9100_MODE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTxtBiasVoltage)
                            .addComponent(jTxtZeroVoltage, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jCBGain, javax.swing.GroupLayout.Alignment.LEADING, 0, 1, Short.MAX_VALUE)
                            .addComponent(jCBLoad, javax.swing.GroupLayout.Alignment.LEADING, 0, 117, Short.MAX_VALUE)
                            .addComponent(jTxtName, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCBVSrc, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jCBIntZ, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel11)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel5)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCBBiasPerc, 0, 110, Short.MAX_VALUE)
                            .addComponent(jCBBiasPol, 0, 1, Short.MAX_VALUE)
                            .addComponent(jCBMode, 0, 1, Short.MAX_VALUE)
                            .addComponent(jCBFetShort, 0, 1, Short.MAX_VALUE))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jTxtBiasVoltage, jTxtZeroVoltage});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jTxtName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jCBBiasPol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCBGain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel6)
                    .addComponent(jCBBiasPerc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jCBLoad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCBVSrc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel7)
                    .addComponent(jCBFetShort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCBIntZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel11)
                    .addComponent(jCBMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jTxtZeroVoltage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jTxtBiasVoltage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jCBVSrcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBVSrcActionPerformed
        updateInternalZero();
        updateBiasVoltage();
    }//GEN-LAST:event_jCBVSrcActionPerformed

    private void jCBIntZActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBIntZActionPerformed
        updateInternalZero();
    }//GEN-LAST:event_jCBIntZActionPerformed

    private void jCBBiasPolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBBiasPolActionPerformed
        updateBiasVoltage();
    }//GEN-LAST:event_jCBBiasPolActionPerformed

    private void jCBBiasPercActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBBiasPercActionPerformed
        updateBiasVoltage();
    }//GEN-LAST:event_jCBBiasPercActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jCBBiasPerc;
    private javax.swing.JComboBox jCBBiasPol;
    private javax.swing.JComboBox jCBFetShort;
    private javax.swing.JComboBox jCBGain;
    private javax.swing.JComboBox jCBIntZ;
    private javax.swing.JComboBox jCBLoad;
    private javax.swing.JComboBox jCBMode;
    private javax.swing.JComboBox jCBVSrc;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField jTxtBiasVoltage;
    private javax.swing.JTextField jTxtName;
    private javax.swing.JTextField jTxtZeroVoltage;
    private airsenseur.dev.chemsensorpanel.dataModel.LMP9100_TIA_Gain lMP9100TIAGain;
    private airsenseur.dev.chemsensorpanel.dataModel.LMP9100_MODE lMP9100_MODE;
    private airsenseur.dev.chemsensorpanel.dataModel.LMP9100_MODE_FetShort lMP9100_MODE_FetShort;
    private airsenseur.dev.chemsensorpanel.dataModel.LMP9100_REF_Bias lMP9100_REF_Bias;
    private airsenseur.dev.chemsensorpanel.dataModel.LMP9100_REF_BiasSign lMP9100_REF_BiasSign;
    private airsenseur.dev.chemsensorpanel.dataModel.LMP9100_REF_IntZ lMP9100_REF_IntZ;
    private airsenseur.dev.chemsensorpanel.dataModel.LMP9100_REF_Source lMP9100_REF_Source;
    private airsenseur.dev.chemsensorpanel.dataModel.LMP9100_TIA_RLoad lMP9100_TIA_RLoad;
    // End of variables declaration//GEN-END:variables

}
