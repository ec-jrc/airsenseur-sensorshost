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

package airsenseur.dev.persisters.influxdb;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SamplesPersister;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a sample persister through Influx DB
 * @author marco
 */
public class SamplePersisterInfluxDB extends PersiterInfluxDB implements SamplesPersister {
    
    private final Logger log = LoggerFactory.getLogger(SamplePersisterInfluxDB.class);
    
    public SamplePersisterInfluxDB(String dataSetName, String dbHost, int dbPort, String dbName, String dbUser, String dbPassword, boolean useLineProtocol, boolean useSSL, int timeout) {
        super(dataSetName, dbHost, dbPort, dbName, dbUser, dbPassword, useLineProtocol, useSSL, timeout);
    }
    
    @Override
    public boolean startNewLog() throws PersisterException {
        
        // Nothing to do. We suppose to start a new connection each time is required
        log.info("SamplePeristerInfluxDB enabled");
        return true;
    }

    @Override
    public void stop() {
        log.info("SamplePersisterInfluxDB stopped");
    }

    /**
     * Push a single sample to the influxDB server
     * @param sample
     * @return
     * @throws PersisterException 
     */
    @Override
    public boolean addSample(SampleDataContainer sample) throws PersisterException {
        
        SampleDataSerie serie = new SampleDataSerie(getDataSetName());
        serie.addSampleData(sample);
        
        Series series = new Series();
        series.add(serie);
        
        boolean bResult = sendDataToInfluxDB(series);
        
        return bResult;
    }
    
    /**
     * Push several samples to the influxDB server
     * Samples are pushed together in a single connect, thus reducing
     * the transferred data
     * @param samples
     * @return
     * @throws PersisterException 
     */
    @Override
    public boolean addSamples(List<SampleDataContainer> samples) throws PersisterException {
        
        if (samples == null) {
            throw new PersisterException(("Invalid parameter on addSamples"));
        }
        
        SampleDataSerie serie = new SampleDataSerie(getDataSetName());
        for (SampleDataContainer sample:samples) {
            serie.addSampleData(sample);
        }
        
        Series series = new Series();
        series.add(serie);
        
        boolean bResult = sendDataToInfluxDB(series);
        
        return bResult;
    }

    @Override
    public String getPersisterMarker(int channel) {
        return HistoryEventContainer.EVENT_LATEST_INFLUXDB_SAMPLEPUSH_TS;
    }
}
