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
import airsenseur.dev.chemsensorhost.sensors.SensorConfig;
import airsenseur.dev.chemsensorhost.sensors.SensorInfo;
import airsenseur.dev.chemsensorhost.sensors.SensorValue;
import airsenseur.dev.chemsensorhost.sensors.hostsensors.HostSensor;
import airsenseur.dev.chemsensorhost.sensors.hostsensors.HostSensorBoard;
import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.ShieldProtocolLayer;
import airsenseur.dev.exceptions.GenericException;
import airsenseur.dev.helpers.Pair;
import expr.Expr;
import expr.Parser;
import expr.SyntaxException;
import expr.Variable;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Poll known sensors to retrieve sampled data
 * @author marco
 */
public class ChemSensorEngineStateSampling implements ChemSensorEngineState {
    
    private final static int POLLPERIOD_OVERSAMPLING = 3;
    
    private ChemSensorHostEngine parent;
    private final Logger log = LoggerFactory.getLogger(ChemSensorEngineStateSampling.class);
    
    @Override
    public void init(ChemSensorHostEngine parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return "Sampling";
    }

    @Override
    public boolean enter() throws GenericException {
        
        // Initialize all data structures (i.e. sampling poll-time)
        // When not auto-discovered, force all sampling poll time to the general poll-time
        int pollTime = Configuration.getConfig().getPollPeriod();
        for (SensorInfo sensor : parent.getSensors()) {
            
            int sensorPollPeriod = pollTime;            
            if (sensor.getSensorConfig().getSamplingPeriod().isSet()) {
                sensorPollPeriod = sensor.getSensorConfig().getSamplingPeriod().getValue() / POLLPERIOD_OVERSAMPLING;
            }
            sensor.getSensorValue().setPollPeriod(sensorPollPeriod);
            sensor.getSensorValue().setLastPollTimestamp(0);
        }
                
        return true;
    }

    @Override
    public boolean inquirySensors() throws GenericException {
        
        // Poll Sensors based on each specific sensor's sampling time
        long now = System.currentTimeMillis();
        for (SensorInfo sensor : parent.getSensors()) {
            
            SensorConfig sensorConfig = sensor.getSensorConfig();

            // Skip disabled sensors
            if (Objects.equals(sensorConfig.getEnabled().getValue(), Boolean.FALSE)) {
                continue;
            }
            
            SensorValue sensorValue = sensor.getSensorValue();
            
            // It's not time to poll this sensor
            if ((now - sensorValue.getLastPollTimestamp()) < sensorValue.getPollTime()) {
                continue;
            }
            
            // Ok. It's time to poll this sensor
            sensor.getSensorValue().setLastPollTimestamp(now);
            
            // Handle Sensor Bus/Host sensors
            if (sensorConfig.getBoardId() == HostSensorBoard.HOST_BOARD_ID) {
                
                // Host sensors
                HostSensor hostSensor = parent.getHostSensorProtocolLayer(sensorConfig.getChannel());
                if (hostSensor != null) {
                    loadSample(sensor, hostSensor.getValue(), hostSensor.getTimestamp());
                }
            } else {
                
                // Sensor Bus sensors.
                // All sensors with no math expressions will be evaluated with the High Resolution GetLastSample.
                // Sensors with defined math expression will be evaluate with the legacy 16bitwise GetLastSample.
                if(sensorConfig.getMathExpression().isSet()) {
                    parent.getSensorBusProtocolLayer().renderGetLastSample(sensorConfig.getBoardId(), sensorConfig.getChannel());
                } else {
                    parent.getSensorBusProtocolLayer().renderGetLastSampleHRes(sensorConfig.getBoardId(), sensorConfig.getChannel());
                }
            }
        }
        
        return true;
    }

    @Override
    public boolean evaluateAnswer(AppDataMessage rxMessage) throws GenericException {
        
        // Loop on each known sensor and evaluate the received message
        ShieldProtocolLayer sensorBus = parent.getSensorBusProtocolLayer();
        for (SensorInfo sensor : parent.getSensors()) {
            
            SensorConfig sensorConfig = sensor.getSensorConfig();
            
            // All sensors with no math expressions will be evaluated with the High Resolution GetLastSample.
            // Sensors with defined math expression will be evaluate with the legacy 16bitwise GetLastSample.
            if(sensorConfig.getMathExpression().isSet()) {
                List<Integer> result = sensorBus.evalLastSampleInquiry(rxMessage, sensorConfig.getBoardId(), sensorConfig.getChannel());
                if ((result != null) && (result.size() == 2)) {

                    int sample = result.get(0);
                    int timeStamp = result.get(1);
                    loadSample(sensor, sample, timeStamp);

                    return true;
                }
            } else {
                Pair<Integer, Float> result = sensorBus.evalLastSampleHResInquiry(rxMessage, sensorConfig.getBoardId(), sensorConfig.getChannel());
                if ((result != null) && (result.first != null) && (result.second != null)) {
                    
                    SensorValue sensorValue = sensor.getSensorValue();
                    if (sensorValue.load(0, result.first)) {
                        sensorValue.setEvalSampleVal(result.second);
                    }
                }
            }
        }
        
        return false;
    }

    // Load sample with value and evaluate the math expression, if any
    private void loadSample(SensorInfo sensor, int sample, int timeStamp) {
        
        SensorValue sensorValue = sensor.getSensorValue();
        if (sensorValue.load(sample, timeStamp)) {
            
            SensorConfig sensorConfig = sensor.getSensorConfig();
            
            // Evaluate the math expression, if any
            if (sensorConfig.getMathExpression().isSet()) {
                
                String mathExpression = sensorConfig.getMathExpression().getValue();
                double evalSampleVal = evaluateMath(mathExpression, sample);
                sensorValue.setEvalSampleVal(evalSampleVal);
            }
        }
    }

    @Override
    public boolean terminated() {
        return false;
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
