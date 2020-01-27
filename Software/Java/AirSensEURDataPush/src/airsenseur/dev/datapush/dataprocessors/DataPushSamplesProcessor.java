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

import airsenseur.dev.datapush.Configuration;
import airsenseur.dev.datapush.MinMax;
import airsenseur.dev.datapush.datacontainers.DataPushDataContainer;
import airsenseur.dev.datapush.datacontainers.DataPushSamplesDataContainer;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SampleLoader;
import airsenseur.dev.persisters.SamplesPersister;
import java.util.List;

/**
 *
 * @author marco
 */
public class DataPushSamplesProcessor implements DataPushProcessor {
    
    private final Configuration.workingMode workingMode;
    private final SampleLoader sampleLoader;
    private final SamplesPersister samplePersister;

    public DataPushSamplesProcessor(Configuration.workingMode workingMode, SampleLoader sampleLoader, SamplesPersister samplePersister) {
        this.workingMode = workingMode;
        this.sampleLoader = sampleLoader;
        this.samplePersister = samplePersister;
    }

    @Override
    public String getPersisterMarker(int channel) {
        if (workingMode == workingMode.INFLUX) {
            return HistoryEventContainer.EVENT_LATEST_SAMPLEPUSH_TS;
        } else {
            return "" + channel;
        }
    }

    @Override
    public MinMax getMinMaxTxForChannel(int channel) throws PersisterException {
        long minTs = sampleLoader.getMinimumTimestamp(channel);
        long maxTs = sampleLoader.getMaximumTimestamp(channel);
        
        return new MinMax(minTs, maxTs);
    }

    @Override
    public DataPushDataContainer loadDataSetFromLocalPersistence(int channel, MinMax minMaxTs) throws PersisterException {
        return new DataPushSamplesDataContainer(sampleLoader.loadSamples(channel, minMaxTs.getMin(), minMaxTs.getMax()));
    }

    @Override
    public boolean sendDataToRemotePersistence(DataPushDataContainer dataContainer) throws PersisterException {
        List<SampleDataContainer> sampleDataList = dataContainer.getDataSet();
        
        return samplePersister.addSamples(sampleDataList);
    }

    @Override
    public DataPushDataContainer clearDataContainer() {
        return new DataPushSamplesDataContainer();
    }

    @Override
    public long getLatestTimestampInDataContainer(DataPushDataContainer dataContainer) {
        List<SampleDataContainer> sampleDataList = dataContainer.getDataSet();
        
        return (long)sampleDataList.get(sampleDataList.size()-1).getCollectedTimestamp() + 1;
    }
}
