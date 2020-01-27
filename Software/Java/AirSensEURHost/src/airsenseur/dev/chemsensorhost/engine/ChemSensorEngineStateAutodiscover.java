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

import airsenseur.dev.chemsensorhost.sensors.SensorBoardInfo;
import airsenseur.dev.chemsensorhost.sensors.SensorConfig;
import airsenseur.dev.chemsensorhost.sensors.SensorInfo;
import airsenseur.dev.chemsensorhost.helpers.SensorInfoSorter;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.exceptions.GenericException;
import airsenseur.dev.exceptions.SensorBusException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auto-discover all sensors connected to the device either through sensor bus or directly to the host
 * @author marco
 */
public class ChemSensorEngineStateAutodiscover implements ChemSensorEngineState {

    private final Logger log = LoggerFactory.getLogger(ChemSensorEngineStateAutodiscover.class);
    
    private final static int WAIT_BOARDID_ANSWERS_TIMEOUT = 5000; /* in milliseconds */
    private final static int WAIT_BOARDID_INQUIRY_TIMEOUT = 20000;  /* in milliseconds */
    private final static int WAIT_CHANNEL_INQUIRY_TIMEOUT = 35000; /* in milliseconds */

    private enum Substate {
        START_DISCOVERING_BOARDS, 
        WAIT_BOARD_ANSWERS,
        INQUIRY_SBUS_BOARDS_INFO,
        WAIT_SBUS_BOARDS_INFO_ANSWERS,
        INQUIRY_SBUS_CHANNELS,
        WAIT_SBUS_CHANNELS_ANSWERS,
        COLLECTION_READY,
        TERMINATED,
    };
    
    private ChemSensorHostEngine parent;
    private Substate substate = Substate.START_DISCOVERING_BOARDS;
    private long timer = 0;
    
    @Override
    public void init(ChemSensorHostEngine parent) {
        this.parent = parent;
        substate = Substate.START_DISCOVERING_BOARDS;
    }
    
    @Override
    public String getName() {
        return "Auto Discover";
    }
    
    @Override
    public boolean enter() throws GenericException {
        
        // Clear all collected board and sensors info
        parent.getBoards().clear();
        parent.getSensors().clear();
        
        // Start with discovery
        substate = Substate.START_DISCOVERING_BOARDS;
        
        return true;
    }

    @Override
    public synchronized boolean inquirySensors() throws GenericException {
        
        switch (substate) {
            case START_DISCOVERING_BOARDS: {
                
                discoverBoardsOnSensorBus();
                
                // Wait for all answers or timeout
                timer = System.currentTimeMillis();                
                substate = Substate.WAIT_BOARD_ANSWERS;
            }
            break;
            
            case WAIT_BOARD_ANSWERS: {

                if (((System.currentTimeMillis() - timer) > WAIT_BOARDID_ANSWERS_TIMEOUT) ||
                        parent.getBoards().size() == (AppDataMessage.MAX_VALID_BOARD_ID_ON_SBUS - AppDataMessage.MIN_VALID_BOARD_ID_ON_SBUS)) {
                    
                    // Timeout occurred or the maximum number of availabe boards on Sensor Bus have been detected
                    timer = System.currentTimeMillis();
                    substate = Substate.INQUIRY_SBUS_BOARDS_INFO;
                }
            }
            break;
            
            
            case INQUIRY_SBUS_BOARDS_INFO: {
                
                inquiryBoardsOnSensorBus();
                
                // Wait for all answers or timeout
                timer = System.currentTimeMillis();
                substate = Substate.WAIT_SBUS_BOARDS_INFO_ANSWERS;
            }
            break;
            
            case WAIT_SBUS_BOARDS_INFO_ANSWERS: {
                
                // Handle timeout
                if ((System.currentTimeMillis() - timer) > WAIT_BOARDID_INQUIRY_TIMEOUT) {

                    log.info("Timeout occurred when inquirying board information. Board info-set may not be completed.");
                    timer = System.currentTimeMillis();
                    substate = Substate.INQUIRY_SBUS_CHANNELS;
                } 
                
                // Check for all data availability in order to proceed on next step
                if (checkBoardInfoValidity()) {
                    timer = System.currentTimeMillis();
                    substate = Substate.INQUIRY_SBUS_CHANNELS;
                }
            }
            break;
            
            case INQUIRY_SBUS_CHANNELS: {
                
                discoverChannelsOnSensorBus();
                
                // Wait for all answers or timeout
                timer = System.currentTimeMillis();
                substate = Substate.WAIT_SBUS_CHANNELS_ANSWERS;
            }
            break;
            
            case WAIT_SBUS_CHANNELS_ANSWERS : {
                
                if ((System.currentTimeMillis() - timer) > WAIT_CHANNEL_INQUIRY_TIMEOUT) {
                    log.info("Timeout occurred when inquirying channel information. Channel info-set may not be completed.");

                    // Proceed with collected data analysis
                    substate = Substate.COLLECTION_READY;
                } 
                
                // Check for all data availability in order to proceed on next step
                if (checkChannelInfoValidity()) {
                    substate = Substate.COLLECTION_READY;
                }
            }
            break;
            
            case COLLECTION_READY : {
                
                // Sort channels
                sortChannelList();
                
                // Signal we've update the configuration
                parent.refreshLastConfigurationTimestamp();
                
                // Signal we've terminated the auto-discovery procedure
                substate = Substate.TERMINATED;
            }
            break;    
            
        }
        return true;
    }

    @Override
    public synchronized boolean evaluateAnswer(AppDataMessage rxMessage) throws GenericException {
        
        switch (substate) {
            case WAIT_BOARD_ANSWERS: {

                evaluateBoardIDAnswerOnSensorBus(rxMessage);
                
            }
            break;
            
            case WAIT_SBUS_BOARDS_INFO_ANSWERS: {
                
                evaluateInquiryBoardsOnSensorBus(rxMessage);
            }
            break;
            
            case WAIT_SBUS_CHANNELS_ANSWERS: {
                
                evaluateInquiryChannelsOnSensorBus(rxMessage);
            }
        }
        return true;
    }

    @Override
    public synchronized boolean terminated() {
        return (substate == Substate.TERMINATED);
    }
    
    
    private void discoverBoardsOnSensorBus() throws GenericException {
        
        log.info("Discovering boards on Sensor Bus");
        
        for (int boardId = AppDataMessage.MIN_VALID_BOARD_ID_ON_SBUS; 
                boardId <= AppDataMessage.MAX_VALID_BOARD_ID_ON_SBUS; boardId++) {
            
            parent.getSensorBusProtocolLayer().renderReadBoardType(boardId);
        }
    }
    
    
    private void evaluateBoardIDAnswerOnSensorBus(AppDataMessage rxMessage) {
    
        for (int boardId = AppDataMessage.MIN_VALID_BOARD_ID_ON_SBUS; 
                boardId <= AppDataMessage.MAX_VALID_BOARD_ID_ON_SBUS; boardId++) {
            
            List<Integer> result = parent.getSensorBusProtocolLayer().evalReadBoardType(rxMessage, boardId);
            if ((result != null) && (result.size() == 2)) {
                Integer boardType = result.get(0);
                Integer numChannels = result.get(1);
                
                // Update the list of known boards
                parent.getBoards().put(boardId, new SensorBoardInfo(boardId, boardType, numChannels));
                log.info("Found board " + ShieldProtocolLayer.getBoardTypeString(boardType) + "  with ID " + boardId + " and num channels " + numChannels);
                
                // Add the required number of channels in the sensors list
                for (int channel = 0; channel < numChannels; channel++) {
                    SensorInfo sensorInfo = new SensorInfo();
                    sensorInfo.getSensorConfig().init(boardId, channel);
                    parent.getSensors().add(sensorInfo);
                }
                break;
            }
        }
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
    
    private boolean checkBoardInfoValidity() {
        
        // Loop on each known board
        boolean result = true;
        for (SensorBoardInfo board : parent.getBoards().values()) {
            result &= board.getFirmware().isSet();
            result &= board.getSerial().isSet();
        }
        return result;
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
            if (!sensorConfig.getSamplingPeriod().isSet()) {
                sensorBus.renderReadSamplePeriod(boardId, channel);
                atLeastOneUnknownParameter = true;
            }
            if (!sensorConfig.getMeasurementUnits().isSet()) {
                sensorBus.renderReadUnits(boardId, channel);
                atLeastOneUnknownParameter = true;
            }
            if (!sensorConfig.getEnabled().isSet()) {
                sensorBus.renderReadChannelEnable(boardId, channel);
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
            result &= sensorConfig.getSerial().isSet();
            result &= sensorConfig.getSamplingPeriod().isSet();
            result &= sensorConfig.getMeasurementUnits().isSet();
            result &= sensorConfig.getEnabled().isSet();
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
            
            String measurementUnits = sensorBus.evalReadUnits(rxMessage, boardId, channel);
            if (measurementUnits != null) {
                sensorConfig.getMeasurementUnits().autodiscoveredValue(measurementUnits);
            }
            
            Boolean enabled = sensorBus.evalReadChannelEnable(rxMessage, boardId, channel);
            if (enabled != null) {
                sensorConfig.getEnabled().autodiscoveredValue(enabled);
            }
        }
        
        return false;
    }    
    

    // Sort the discevered channel list based on boardId, channel
    private void sortChannelList() {
        
        Collections.sort(parent.getSensors(), new SensorInfoSorter());
    }
}
