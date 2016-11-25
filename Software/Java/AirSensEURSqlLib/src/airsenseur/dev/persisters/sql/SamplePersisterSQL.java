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
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements an SQLite based persisted sample dataset entry point
 * @author marco
 */
public class SamplePersisterSQL implements SamplesPersister {
    
    private final static String DATABASE_NAME = "airsenseur.db";
    private final static int MAX_RETRY_BEFORE_ERROR = 10;
    private final static int MAX_QUEUED_DATA_BEFORE_CLEARING = (MAX_RETRY_BEFORE_ERROR * 10);
    
    private final String folderPath;
    
    private final List<SampleDataContainer> sampleQueue = new ArrayList<>();
    private SQLiteConnection db = null;
    
    private static final Logger log = LoggerFactory.getLogger(SamplePersisterSQL.class);

    public SamplePersisterSQL() {
        this.folderPath = "";
    }

    public SamplePersisterSQL(String folderPath) {
        
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }
        
        this.folderPath = folderPath;
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
        commitPendingData();
        
        return sampleQueue.size() < MAX_RETRY_BEFORE_ERROR;
    }

    @Override
    public boolean addSamples(List<SampleDataContainer> samples) throws PersisterException {
        for (SampleDataContainer sample:samples) {
            queueSample(sample);
        }
        
        commitPendingData();
        return sampleQueue.size() < MAX_RETRY_BEFORE_ERROR;
    }
    
 
    private void connectToDb() throws PersisterException {
        
        releaseDb();
        
        String fullFileName = folderPath + DATABASE_NAME;
        db = new SQLiteConnection(new File(fullFileName));
        try {
            db.open(true);
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());            
            throw new PersisterException("Impossible to open the database at " + fullFileName);
        }
        
        // I know: this could loose queued data if we're trying to reconnect
        // to a database because it was locked by an external process. But could prevent problems
        // if queued data are partially broken and we're no more able to commit them due
        // to a sql exception
        if(sampleQueue.size() >= MAX_QUEUED_DATA_BEFORE_CLEARING) {
            sampleQueue.clear();
        }
        
        // Create the table if not exists
        createTable();
        createIndex("clttst_index", "(collectedts)");
        createIndex("clttst_chn_index", "(collectedts,channel)");
    }
    
    private void createTable() throws PersisterException {

        String sqlTable = "CREATE TABLE IF NOT EXISTS `measures` (" +
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
    }
    private void createIndex(String indexName, String indexComposition) throws PersisterException {

        String sqlTable = "CREATE INDEX IF NOT EXISTS " + indexName + 
                            " ON `measures` " + indexComposition + "; ";
        
        SQLiteStatement st0 = null;
        try {
            st0 = db.prepare(sqlTable);
            st0.step();
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());
            throw new PersisterException("Error creating SQL index for samples");
        } finally {
            if (st0 != null) {
                st0.dispose();
            }
        }
    }
    
    private void releaseDb() {
        
        if (db != null) {
            db.dispose();
        }
    }
    
    private void queueSample(SampleDataContainer sample) {
        
        sampleQueue.add(sample.clone());
    }
    
    private void commitPendingData() {
        
        SQLiteStatement st1 = null;
        try {
            db.exec("BEGIN TRANSACTION;");

            st1 = db.prepare("INSERT INTO measures"
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
            } catch (SQLiteException ex1) {
            }
        } finally {
            if(st1 != null) {
                st1.dispose();
            }
        }
    }
}
