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
import airsenseur.dev.persisters.SamplesPersister;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements an SQLite based persisted sample dataset entry point
 * @author marco
 */
public class SamplePersisterSQL extends PersisterSQL implements SamplesPersister {
    
    private final static int MAX_RETRY_BEFORE_ERROR = 100;
    private final static int MAX_QUEUED_DATA_BEFORE_CLEARING = (MAX_RETRY_BEFORE_ERROR * 10);
    
    private final static String SAMPLES_TABLE_NAME = "measures";
    
    private final List<SampleDataContainer> sampleQueue = new ArrayList<>();
    
    private static final Logger log = LoggerFactory.getLogger(SamplePersisterSQL.class);

    public SamplePersisterSQL() {
        super();
    }

    public SamplePersisterSQL(String folderPath) {
        super(folderPath);
    }

    @Override
    public boolean startNewLog() throws PersisterException {
        
        connectToDb();
        return true;
    }

    @Override
    public void stop() {
        
        releaseDb();
    }

    @Override
    public boolean addSample(SampleDataContainer sample) throws PersisterException {

        queueSample(sample);
        if (!checkConnection()) {
            return false;
        }
        
        flushCachedData();
                    
        return (sampleQueue.size() < MAX_RETRY_BEFORE_ERROR);
    }

    @Override
    public boolean addSamples(List<SampleDataContainer> samples) throws PersisterException {
        
        // DB Never connected
        if (!checkConnection()) {
            return false;
        }
        
        for (SampleDataContainer sample:samples) {
            queueSample(sample);
        }
        
        flushCachedData();
        
        return sampleQueue.size() < MAX_RETRY_BEFORE_ERROR;
    }
    
 
    @Override
    protected void createTable() throws PersisterException {

        String sqlTable = "CREATE TABLE IF NOT EXISTS `" + SAMPLES_TABLE_NAME + "` (" +
                            "`value` INT NOT NULL, " +
                            "`evvalue` REAL NOT NULL, " +
                            "`timestamp` INT NOT NULL, " +
                            "`channel` INT NOT NULL, " +
                            "`channelName` VARCHAR(255) NOT NULL, " +
                            "`gpstimestamp` REAL NOT NULL, " +
                            "`gpslatitude` REAL NOT NULL, " +
                            "`gpslongitude` REAL NOT NULL, " +
                            "`gpsaltitude` REAL NOT NULL," +
                            "`collectedts` INT NOT NULL, " +
                            "`calibrated` REAL NOT NULL " +
                            ") ";
        
        SQLiteStatement st0 = null;
        try {
            st0 = db.prepare(sqlTable);
            st0.step();
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());
            throw new PersisterException("Error creating SQL table for samples");
        } finally {
            if (st0 != null) {
                st0.dispose();
            }
        }
        
        createIndex(SAMPLES_TABLE_NAME, "clttst_index", "(collectedts)");
        createIndex(SAMPLES_TABLE_NAME, "clttst_chn_index", "(collectedts,channel)");
    }
    
    protected void purgeCachedData() {
        
        if(sampleQueue.size() >= MAX_QUEUED_DATA_BEFORE_CLEARING) {
            sampleQueue.clear();
        }
    }
    
    private void queueSample(SampleDataContainer sample) throws PersisterException {
        
        try {
            sampleQueue.add(sample.clone());
        } catch (CloneNotSupportedException e) {
            throw new PersisterException("Sample.clone not supported: " + e.getMessage());
        }
    }
        
    @Override
    protected void flushCachedData() {
                
        // Nothing to flush
        if (sampleQueue.isEmpty()) {
            return;
        }
        
        SQLiteStatement st1 = null;
        try {
            db.exec("BEGIN TRANSACTION;");

            st1 = db.prepare("INSERT INTO " + SAMPLES_TABLE_NAME 
                    + "       (`value`, `evvalue`, `timestamp`, `channel`, `channelName`, `gpstimestamp`, `gpslatitude`, `gpslongitude`, `gpsaltitude`, `collectedts`, `calibrated`) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", true);

            for (SampleDataContainer sample:sampleQueue) {

                st1.bind(1, sample.getSampleVal());
                st1.bind(2, sample.getSampleEvaluatedVal());
                st1.bind(3, sample.getTimeStamp());
                st1.bind(4, sample.getChannel());
                st1.bind(5, sample.getName());
                st1.bind(6, sample.getGpsTimestamp());
                st1.bind(7, sample.getLatitude());
                st1.bind(8, sample.getLongitude());
                st1.bind(9, sample.getAltitude());
                st1.bind(10, sample.getCollectedTimestamp());
                st1.bind(11, sample.getCalibratedVal());

                st1.step();

                st1.reset();
            }

            db.exec("COMMIT TRANSACTION;");

            sampleQueue.clear();

        } catch (SQLiteException ex) {
            try {
                db.exec("ROLLBACK TRANSACTION");
                log.error("SQL Task commit error." + ex.getMessage());
                
                // Let's try to remove the cached data to see if the problem
                // we arise is related to the dataset
                purgeCachedData();
                
            } catch (SQLiteException ex1) {
            }
        } finally {
            if(st1 != null) {
                st1.dispose();
            }
        }
    }
}
