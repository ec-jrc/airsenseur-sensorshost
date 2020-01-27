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

import airsenseur.dev.chemsensorhost.sensors.SensorBoardInfo;
import airsenseur.dev.chemsensorhost.sensors.SensorConfig;
import airsenseur.dev.chemsensorhost.sensors.SensorInfo;
import airsenseur.dev.chemsensorhost.sensors.SensorValue;
import airsenseur.dev.chemsensorhost.engine.ChemSensorHostEngine;
import airsenseur.dev.exceptions.SensorBusException;
import airsenseur.dev.json.BoardInfo;
import airsenseur.dev.json.ChemSensorService;
import airsenseur.dev.json.HostStatus;
import airsenseur.dev.json.RawCommand;
import airsenseur.dev.json.SampleData;
import java.util.ArrayList;
import java.util.List;


/**
 * Implementation of services available through JSON
 * @author marco
 */
public class ChemSensorServiceImpl implements ChemSensorService {
    
    private ChemSensorHostEngine sensorHost;

    public void setSensorHost(ChemSensorHostEngine sensorHost) {
        this.sensorHost = sensorHost;
    }

    @Override    
    public HostStatus getHostStatus() {
        
        HostStatus result = new HostStatus(sensorHost.getIsReady()? HostStatus.STATUS_READY : HostStatus.STATUS_BUSY);
        
        return result;
    }

    @Override
    public List<BoardInfo> getSensorBoardsInfo() {
        
        List<BoardInfo> boardInfoList = new ArrayList<>();
        for (SensorBoardInfo info:sensorHost.getBoards().values()) {
            BoardInfo boardInfo = new BoardInfo();
            boardInfo.boardId = info.getBoardId();
            boardInfo.timestamp = sensorHost.getLastConfigurationTimestamp();
            boardInfo.fwRevision = info.getFirmware().getValue();
            boardInfo.serial = info.getSerial().getValue();
            boardInfo.boardType = "" + info.getBoardType();
            boardInfoList.add(boardInfo);
        }
        
        return boardInfoList;
    }
    
    @Override
    public airsenseur.dev.json.SensorConfig getSensorConfig(int sensorId) {
        
        if (sensorId < sensorHost.getSensors().size()) {
            SensorInfo sensorInfo = sensorHost.getSensors().get(sensorId);
            SensorConfig sensorConfig = sensorInfo.getSensorConfig();
            
            String name = sensorConfig.getName().isSet()? sensorConfig.getName().getValue() : "";
            String serial = sensorConfig.getSerial().isSet()? sensorConfig.getSerial().getValue() : "";
            String measurementUnits = sensorConfig.getMeasurementUnits().isSet()? sensorConfig.getMeasurementUnits().getValue() : "";
            Integer samplingPeriod = sensorConfig.getSamplingPeriod().isSet()? sensorConfig.getSamplingPeriod().getValue() : 0;
            Boolean enabled = sensorConfig.getEnabled().isSet()? sensorConfig.getEnabled().getValue() : Boolean.TRUE;
            
            return new airsenseur.dev.json.SensorConfig(name, serial, measurementUnits, sensorId, samplingPeriod, sensorHost.getLastConfigurationTimestamp(), enabled);
        }
        
        return null;
    }
    

    @Override
    public boolean startSampling() {
        try {
            sensorHost.startSampling();
            return true;
            
        } catch (SensorBusException ex) {
            return false;
        }
    }

    @Override
    public boolean stopSampling() {
        try {
            sensorHost.stopSampling();
            return true;
            
        } catch (SensorBusException ex) {
            return false;
        }
    }

    @Override
    public SampleData getLastSample(int sensorId) {
        
        if (sensorId < sensorHost.getSensors().size()) {
            SensorInfo sensorInfo = sensorHost.getSensors().get(sensorId);
            SensorConfig sensorConfig = sensorInfo.getSensorConfig();
            SensorValue sensorValue = sensorInfo.getSensorValue();
            
            String name = sensorConfig.getName().isSet()? sensorConfig.getName().getValue() : "";
            String serial = sensorConfig.getSerial().isSet()? sensorConfig.getSerial().getValue() : "";
                        
            return new SampleData(name, serial, sensorValue.getValue(), sensorValue.getTimeStamp(), sensorValue.getEvalSampleVal());
        }
        
        return null;
    }

    @Override
    public List<RawCommand> sendRawData(List<RawCommand> rawData) {
        
        return sensorHost.sendRawData(rawData);
    }

    @Override
    public int getNumSensors() {
        return sensorHost.getSensors().size();
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
