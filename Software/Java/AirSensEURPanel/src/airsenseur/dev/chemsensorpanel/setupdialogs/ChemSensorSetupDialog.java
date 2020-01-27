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
public class ChemSensorSetupDialog extends SensorSetupDialog {
    
    /**
     * Creates new form ChemSensorSetupDialog
     * @param parent
     * @param modal
     * @param sensorId
     */
    public ChemSensorSetupDialog(MainApplicationFrame parent, boolean modal, int sensorId) {
        super(parent, modal, sensorId);
        
        initComponents();
        
        lMP91000Panel.setBoardId(boardId);
        lMP91000Panel.setChannelId(sensorId);
        
        aD5694RPanel.setLmp9100Panel(lMP91000Panel);
        aD5694RPanel.setBoardId(boardId);
        aD5694RPanel.setChannelId(sensorId);
        
        iIRAndAvgPanel.setBoardId(boardId);
        iIRAndAvgPanel.setChannelId(sensorId);
        
        sensorPresetLoadPanel.registerPresetDrivenPanel(lMP91000Panel);        
        sensorPresetLoadPanel.registerPresetDrivenPanel(aD5694RPanel);
    }

    @Override
    public void setBoardId(int boardId) {
        super.setBoardId(boardId);
        
        lMP91000Panel.setBoardId(boardId);
        aD5694RPanel.setBoardId(boardId);
        iIRAndAvgPanel.setBoardId(boardId);
    }

    @Override
    public void setShieldProtocolLayer(ShieldProtocolLayer shieldProtocolLayer) {
        super.setShieldProtocolLayer(shieldProtocolLayer);

        lMP91000Panel.setShieldProtocolLayer(shieldProtocolLayer);
        aD5694RPanel.setShieldProtocolLayer(shieldProtocolLayer);
        iIRAndAvgPanel.setShieldProtocolLayer(shieldProtocolLayer);
    }
    
    
    /**
     * Update the board with the information set on this setup panel
     */
    @Override
    public void storeToBoard() throws SensorBusException {

        aD5694RPanel.storeToBoard();        
        iIRAndAvgPanel.storeToBoard();
        lMP91000Panel.storeToBoard();
    }

    @Override
    public void readFromBoard() throws SensorBusException {
        aD5694RPanel.readFromBoard();        
        iIRAndAvgPanel.readFromBoard();
        lMP91000Panel.readFromBoard();
    }

    @Override
    public void evaluateRxMessage(AppDataMessage rxMessage) {
        
        aD5694RPanel.evaluateRxMessage(rxMessage);        
        lMP91000Panel.evaluateRxMessage(rxMessage);
        iIRAndAvgPanel.evaluateRxMessage(rxMessage);
    }
    
    @Override
    public void onDataMessageFromConfiguration(AppDataMessage rxMessage) {
        aD5694RPanel.onDataMessageFromConfiguration(rxMessage);
        lMP91000Panel.onDataMessageFromConfiguration(rxMessage);
        iIRAndAvgPanel.onDataMessageFromConfiguration(rxMessage);
    }    
    
    @Override
    public void onSensorPresetDatabaseChanged() {
        sensorPresetLoadPanel.onSensorPresetDatabaseChanged();
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lMP91000Panel = new airsenseur.dev.chemsensorpanel.widgets.LMP91000Panel();
        iIRAndAvgPanel = new airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel();
        aD5694RPanel = new airsenseur.dev.chemsensorpanel.widgets.AD5694RPanel();
        sensorPresetLoadPanel = new airsenseur.dev.chemsensorpanel.SensorPresetLoadPanel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Chemical Sensor Channel " + (sensorId + 1));
        setResizable(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lMP91000Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aD5694RPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(sensorPresetLoadPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 475, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator2)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jSeparator1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sensorPresetLoadPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lMP91000Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(aD5694RPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.widgets.AD5694RPanel aD5694RPanel;
    private airsenseur.dev.chemsensorpanel.widgets.IIRAndAvgPanel iIRAndAvgPanel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private airsenseur.dev.chemsensorpanel.widgets.LMP91000Panel lMP91000Panel;
    private airsenseur.dev.chemsensorpanel.SensorPresetLoadPanel sensorPresetLoadPanel;
    // End of variables declaration//GEN-END:variables

}
