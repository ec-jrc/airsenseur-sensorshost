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
import airsenseur.dev.datapush.datacontainers.DataPushBoardInfoDataContainer;
import airsenseur.dev.datapush.datacontainers.DataPushDataContainer;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.json.BoardInfo;
import airsenseur.dev.persisters.SampleAndConfigurationLoader;
import airsenseur.dev.persisters.influxdb.BoardPersisterInfluxDB;
import java.util.List;

/**
 *
 * @author marco
 */
public class DataPushBoardInfoProcessor implements DataPushProcessor {
    
    private final SampleAndConfigurationLoader dataLoader;
    private final BoardPersisterInfluxDB dataPersister;

    public DataPushBoardInfoProcessor(SampleAndConfigurationLoader dataLoader, BoardPersisterInfluxDB dataPersister) {
        this.dataLoader = dataLoader;
        this.dataPersister = dataPersister;
    }

    @Override
    public String getPersisterMarker(int channel) {
        return HistoryEventContainer.EVENT_LATEST_BOARDINFOPUSH_TS;
    }

    @Override
    public MinMax getMinMaxTxForChannel(int channel) throws PersisterException {
        long minTs = dataLoader.getBoardInfoMinimumTimestamp();
        long maxTs = dataLoader.getBoardInfoMaximumTimestmp();
        
        return new MinMax(minTs, maxTs);
    }

    @Override
    public DataPushDataContainer loadDataSetFromLocalPersistence(int channel, MinMax minMaxTs) throws PersisterException {
        return new DataPushBoardInfoDataContainer(dataLoader.loadBoardInfo(minMaxTs.getMin(), minMaxTs.getMax()));
    }

    @Override
    public boolean sendDataToRemotePersistence(DataPushDataContainer dataContainer) throws PersisterException {
        List<BoardInfo> dataSet = dataContainer.getDataSet();
        
        return dataPersister.addBoardsInfo(dataSet);
    }

    @Override
    public DataPushDataContainer clearDataContainer() {
        return new DataPushBoardInfoDataContainer();
    }

    @Override
    public long getLatestTimestampInDataContainer(DataPushDataContainer dataContainer) {
        List<BoardInfo> dataSet = dataContainer.getDataSet();
        
        return (long)dataSet.get(dataSet.size()-1).timestamp + 1;
    }
}
