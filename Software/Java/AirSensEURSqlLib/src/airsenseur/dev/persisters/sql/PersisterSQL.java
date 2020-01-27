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
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic SQLite persister
 * @author marco
 */
public abstract class PersisterSQL {
    
    private final static String DATABASE_NAME = "airsenseur.db";
    
    private final String folderPath;
    protected SQLiteConnection db = null;  
    
    private static final Logger log = LoggerFactory.getLogger(PersisterSQL.class);

    public PersisterSQL() {
        this.folderPath = "";
    }

    public PersisterSQL(String folderPath) {
        
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }
        
        this.folderPath = folderPath;
    }
    
    abstract protected void createTable() throws PersisterException;
    abstract protected void flushCachedData() throws PersisterException;

    protected void connectToDb() throws PersisterException {
        
        releaseDb();
        
        String fullFileName = folderPath + DATABASE_NAME;
        db = new SQLiteConnection(new File(fullFileName));
        try {
            db.open(true);
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());            
            throw new PersisterException("Impossible to open the database at " + fullFileName);
        }
        
        // Create the table if not exists
        createTable();        
    }    
    
    protected void createIndex(String tableName, String indexName, String indexComposition) throws PersisterException {

        String sqlTable = "CREATE INDEX IF NOT EXISTS " + indexName + 
                            " ON `" + tableName + "` " + indexComposition + "; ";
        
        SQLiteStatement st0 = null;
        try {
            st0 = db.prepare(sqlTable);
            st0.step();
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());
            throw new PersisterException("Error creating SQL index " + indexName + " for " + tableName);
        } finally {
            if (st0 != null) {
                st0.dispose();
            }
        }
    }
    
    protected void releaseDb() {
        
        if (db != null) {
            db.dispose();
        }
    }    
    
    // Returns true if at least a 1st connection has been already performed
    protected boolean checkConnection() {
        return (db != null);
    }
    
}
