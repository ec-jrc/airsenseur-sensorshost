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

package airsenseur.dev.chemsensorhost.json;

import airsenseur.dev.chemsensorhost.ChemSensorHost;
import airsenseur.dev.json.ChemSensorService;
import airsenseur.dev.json.FreeMemory;
import airsenseur.dev.json.RawCommand;
import airsenseur.dev.json.SampleData;
import java.util.List;


/**
 * Implementation of services available through JSON
 * @author marco
 */
public class ChemSensorServiceImpl implements ChemSensorService {
    
    private ChemSensorHost sensorHost;

    public void setSensorHost(ChemSensorHost sensorHost) {
        this.sensorHost = sensorHost;
    }

    @Override
    public FreeMemory getFreeMemory() {
        
        FreeMemory result = new FreeMemory(sensorHost.getCollectedData().getFreeMemory());
        
        return result;
    }

    @Override
    public void startSampling() {
        sensorHost.startSampling();
    }

    @Override
    public void stopSampling() {
        sensorHost.stopSampling();
    }

    @Override
    public SampleData getLastSample(int sensorId) {
        
        if (sensorId < sensorHost.getCollectedData().getSensors().size()) {
            ChemSensorHost.SensorData data = sensorHost.getCollectedData().getSensors().get(sensorId);
                        
            return new SampleData(data.getChannelName(), data.getChannelSerial(), data.getValue(), data.getTimeStamp(), data.getEvalSampleVal());
        }
        
        return null;
    }

    @Override
    public List<RawCommand> sendRawData(List<RawCommand> rawData) {
        
        return sensorHost.sendRawData(rawData);
    }

    @Override
    public int getNumSensors() {
        return sensorHost.getCollectedData().getSensors().size();
    }

    @Override
    public void takeOwnership() {
        sensorHost.takeOwnership();
    }

    @Override
    public void releaseOnwnership() {
        sensorHost.releaseOwnership();
    }
}
