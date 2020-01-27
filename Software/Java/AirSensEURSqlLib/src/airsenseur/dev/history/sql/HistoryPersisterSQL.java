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

package airsenseur.dev.history.sql;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.history.HistoryPersister;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * History persister through SQL Lite engine
 * @author marco
 */
public class HistoryPersisterSQL implements HistoryPersister {
    
    private final static String DATABASE_NAME = "history.db";
    
    private final String folderPath;
    private SQLiteConnection db = null;
    
    private static final Logger log = LoggerFactory.getLogger(HistoryPersisterSQL.class);

    public HistoryPersisterSQL() {
        this.folderPath = "";
    }

    public HistoryPersisterSQL(String folderPath) {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }
        
        this.folderPath = folderPath;
    }

    @Override
    public boolean openLog(boolean read) throws PersisterException {
        connectToDb();
        return true;
    }

    @Override
    public void closeLog() {
        releaseDb();
    }

    @Override
    public boolean saveEvent(HistoryEventContainer event) throws PersisterException {
        
        SQLiteStatement st1 = null;

        try {
            st1 = db.prepare("INSERT INTO history"
                    + "       (`name`, `value`)"
                    + "VALUES (?, ?);", true);
            
            st1.bind(1, event.getEventName());
            st1.bind(2, event.getEventValue());
            
            st1.step();
        
        } catch (SQLiteException ex) {
            log.error("SQL History insert error." + ex.getMessage());
            throw new PersisterException("SQL History insert error." + ex.getMessage());
        } finally {
            if(st1 != null) {
                st1.dispose();
            }
        }
        
        return true;
    }

    @Override
    public HistoryEventContainer loadEvent(String eventName) throws PersisterException {
        
        HistoryEventContainer result = null;
        SQLiteStatement st1 = null;
        try {
            //                          0         1      2
            st1 = db.prepare("SELECT `name`, `value`, `timestamp` "
                            + "FROM history "
                            + "WHERE name = ? ORDER BY timestamp DESC LIMIT 1");
            
            st1.bind(1, eventName);
            
            if (st1.step()) {
                String name = st1.columnString(0);
                String eventValue = st1.columnString(1);
                result = new HistoryEventContainer(name, eventValue);
            }
        } catch (SQLiteException ex) {
            log.error("Error loading history event from database");
            throw new PersisterException(ex.getMessage());
        } finally {
            if (st1 != null) {
                st1.dispose();
            }
        }
        
        return result;
    }
    
    private void connectToDb() throws PersisterException {
        
        releaseDb();
        
        String fullFileName = folderPath + DATABASE_NAME;
        db = new SQLiteConnection(new File(fullFileName));
        try {
            db.open();
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());            
            throw new PersisterException("Impossible to open the database at " + fullFileName);
        }
        
        createTable();
        createIndex();
    }
    
    private void releaseDb() {
        
        if (db != null) {
            db.dispose();
        }
    }
    
    private void createTable() throws PersisterException {

        String sqlTable = "CREATE TABLE IF NOT EXISTS `history` (" +
                            "`name` VARCHAR(255) NOT NULL, " +
                            "`value` VARCHAR(255) NOT NULL, " +
                            "`timestamp` DATE DEFAULT (datetime('now','localtime'))" +
                            ") ";
        
        SQLiteStatement st0 = null;
        try {
            st0 = db.prepare(sqlTable);
            st0.step();
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());
            throw new PersisterException("Error creating SQL table for history");
        } finally {
            if (st0 != null) {
                st0.dispose();
            }
        }
    }    
    
    private void createIndex() throws PersisterException {

        String sqlTable = "CREATE INDEX IF NOT EXISTS tstname_index ON `history` (timestamp,name); ";
        
        SQLiteStatement st0 = null;
        try {
            st0 = db.prepare(sqlTable);
            st0.step();
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());
            throw new PersisterException("Error creating SQL index for history");
        } finally {
            if (st0 != null) {
                st0.dispose();
            }
        }
    }    
}
