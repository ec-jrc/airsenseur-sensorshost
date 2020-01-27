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

package airsenseur.dev.datapush.dataprocessors;

import airsenseur.dev.datapush.MinMax;
import airsenseur.dev.datapush.datacontainers.DataPushDataContainer;
import airsenseur.dev.datapush.datacontainers.DataPushSensorConfigDataContainer;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.json.SensorConfig;
import airsenseur.dev.persisters.SampleAndConfigurationLoader;
import airsenseur.dev.persisters.influxdb.SensorConfigPersisterInfluxDB;
import java.util.List;

/**
 *
 * @author marco
 */
public class DataPushSensorConfigProcessor implements DataPushProcessor {
    
    private final SampleAndConfigurationLoader dataLoader;
    private final SensorConfigPersisterInfluxDB dataPersister;

    public DataPushSensorConfigProcessor(SampleAndConfigurationLoader dataLoader, SensorConfigPersisterInfluxDB dataPersister) {
        this.dataLoader = dataLoader;
        this.dataPersister = dataPersister;
    }
    
    @Override
    public String getPersisterMarker(int channel) {
        return HistoryEventContainer.EVENT_LATEST_SENSORCONFIGPUSH_TS;
    }

    @Override
    public MinMax getMinMaxTxForChannel(int channel) throws PersisterException {
        long minTs = dataLoader.getSensorInfoMinimumTimestamp();
        long maxTs = dataLoader.getSensorInfoMaximumTimestamp();
        
        return new MinMax(minTs, maxTs);
    }

    @Override
    public DataPushDataContainer loadDataSetFromLocalPersistence(int channel, MinMax minMaxTs) throws PersisterException {
        return new DataPushSensorConfigDataContainer(dataLoader.loadSensorInfo(minMaxTs.getMin(), minMaxTs.getMax()));
    }

    @Override
    public boolean sendDataToRemotePersistence(DataPushDataContainer dataContainer) throws PersisterException {
        List<SensorConfig> dataSet = dataContainer.getDataSet();
        return dataPersister.addSensorsConfig(dataSet);
    }

    @Override
    public DataPushDataContainer clearDataContainer() {
        return new DataPushSensorConfigDataContainer();
    }

    @Override
    public long getLatestTimestampInDataContainer(DataPushDataContainer dataContainer) {
        List<SensorConfig> dataSet = dataContainer.getDataSet();
        return (long)dataSet.get(dataSet.size()-1).startSamplingTimestamp + 1;
    }
}
