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
import airsenseur.dev.persisters.SampleLoader;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class LoaderSQL {
    
    private final static String DATABASE_NAME = "airsenseur.db";
    
    private final String folderPath;
    private SQLiteConnection db = null;
    
    private static final Logger log = LoggerFactory.getLogger(LoaderSQL.class);    

    public LoaderSQL() {
        this.folderPath = "";
    }

    public LoaderSQL(String folderPath) {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }
        
        this.folderPath = folderPath;
    }
    
    protected void connectToDb() throws PersisterException {
        
        releaseDb();
        
        String fullFileName = folderPath + DATABASE_NAME;
        db = new SQLiteConnection(new File(fullFileName));
        try {
            db.open(false);
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());            
            throw new PersisterException("Impossible to open the database at " + fullFileName);
        }
    }
    
    protected void releaseDb() {
        
        if (db != null) {
            db.dispose();
        }
    }    
    
    protected SQLiteConnection getDb() {
        return db;
    }
    
    protected long getMinimumTimestamp(String measurementTable, String timestampName, int channel) throws PersisterException {
        
        long timestamp = 0;
        
        SQLiteStatement st1 = null;
        try {
            String channelSql = "";
            if (channel != SampleLoader.CHANNEL_INVALID) {
                channelSql = " AND channel = ? ";
            }
            st1 = db.prepare("SELECT `"+ timestampName + "` "
                            + "FROM " + measurementTable + " " 
                            + "WHERE `" + timestampName + "` != 0 " + channelSql + " ORDER BY `" + timestampName + "` ASC LIMIT 1");
            
            if (channel != SampleLoader.CHANNEL_INVALID) {
                st1.bind(1, channel);
            }
            
            if (st1.step()) {
                timestamp = st1.columnLong(0);
            }
        } catch (SQLiteException ex) {
            log.error("Error loading minimum timestamp from " + measurementTable + " database");
            throw new PersisterException(ex.getMessage());
        } finally {
            if (st1 != null) {
                st1.dispose();
            }
        }
        
        return timestamp;
    }     
    
    protected long getMaximumTimestamp(String measurementTable, String timestampName, int channel) throws PersisterException {
        long timestamp = 0;
        
        SQLiteStatement st1 = null;
        try {
            String channelSql = "";
            if (channel != SampleLoader.CHANNEL_INVALID) {
                channelSql = " AND channel = ? ";
            }
            st1 = db.prepare("SELECT `" + timestampName + "` "
                            + "FROM " + measurementTable + " "
                            + "WHERE 1 " + channelSql +  "  ORDER BY " + timestampName + " DESC LIMIT 1");

            if (channel != SampleLoader.CHANNEL_INVALID) {
                st1.bind(1, channel);
            }
            
            if (st1.step()) {
                timestamp = st1.columnLong(0);
            }
        } catch (SQLiteException ex) {
            log.error("Error loading maximum timestamp from " + measurementTable + " database");
            throw new PersisterException(ex.getMessage());
        } finally {
            if (st1 != null) {
                st1.dispose();
            }
        }
        
        return timestamp;        
    }    
}
