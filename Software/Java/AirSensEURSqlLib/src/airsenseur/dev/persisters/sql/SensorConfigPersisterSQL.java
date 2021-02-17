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
import airsenseur.dev.json.BoardInfo;
import airsenseur.dev.json.SensorConfig;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements an SQLite based persisted sample and sensor configuration dataset entry point
 * @author marco
 */
public class SensorConfigPersisterSQL extends SamplePersisterSQL {
    
    private static final Logger log = LoggerFactory.getLogger(SensorConfigPersisterSQL.class);
    
    private static final String SENSORS_TABLE_NAME = "sensors";
    private static final String BOARDS_TABLE_NAME = "boards";
    
    private final List<SensorConfig> sensorsConfig = new ArrayList<>();
    private final List<BoardInfo> boardsInfo = new ArrayList<>();

    public SensorConfigPersisterSQL(String folderPath) {
        super(folderPath);
    }

    @Override
    protected void connectToDb() throws PersisterException {
        super.connectToDb();
        
        // The registry table (for sensors information)
        createSensorsRegistryTable();
        
        // The registry table (for boards information)
        createBoardsRegistryTable();
    }
    
    private void createSensorsRegistryTable() throws PersisterException {

        String sqlTable = "CREATE TABLE IF NOT EXISTS `" + SENSORS_TABLE_NAME + "` (" +
                            "`timestamp` INT NOT NULL, " +
                            "`channel` INT NOT NULL, " +
                            "`boardId` INT NOT NULL, " +                
                            "`channelName` VARCHAR(255) NOT NULL, " +
                            "`serialId` VARCHAR(255) NOT NULL, " +
                            "`unit` VARCHAR(255) NOT NULL, " +
                            "`samplePeriod` INT NOT NULL, " + 
                            "`enabled` INT NOT NULL " + 
                            ") ";
        
        SQLiteStatement st0 = null;
        try {
            st0 = db.prepare(sqlTable);
            st0.step();
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());
            throw new PersisterException("Error creating SQL table for sensor information");
        } finally {
            if (st0 != null) {
                st0.dispose();
            }
        }
        
        createIndex(SENSORS_TABLE_NAME, "tst_index", "(timestamp)");
        createIndex(SENSORS_TABLE_NAME, "tst_chn_index", "(timestamp,channel)");
    }    
    
    private void createBoardsRegistryTable() throws PersisterException {

        String sqlTable = "CREATE TABLE IF NOT EXISTS `" + BOARDS_TABLE_NAME + "` (" +
                            "`timestamp` INT NOT NULL, " +
                            "`boardId` INT NOT NULL, " +                
                            "`boardType` VARCHAR(255) NOT NULL, " +
                            "`boardFirmware` VARCHAR(255) NOT NULL, " +
                            "`boardSerial` VARCHAR(255) NOT NULL " +                
                            ") ";
        
        SQLiteStatement st0 = null;
        try {
            st0 = db.prepare(sqlTable);
            st0.step();
        } catch (SQLiteException ex) {
            log.error(ex.getMessage());
            throw new PersisterException("Error creating SQL table for board information");
        } finally {
            if (st0 != null) {
                st0.dispose();
            }
        }
        
        createIndex(BOARDS_TABLE_NAME, "tst_index", "(timestamp)");
        createIndex(BOARDS_TABLE_NAME, "tst_index_boardId", "(timestamp,boardId)");
    }        

    public void insertSensorsConfig(List<SensorConfig> sensorsConfig) {
        this.sensorsConfig.addAll(sensorsConfig);
    }
    
    public void insertBoardsInfo(List<BoardInfo> boardsInfo) {
        this.boardsInfo.addAll(boardsInfo);
    }
    
    @Override
    protected void flushCachedData() {
        super.flushCachedData();
        
        flushSensorsConfig();
        flushBoardsInfo();
    }

        
    private void flushSensorsConfig() {
        
        // Nothing to flush
        if (sensorsConfig.isEmpty()) {
            return;
        }
        
        SQLiteStatement st1 = null;
        try {
            db.exec("BEGIN TRANSACTION;");
                        

            st1 = db.prepare("INSERT INTO " + SENSORS_TABLE_NAME 
                    + "       (`timestamp`, `channel`, `boardId`, `channelName`, `serialId`, `unit`, `samplePeriod`, `enabled` ) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);", true);

            for (SensorConfig sensorConfig : sensorsConfig) {

                st1.bind(1, sensorConfig.startSamplingTimestamp);
                st1.bind(2, sensorConfig.sensorId);
                st1.bind(3, sensorConfig.boardId);
                st1.bind(4, sensorConfig.name);
                st1.bind(5, sensorConfig.serial);
                st1.bind(6, sensorConfig.measurementUnits);
                st1.bind(7, sensorConfig.samplingPeriod);
                st1.bind(8, (sensorConfig.enabled)? 1 : 0);

                st1.step();

                st1.reset();
            }

            db.exec("COMMIT TRANSACTION;");
            
            sensorsConfig.clear();

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
    
    private void flushBoardsInfo() {
        
        // Nothing to flush
        if (boardsInfo.isEmpty()) {
            return;
        }
        
        SQLiteStatement st1 = null;
        try {
            db.exec("BEGIN TRANSACTION;");
                        
            st1 = db.prepare("INSERT INTO " + BOARDS_TABLE_NAME 
                    + "       (`timestamp`, `boardId`, `boardType`, `boardFirmware`, `boardSerial`) "
                    + "VALUES (?, ?, ?, ?, ?);", true);

            for (BoardInfo boardInfo:boardsInfo) {

                st1.bind(1, boardInfo.timestamp);
                st1.bind(2, boardInfo.boardId);
                st1.bind(3, boardInfo.boardType);
                st1.bind(4, boardInfo.fwRevision);
                st1.bind(5, boardInfo.serial);

                st1.step();

                st1.reset();
            }

            db.exec("COMMIT TRANSACTION;");
            
            boardsInfo.clear();

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


