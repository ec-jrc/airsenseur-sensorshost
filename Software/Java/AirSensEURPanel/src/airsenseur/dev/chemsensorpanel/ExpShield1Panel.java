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
import airsenseur.dev.chemsensorpanel.setupdialogs.GenericBoardInfoDialog;
import airsenseur.dev.chemsensorpanel.setupdialogs.GenericSensorSetupDIalog;
import airsenseur.dev.chemsensorpanel.setupdialogs.OPCN3SensorSetupDIalog;
import airsenseur.dev.chemsensorpanel.setupdialogs.PMS5003SensorSetupDIalog;
import airsenseur.dev.chemsensorpanel.setupdialogs.SPS30SensorSetupDIalog;
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
public class ExpShield1Panel extends GenericTabPanel {
    
    public final static int RD200M_CHANNEL = 0;
    public final static int RD200M_NUM_OF_CHANNELS = 1;
    
    public final static int D300_CHANNEL = RD200M_CHANNEL + RD200M_NUM_OF_CHANNELS;
    public final static int D300_NUM_OF_CHANNELS = 1;    
    
    public final static int PMS5003_CHANNEL = D300_CHANNEL + D300_NUM_OF_CHANNELS;
    public final static int PMS5003_NUM_OF_CHANNELS = 12;
    
    public final static int OPCN3_CHANNEL = PMS5003_CHANNEL + PMS5003_NUM_OF_CHANNELS;
    public final static int OPCN3_NUM_OF_CHANNELS = 33;
    
    public final static int SPS30_CHANNEL = OPCN3_CHANNEL + OPCN3_NUM_OF_CHANNELS;
    public final static int SPS30_NUM_OF_CHANNELS = 10;
    
    public final static int PMS5003_HISTOGRAM_BINS_NUM = 6;
    public final static int PMS5003_HISTOGRAM_PMAT_NUM = 3;
    public final static int PMS5003_TOTAL_PMAT_NUM = 6;
    public final static int PMS5003_HISTOGRAM_BIN0_CHANNEL = PMS5003_CHANNEL + 6;
    public final static int PMS5003_HISTOGRAM_PMAT_CHANNEL = PMS5003_CHANNEL + 3;
    
    public final static int OPCN3_HISTOGRAM_BINS_NUM = 24;
    public final static int OPCN3_HISTOGRAM_PM_NUM = 3;
    public final static int OPCN3_HISTOGRAM_BIN0_CHANNEL = OPCN3_CHANNEL;
    public final static int OPCN3_HISTOGRAM_PM_CHANNEL = OPCN3_HISTOGRAM_BIN0_CHANNEL + OPCN3_HISTOGRAM_BINS_NUM;
    public final static int OPCN3_HISTOGRAM_TEMP_CHANNEL = OPCN3_HISTOGRAM_PM_CHANNEL + OPCN3_HISTOGRAM_PM_NUM;
    public final static int OPCN3_HISTOGRAM_HUM_CHANNEL = OPCN3_HISTOGRAM_TEMP_CHANNEL + 1;
    public final static int OPCN3_HISTOGRAM_VOLUME_CHANNEL = OPCN3_HISTOGRAM_HUM_CHANNEL + 1;
    public final static int OPCN3_HISTOGRAM_SAMPLINGTIME_CHANNEL = OPCN3_HISTOGRAM_VOLUME_CHANNEL + 1;
    public final static int OPCN3_HISTOGRAM_SAMPLINGFLOWRATE_CHANNEL = OPCN3_HISTOGRAM_SAMPLINGTIME_CHANNEL + 1;
    public final static int OPCN3_HISTOGRAM_LASERPOT = OPCN3_HISTOGRAM_SAMPLINGFLOWRATE_CHANNEL + 1;
    public final static int OPCN3_DEBUG_CHANNELNUM = OPCN3_HISTOGRAM_LASERPOT - OPCN3_HISTOGRAM_VOLUME_CHANNEL + 1;
    
    public final static int SPS30_HISTOGRAM_BINS_NUM = 5;
    public final static int SPS30_HISTOGRAM_PM_NUM  = 4;
    public final static int SPS30_HISTOGRAM_BIN0_CHANNEL = SPS30_CHANNEL + SPS30_HISTOGRAM_PM_NUM;
    public final static int SPS30_HISTOGRAM_PM_CHANNEL = SPS30_CHANNEL;
    public final static int SPS30_TYPSIZE_CHANNEL = SPS30_CHANNEL + SPS30_NUM_OF_CHANNELS - 1;
        
    public final static int EXPSHIELD1_NUM_OF_CHANNELS = SPS30_CHANNEL + SPS30_NUM_OF_CHANNELS;

    public final static int EXPSHIELD1_SETUP_DIALOG_RD200M = 0;
    public final static int EXPSHIELD1_SETUP_DIALOG_D300 = 1;
    public final static int EXPSHIELD1_SETUP_DIALOG_PMS5003 = 2;
    public final static int EXPSHIELD1_SETUP_DIALOG_OPCN3 = 3;
    public final static int EXPSHIELD1_SETUP_DIALOG_SPS30 = 4;
    public final static int EXPSHIELD1_SETUP_GENERIC_INFO = 5;
    
    private int selectedBoardId = AppDataMessage.BOARD_ID_UNDEFINED;
    private boolean boardEnabled = false;
    
    private final static String DEFAULT_CHANNEL_MATH_EXPRESSION =  "x";
    private final static String RD200_CHANNEL_MATH_EXPRESSION = "x/100";
    private final static String OPCN3_BINS_MATH_EXPRESSION = "x/32";
    private final static String OPCN3_PM_MATH_EXPRESSION = "x/32";
    private final static String OPCN3_TEMP_MATH_EXPRESSION = "x/1000";    
    private final static String OPCN3_HUM_MATH_EXPRESSION = "x/1000";    
    private final static String OPCN3_VOL_MATH_EXPRESSION = "x/64";
    private final static String SPS30_PM_MATH_EXPRESSION = "x/32";
    private final static String SPS30_BINS_MATH_EXPRESSION = "x/32";
    private final static String SPS30_TYPSIZE_MATH_EXPRESSION = "x/65536";

    // The chemical sensors setup panels
    private final List<SensorSetupDialog> sensorSetupDialogs = new ArrayList<>();
    
    private final List<SampleLogger> sampleLoggerPanels = new ArrayList<>();
    
    private String boardSerialNumber = "";
    
    // This is used only by the graphical composition tool in the Netbeans IDE
    public ExpShield1Panel() {
        initComponents();
    }

    /**
     * Creates new form ChemSensorPanel
     * @param parent
     * @param shieldProtocolLayer
     * @param logger
     */
    public ExpShield1Panel(MainApplicationFrame parent, ShieldProtocolLayer shieldProtocolLayer, FileLogger logger) {
        super(shieldProtocolLayer, logger);
        
        // Generate the sensor setup dialogs
        sensorSetupDialogs.add(new GenericSensorSetupDIalog("RD200M", RD200M_CHANNEL, 1, false, true, true, parent, false));
        sensorSetupDialogs.add(new GenericSensorSetupDIalog("D300", D300_CHANNEL, 1, false, true, true, parent, false));
        sensorSetupDialogs.add(new PMS5003SensorSetupDIalog(PMS5003_CHANNEL, PMS5003_TOTAL_PMAT_NUM, PMS5003_HISTOGRAM_BIN0_CHANNEL, PMS5003_HISTOGRAM_BINS_NUM, parent, false));
        sensorSetupDialogs.add(new OPCN3SensorSetupDIalog(OPCN3_HISTOGRAM_PM_CHANNEL, OPCN3_HISTOGRAM_PM_NUM, OPCN3_HISTOGRAM_BIN0_CHANNEL, OPCN3_HISTOGRAM_BINS_NUM, OPCN3_HISTOGRAM_TEMP_CHANNEL, OPCN3_HISTOGRAM_VOLUME_CHANNEL, OPCN3_DEBUG_CHANNELNUM, parent, false));
        sensorSetupDialogs.add(new SPS30SensorSetupDIalog(SPS30_CHANNEL, SPS30_HISTOGRAM_PM_NUM, SPS30_HISTOGRAM_BIN0_CHANNEL, SPS30_HISTOGRAM_BINS_NUM, SPS30_TYPSIZE_CHANNEL, parent, false));
        sensorSetupDialogs.add(new GenericBoardInfoDialog(parent, false, "ExpShield1 Generic Info"));
        
        initComponents();
        
        // Aggregate sample loggers so it's more easy to handle them
        sampleLoggerPanels.add(sampleLoggerPMSBins);
        sampleLoggerPanels.add(sampleLoggerPMSPM);
        sampleLoggerPanels.add(sampleLoggerOPCBins);
        sampleLoggerPanels.add(sampleLoggerOPCPM);
        sampleLoggerPanels.add(sampleLoggerOPCVol);
        sampleLoggerPanels.add(sampleLoggerOPCTemp);
        sampleLoggerPanels.add(sampleLoggerOPCHum);
        sampleLoggerPanels.add(sampleLoggerOPCFlowRate);
        sampleLoggerPanels.add(sampleLoggerOPCTSample);
        sampleLoggerPanels.add(sampleLoggerRD200);
        sampleLoggerPanels.add(sampleLoggerD300);
        sampleLoggerPanels.add(sampleLoggerSPS30Bins);
        sampleLoggerPanels.add(sampleLoggerSPS30PM);
        sampleLoggerPanels.add(sampleLoggerSPS30PSize);
        
        // Initialize all loggers with common properties
        sampleLoggerPMSBins.setLoggerProperties("Bins [#/100ml]", 0, 5, PMS5003_HISTOGRAM_BINS_NUM);
        sampleLoggerPMSBins.setSensorId(PMS5003_HISTOGRAM_BIN0_CHANNEL);
        
        sampleLoggerPMSPM.setLoggerProperties("PM1, 2.5, 10 [ug/m3]", 0, 5, PMS5003_HISTOGRAM_PMAT_NUM);
        sampleLoggerPMSPM.setSensorId(PMS5003_HISTOGRAM_PMAT_CHANNEL);
        
        sampleLoggerRD200.setLoggerProperties("RD200 [pCi/L] x 100", 0, -1, 10);
        sampleLoggerRD200.setSensorId(RD200M_CHANNEL);
        
        sampleLoggerD300.setLoggerProperties("D300 [ppm]", 0, -1, 10);
        sampleLoggerD300.setSensorId(D300_CHANNEL);
        
        sampleLoggerOPCBins.setLoggerProperties("Bins [#/ml] x 1000", 0, 5, OPCN3_HISTOGRAM_BINS_NUM);
        sampleLoggerOPCBins.setSensorId(OPCN3_HISTOGRAM_BIN0_CHANNEL);
        sampleLoggerOPCBins.setDataProcessing(new SampleLogger.DataProcessing() {

            @Override
            public double processSample(double sample) {
                return (sample / 32.0f) * 1000.0f;
            }
        });
        
        sampleLoggerOPCPM.setLoggerProperties("PM1, 2.5, 10 [ug/m3]", 0, 5, OPCN3_HISTOGRAM_PM_NUM);
        sampleLoggerOPCPM.setSensorId(OPCN3_HISTOGRAM_PM_CHANNEL);
        sampleLoggerOPCPM.setDataProcessing(new SampleLogger.DataProcessing() {

            @Override
            public double processSample(double sample) {
                return (sample / 32.0f);
            }
        });
        
        sampleLoggerOPCTemp.setLoggerProperties("Temp", 0, 5, 0);
        sampleLoggerOPCTemp.setSensorId(OPCN3_HISTOGRAM_TEMP_CHANNEL);
        sampleLoggerOPCTemp.setDataProcessing(new SampleLogger.DataProcessing() {

            @Override
            public double processSample(double sample) {
                return (sample / 1000.0f);
            }
        });
        
        sampleLoggerOPCHum.setLoggerProperties("Hum", 0, 5, 0);
        sampleLoggerOPCHum.setSensorId(OPCN3_HISTOGRAM_HUM_CHANNEL);
        sampleLoggerOPCHum.setDataProcessing(new SampleLogger.DataProcessing() {

            @Override
            public double processSample(double sample) {
                return (sample / 1000.0f);
            }
        });
        
        sampleLoggerOPCVol.setLoggerProperties("Vol", 0, 5, 0);
        sampleLoggerOPCVol.setSensorId(OPCN3_HISTOGRAM_VOLUME_CHANNEL);
        sampleLoggerOPCVol.setDataProcessing(new SampleLogger.DataProcessing() {

            @Override
            public double processSample(double sample) {
                return (sample / 64.0f);
            }
        });
        
        sampleLoggerOPCTSample.setLoggerProperties("TSampling", 0, 5, 0);
        sampleLoggerOPCTSample.setSensorId(OPCN3_HISTOGRAM_SAMPLINGTIME_CHANNEL);
        
        sampleLoggerOPCFlowRate.setLoggerProperties("FlowRate", 0, 5, 0);
        sampleLoggerOPCFlowRate.setSensorId(OPCN3_HISTOGRAM_SAMPLINGFLOWRATE_CHANNEL);
        

        sampleLoggerSPS30Bins.setLoggerProperties("Bins [#/ml] x 1000", 0, 5, SPS30_HISTOGRAM_BINS_NUM);
        sampleLoggerSPS30Bins.setSensorId(SPS30_HISTOGRAM_BIN0_CHANNEL);
        sampleLoggerSPS30Bins.setDataProcessing(new SampleLogger.DataProcessing() {

            @Override
            public double processSample(double sample) {
                return (sample / 32.0f) * 1000.0f;
            }
        });
        
        sampleLoggerSPS30PM.setLoggerProperties("PM1, 2.5, 4, 10 [ug/m3]", 0, 5, SPS30_HISTOGRAM_PM_NUM);
        sampleLoggerSPS30PM.setSensorId(SPS30_HISTOGRAM_PM_CHANNEL);
        sampleLoggerSPS30PM.setDataProcessing(new SampleLogger.DataProcessing() {

            @Override
            public double processSample(double sample) {
                return (sample / 128.0f);
            }
        });
        
        sampleLoggerSPS30PSize.setLoggerProperties("P.Size", 0, 5, 0);
        sampleLoggerSPS30PSize.setSensorId(SPS30_TYPSIZE_CHANNEL);
        sampleLoggerSPS30PSize.setDataProcessing(new SampleLogger.DataProcessing() {

            @Override
            public double processSample(double sample) {
                return (sample / 32768.0f);
            }
        });
         
        
        for (int n = 0; n < sampleLoggerPanels.size(); n++) {
            sampleLoggerPanels.get(n).setLogger(logger);
            sampleLoggerPanels.get(n).setBoardId(selectedBoardId);
            sampleLoggerPanels.get(n).setShieldProtocolLayer(shieldProtocolLayer);            
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
        
        // Get the PMS5003, OPC-N3 and SPS30 serials
        shieldProtocolLayer.renderReadSensorSerialNumber(selectedBoardId, PMS5003_HISTOGRAM_BIN0_CHANNEL);
        shieldProtocolLayer.renderReadSensorSerialNumber(selectedBoardId, OPCN3_HISTOGRAM_BIN0_CHANNEL);
        shieldProtocolLayer.renderReadSensorSerialNumber(selectedBoardId, SPS30_HISTOGRAM_PM_CHANNEL);
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
        
        String pms5003Serial = shieldProtocolLayer.evalReadSensorSerialNumber(rxMessage, selectedBoardId, PMS5003_HISTOGRAM_BIN0_CHANNEL);
        if (pms5003Serial != null) {
            jLabelPMS5003SerialNumber.setText(pms5003Serial);
        }
        
        String opcN3Serial = shieldProtocolLayer.evalReadSensorSerialNumber(rxMessage, selectedBoardId, OPCN3_HISTOGRAM_BIN0_CHANNEL);
        if (opcN3Serial != null) {
            jLabelOPCN3SerialNumber.setText(opcN3Serial);
        }
        
        String sps30Serial = shieldProtocolLayer.evalReadSensorSerialNumber(rxMessage, selectedBoardId, SPS30_HISTOGRAM_PM_CHANNEL);
        if (sps30Serial != null) {
            jLabelSPS30SerialNumber.setText(sps30Serial);
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
        
        // All channels in the OPCN2 Shields have the same behaviour. 
        // Channel name is read from the shield at startup
        for (int n = 0; n <= EXPSHIELD1_NUM_OF_CHANNELS; n++) {
            HostConfigSensorProperties sensorProperties = hostConfigWriter.addNewSensor();
            sensorProperties.setSensorBoardId(selectedBoardId);
            sensorProperties.setSensorChannel(n);
            
            if (n == RD200M_CHANNEL) {
                sensorProperties.setSensorExpression(RD200_CHANNEL_MATH_EXPRESSION);
            } else if ((n >= OPCN3_HISTOGRAM_BIN0_CHANNEL) && (n < OPCN3_HISTOGRAM_PM_CHANNEL) ) {
                sensorProperties.setSensorExpression(OPCN3_BINS_MATH_EXPRESSION);
            } else if ((n >= OPCN3_HISTOGRAM_PM_CHANNEL) && (n < OPCN3_HISTOGRAM_TEMP_CHANNEL) ) {
                sensorProperties.setSensorExpression(OPCN3_PM_MATH_EXPRESSION);
            } else if (n == OPCN3_HISTOGRAM_TEMP_CHANNEL) {
                sensorProperties.setSensorExpression(OPCN3_TEMP_MATH_EXPRESSION);
            } else if (n == OPCN3_HISTOGRAM_HUM_CHANNEL) {
                sensorProperties.setSensorExpression(OPCN3_HUM_MATH_EXPRESSION);
            } else if (n == OPCN3_HISTOGRAM_VOLUME_CHANNEL) {
                sensorProperties.setSensorExpression(OPCN3_VOL_MATH_EXPRESSION);
            } else if ((n >= SPS30_HISTOGRAM_PM_CHANNEL) && (n < SPS30_HISTOGRAM_BIN0_CHANNEL)) {
                sensorProperties.setSensorExpression(SPS30_PM_MATH_EXPRESSION);
            } else if ((n >= SPS30_HISTOGRAM_BIN0_CHANNEL) && (n < SPS30_TYPSIZE_CHANNEL)) {
                sensorProperties.setSensorExpression(SPS30_BINS_MATH_EXPRESSION);
            } else if (n == SPS30_TYPSIZE_CHANNEL) {
                sensorProperties.setSensorExpression(SPS30_TYPSIZE_MATH_EXPRESSION);
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
        sampleLoggerRD200 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        sampleLoggerD300 = new airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        sampleLoggerPMSBins = new airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel();
        sampleLoggerPMSPM = new airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabelPMS5003SerialNumber = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        sampleLoggerOPCBins = new airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel();
        sampleLoggerOPCPM = new airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel();
        sampleLoggerOPCVol = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerOPCTemp = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerOPCHum = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerOPCTSample = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        sampleLoggerOPCFlowRate = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        jLabel5 = new javax.swing.JLabel();
        jLabelOPCN3SerialNumber = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        sampleLoggerSPS30Bins = new airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel();
        sampleLoggerSPS30PM = new airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel();
        sampleLoggerSPS30PSize = new airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite();
        jLabel4 = new javax.swing.JLabel();
        jLabelSPS30SerialNumber = new javax.swing.JLabel();
        jLabelBoardSerialNumber = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(858, 547));

        jCBBoardId.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Not Connected", "ID 0", "ID 1", "ID 2", "ID 3", "ID 4", "ID 5", "ID 6", "ID 7", "ID 8", "ID 9", "ID 10", "ID 11", "ID 12", "ID 13", "ID 14" }));
        jCBBoardId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBBoardIdActionPerformed(evt);
            }
        });

        jLabel2.setText("Board ID:");

        jLabel1.setText("Serial:");

        jLabelPMS5003SerialNumber.setText("--");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sampleLoggerPMSBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerPMSPM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelPMS5003SerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLoggerPMSBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerPMSPM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 12, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabelPMS5003SerialNumber))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("PMS5003", jPanel1);

        jLabel5.setText("Serial:");

        jLabelOPCN3SerialNumber.setText("--");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sampleLoggerOPCBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerOPCPM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLoggerOPCVol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerOPCTemp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerOPCHum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerOPCTSample, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerOPCFlowRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabelOPCN3SerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLoggerOPCBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerOPCPM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 12, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabelOPCN3SerialNumber))
                .addGap(20, 20, 20)
                .addComponent(sampleLoggerOPCVol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerOPCTemp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerOPCHum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerOPCTSample, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerOPCFlowRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("OPC-N3", jPanel2);

        jLabel4.setText("Serial:");

        jLabelSPS30SerialNumber.setText("--");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sampleLoggerSPS30Bins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sampleLoggerSPS30PM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabelSPS30SerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(sampleLoggerSPS30PSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleLoggerSPS30Bins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleLoggerSPS30PM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 12, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabelSPS30SerialNumber))
                .addGap(18, 18, 18)
                .addComponent(sampleLoggerSPS30PSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("SPS30", jPanel3);

        jLabelBoardSerialNumber.setText("--");

        jLabel3.setText("Board serial number:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(sampleLoggerRD200, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sampleLoggerD300, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelBoardSerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(jLabelBoardSerialNumber))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jCBBoardId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sampleLoggerD300, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sampleLoggerRD200, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
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
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabelBoardSerialNumber;
    private javax.swing.JLabel jLabelOPCN3SerialNumber;
    private javax.swing.JLabel jLabelPMS5003SerialNumber;
    private javax.swing.JLabel jLabelSPS30SerialNumber;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLoggerD300;
    private airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel sampleLoggerOPCBins;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerOPCFlowRate;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerOPCHum;
    private airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel sampleLoggerOPCPM;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerOPCTSample;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerOPCTemp;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerOPCVol;
    private airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel sampleLoggerPMSBins;
    private airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel sampleLoggerPMSPM;
    private airsenseur.dev.chemsensorpanel.widgets.LineGraphSampleLoggerPanel sampleLoggerRD200;
    private airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel sampleLoggerSPS30Bins;
    private airsenseur.dev.chemsensorpanel.widgets.HistogramGraphSampleLoggerPanel sampleLoggerSPS30PM;
    private airsenseur.dev.chemsensorpanel.widgets.TextBasedSampleLoggerPanelLite sampleLoggerSPS30PSize;
    // End of variables declaration//GEN-END:variables

}
