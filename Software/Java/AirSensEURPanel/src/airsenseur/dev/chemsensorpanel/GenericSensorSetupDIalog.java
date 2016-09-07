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

import airsenseur.dev.comm.CommProtocolHelper;

/**
 *
 * @author marco
 */
public class GenericSensorSetupDIalog extends SensorSetupDialog {
    
    private String sensorName = "Generic Sensor";
    private int sensorId = 0;

    /**
     * Creates new form GenericSensorSetupDIalog
     * @param sensorName
     * @param sensorId
     * @param parent
     * @param modal
     */
    public GenericSensorSetupDIalog(String sensorName, int sensorId, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.sensorName = sensorName;
        this.sensorId = sensorId;
        
        initComponents();
    }
    
    /**
     * Update the board with the information set on this setup panel
     */
    @Override
    public void storeToBoard() {
        iIRAndAvgPanel.storeToBoard(sensorId);
        
        CommProtocolHelper.instance().renderSavePresetWithName(sensorId, "unnamed");
    }    

    @Override
    public void readFromBoard() {

        iIRAndAvgPanel.readFromBoard(sensorId);
    }

    @Override
    public void evaluateRxMessage(CommProtocolHelper.DataMessage rxMessage) {
        
        iIRAndAvgPanel.evaluateRxMessage(rxMessage, sensorId);
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

        iIRAndAvgPanel = new airsenseur.dev.chemsensorpanel.IIRAndAvgPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(sensorName);
        setResizable(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(iIRAndAvgPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private airsenseur.dev.chemsensorpanel.IIRAndAvgPanel iIRAndAvgPanel;
    // End of variables declaration//GEN-END:variables

}
