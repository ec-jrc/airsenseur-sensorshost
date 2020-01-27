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

import airsenseur.dev.chemsensorpanel.dataModel.DACChannelDataModel;
import airsenseur.dev.chemsensorpanel.dataModel.DACGainModel;
import airsenseur.dev.chemsensorpanel.exceptions.PresetException;
import airsenseur.dev.chemsensorpanel.sensorsdb.PresetValue;
import airsenseur.dev.chemsensorpanel.sensorsdb.PresetValues;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.exceptions.SensorBusException;
import java.util.List;

/**
 *
 * @author marco
 */
public class AD5694RPanel extends PresetDrivenPanel {
    
    private final char IDDAC_A = 0x00;
    private final char IDDAC_B = 0x01;
    private final char IDDAC_C = 0x02;

    /**
     * Creates new form AD5694RPanel
     */
    public AD5694RPanel() {
        initComponents();
        
        dacModelA.setSubChannel(IDDAC_A);
        dacModelB.setSubChannel(IDDAC_B);
        dacModelC.setSubChannel(IDDAC_C);
    }
    
    private String getVoltageLabel(int value, boolean doubleGain) {
        
        double dbValue = ((doubleGain == true)? 5.0f : 2.5f) * value / 4095f;
        
        String voltageValue = String.format("%.3f V", dbValue);
        
        return voltageValue;
    }
    
    @Override
    public void storeToBoard() throws SensorBusException {
        storeToBoard(dacModelA);
        storeToBoard(dacModelB);
        storeToBoard(dacModelC);
    }
    
    private void storeToBoard(DACChannelDataModel dataModel) throws SensorBusException {
        
        int subChannel = dataModel.getSubChannel();
        int value = dataModel.getValue();
        int gain = ((DACGainModel)jcbGain.getModel()).getRegisterSelectedValue();
        
        shieldProtocolLayer.renderDAC5694RegSetup(boardId, channelId, subChannel, value, gain);
    }
    
    @Override
    public void readFromBoard() throws SensorBusException {
        shieldProtocolLayer.renderDAC5694ReadSetup(boardId, channelId, IDDAC_A);
        shieldProtocolLayer.renderDAC5694ReadSetup(boardId, channelId, IDDAC_B);
        shieldProtocolLayer.renderDAC5694ReadSetup(boardId, channelId, IDDAC_C);
    }
    
    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        List<Integer> registers;
        registers = shieldProtocolLayer.evalDACRegistersInquiry(rxMessage, boardId, channelId);
        if ((registers != null) && (!registers.isEmpty())) {
            
            int subChannel = registers.get(0);
            int value = registers.get(1);
            int gain = registers.get(2);
            
            dacModelA.setValue(value, subChannel);
            dacModelB.setValue(value, subChannel);
            dacModelC.setValue(value, subChannel);
            ((DACGainModel)jcbGain.getModel()).setSelectedItem(gain);
            
            updateAllSliders();
        }
    }
    
    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        evaluateRxMessage(rxMessage);
    }

    @Override
    public void loadPresetValues(PresetValues presetValues) throws PresetException {
        
        dacModelA.setValue(presetValues.get("AD5694_0").getValueAsInteger(), dacModelA.getSubChannel());
        dacModelB.setValue(presetValues.get("AD5694_1").getValueAsInteger(), dacModelB.getSubChannel());
        dacModelC.setValue(presetValues.get("AD5694_2").getValueAsInteger(), dacModelC.getSubChannel());
        ((DACGainModel)jcbGain.getModel()).setSelectedItem(presetValues.get("AD5694_G").getValueAsInteger());
        
        updateAllSliders();
        jcbGain.updateUI();
    }

    @Override
    public void savePresetValues(PresetValues presetValues) throws PresetException {
        
        presetValues.add(new PresetValue("AD5694_0", dacModelA.getValue()));
        presetValues.add(new PresetValue("AD5694_1", dacModelB.getValue()));
        presetValues.add(new PresetValue("AD5694_2", dacModelC.getValue()));
        presetValues.add(new PresetValue("AD5694_G", ((DACGainModel)jcbGain.getModel()).getRegisterSelectedValue()));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dacModelA = new airsenseur.dev.chemsensorpanel.dataModel.DACChannelDataModel();
        dacModelB = new airsenseur.dev.chemsensorpanel.dataModel.DACChannelDataModel();
        dacModelC = new airsenseur.dev.chemsensorpanel.dataModel.DACChannelDataModel();
        dacGainModel = new airsenseur.dev.chemsensorpanel.dataModel.DACGainModel();
        jSliderA = new javax.swing.JSlider();
        jSliderB = new javax.swing.JSlider();
        jSliderC = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jlblVOutA = new javax.swing.JLabel();
        jlblVOutB = new javax.swing.JLabel();
        jlblVOutC = new javax.swing.JLabel();
        jcbGain = new javax.swing.JComboBox();

        setPreferredSize(new java.awt.Dimension(427, 195));

        jSliderA.setMinorTickSpacing(512);
        jSliderA.setPaintTicks(true);
        jSliderA.setModel(dacModelA);
        jSliderA.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderAStateChanged(evt);
            }
        });

        jSliderB.setMinorTickSpacing(512);
        jSliderB.setPaintTicks(true);
        jSliderB.setModel(dacModelB);
        jSliderB.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderBStateChanged(evt);
            }
        });

        jSliderC.setMinorTickSpacing(512);
        jSliderC.setPaintTicks(true);
        jSliderC.setModel(dacModelC);
        jSliderC.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderCStateChanged(evt);
            }
        });

        jLabel1.setText("Ref-");

        jLabel2.setText("Ref AD");

        jLabel3.setText("Ref AFE");

        jlblVOutA.setText("?");

        jlblVOutB.setText("?");

        jlblVOutC.setText("?");

        jcbGain.setModel(dacGainModel);
        jcbGain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcbGainActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addGap(42, 42, 42)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jSliderA, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                    .addComponent(jSliderB, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jSliderC, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jlblVOutA, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                    .addComponent(jlblVOutB, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jlblVOutC, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcbGain, 0, 95, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jlblVOutA)
                            .addComponent(jSliderA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jSliderB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jlblVOutB)
                    .addComponent(jcbGain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSliderC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jlblVOutC, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jSliderAStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderAStateChanged
        jlblVOutA.setText(getVoltageLabel(jSliderA.getValue(), dacGainModel.getRegisterSelectedValue() != 0));
    }//GEN-LAST:event_jSliderAStateChanged

    private void jSliderBStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderBStateChanged
        jlblVOutB.setText(getVoltageLabel(jSliderB.getValue(), dacGainModel.getRegisterSelectedValue() != 0));        
    }//GEN-LAST:event_jSliderBStateChanged

    private void jSliderCStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderCStateChanged
        boolean doubleGain = dacGainModel.getRegisterSelectedValue() != 0;
        int value = jSliderC.getValue();
        jlblVOutC.setText(getVoltageLabel(value, doubleGain));
        
        double dbValue = ((doubleGain == true)? 5.0f : 2.5f) * value / 4095f;
        if (lmp9100Panel != null) {
            lmp9100Panel.externalVoltageUpdated(dbValue);
        }
    }//GEN-LAST:event_jSliderCStateChanged

    private void jcbGainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbGainActionPerformed
        updateAllSliders();
    }//GEN-LAST:event_jcbGainActionPerformed

    private void updateAllSliders() {
        jSliderAStateChanged(null);
        jSliderBStateChanged(null);
        jSliderCStateChanged(null);
    }
    
    /**
     * @param lmp9100Panel the lmp9100Panel to set
     */
    public void setLmp9100Panel(LMP91000Panel lmp9100Panel) {
        this.lmp9100Panel = lmp9100Panel;
        updateAllSliders();        
    }
    
    private LMP91000Panel lmp9100Panel;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.dataModel.DACGainModel dacGainModel;
    private airsenseur.dev.chemsensorpanel.dataModel.DACChannelDataModel dacModelA;
    private airsenseur.dev.chemsensorpanel.dataModel.DACChannelDataModel dacModelB;
    private airsenseur.dev.chemsensorpanel.dataModel.DACChannelDataModel dacModelC;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JSlider jSliderA;
    private javax.swing.JSlider jSliderB;
    private javax.swing.JSlider jSliderC;
    private javax.swing.JComboBox jcbGain;
    private javax.swing.JLabel jlblVOutA;
    private javax.swing.JLabel jlblVOutB;
    private javax.swing.JLabel jlblVOutC;
    // End of variables declaration//GEN-END:variables

}
