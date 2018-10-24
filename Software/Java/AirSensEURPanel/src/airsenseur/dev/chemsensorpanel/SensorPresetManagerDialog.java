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

import airsenseur.dev.chemsensorpanel.dataModel.SensorPresetsDataModel;
import airsenseur.dev.chemsensorpanel.exceptions.PresetException;
import airsenseur.dev.chemsensorpanel.sensorsdb.PresetDao;
import airsenseur.dev.chemsensorpanel.sensorsdb.SensorDbFactory;
import javax.swing.JOptionPane;

/**
 *
 * @author marco
 */
public class SensorPresetManagerDialog extends javax.swing.JDialog {
    
    public static interface CallBack {
        void onDatabaseChanged();
    }
    
    private CallBack callBack;

    /**
     * Creates new form SensorPresetManagerDialog
     * @param parent
     * @param modal
     */
    public SensorPresetManagerDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        aD5694RPanel.setLmp9100Panel(lMP91000Panel);
        
        if(jComboBoxPresets.getItemCount() > 0) {
            jComboBoxPresets.setSelectedIndex(0);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        try {
            sensorPresetsDataModel = new airsenseur.dev.chemsensorpanel.dataModel.SensorPresetsDataModel();
        } catch (airsenseur.dev.chemsensorpanel.exceptions.PresetException e1) {
            e1.printStackTrace();
        }
        jComboBoxPresets = new javax.swing.JComboBox();
        jButtonSave = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        lMP91000Panel = new airsenseur.dev.chemsensorpanel.widgets.LMP91000Panel();
        aD5694RPanel = new airsenseur.dev.chemsensorpanel.widgets.AD5694RPanel();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        jComboBoxPresets.setEditable(true);
        jComboBoxPresets.setModel(sensorPresetsDataModel);
        jComboBoxPresets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxPresetsActionPerformed(evt);
            }
        });

        jButtonSave.setText("Save");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jLabel1.setText("Preset Name:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lMP91000Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jComboBoxPresets, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButtonSave)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonDelete)
                                .addGap(6, 6, 6))
                            .addComponent(jSeparator1)
                            .addComponent(aD5694RPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxPresets, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSave)
                    .addComponent(jButtonDelete)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lMP91000Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(aD5694RPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        savePreset();
    }//GEN-LAST:event_jButtonSaveActionPerformed

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        deletePreset();
    }//GEN-LAST:event_jButtonDeleteActionPerformed

    private void jComboBoxPresetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxPresetsActionPerformed
        
        Object selectedObj = jComboBoxPresets.getSelectedItem();
        
        if (selectedObj instanceof PresetDao) {
            try {
                PresetDao preset = (PresetDao)selectedObj;
                
                lMP91000Panel.loadPresetValues(preset.getValues());
                aD5694RPanel.loadPresetValues(preset.getValues());
            } catch (PresetException ex) {
                JOptionPane.showMessageDialog(this, ex.getErrorMessage(), "Save Preset", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jComboBoxPresetsActionPerformed

    public void registerCallBack(CallBack callBack) {
        this.callBack = callBack;
    }
    
    private void savePreset() {

        Object item = jComboBoxPresets.getSelectedItem();
        if (item == null) {
            return;
        }
        
        String presetName = item.toString();
        if ((presetName == null) || presetName.isEmpty()) {
            return;
        }
        
        PresetDao preset = new PresetDao();
        preset.setPresetName(presetName);
        try {
            if (SensorDbFactory.getInstance().isKnownPreset(preset)) {
                int res = JOptionPane.showConfirmDialog(this, 
                                                    "Preset still exists. Do you want to override it?", 
                                                    "Save Preset", 
                                                    JOptionPane.OK_CANCEL_OPTION, 
                                                    JOptionPane.WARNING_MESSAGE);
                if (res != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            
            lMP91000Panel.savePresetValues(preset.getValues());
            aD5694RPanel.savePresetValues(preset.getValues());
            
            SensorDbFactory.getInstance().savePreset(preset);
            
            refreshPresetList();
            
        } catch (PresetException ex) {
            
            JOptionPane.showMessageDialog(this, ex.getErrorMessage(), "Save Preset", JOptionPane.ERROR_MESSAGE);
        }
    }
        
    private void deletePreset() {
        
        Object item = jComboBoxPresets.getSelectedItem();
        if (item == null) {
            return;
        }
        
        String presetName = item.toString();
        if ((presetName == null) || presetName.isEmpty()) {
            return;
        }
        
        PresetDao preset = new PresetDao();
        preset.setPresetName(presetName);

        int res = JOptionPane.showConfirmDialog(this, 
                                            "Are you sure you want to delete this preset?", 
                                            "Delete Preset", 
                                            JOptionPane.OK_CANCEL_OPTION, 
                                            JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.OK_OPTION) {
            return;
        }
        
        try {
            SensorDbFactory.getInstance().deletePreset(preset);
            
        } catch (PresetException ex) {
            
            JOptionPane.showMessageDialog(this, ex.getErrorMessage(), "Delete Preset", JOptionPane.ERROR_MESSAGE);
        }
        
        refreshPresetList();
    }
    
    private void refreshPresetList() {
        
        try {        
            sensorPresetsDataModel = new SensorPresetsDataModel();
            jComboBoxPresets.setModel(sensorPresetsDataModel);
        } catch (PresetException ex) {
        }
        
        if(jComboBoxPresets.getItemCount() > 0) {
            jComboBoxPresets.setSelectedIndex(0);
        }
        
        // Propagate this event to an external listener
        if (callBack != null) {
            callBack.onDatabaseChanged();
        }
    }
    
            
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.widgets.AD5694RPanel aD5694RPanel;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JComboBox jComboBoxPresets;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JSeparator jSeparator1;
    private airsenseur.dev.chemsensorpanel.widgets.LMP91000Panel lMP91000Panel;
    private airsenseur.dev.chemsensorpanel.dataModel.SensorPresetsDataModel sensorPresetsDataModel;
    // End of variables declaration//GEN-END:variables
}
