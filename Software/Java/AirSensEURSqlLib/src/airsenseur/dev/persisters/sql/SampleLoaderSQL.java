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

package airsenseur.dev.persisters.sql;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SampleLoader;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load samples from an SQL database
 * @author marco
 */
public class SampleLoaderSQL extends LoaderSQL implements SampleLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(SampleLoaderSQL.class);
    
    private final static String SAMPLES_TABLE_NAME = "measures"; 
    private final static String TIMESTAMP_COLUMN_NAME = "collectedts";

    public SampleLoaderSQL() {
        super();
    }

    public SampleLoaderSQL(String folderPath) {
        super(folderPath);
    }

    @Override
    public boolean supportChannels() {
        return true;
    }

    @Override
    public boolean openLog() throws PersisterException {
        connectToDb();
        return true;
    }

    @Override
    public void stop() {
        releaseDb();
    }

    @Override
    public long getMinimumTimestamp(int channel) throws PersisterException {
        return getMinimumTimestamp(SAMPLES_TABLE_NAME, TIMESTAMP_COLUMN_NAME, channel);
    }

    @Override
    public long getMaximumTimestamp(int channel) throws PersisterException {
        return getMaximumTimestamp(SAMPLES_TABLE_NAME, TIMESTAMP_COLUMN_NAME, channel);
    }
    
    @Override
    public List<SampleDataContainer> loadSamples(int channel, long firstTimeStamp, long lastTimeStamp) throws PersisterException {

        List<SampleDataContainer> result = new ArrayList<>();
        SQLiteStatement st1 = null;
        try {
            String channelSql = "";
            if (channel != SampleLoader.CHANNEL_INVALID) {
                channelSql = " AND channel = ? ";
            }
            
            //                          0       1           2            3           4               5               6              7               8               9          10
            st1 = getDb().prepare("SELECT `value`, `evvalue`, `timestamp`, `channel`, `channelName`, `gpstimestamp`, `gpslatitude`, `gpslongitude`, `gpsaltitude`, `collectedts`, `calibrated` "
                            + "FROM " + SAMPLES_TABLE_NAME + " " 
                            + "WHERE collectedts >= ? AND collectedts < ? " + channelSql +  " ORDER BY collectedts ASC");
            
            st1.bind(1, firstTimeStamp);
            st1.bind(2, lastTimeStamp);
            
            if (channel != SampleLoader.CHANNEL_INVALID) {
                st1.bind(3, channel);
            }
            
            while (st1.step()) {
                int value = st1.columnInt(0);
                double evvalue = st1.columnDouble(1);
                int sensorTimeStamp = st1.columnInt(2);
                int sensorChannel = st1.columnInt(3);
                String name = st1.columnString(4);
                double gpsTimeStamp = st1.columnDouble(5);
                double gpsLatitude = st1.columnDouble(6);
                double gpsLongitude = st1.columnDouble(7);
                double gpsAltitude = st1.columnDouble(8);
                long collectedTs = st1.columnLong(9);
                double calibratedVal = st1.columnDouble(10);
                
                SampleDataContainer data = new SampleDataContainer(sensorChannel);
                data.setName(name);
                data.updateSample(value, evvalue, sensorTimeStamp);
                data.updateGPSValues(gpsTimeStamp, gpsLatitude, gpsLongitude, gpsAltitude);
                data.setCollectedTimestamp(collectedTs);
                data.setCalibratedVal(calibratedVal);
                
                result.add(data);
            }
            
        } catch (SQLiteException ex) {
            LOG.error("Error loading samples from database");
            throw new PersisterException(ex.getMessage());
        } finally {
            if (st1 != null) {
                st1.dispose();
            }
        }
        
        return result;
    }

    @Override
    public List<SampleDataContainer> loadSamples(int channel, long firstTimeStamp, long lastTimeStamp, long averageTime) throws PersisterException {
        
        List<SampleDataContainer> result = new ArrayList<>();
        SQLiteStatement st1 = null;
        try {
            String channelSql = "";
            if (channel != SampleLoader.CHANNEL_INVALID) {
                channelSql = " AND channel = ? ";
            }
            
            st1 = getDb().prepare("SELECT AVG(`value`), AVG(`evvalue`), MAX(`timestamp`), `channel`, `channelName`, AVG(`gpstimestamp`), AVG(`gpslatitude`), AVG(`gpslongitude`), AVG(`gpsaltitude`), MAX(`collectedts`), AVG(`calibrated`) "
                            + "FROM " + SAMPLES_TABLE_NAME + " " 
                            + "WHERE collectedts >= ? AND collectedts < ? " + channelSql +  "GROUP BY channel ORDER BY collectedts ASC");
            
            st1.bind(1, firstTimeStamp);
            st1.bind(2, lastTimeStamp);
            
            if (channel != SampleLoader.CHANNEL_INVALID) {
                st1.bind(3, channel);
            }
            
            while (st1.step()) {
                int value = st1.columnInt(0);
                double evvalue = st1.columnDouble(1);
                int sensorTimeStamp = st1.columnInt(2);
                int sensorChannel = st1.columnInt(3);
                String name = st1.columnString(4);
                double gpsTimeStamp = st1.columnDouble(5);
                double gpsLatitude = st1.columnDouble(6);
                double gpsLongitude = st1.columnDouble(7);
                double gpsAltitude = st1.columnDouble(8);
                long collectedTs = st1.columnLong(9);
                double calibratedVal = st1.columnDouble(10);
                
                SampleDataContainer data = new SampleDataContainer(sensorChannel);
                data.setName(name);
                data.updateSample(value, evvalue, sensorTimeStamp);
                data.updateGPSValues(gpsTimeStamp, gpsLatitude, gpsLongitude, gpsAltitude);
                data.setCollectedTimestamp(collectedTs);
                data.setCalibratedVal(calibratedVal);
                
                result.add(data);
            }
            
        } catch (SQLiteException ex) {
            LOG.error("Error loading samples from database");
            throw new PersisterException(ex.getMessage());
        } finally {
            if (st1 != null) {
                st1.dispose();
            }
        }
        
        return result;        
    }
}
