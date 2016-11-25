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

package airsenseur.dev.chemsensorhost;

import airsenseur.dev.chemsensorhost.comm.ChemSensorHostCommHandler;
import airsenseur.dev.chemsensorhost.comm.ChemSensorHostCommHandler.ChemSensorMessageHandler;
import airsenseur.dev.comm.CommProtocolHelper;
import airsenseur.dev.comm.CommProtocolHelper.DataMessage;
import airsenseur.dev.exceptions.ChemSensorBoardException;
import airsenseur.dev.helpers.TaskScheduler;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class ChemSensorHost extends TaskScheduler implements ChemSensorMessageHandler {
    
    private final ChemSensorHostCommHandler boardHandler = new ChemSensorHostCommHandler(this);
    private final CommProtocolHelper protocolHelper = CommProtocolHelper.instance();
    
    private final Logger log = LoggerFactory.getLogger(ChemSensorHost.class);
    
    public static class SensorData {
        private int value;
        private int timeStamp;
        
        public void load(int value, int timestamp) {
            if (this.timeStamp != timestamp) {
                this.timeStamp = timestamp;
                this.value = value;
            }
        }

        public int getValue() {
            return value;
        }

        public int getTimeStamp() {
            return timeStamp;
        }
    };
    
    public static class CollectedData {
        
        private final List<SensorData> sensors = new ArrayList<>();
        private int freeMemory;
        
        public void reset(int numSensors) {
            sensors.clear();
            for (int n = 0; n < numSensors; n++) {
                sensors.add(new SensorData());
            }
            
            freeMemory = 0;
        }

        public List<SensorData> getSensors() {
            return sensors;
        }

        public void setFreeMemory(int freeMemory) {
            this.freeMemory = freeMemory;
        }

        public int getFreeMemory() {
            return freeMemory;
        }        
    };
    
    private final CollectedData collectedData = new CollectedData();
    
    public boolean start(int pollMs, int numSensors) {
        
        try {
            boardHandler.connectToBoard(Configuration.getConfig().serialPort());
            
        } catch (ChemSensorBoardException ex) {
            log.error(ex.getErrorMessage());
            return false;
        }
        
        // Initialize the collected data
        collectedData.reset(numSensors);
        
        startPeriodic(pollMs);
        
        return true;
    }
        
    public void exit() {
        
        stop();
        boardHandler.disConnectFromBoard();
    }

    @Override
    public void taskMain() {
        
        // Ask for free memory
        protocolHelper.renderGetFreeMemory();
        
        // Ask for samples
        for (int n = 0; n < collectedData.getSensors().size(); n++) {
            protocolHelper.renderGetLastSample(n);
        }
        
        // Schedule a buffer flush
        boardHandler.writeBufferToBoard();
    }
    
    @Override
    public void onNewPacketReady() {
        
        // Evaluate the incoming data
        DataMessage rxData = protocolHelper.getNextRxDataMessage();
        
        // FreeMemory
        Integer freeMemory = protocolHelper.evalFreeMemory(rxData);
        if (freeMemory != null) {
            collectedData.setFreeMemory(freeMemory);
            return;
        }
        
        // Last data sample and timestamp
        int channel = 0;
        for (SensorData sensorData:collectedData.getSensors()) {
            
            List<Integer> result = protocolHelper.evalLastSampleInquiry(rxData, channel);
            if ((result != null) && (result.size() == 2)) {
                
                int sample = result.get(0);
                int timeStamp = result.get(1);                
                sensorData.load(sample, timeStamp);
                
                return;
            }
            
            channel++;
        }
        
    }

    public CollectedData getCollectedData() {
        return collectedData;
    }
    
    public void startSampling() {
        protocolHelper.renderStartSample();
    }
    
    public void stopSampling() {
        protocolHelper.renderStopSample();
    }
        
    public void sendRawData(String commandString, String commandComment) {
        
        if ((commandString == null) || (commandString.isEmpty()) || (commandComment == null)) {
            
            // Discard this item
            return;
        }
        
        DataMessage dataMessage = new DataMessage(commandString, commandComment);
        protocolHelper.renderRawData(dataMessage);
        log.debug("RawDataMessageReceived: [" + commandComment + "] (" + commandString + ")");

        // Schedule a buffer flush
        boardHandler.writeBufferToBoard();
    }
}
