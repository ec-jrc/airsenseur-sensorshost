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

package airsenseur.dev.chemsensorhost.engine;

import airsenseur.dev.chemsensorhost.Configuration;
import airsenseur.dev.chemsensorhost.sensors.SensorBoardInfo;
import airsenseur.dev.chemsensorhost.sensors.SensorConfig;
import airsenseur.dev.chemsensorhost.sensors.SensorInfo;
import airsenseur.dev.chemsensorhost.helpers.SensorInfoSorter;
import airsenseur.dev.chemsensorhost.sensors.hostsensors.HostSensor;
import airsenseur.dev.chemsensorhost.sensors.hostsensors.HostSensorBoard;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.exceptions.GenericException;
import airsenseur.dev.exceptions.SensorBusException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read sensor information from the configuration overriding what auto-discovered (if any)
 * @author marco
 */
public class ChemSensorEngineStateReadConfig implements ChemSensorEngineState {
    
    private final static int WAIT_CHANNEL_INQUIRY_TIMEOUT = 20000; /* in milliseconds */
    
    private ChemSensorHostEngine parent;
    private final Logger log = LoggerFactory.getLogger(ChemSensorEngineStateReadConfig.class);
    
    private enum Substate {
        INQUIRY_SBUS_CHANNELS,
        WAIT_SBUS_CHANNELS_ANSWERS,
        SBUS_COLLECTION_READY,
        TERMINATED,
    };
    private Substate substate = Substate.INQUIRY_SBUS_CHANNELS;
    private long timer = 0;

    @Override
    public void init(ChemSensorHostEngine parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return "Read Configuration";
    }

    @Override
    public boolean enter() throws GenericException {
        
        Configuration config = Configuration.getConfig();        
        
        // If the configuration reports more boards than
        // what is currently present in the board-set (maybe auto-discovered)
        // integrate our list with info found in configuration
        HashMap<Integer, SensorBoardInfo> boards = parent.getBoards();
        HashMap<Integer, Integer> boardIds = new HashMap<>();
        for (int n = 0; n < config.getNumSensors(); n++) {
            int boardId = config.getBoardIdForSensor(n);
            int channel = config.getChannelForSensor(n);
            if (boardIds.containsKey(boardId)) {
                int setChannel = boardIds.get(boardId);
                if (channel > setChannel) {
                    boardIds.put(boardId, channel);
                }
            } else {
                boardIds.put(boardId, channel);
            }
        }
        if (boards.size() < boardIds.size()) {
            for (Integer board : boardIds.keySet()) {
                if (!boards.containsKey(board)) {
                    boards.put(board, new SensorBoardInfo(board, 0, boardIds.get(board)));
                }
            }
        }
        
        // If the configuration reports more sensors than 
        // what is currently present in the sensor-set (maybe auto-discovered)
        // integrate our list with info found in configuration
        List<SensorInfo> sensors = parent.getSensors();
        if (sensors.size() < config.getNumSensors()) {

            for (int n = sensors.size(); n < config.getNumSensors(); n++) {
                
                int boardId = config.getBoardIdForSensor(n);
                int channel = config.getChannelForSensor(n);
                
                SensorInfo sensorInfo = new SensorInfo();
                sensorInfo.getSensorConfig().init(boardId, channel);
                sensors.add(sensorInfo);
            }
        }
        
        // Sort the sensors list by boardId and channel
        Collections.sort(sensors, new SensorInfoSorter());
        
        // Start inquirying names and serials
        substate = Substate.INQUIRY_SBUS_CHANNELS;
        timer = System.currentTimeMillis();
        
        return true;
    }

    @Override
    public synchronized boolean inquirySensors() throws GenericException {
        
        switch (substate) {
            case INQUIRY_SBUS_CHANNELS: {
                
                // Ask information for all configured boards and sensors
                inquiryBoardsOnSensorBus();
                discoverChannelsOnSensorBus();
                
                // Wait for all answers or timeout
                timer = System.currentTimeMillis();                
                substate = Substate.WAIT_SBUS_CHANNELS_ANSWERS;
            }
            break;
            
            case WAIT_SBUS_CHANNELS_ANSWERS: {
                
                if (!checkChannelInfoValidity()) {
                    
                    if ((System.currentTimeMillis() - timer) > WAIT_CHANNEL_INQUIRY_TIMEOUT) {
                        log.info("Timeout occurred when inquirying channel information. Channel info-set may not be completed.");
                        
                        // Proceed with collected data analysis
                        substate = Substate.SBUS_COLLECTION_READY;
                    }
                } else {
                    
                    // Proceed with collected data analysis
                    substate = Substate.SBUS_COLLECTION_READY;
                }
                
            }
            break;
            
            case SBUS_COLLECTION_READY: {
                
                // Append host related sensors list, if any
                appendHostBoardInfo();
                appendHostSensors();
                
                // Override with configuration values, if any
                overrideFromConfiguration();
                
                // Signal we've update the configuration
                parent.refreshLastConfigurationTimestamp();
                
                // Then terminates
                substate = Substate.TERMINATED;
            }
            break;
                
                
        }
        
        return true;
    }

    @Override
    public synchronized boolean evaluateAnswer(AppDataMessage rxMessage) throws GenericException {
        
        if (substate == Substate.WAIT_SBUS_CHANNELS_ANSWERS) {
            if (!evaluateInquiryBoardsOnSensorBus(rxMessage)) {
                evaluateInquiryChannelsOnSensorBus(rxMessage);
            }
        }
        
        return true;
    }

    @Override
    public synchronized boolean terminated() {
        return (substate == Substate.TERMINATED);
    }
    
    private boolean inquiryBoardsOnSensorBus() throws SensorBusException {
        
        log.info("Inquirying boards information on Sensor Bus");
        
        boolean atLeastOneUnknownParameter = false;
        
        ShieldProtocolLayer sensorBus = parent.getSensorBusProtocolLayer();
        
        // Loop on each known board
        for (SensorBoardInfo board : parent.getBoards().values()) {
            
            int boardId = board.getBoardId();

            // Loop on each unknown value in the board
            if (!board.getFirmware().isSet()) {
                sensorBus.renderReadFirmwareVersion(boardId);
                atLeastOneUnknownParameter = true;
            }
            if (!board.getSerial().isSet()) {
                sensorBus.renderReadBoardSerialNumber(boardId);
                atLeastOneUnknownParameter = true;
            }
        }
        
        return atLeastOneUnknownParameter;
    }    
    
    private boolean evaluateInquiryBoardsOnSensorBus(AppDataMessage rxMessage) {
        
        ShieldProtocolLayer sensorBus = parent.getSensorBusProtocolLayer();
        
        // Loop on each known boardId to find the proper matching
        for (SensorBoardInfo board : parent.getBoards().values()) {
            
            int boardId = board.getBoardId();
            
            String firmwareVersion = sensorBus.evalReadFirmwareVersion(rxMessage, boardId);
            if (firmwareVersion != null) {
                board.getFirmware().autodiscoveredValue(firmwareVersion);
                return true;
            } 
            
            String boardSerialNumber = sensorBus.evalReadBoardSerialNumber(rxMessage, boardId);
            if (boardSerialNumber != null) {
                board.getSerial().autodiscoveredValue(boardSerialNumber);
                return true;
            }
        }
        
        return false;
    }
       
    
    private boolean discoverChannelsOnSensorBus() throws SensorBusException {
        
        log.info("Inquirying channels information on Sensor Bus");
        
        boolean atLeastOneUnknownParameter = false;
        
        ShieldProtocolLayer sensorBus = parent.getSensorBusProtocolLayer();
        
        // Loop on each known channel
        for (SensorInfo sensorInfo : parent.getSensors()) {
            
            SensorConfig sensorConfig = sensorInfo.getSensorConfig();
            int boardId = sensorConfig.getBoardId();
            int channel = sensorConfig.getChannel();
            
            if (!sensorConfig.getName().isSet()) {
                sensorBus.renderSensorInquiry(boardId, channel);
                atLeastOneUnknownParameter = true;
            }
            if (!sensorConfig.getSerial().isSet()) {
                sensorBus.renderReadSensorSerialNumber(boardId, channel);
                atLeastOneUnknownParameter = true;
            }
        }
        
        return atLeastOneUnknownParameter;
    }
    
    private boolean checkChannelInfoValidity() {
        
        // Loop on each known channel
        boolean result = true;
        for (SensorInfo sensorInfo : parent.getSensors()) {
            SensorConfig sensorConfig = sensorInfo.getSensorConfig();
            
            result &= sensorConfig.getName().isSet();
        }
        
        return result;
    }
    
    private boolean evaluateInquiryChannelsOnSensorBus(AppDataMessage rxMessage) {
        
        ShieldProtocolLayer sensorBus = parent.getSensorBusProtocolLayer();
        
        // Loop on each known channel to find the proper matching
        for (SensorInfo sensorInfo : parent.getSensors()) {
            
            SensorConfig sensorConfig = sensorInfo.getSensorConfig();
            int boardId = sensorConfig.getBoardId();
            int channel = sensorConfig.getChannel();
            
            String name = sensorBus.evalSensorInquiry(rxMessage, boardId, channel);
            if (name != null) {
                sensorConfig.getName().autodiscoveredValue(name);
                return true;
            }
            
            String serial = sensorBus.evalReadSensorSerialNumber(rxMessage, boardId, channel);
            if (serial != null) {
                sensorConfig.getSerial().autodiscoveredValue(serial);
                return true;
            }
            
            Integer samplingPeriod = sensorBus.evalReadSamplePeriod(rxMessage, boardId, channel);
            if (samplingPeriod != null) {
                sensorConfig.getSamplingPeriod().autodiscoveredValue(samplingPeriod);
                return true;
            }
        }
        
        return false;
    }        
    
    
    private void overrideFromConfiguration() {
                
        // Now that we're sure that at least the configured channels and boards
        // are present in our dataset, merge the information read from configuration, if any
        List<SensorInfo> sensors = parent.getSensors();
        Configuration config = Configuration.getConfig();
        for (int sensorId = 0; sensorId < sensors.size(); sensorId++) {
            
            SensorInfo sensorInfo = sensors.get(sensorId);
                        
            String name = config.getSensorNameForChannel(sensorId);
            if (!name.isEmpty()) {
                sensorInfo.getSensorConfig().getName().overrideValue(name);
            }
            
            String mathExpression = config.getMathExpressionForSensor(sensorId);
            if (!mathExpression.isEmpty()) {
                sensorInfo.getSensorConfig().getMathExpression().overrideValue(mathExpression);
            }
            
            boolean sensorDisabled = config.getIsSensorDisabled(sensorId);
            if (sensorDisabled) {
                sensorInfo.getSensorConfig().getEnabled().overrideValue(!sensorDisabled);
            }
        }
    } 
    
    // Host sensors are not connected through the SensorBus but directly
    // located on the AirSensEUR Host and, mainly, available through the
    // kernel filesystem. For this reason they're marked with BOARD_ID_UNDEFINED Id
    public void appendHostBoardInfo() {
        
        // Nothing to do if host sensor retrieval is disabled by configuration
        if (Configuration.getConfig().skipHostSensors()) {
            return;
        }
        
        HostSensorBoard hostSensorBoardHandler = new HostSensorBoard();
        SensorBoardInfo hostBoard = new SensorBoardInfo(hostSensorBoardHandler.getBoardId(), 
                                                        hostSensorBoardHandler.boardType(),
                                                        hostSensorBoardHandler.numChannels());
        hostBoard.getSerial().autodiscoveredValue(hostSensorBoardHandler.getSerial());
        hostBoard.getFirmware().autodiscoveredValue(hostSensorBoardHandler.getFirmware());
        
        parent.getBoards().put(hostSensorBoardHandler.getBoardId(), hostBoard);
    }
    
    public void appendHostSensors() {
        
        // Nothing to do if host sensor retrieval is disabled by configuration
        if (Configuration.getConfig().skipHostSensors()) {
            return;
        }
        
        List<SensorInfo> sensors = parent.getSensors();
        for (HostSensor hostSensor : parent.getHostSensorProtocolLayer()) {
            
            SensorInfo sensor = new SensorInfo();
            SensorConfig sensorConfig = sensor.getSensorConfig();
            sensorConfig.init(hostSensor.getBoardId(), hostSensor.getChannel());
            sensorConfig.getName().autodiscoveredValue(hostSensor.getName());
            sensorConfig.getSerial().autodiscoveredValue(hostSensor.getSerial());
            sensorConfig.getMeasurementUnits().autodiscoveredValue(hostSensor.getMeasurementUnits());
            sensorConfig.getMathExpression().autodiscoveredValue(hostSensor.getMathExpression());
            sensorConfig.getSamplingPeriod().autodiscoveredValue(hostSensor.getSamplingPeriod());

            sensors.add(sensor);
        }
    }
}
