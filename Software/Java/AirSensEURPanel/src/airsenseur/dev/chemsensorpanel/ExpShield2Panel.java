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

import airsenseur.dev.chemsensorpanel.helpers.FileLogger;
import airsenseur.dev.chemsensorpanel.helpers.HostConfigSensorProperties;
import airsenseur.dev.chemsensorpanel.helpers.HostConfigWriter;
import airsenseur.dev.chemsensorpanel.setupdialogs.ADT7470SetupDialog;
import airsenseur.dev.chemsensorpanel.setupdialogs.EXP2PIDSetupDIalog;
import airsenseur.dev.chemsensorpanel.setupdialogs.GenericBoardInfoDialog;
import airsenseur.dev.chemsensorpanel.setupdialogs.GenericSensorSetupDIalog;
import airsenseur.dev.chemsensorpanel.setupdialogs.K96SensorSetupDIalog;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.exceptions.SensorBusException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author marco
 */
public class ExpShield2Panel extends GenericTabPanel {
    
    public final static int CHANNEL_TEMPERATURE_I = 0x00;
    public final static int CHANNEL_HUMIDIDY_I = 0x01;
    public final static int CHANNEL_TEMPERATURE_E = 0x02;
    public final static int CHANNEL_HUMIDIDY_E = 0x03;
    public final static int CHANNEL_PELTIER_V = 0x04;
    public final static int CHANNEL_PELTIER_C = 0x05;
    public final static int CHANNEL_VIN_FBK = 0x06;
    public final static int CHANNEL_UCDIE_TEMPERATURE = 0x07;
    public final static int CHANNEL_PID_HEATER = 0x08;
    public final static int CHANNEL_PID_COOLER = 0x09;
    public final static int CHANNEL_ADT7470_T_INT_CHAMBER = 0x0A;
    public final static int CHANNEL_ADT7470_T_EXT_HEATSINK = 0x0B;
    public final static int CHANNEL_ADT7470_T_INT_HEATSINK = 0x0C;
    public final static int CHANNEL_ADT7470_F_EXT_HEATSINK = 0x0D;
    public final static int CHANNEL_ADT7470_F_INT_HEATSINK = 0x0E;
    public final static int CHANNEL_ADT7470_F_AIR_CIR = 0x0F;
    public final static int CHANNEL_D300 = 0x10;    
    public final static int CHANNEL_K96_LPL_PC_FLT = 0x11;
    public final static int CHANNEL_K96_SPL_PC_FLT = 0x12;
    public final static int CHANNEL_K96_MPL_PC_FLT = 0x13;
    public final static int CHANNEL_K96_PRESS0 = 0x14;
    public final static int CHANNEL_K96_TEMP_NTC0 = 0x15;
    public final static int CHANNEL_K96_TEMP_NTC1 = 0x16;
    public final static int CHANNEL_K96_TEMP_UCDIE = 0x17;
    public final static int CHANNEL_K96_RH0 = 0x18;
    public final static int CHANNEL_K96_T_RH0 = 0x19;
    public final static int CHANNEL_K96_ERRORSTATUS = 0x1A;
    
    public final static int CHANNEL_K96_NUM_CHANNELS = CHANNEL_K96_ERRORSTATUS - CHANNEL_K96_LPL_PC_FLT + 1;
    
    public final static int EXPSHIELD2_NUM_OF_CHANNELS = CHANNEL_K96_ERRORSTATUS + 1;
    
    public final static int EXPSHIELD2_SETUP_DIALOG_SHT31I = 0;    
    public final static int EXPSHIELD2_SETUP_DIALOG_SHT31E = 1;
    public final static int EXPSHIELD2_SETUP_DIALOG_INTAD = 2;
    public final static int EXPSHIELD2_SETUP_DIALOG_PID = 3;
    public final static int EXPSHIELD2_SETUP_DIALOG_ADT7470 = 4;
    public final static int EXPSHIELD2_SETUP_DIALOG_D300 = 5;    
    public final static int EXPSHIELD2_SETUP_DIALOG_K96 = 6;
    public final static int EXPSHIELD2_SETUP_GENERIC_INFO = 7;
    
    private int selectedBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
    private boolean boardEnabled = false;
    
    private final static String DEFAULT_CHANNEL_MATH_EXPRESSION =  "x";
    private final static String SHT31_TEMP_CHANNEL_MATH_EXPRESSION = "((x/65535*175) - 45.0)";
    private final static String SHT31_HUMIDITY_CHANNEL_MATH_EXPRESSION = "(x/65535)*100.0";
    private final static String INTAD_12V_MATH_EXPRESSION = "x * (13.6 / 3.6) * 1000";
    private final static String INTAD_CHANNEL_PLTC_MATH_EXPRESSION = "x * (1/(100 * 0.003)) * 1000";
    private final static String DIVIDE_BY_100_MATH_EXPRESSION = "x/100";
    private final static String DIVIDE_BY_10_MATH_EXPRESSION = "x/10";
    private final static String ADT7470_FAN_SPEED_MAT_EXPRESSION = "if(x>0,(90000*60)/x,0)";
    
    // The chemical sensors setup panels
    private final List<SensorSetupDialog> sensorSetupDialogs = new ArrayList<>();
    
    private final List<SampleLogger> sampleLoggerPanels = new ArrayList<>();
    
    private String boardSerialNumber = "";
    
    // This is used only by the graphical composition tool in the Netbeans IDE
    public ExpShield2Panel() {
        initComponents();
    }

    /**
     * Creates new form ChemSensorPanel
     * @param parent
     * @param shieldProtocolLayer
     * @param logger
     */
    public ExpShield2Panel(MainApplicationFrame parent, ShieldProtocolLayer shieldProtocolLayer, FileLogger logger) {
        super(shieldProtocolLayer, logger);
        
        // Generate the sensor setup dialogs
        sensorSetupDialogs.add(new GenericSensorSetupDIalog("SHT31I", CHANNEL_TEMPERATURE_I, 2, true, false, true, parent, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog("SHT31E", CHANNEL_TEMPERATURE_E, 2, true, false, true, parent, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog("INTAD", CHANNEL_PELTIER_V, 4, true, true, true, parent, false));
        sensorSetupDialogs.add(new EXP2PIDSetupDIalog(CHANNEL_PID_HEATER, CHANNEL_PID_COOLER, CHANNEL_PID_HEATER, parent, false));
        sensorSetupDialogs.add(new ADT7470SetupDialog(CHANNEL_ADT7470_T_INT_CHAMBER, CHANNEL_ADT7470_T_INT_CHAMBER, CHANNEL_ADT7470_F_EXT_HEATSINK,
                                                        CHANNEL_ADT7470_T_INT_CHAMBER, CHANNEL_ADT7470_F_EXT_HEATSINK,
                                                        CHANNEL_ADT7470_F_INT_HEATSINK, CHANNEL_ADT7470_F_AIR_CIR, parent, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog("D300", CHANNEL_D300, 1, false, true, true, parent, false));        
        sensorSetupDialogs.add(new K96SensorSetupDIalog(CHANNEL_K96_LPL_PC_FLT, 
                                                        CHANNEL_K96_LPL_PC_FLT, 
                                                        CHANNEL_K96_SPL_PC_FLT, CHANNEL_K96_MPL_PC_FLT, 
                                                        CHANNEL_K96_PRESS0, CHANNEL_K96_TEMP_NTC0, 
                                                        CHANNEL_K96_RH0, CHANNEL_K96_ERRORSTATUS, parent, false));
        sensorSetupDialogs.add(new GenericBoardInfoDialog(parent, false, "ExpShield2 Generic Info"));
                
        initComponents();
        
        // Aggregate sample loggers so it's more easy to handle them
        sampleLoggerPanels.add(sampleLoggerD300);
        sampleLoggerPanels.add(sampleLoggerK96);
        sampleLoggerPanels.add(sampleLoggerK96_SPL);
        sampleLoggerPanels.add(sampleLoggerK96_MPL);
        sampleLoggerPanels.add(sampleLoggerK96_Press0);
        sampleLoggerPanels.add(sampleLoggerK96_NTC0);
        sampleLoggerPanels.add(sampleLoggerK96_NTC1);
        sampleLoggerPanels.add(sampleLoggerK96_uCDie);
        sampleLoggerPanels.add(sampleLoggerK96_RH0);
        sampleLoggerPanels.add(sampleLoggerK96_T_RH0);
        sampleLoggerPanels.add(sampleLoggerK96_Error);
        sampleLoggerPanels.add(sampleLoggerSHT31E_H);
        sampleLoggerPanels.add(sampleLoggerSHT31E_T);
        sampleLoggerPanels.add(sampleLoggerSHT31I_H);
        sampleLoggerPanels.add(sampleLoggerSHT31I_T);
        sampleLoggerPanels.add(sampleLoggerPLT_C);
        sampleLoggerPanels.add(sampleLoggerPLT_V);
        sampleLoggerPanels.add(sampleLoggerPID_C);
        sampleLoggerPanels.add(sampleLoggerPID_H);
        sampleLoggerPanels.add(sampleLoggerAirCircFan);
        sampleLoggerPanels.add(sampleLoggerExtFan);
        sampleLoggerPanels.add(sampleLoggerIntFan);
        sampleLoggerPanels.add(sampleLoggerT_Ext_Heatsink);
        sampleLoggerPanels.add(sampleLoggerT_Int_Chamber);
        sampleLoggerPanels.add(sampleLoggerT_Int_Heatsink);
        sampleLoggerPanels.add(sampleLoggerT_uC);
        sampleLoggerPanels.add(sampleLoggerVIN);
        
        // Initialize all loggers with common properties
        sampleLoggerD300.setLoggerProperties("D300 [ppm]", 0, -1, 10);
        sampleLoggerD300.setSensorId(CHANNEL_D300);
        
        sampleLoggerK96.setLoggerProperties("K96 LPL [ppm]", 0, -1, 10);
        sampleLoggerK96.setSensorId(CHANNEL_K96_LPL_PC_FLT);
        
        sampleLoggerK96_SPL.setSensorId(CHANNEL_K96_SPL_PC_FLT);
        sampleLoggerK96_MPL.setSensorId(CHANNEL_K96_MPL_PC_FLT);
        sampleLoggerK96_Press0.setSensorId(CHANNEL_K96_PRESS0);
        sampleLoggerK96_NTC0.setSensorId(CHANNEL_K96_TEMP_NTC0);
        sampleLoggerK96_NTC1.setSensorId(CHANNEL_K96_TEMP_NTC1);
        sampleLoggerK96_uCDie.setSensorId(CHANNEL_K96_TEMP_UCDIE);
        sampleLoggerK96_RH0.setSensorId(CHANNEL_K96_RH0);
        sampleLoggerK96_T_RH0.setSensorId(CHANNEL_K96_T_RH0);
        sampleLoggerK96_Error.setSensorId(CHANNEL_K96_ERRORSTATUS);
        sampleLoggerK96_Error.setDataFormatting(SampleLogger.formatToFourDigitHex);
        
        sampleLoggerSHT31I_T.setSensorId(CHANNEL_TEMPERATURE_I);
        sampleLoggerSHT31I_H.setSensorId(CHANNEL_HUMIDIDY_I);
        
        sampleLoggerSHT31E_T.setSensorId(CHANNEL_TEMPERATURE_E);
        sampleLoggerSHT31E_H.setSensorId(CHANNEL_HUMIDIDY_E);
        
        sampleLoggerPLT_C.setSensorId(CHANNEL_PELTIER_C);
        sampleLoggerPLT_V.setSensorId(CHANNEL_PELTIER_V);
        
        sampleLoggerPID_C.setSensorId(CHANNEL_PID_COOLER);
        sampleLoggerPID_H.setSensorId(CHANNEL_PID_HEATER);
        
        sampleLoggerAirCircFan.setSensorId(CHANNEL_ADT7470_F_AIR_CIR);
        sampleLoggerExtFan.setSensorId(CHANNEL_ADT7470_F_EXT_HEATSINK);
        sampleLoggerIntFan.setSensorId(CHANNEL_ADT7470_F_INT_HEATSINK);
        
        sampleLoggerT_Ext_Heatsink.setSensorId(CHANNEL_ADT7470_T_EXT_HEATSINK);
        sampleLoggerT_Int_Chamber.setSensorId(CHANNEL_ADT7470_T_INT_CHAMBER);
        sampleLoggerT_Int_Heatsink.setSensorId(CHANNEL_ADT7470_T_INT_HEATSINK);
        
        sampleLoggerT_uC.setSensorId(CHANNEL_UCDIE_TEMPERATURE);
        sampleLoggerVIN.setSensorId(CHANNEL_VIN_FBK);
                
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).setLogger(logger);
            sampleLoggerPanels.get(n).setBoardId(selectedBoardId);
            sampleLoggerPanels.get(n).setShieldProtocolLayer(shieldProtocolLayer);
            sampleLoggerPanels.get(n).setHighResolutionMode();
        }

        // Initialize al sensorSetupDialogs
        for (SensorSetupDialog dialog:sensorSetupDialogs) {
            dialog.setShieldProtocolLayer(shieldProtocolLayer);
            dialog.setBoardId(selectedBoardId);
        }
        
        // Board ID handling
        onBoardIDChanged();
    }

    @Override
    public List<SensorSetupDialog> getSensorSetupDialogs() {
        return sensorSetupDialogs;
    }
    
    @Override
    public void onRefreshTimer() throws SensorBusException {
        if (!boardEnabled) {
            return;
        }        
        
        // Ask for a sample
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).readFromBoard();
        }
        
        // Get the board sensor name
        shieldProtocolLayer.renderReadBoardSerialNumber(selectedBoardId);
    }

    @Override
    public int getSelectedBoardId() {
        return selectedBoardId;
    }

    @Override
    public boolean getIsEnabled() {
        return boardEnabled;
    }

    @Override
    public void setConnected(boolean connected) {
        jCBBoardId.setEnabled(!connected);
        
        // Propagate to the configuration dialogs, they may require this information
        for (SensorSetupDialog dialog : sensorSetupDialogs) {
            dialog.setConnected(connected);
        }
    }
    
    @Override
    public void onDataReceived(AppDataMessage rxMessage) {
        
        if (!boardEnabled) {
            return;
        }        
        
        // Loop on each panel and propagate this message
        for(SensorSetupDialog setupDialog : sensorSetupDialogs) {
            setupDialog.evaluateRxMessage(rxMessage);
        }

        // Update the sample loggers
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).evaluateRxMessage(rxMessage);
        }
        
        String serialBoard = shieldProtocolLayer.evalReadBoardSerialNumber(rxMessage, selectedBoardId);
        if (serialBoard != null) {
            boardSerialNumber = serialBoard;
            jLabelBoardSerialNumber.setText(serialBoard);
        }
    }

    @Override
    public void storeToBoard() throws SensorBusException {
        if (!boardEnabled) {
            return;
        }        
        
        // Generate the command list to be sent to be board
        for(SensorSetupDialog setupDialog : sensorSetupDialogs) {

            // Render the command for this dialog
            setupDialog.storeToBoard();

            try {
                // Wait some time
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(AirSensEURPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }   
    }

    @Override
    public void readFromBoard() throws SensorBusException {
        if (!boardEnabled) {
            return;
        }        
        
        // Generate the command list to be sent to the board
        for (SensorSetupDialog setupDialog : sensorSetupDialogs) {

            // Render commands for this dialog
            setupDialog.readFromBoard();

            try {
                // Wait some time
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(AirSensEURPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void startSample() throws SensorBusException {
        if (!boardEnabled) {
            return;
        }
        
        // Add the start sample command to the buffer
        shieldProtocolLayer.renderStartSample(selectedBoardId);
    }

    @Override
    public void stopSample() throws SensorBusException {
        if (!boardEnabled) {
            return;
        }
        
        // Add the stop sample command to the buffer
        shieldProtocolLayer.renderStopSample(selectedBoardId);
    }

    @Override
    public void onDataMessageFromConfiguration(AppDataMessage configurationMessage) {
        
        // Target the configuration message to the selected board Id
        AppDataMessage targetConfigurationMessage = new AppDataMessage(selectedBoardId, configurationMessage.getCommandString(), configurationMessage.getCommandComment());
        
        // Loop on each panel and propagate this message
        for(SensorSetupDialog setupDialog : sensorSetupDialogs) {
            setupDialog.onDataMessageFromConfiguration(targetConfigurationMessage);
        }
    }

    @Override
    public void onGetCurrentConfiguation(boolean forceRestartSampling) throws SensorBusException {
        
        // Add the stop sample command to the buffer, if required
        if (forceRestartSampling) {
            shieldProtocolLayer.renderStopSample(selectedBoardId);
        }
        
        for(SensorSetupDialog setupDialog : sensorSetupDialogs) {

            // Render the command for this dialog
            setupDialog.storeToBoard();
        }
        
        // Add the start sample command to the buffer, if required
        if (forceRestartSampling) {
            shieldProtocolLayer.renderStartSample(selectedBoardId);
        }
    }
    
    @Override
    public String getBoardSerialNumber() {
        return boardSerialNumber;
    }
    
    @Override
    public void onDatabaseChanged() {
        // Nothing to do with presets
    }
    
    @Override
    public void collectHostConfigurationInformation(HostConfigWriter hostConfigWriter) {
        
        // All channels in the Expansion Two Shields have the same behaviour. 
        // Channel name is read from the shield at startup
        for (int n = 0; n <= EXPSHIELD2_NUM_OF_CHANNELS; n++) {
            HostConfigSensorProperties sensorProperties = hostConfigWriter.addNewSensor();
            sensorProperties.setSensorBoardId(selectedBoardId);
            sensorProperties.setSensorChannel(n);
            
            if ((n == CHANNEL_TEMPERATURE_I) || (n == CHANNEL_TEMPERATURE_E)) {
                sensorProperties.setSensorExpression(SHT31_TEMP_CHANNEL_MATH_EXPRESSION);
            } else if ((n == CHANNEL_HUMIDIDY_I) || (n == CHANNEL_HUMIDIDY_E)) {
                sensorProperties.setSensorExpression(SHT31_HUMIDITY_CHANNEL_MATH_EXPRESSION);
            } else if ( ( n == CHANNEL_PELTIER_V ) || (n == CHANNEL_VIN_FBK)) {
                sensorProperties.setSensorExpression(INTAD_12V_MATH_EXPRESSION);
            } else if ( n == CHANNEL_PELTIER_C ) {
                sensorProperties.setSensorExpression(INTAD_CHANNEL_PLTC_MATH_EXPRESSION);
            } else if ( (n >= CHANNEL_PID_HEATER) && (n <= CHANNEL_ADT7470_T_INT_HEATSINK)) {
                sensorProperties.setSensorExpression(DIVIDE_BY_100_MATH_EXPRESSION);
            } else if ( (n >= CHANNEL_ADT7470_F_EXT_HEATSINK) && (n <= CHANNEL_ADT7470_F_AIR_CIR)) {
                sensorProperties.setSensorExpression(ADT7470_FAN_SPEED_MAT_EXPRESSION);
            } else if ( (n >= CHANNEL_K96_LPL_PC_FLT) && (n < CHANNEL_K96_PRESS0)) {
                sensorProperties.setSensorExpression(DEFAULT_CHANNEL_MATH_EXPRESSION);
            } else if ( n == CHANNEL_K96_PRESS0 ) {
                sensorProperties.setSensorExpression(DIVIDE_BY_10_MATH_EXPRESSION);
            } else if ( (n >= CHANNEL_K96_TEMP_NTC0) && (n < CHANNEL_K96_ERRORSTATUS) ) {
                sensorProperties.setSensorExpression(DIVIDE_BY_100_MATH_EXPRESSION);
            } else {
                sensorProperties.setSensorExpression(DEFAULT_CHANNEL_MATH_EXPRESSION);
            }
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

        jCBBoardId = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        sampleLoggerK96 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        sampleLoggerD300 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        jLabelBoardSerialNumber = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        sampleLoggerVIN = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        jPanelFans = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        sampleLoggerExtFan = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerIntFan = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerAirCircFan = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        jPanelPID = new javax.swing.JPanel();
        sampleLoggerPID_C = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        sampleLoggerPID_H = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        jPanelPeltier = new javax.swing.JPanel();
        sampleLoggerPLT_C = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        sampleLoggerPLT_V = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        jPanelTemperatures = new javax.swing.JPanel();
        sampleLoggerT_Int_Chamber = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerT_uC = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerT_Int_Heatsink = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerT_Ext_Heatsink = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        jPanelSHT = new javax.swing.JPanel();
        sampleLoggerSHT31I_T = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        sampleLoggerSHT31I_H = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerSHT31E_H = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerSHT31E_T = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel();
        jPanelK96Extended = new javax.swing.JPanel();
        sampleLoggerK96_SPL = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerK96_MPL = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerK96_Press0 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerK96_NTC0 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerK96_NTC1 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerK96_uCDie = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerK96_RH0 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerK96_T_RH0 = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerK96_Error = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();

        setPreferredSize(new java.awt.Dimension(858, 547));

        jCBBoardId.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Not Connected", "ID 0", "ID 1", "ID 2", "ID 3", "ID 4", "ID 5", "ID 6", "ID 7", "ID 8", "ID 9", "ID 10", "ID 11", "ID 12", "ID 13", "ID 14" }));
        jCBBoardId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBBoardIdActionPerformed(evt);
            }
        });

        jLabel2.setText("Board ID:");

        jLabelBoardSerialNumber.setText("--");

        jLabel3.setText("Board serial number:");

        jPanelFans.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel13.setText("Fans:");

        javax.swing.GroupLayout jPanelFansLayout = new javax.swing.GroupLayout(jPanelFans);
        jPanelFans.setLayout(jPanelFansLayout);
        jPanelFansLayout.setHorizontalGroup(
            jPanelFansLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 202, Short.MAX_VALUE)
            .addGroup(jPanelFansLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelFansLayout.createSequentialGroup()
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelFansLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelFansLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addComponent(sampleLoggerExtFan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(sampleLoggerAirCircFan, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sampleLoggerIntFan, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap()))
        );
        jPanelFansLayout.setVerticalGroup(
            jPanelFansLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
            .addGroup(jPanelFansLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanelFansLayout.createSequentialGroup()
                    .addGap(8, 8, 8)
                    .addComponent(jLabel13)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(sampleLoggerExtFan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(sampleLoggerIntFan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(sampleLoggerAirCircFan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(10, Short.MAX_VALUE)))
        );

        jPanelPID.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        javax.swing.GroupLayout jPanelPIDLayout = new javax.swing.GroupLayout(jPanelPID);
        jPanelPID.setLayout(jPanelPIDLayout);
        jPanelPIDLayout.setHorizontalGroup(
            jPanelPIDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelPIDLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sampleLoggerPID_C, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanelPIDLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sampleLoggerPID_H, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelPIDLayout.setVerticalGroup(
            jPanelPIDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPIDLayout.createSequentialGroup()
                .addComponent(sampleLoggerPID_C, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerPID_H, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelPeltier.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        javax.swing.GroupLayout jPanelPeltierLayout = new javax.swing.GroupLayout(jPanelPeltier);
        jPanelPeltier.setLayout(jPanelPeltierLayout);
        jPanelPeltierLayout.setHorizontalGroup(
            jPanelPeltierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPeltierLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPeltierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLoggerPLT_C, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerPLT_V, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelPeltierLayout.setVerticalGroup(
            jPanelPeltierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPeltierLayout.createSequentialGroup()
                .addComponent(sampleLoggerPLT_C, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerPLT_V, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelTemperatures.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        javax.swing.GroupLayout jPanelTemperaturesLayout = new javax.swing.GroupLayout(jPanelTemperatures);
        jPanelTemperatures.setLayout(jPanelTemperaturesLayout);
        jPanelTemperaturesLayout.setHorizontalGroup(
            jPanelTemperaturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTemperaturesLayout.createSequentialGroup()
                .addGroup(jPanelTemperaturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLoggerT_uC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerT_Int_Chamber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerT_Int_Heatsink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerT_Ext_Heatsink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 12, Short.MAX_VALUE))
        );
        jPanelTemperaturesLayout.setVerticalGroup(
            jPanelTemperaturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTemperaturesLayout.createSequentialGroup()
                .addComponent(sampleLoggerT_Int_Chamber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerT_uC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerT_Int_Heatsink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerT_Ext_Heatsink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanelSHTLayout = new javax.swing.GroupLayout(jPanelSHT);
        jPanelSHT.setLayout(jPanelSHTLayout);
        jPanelSHTLayout.setHorizontalGroup(
            jPanelSHTLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSHTLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSHTLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSHTLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanelSHTLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleLoggerSHT31E_T, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLoggerSHT31I_H, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLoggerSHT31E_H, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(sampleLoggerSHT31I_T, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanelSHTLayout.setVerticalGroup(
            jPanelSHTLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelSHTLayout.createSequentialGroup()
                .addComponent(sampleLoggerSHT31I_T, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerSHT31I_H, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerSHT31E_T, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerSHT31E_H, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout jPanelK96ExtendedLayout = new javax.swing.GroupLayout(jPanelK96Extended);
        jPanelK96Extended.setLayout(jPanelK96ExtendedLayout);
        jPanelK96ExtendedLayout.setHorizontalGroup(
            jPanelK96ExtendedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelK96ExtendedLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelK96ExtendedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelK96ExtendedLayout.createSequentialGroup()
                        .addComponent(sampleLoggerK96_SPL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleLoggerK96_NTC0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleLoggerK96_RH0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelK96ExtendedLayout.createSequentialGroup()
                        .addComponent(sampleLoggerK96_MPL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleLoggerK96_NTC1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleLoggerK96_T_RH0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelK96ExtendedLayout.createSequentialGroup()
                        .addComponent(sampleLoggerK96_Press0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleLoggerK96_uCDie, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleLoggerK96_Error, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelK96ExtendedLayout.setVerticalGroup(
            jPanelK96ExtendedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelK96ExtendedLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelK96ExtendedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelK96ExtendedLayout.createSequentialGroup()
                        .addGroup(jPanelK96ExtendedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleLoggerK96_SPL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLoggerK96_NTC0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLoggerK96_RH0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelK96ExtendedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleLoggerK96_MPL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleLoggerK96_T_RH0, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(sampleLoggerK96_NTC1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelK96ExtendedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLoggerK96_Press0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerK96_uCDie, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerK96_Error, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelBoardSerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(sampleLoggerK96, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sampleLoggerD300, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addComponent(jPanelSHT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sampleLoggerVIN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jPanelK96Extended, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(jPanelPeltier, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanelPID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanelFans, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelTemperatures, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jPanelSHT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabelBoardSerialNumber))
                        .addGap(7, 7, 7)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(sampleLoggerK96, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(sampleLoggerD300, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelK96Extended, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(sampleLoggerVIN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanelPID, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelFans, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelTemperatures, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelPeltier, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jCBBoardIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBBoardIdActionPerformed
        onBoardIDChanged();
    }//GEN-LAST:event_jCBBoardIdActionPerformed


    private void onBoardIDChanged() {
        int selected = jCBBoardId.getSelectedIndex();
        if (selected == 0) {
            boardEnabled = false;
            selectedBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
        } else {
            boardEnabled = true;
            selectedBoardId = selected - 1;
        }
        
        // Update loggers
        for (SampleLogger sampleLogger:sampleLoggerPanels) {
            sampleLogger.setBoardId(selectedBoardId);
        }
        
        // Update setup dialogs
        for (SensorSetupDialog setupDialog:sensorSetupDialogs) {
            setupDialog.setBoardId(selectedBoardId);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jCBBoardId;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabelBoardSerialNumber;
    private javax.swing.JPanel jPanelFans;
    private javax.swing.JPanel jPanelK96Extended;
    private javax.swing.JPanel jPanelPID;
    private javax.swing.JPanel jPanelPeltier;
    private javax.swing.JPanel jPanelSHT;
    private javax.swing.JPanel jPanelTemperatures;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerAirCircFan;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLoggerD300;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerExtFan;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerIntFan;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLoggerK96;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerK96_Error;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerK96_MPL;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerK96_NTC0;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerK96_NTC1;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerK96_Press0;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerK96_RH0;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerK96_SPL;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerK96_T_RH0;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerK96_uCDie;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLoggerPID_C;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerPID_H;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLoggerPLT_C;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerPLT_V;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerSHT31E_H;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLoggerSHT31E_T;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerSHT31I_H;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanel sampleLoggerSHT31I_T;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerT_Ext_Heatsink;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerT_Int_Chamber;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerT_Int_Heatsink;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerT_uC;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerVIN;
    // End of variables declaration//GEN-END:variables

}
