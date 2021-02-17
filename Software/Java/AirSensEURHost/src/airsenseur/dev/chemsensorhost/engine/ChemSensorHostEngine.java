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
import airsenseur.dev.chemsensorhost.RawCommandQueue;
import airsenseur.dev.chemsensorhost.sensors.SensorBoardInfo;
import airsenseur.dev.chemsensorhost.sensors.SensorInfo;
import airsenseur.dev.chemsensorhost.comm.SensorBusCommunicationHandler;
import airsenseur.dev.chemsensorhost.sensors.hostsensors.HostSensor;
import airsenseur.dev.chemsensorhost.sensors.hostsensors.HostSensorBatteryChargerStatus;
import airsenseur.dev.chemsensorhost.sensors.hostsensors.HostSensorBatteryCoulombCounter;
import airsenseur.dev.chemsensorhost.sensors.hostsensors.HostSensorBatteryVoltage;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.CommChannelFactory;
import airsenseur.dev.comm.SensorBus;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.comm.TransportLogicFactory;
import airsenseur.dev.exceptions.GenericException;
import airsenseur.dev.exceptions.SensorBusException;
import airsenseur.dev.helpers.TaskScheduler;
import airsenseur.dev.json.RawCommand;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main sensor-host engine class
 * @author marco
 */
public class ChemSensorHostEngine extends TaskScheduler implements SensorBus.SensorBusMessageConsumer {
    
    // All sensors connected to this system
    private final List<SensorInfo> sensors = new ArrayList<>();
    
    // All boards connected to this system
    private final HashMap<Integer, SensorBoardInfo> boards = new HashMap<>();
    
    // Current state machine handler
    private final List<EngineStatus> engineStatuses = new ArrayList<>();
    private int currentStatusId = 0;
    
    // Various state machine handlers
    private final ChemSensorEngineStateAutodiscover stateAutodiscover = new ChemSensorEngineStateAutodiscover();
    private final ChemSensorEngineStateReadConfig stateReadConfig = new ChemSensorEngineStateReadConfig();
    private final ChemSensorEngineStateSampling stateSampling = new ChemSensorEngineStateSampling();
    
    // Sensor Bus Handler and sensor bus queue command
    private final SensorBusCommunicationHandler sensorBusHandler = new SensorBusCommunicationHandler();
    private final ShieldProtocolLayer sensorBusProtocolLayer = new ShieldProtocolLayer(sensorBusHandler);
    private final RawCommandQueue sensorBusRawCommandQueue = new RawCommandQueue(sensorBusProtocolLayer);
    
    // Host Sensor Handlers. The list should be sorted by relative sensor channel
    private final List<HostSensor> hostSensorsHandlers = new ArrayList<>();
    
    // Start or last configuration discovery timestamp
    private long lastConfigurationTimestamp;
    
    // Ownership timeout and timer
    private final long OWNERSHIP_AUTOFALLBACK_TIMEOUT = 120000;
    private long ownershipTimestamp = 0;
    
    private final Logger log = LoggerFactory.getLogger(ChemSensorHostEngine.class);
    
    private static class EngineStatus {
        public ChemSensorEngineState statusHandler;
        public ChemSensorEngineState nextStatusHandler;

        public EngineStatus(ChemSensorEngineState statusHandler, ChemSensorEngineState nextStatusHandler) {
            this.statusHandler = statusHandler;
            this.nextStatusHandler = nextStatusHandler;
        }
    }
    
    public List<SensorInfo> getSensors() {
        return sensors;
    }
    
    public HashMap<Integer, SensorBoardInfo> getBoards() {
        return boards;
    }
    
    public ShieldProtocolLayer getSensorBusProtocolLayer() {
        return sensorBusProtocolLayer;
    }
    
    public HostSensor getHostSensorProtocolLayer(int channel) {
        if (channel < hostSensorsHandlers.size()) {
            return hostSensorsHandlers.get(channel);
        }
        
        return null;
    }
    
    public List<HostSensor> getHostSensorProtocolLayer() {
        return hostSensorsHandlers;
    }
    
    public ChemSensorHostEngine() {
        
        // Initialize the array of available engine statuses
        if (Configuration.getConfig().enableAutoDiscovery()) {
            engineStatuses.add(new EngineStatus(stateAutodiscover, stateReadConfig));
        }
        engineStatuses.add(new EngineStatus(stateReadConfig, stateSampling));
        engineStatuses.add(new EngineStatus(stateSampling, null));
        currentStatusId = engineStatuses.size() - 1;
        
        // Initialize the host sensor handlers
        hostSensorsHandlers.add(new HostSensorBatteryChargerStatus());
        hostSensorsHandlers.add(new HostSensorBatteryCoulombCounter());
        hostSensorsHandlers.add(new HostSensorBatteryVoltage());
    }
    
    // Returns false for fatal exceptions
    public boolean start(int pollMs) {

        try {            
            
            // Initialize the sensor bus transport logic (only point to multipoint allowed here)
            if (Configuration.getConfig().useCRCInSensorBus()) {
                log.info("Inizializing Sensor Bus with CRC enabled");
            }
            
            sensorBusHandler.init(this, CommChannelFactory.commChannelType.SERIAL, TransportLogicFactory.transportLogicType.POINT_TO_MULTIPOINT, Configuration.getConfig().useCRCInSensorBus());
            sensorBusHandler.connectToBus(Configuration.getConfig().serialPort());
        
            stateAutodiscover.init(this);       
            stateReadConfig.init(this);
            stateSampling.init(this);

            // Start with the next status
            goToNextStatus();
            
        } catch (SensorBusException ex) {
            log.error(ex.getErrorMessage());
            return false;
        }
        catch (GenericException ex) {
            log.error(ex.getErrorMessage());
            return false;
        }
        
        startPeriodic(pollMs);
        
        return true;
    }
    
    public void exit() {
        
        stop();
        sensorBusHandler.disConnectFromBus();
    }
    
    /**
     * @return the lastConfigurationTimestamp
     */
    public long getLastConfigurationTimestamp() {
        return lastConfigurationTimestamp;
    }

    /**
     */
    public void refreshLastConfigurationTimestamp() {
        this.lastConfigurationTimestamp = System.currentTimeMillis();
    }
    

    @Override
    // Message received back from the Sensor Bus 
    public void onNewMessageReady(AppDataMessage message) {
        
        // Debug
        log.debug("R> [" + message.getBoardId() + "] (" + message.getCommandString() + ")" );
        
        // Messages received from sensor bus may have been generated by JSON clients. For this reason,
        // rawCommandQueue should evaluate all incoming Sensor Bus messages.
        // Avoid to return if something matches on rawCommandQueue in order to allows messages
        // evaluation by hostengine process, so it could update local datastructures
        sensorBusRawCommandQueue.onNewMessageReady(message);
                
        // Evaluate the message based on current engine status
        if (engineStatuses.get(currentStatusId).statusHandler != null) {
            try {
                engineStatuses.get(currentStatusId).statusHandler.evaluateAnswer(message);
            } catch (GenericException ex) {
                log.error(ex.getErrorMessage());
            }
        }
    }
    
    
    @Override
    public void taskMain() {
        
        // If someone else tooks ownership, does nothing
        if ((ownershipTimestamp != 0) && ((System.currentTimeMillis() - ownershipTimestamp) < OWNERSHIP_AUTOFALLBACK_TIMEOUT)) {
            return;
        }
        
        // Ownership elapsed, reset the timestamp flag
        ownershipTimestamp = 0;
                
        try {
            
            ChemSensorEngineState currentStatus = engineStatuses.get(currentStatusId).statusHandler;
            if (currentStatus != null) {
                currentStatus.inquirySensors();

                if (currentStatus.terminated()) {
                    log.debug("Current engine state terminated: " + currentStatus.getName());
                    
                    // Dump configuration when ready                    
                    if ((currentStatus == stateReadConfig) && (Configuration.getConfig().debugEnabled())) {
                        dumpConfiguration();
                    }
                    
                    // Move to next status
                    goToNextStatus();
                }
            }
            
        } catch (GenericException ex) {
            log.error(ex.getErrorMessage());
        }
        
    }

    @Override
    public String getTaskName() {
        return "ChemSensorHostEngine";
    }
    
    public void startSampling() throws SensorBusException {

        // Start sampling on each boardId found
        for (Integer boardId:boards.keySet()) {
            sensorBusProtocolLayer.renderStartSample(boardId);
        }
    }
    
    public void stopSampling() throws SensorBusException {
        
        // Stop sampling on each boardId found
        for (Integer boardId:boards.keySet()) {
            sensorBusProtocolLayer.renderStopSample(boardId);
        }
    }    
    
    public List<RawCommand> sendRawData(List<RawCommand> data) {
        
        try {
            return sensorBusRawCommandQueue.sendCommandList(data);
        } catch (InterruptedException | SensorBusException ex) {
            return null;
        }
    }
    
    public void takeOwnership() {
        ownershipTimestamp = System.currentTimeMillis();
    }

    public void releaseOwnership() {
        ownershipTimestamp = 0;
    } 
    
    public boolean getIsReady() {
        return (engineStatuses.get(currentStatusId).statusHandler == stateSampling);
    }
    
    private void goToNextStatus() throws GenericException {
        
        if (engineStatuses.get(currentStatusId).nextStatusHandler == null){
            currentStatusId = 0;
        } else {
            currentStatusId++;
            if (currentStatusId >= engineStatuses.size()) {
                currentStatusId = 0;
            }
        }
        
        // Initialize the new status
        engineStatuses.get(currentStatusId).statusHandler.enter();
    }
    
    private void dumpConfiguration() {
        
        // Dump all boards information
        log.info("--- Boards --- (found " + boards.size() + " boards) ---");
        for (SensorBoardInfo board : boards.values()) {
            log.info("Board Id: " +  board.getBoardId());
            log.info("  Type: " + ShieldProtocolLayer.getBoardTypeString(board.getBoardType()));
            log.info("  Serial: " + board.getSerial());
            log.info("  Firmware: " + board.getFirmware());
            log.info("  Channels: " + board.getNumOfChannels());
        }
        
        // Dump all channels information
        log.info("--- Sensors --- (found " + sensors.size() + " sensors) ---");
        int channelId = 0;
        for (SensorInfo sensor : sensors) {
            log.info("Board Id: " + sensor.getSensorConfig().getBoardId());
            log.info("  Relative channel: " + sensor.getSensorConfig().getChannel());
            log.info("  Absolute channel: " + channelId);
            log.info("  Name: " + sensor.getSensorConfig().getName());
            log.info("  Serial: " + sensor.getSensorConfig().getSerial());
            log.info("  Measurement units: " + sensor.getSensorConfig().getMeasurementUnits());
            log.info("  MathExpression: " + sensor.getSensorConfig().getMathExpression());
            log.info("  SamplingPeriod: " + sensor.getSensorConfig().getSamplingPeriod());
            log.info("  Enabled: " + sensor.getSensorConfig().getEnabled());
            channelId++;
        }
    }

}
