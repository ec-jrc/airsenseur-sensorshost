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

import airsenseur.dev.chemsensorhost.comm.SensorBusCommunicationHandler;
import airsenseur.dev.comm.CommChannelFactory;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.SensorBus.SensorBusMessageConsumer;
import airsenseur.dev.comm.TransportLogicFactory;
import airsenseur.dev.exceptions.SensorBusException;
import airsenseur.dev.helpers.TaskScheduler;
import airsenseur.dev.json.RawCommand;
import expr.Expr;
import expr.Parser;
import expr.SyntaxException;
import expr.Variable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class ChemSensorHost extends TaskScheduler implements SensorBusMessageConsumer {
    
    private final long OWNERSHIP_AUTOFALLBACK_TIMEOUT = 60000;
    
    private final SensorBusCommunicationHandler sensorBusHandler = new SensorBusCommunicationHandler();
    private final ShieldProtocolLayer protocolHelper = new ShieldProtocolLayer(sensorBusHandler);
    private final RawCommandQueue rawCommandQueue = new RawCommandQueue(protocolHelper);
    
    private final Logger log = LoggerFactory.getLogger(ChemSensorHost.class);
    
    public long ownershipTimestamp = 0;
    
    public static class SensorData {
        private int value;
        private int timeStamp;
        private double evalSampleVal;
        private String channelName = "";
        private String channelSerial = "";
        private String mathExpression = "";
        private int boardId = AppDataMessage.BOARD_ID_UNDEFINED;
        private int channel = 0;
        private boolean hiResMode = false;
        
        public SensorData() {
        }
        
        public SensorData(int boardId, int channel, String name, String serial, String mathExpression, boolean hiResMode) {
            this.channelName = name;
            this.channelSerial = serial;
            this.boardId = boardId;
            this.channel = channel;
            this.mathExpression = mathExpression;
            this.hiResMode = hiResMode;
        }
        
        public boolean load(int value, int timestamp) {
            if (this.timeStamp != timestamp) {
                this.timeStamp = timestamp;
                this.value = value;
                return true;
            }
            
            return false;
        }

        public int getValue() {
            return value;
        }

        public int getTimeStamp() {
            return timeStamp;
        }
        
        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }
        
        public String getChannelName() {
            return channelName;
        }
        
        public String getMathExpression() {
            return mathExpression;
        }
        
        public int getBoardId() {
            return boardId;
        }
        
        public int getChannel() {
            return channel;
        }

        public double getEvalSampleVal() {
            return evalSampleVal;
        }

        public void setEvalSampleVal(double evalSampleVal) {
            this.evalSampleVal = evalSampleVal;
        }
        
        public boolean getHiResMode() {
            return hiResMode;
        }

        /**
         * @return the channelSerial
         */
        public String getChannelSerial() {
            return channelSerial;
        }

        /**
         * @param channelSerial the channelSerial to set
         */
        public void setChannelSerial(String channelSerial) {
            this.channelSerial = channelSerial;
        }
    };
    
    public static class CollectedData {
        
        private final List<SensorData> sensors = new ArrayList<>();
        
        public void reset(int numSensors) {
            sensors.clear();
            for (int n = 0; n < numSensors; n++) {
                String channelName = Configuration.getConfig().getSensorNameForChannel(n);
                String serial = "";
                Integer boardId = Configuration.getConfig().getBoardIdForSensor(n);
                Integer channel = Configuration.getConfig().getChannelForSensor(n);
                String mathExpression = Configuration.getConfig().getMathExpressionForSensor(n);
                boolean hiResMode = Configuration.getConfig().getHiResSample(n);
                sensors.add(new SensorData(boardId, channel, channelName, serial, mathExpression, hiResMode));
            }
        }

        public List<SensorData> getSensors() {
            return sensors;
        }
        
        // FreeMemory is something is no more available as it's not an
        // important information to be collected (and it's generic and not associated to 
        // a unique node on the SensorBus.
        // FreeMemory is maintained as it's used by the DataAggregator to check for
        // Host process connection validity
        public int getFreeMemory() {
            return 0;
        }        
    };
    
    private final CollectedData collectedData = new CollectedData();
    
    public boolean start(int pollMs, int numSensors) {
        
        try {
            TransportLogicFactory.transportLogicType type = TransportLogicFactory.transportLogicType.POINT_TO_POINT;
            if (Configuration.getConfig().getUseBusProtocol()) {
                type = TransportLogicFactory.transportLogicType.POINT_TO_MULTIPOINT;
            }
            sensorBusHandler.init(this, CommChannelFactory.commChannelType.SERIAL, type);
            sensorBusHandler.connectToBus(Configuration.getConfig().serialPort());
            
        } catch (SensorBusException ex) {
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
        sensorBusHandler.disConnectFromBus();
    }

    // From TaskScheduler
    @Override
    public void taskMain() {
        
        // If someone else tooks ownership, does nothing
        if ((ownershipTimestamp != 0) && ((new Date().getTime() - ownershipTimestamp) < OWNERSHIP_AUTOFALLBACK_TIMEOUT)) {
            return;
        }
        
        // Ownership elapsed, reset the timestamp flag
        ownershipTimestamp = 0;
                
        // Ask for samples values and names (this last only if required)
        for (SensorData sensorData:collectedData.getSensors()) {
            
            if (sensorData.getChannelName().isEmpty()) {
                protocolHelper.renderSensorInquiry(sensorData.getBoardId(), sensorData.getChannel());
            }
            
            if (sensorData.getChannelSerial().isEmpty()) {
                protocolHelper.renderReadSensorSerialNumber(sensorData.boardId, sensorData.getChannel());
            }
            
            if (sensorData.getHiResMode()) {
                protocolHelper.renderGetLastSampleHRes(sensorData.getBoardId(), sensorData.getChannel());
            } else {
                protocolHelper.renderGetLastSample(sensorData.getBoardId(), sensorData.getChannel());
            }
        }
    }

    // From TaskScheduler
    @Override
    public String getTaskName() {
        return "AppMessagesProducer";
    }
    
    // From SensorBusMessageConsumer
    @Override
    public void onNewMessageReady(AppDataMessage dataMessage) {
        
        // Debug
        log.debug("R> [" + dataMessage.getBoardId() + "] (" + dataMessage.getCommandString() + ")" );
        
        // Answers generated by raw commands can be also evaluated for our internal purposes.
        // Avoid to return if something matches on rawCommandQueue.
        rawCommandQueue.onNewMessageReady(dataMessage);
        
        // Last data sample and timestamp
        for (SensorData sensorData:collectedData.getSensors()) {
            
            List<Integer> result = protocolHelper.evalLastSampleInquiry(dataMessage, sensorData.getBoardId(), sensorData.getChannel());
            if(result == null) {
                result = protocolHelper.evalLastSampleHResInquiry(dataMessage, sensorData.getBoardId(), sensorData.getChannel());
            }
            if ((result != null) && (result.size() == 2)) {
                
                int sample = result.get(0);
                int timeStamp = result.get(1);
                if (sensorData.load(sample, timeStamp)) {
                    
                    // Evaluate the mathematical expression
                    String mathExpression = sensorData.getMathExpression();
                    double evalSampleVal = evaluateMath(mathExpression, sample);
                    sensorData.setEvalSampleVal(evalSampleVal);
                }
                
                return;
            }
        }
        
        // Sensor names (does not overwrite if something has been set on the configuration file)
        for (SensorData sensorData:collectedData.getSensors()) {
            String sensorName = protocolHelper.evalSensorInquiry(dataMessage, sensorData.getBoardId(), sensorData.getChannel());
            if ((sensorName != null)) {
                if (sensorData.getChannelName().isEmpty()) {
                    sensorData.setChannelName(sensorName);
                }
                return;
            }
        }
        
        // Sensor serials (they're read from the board at the very beginning)
        for (SensorData sensorData:collectedData.getSensors()) {
            String sensorSerial = protocolHelper.evalReadSensorSerialNumber(dataMessage, sensorData.getBoardId(), sensorData.getChannel());
            if (sensorSerial != null) {
                if (sensorSerial.isEmpty()) {
                    sensorSerial = "NA";
                }
                sensorData.setChannelSerial(sensorSerial);
            }
        }
        
    }

    public CollectedData getCollectedData() {
        return collectedData;
    }
    
    public void startSampling() {

        // Retrieve the overall unique board ID number
        Set<Integer> boardIds = new HashSet<>();
        for (SensorData sensorData:collectedData.getSensors()) {
            boardIds.add(sensorData.boardId);
        }
        
        // Start sampling on each boardId found
        for (Integer boardId:boardIds) {
            protocolHelper.renderStartSample(boardId);
        }
    }
    
    public void stopSampling() {
        
        Set<Integer> boardIds = new HashSet<>();
        for (SensorData sensorData:collectedData.getSensors()) {
            boardIds.add(sensorData.boardId);
        }
        
        // Start sampling on each boardId found
        for (Integer boardId:boardIds) {
            protocolHelper.renderStopSample(boardId);
        }
    }
    
    public List<RawCommand> sendRawData(List<RawCommand> data) {
        
        try {
            return rawCommandQueue.sendCommandList(data);
        } catch (InterruptedException ex) {
            return null;
        }
    }
    
    public void takeOwnership() {
        ownershipTimestamp = new Date().getTime();
    }

    public void releaseOwnership() {
        ownershipTimestamp = 0;
    }
    
        /** 
     * Evaluate a mathematical expression
     * @param mathExpression
     * @param rawData
     * @return 
     */
    private double evaluateMath(String mathExpression, int rawData) {
        
        Expr expr;
        try {
            expr = Parser.parse(mathExpression);
        } catch (SyntaxException ex) {
            return rawData;
        }
        
        Variable x = Variable.make("x");
        x.setValue(rawData);
        return expr.value();
    }        
}
