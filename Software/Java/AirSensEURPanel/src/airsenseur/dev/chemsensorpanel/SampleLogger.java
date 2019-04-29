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
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.comm.AppDataMessage;
import java.util.List;

/**
 *
 * @author marco
 */
public abstract class SampleLogger extends javax.swing.JPanel {
    
    private String name = "NA";
    private String serial = "NA";

    /**
     * @return the serial
     */
    public String getSerial() {
        return serial;
    }
    
    // This interface could be used to apply a specific post-processing
    // on each incoming sample
    public interface DataProcessing {
        public double processSample(double sample);
    }
    
    // This is a simple signed to unsigned routine conversion
    // that could be applied to the chemical sensors A/D converter results
    public final static DataProcessing unsignedConvertion = new DataProcessing() {

        @Override
        public double processSample(double sample) {
            
            // Unsigned. conversion
            if (sample > 32767) {
                sample = sample - 32768;
            } else {
                sample = sample + 32768;
            }
            
            return sample;
        }
    };

    // Other useful standard data processing
    public final static SampleLogger.DataProcessing ur100CDTemperatureDataProcessing = new DataProcessing() {
        @Override
        public double processSample(double sample) {
            return ((sample/16384*165) - 40.0);
        }
    };
            
    public final static SampleLogger.DataProcessing ur100CDHumidityDataProcessing = new DataProcessing() {
        @Override
        public double processSample(double sample) {
            return (sample/16384 * 100.0);
        }
    };
            
    public final static SampleLogger.DataProcessing sht31TemperatureDataProcessing = new DataProcessing() {
        @Override
        public double processSample(double sample) {
            return ((sample/65535*175) - 45.0);
        }
    };
              
    public final static SampleLogger.DataProcessing sht31HumidityDataProcessing = new DataProcessing() {
        @Override
        public double processSample(double sample) {
            return (sample/65535 * 100.0);
        }
    };
    
    public final static SampleLogger.DataProcessing highResSampleBaseDefaultDataProcessing = new DataProcessing() {

        @Override
        public double processSample(double sample) {
            return sample / 10000;
        }
    };

    protected long lastSampleTimeStamp = 0;
    protected int boardId = AppDataMessage.BOARD_ID_UNDEFINED;
    protected int sensorId = 0;  
    protected DataProcessing dataProcessing = null;
    protected FileLogger fileLogger = null;
    protected ShieldProtocolLayer shieldProtocolLayer = null;
    protected boolean highResEnabled = false;
    
    public void setBoardId(int boardId) {
        this.boardId = boardId;
    }
    
    public void setSensorId(int sensorId) {
        this.sensorId = sensorId;
    }
    
    public void setLogger(FileLogger logger) {
        this.fileLogger = logger;
    }
    
    public void setShieldProtocolLayer(ShieldProtocolLayer shieldProtocolLayer) {
        this.shieldProtocolLayer = shieldProtocolLayer;
    }
    
    public abstract void setLoggerProperties(String title, int minVal, int maxVal, int historyLength);
    
    public void setDataProcessing(DataProcessing dataProcessing) {
        this.dataProcessing = dataProcessing;
    }
    
    public void setHighResolutionMode() {
        highResEnabled = true;
    }
    
    public void readFromBoard() {
        
        if (shieldProtocolLayer != null) {
            if (highResEnabled) {
                shieldProtocolLayer.renderGetLastSampleHRes(boardId, sensorId);
            } else {
                shieldProtocolLayer.renderGetLastSample(boardId, sensorId);
            }
        }
    }
    
    public void evaluateRxMessage(AppDataMessage rxMessage) {

        if (shieldProtocolLayer != null) {
            
            // Setup name
            String setupName = shieldProtocolLayer.evalSensorInquiry(rxMessage, boardId, sensorId);
            if ((setupName != null) && !setupName.isEmpty()) {
                name = setupName;
            }
            
            // Setup serial
            String setupSerial = shieldProtocolLayer.evalReadSensorSerialNumber(rxMessage, boardId, sensorId);
            if ((setupSerial != null) && !setupSerial.isEmpty()) {
                serial = setupSerial;
            }
            
            List<Integer> resultList = shieldProtocolLayer.evalLastSampleInquiry(rxMessage, boardId, sensorId);
            if ((resultList == null) && highResEnabled) {
                resultList = shieldProtocolLayer.evalLastSampleHResInquiry(rxMessage, boardId, sensorId);
            }
            if (resultList != null) {

                int sample = resultList.get(0);
                int timestamp = resultList.get(1);

                if (timestamp != lastSampleTimeStamp) {
                    lastSampleTimeStamp = timestamp;

                    // Process the sample
                    double processed = onNewSample(sample, timestamp);

                    // Log the sample to the board
                    if (fileLogger != null) {
                        fileLogger.appendSample(processed, name, getSerial(), boardId, sensorId, timestamp);
                    }
                }        
            }
        }
    }
    
    protected double onNewSample(double sample, int timestamp) {
        
        // Apply data processing if required
        if (dataProcessing != null) {
            sample = dataProcessing.processSample(sample);
        }
                
        return sample;
    }
}
