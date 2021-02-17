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
import airsenseur.dev.json.JsonFieldsUtils;
import airsenseur.dev.json.SensorConfig;
import airsenseur.dev.persisters.SampleAndConfigurationLoader;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class SensorConfigLoaderSQL extends SampleLoaderSQL implements SampleAndConfigurationLoader{
    
    private static final Logger log = LoggerFactory.getLogger(SampleLoaderSQL.class);
    
    private static final String SENSORS_TABLE_NAME = "sensors";
    private static final String BOARDS_TABLE_NAME = "boards";
    
    private final static String TIMESTAMP_COLUMN_NAME = "timestamp";

    public SensorConfigLoaderSQL() {
        super();
    }
    
    public SensorConfigLoaderSQL(String folderPath) {
        super(folderPath);
    }
    
    @Override
    public long getSensorInfoMinimumTimestamp() throws PersisterException {
        return getMinimumTimestamp(SENSORS_TABLE_NAME, TIMESTAMP_COLUMN_NAME, CHANNEL_INVALID);
    }
    
    @Override
    public long getSensorInfoMaximumTimestamp() throws PersisterException {
        return getMaximumTimestamp(SENSORS_TABLE_NAME, TIMESTAMP_COLUMN_NAME, CHANNEL_INVALID);
    }
    
    @Override
    public long getBoardInfoMinimumTimestamp() throws PersisterException {
        return getMinimumTimestamp(BOARDS_TABLE_NAME, TIMESTAMP_COLUMN_NAME, CHANNEL_INVALID);
    }
    
    @Override
    public long getBoardInfoMaximumTimestmp() throws PersisterException {
        return getMaximumTimestamp(BOARDS_TABLE_NAME, TIMESTAMP_COLUMN_NAME, CHANNEL_INVALID);
    }
    
    @Override
    public List<BoardInfo> loadBoardInfo(long firstTimeStamp, long lastTimeStamp) throws PersisterException {
        
        List<BoardInfo> result = new ArrayList<>();
        SQLiteStatement st1 = null;
        try {
            
            //                              0               1           2            3           4           
            st1 = getDb().prepare("SELECT `timestamp`, `boardId`, `boardType`, `boardFirmware`, `boardSerial` "
                            + "FROM " + BOARDS_TABLE_NAME +  " "
                            + "WHERE " + TIMESTAMP_COLUMN_NAME + " >= ? AND " + TIMESTAMP_COLUMN_NAME + "  < ?  ORDER BY " + TIMESTAMP_COLUMN_NAME + "  ASC");
            
            st1.bind(1, firstTimeStamp);
            st1.bind(2, lastTimeStamp);
            
            while (st1.step()) {
                long timestamp = st1.columnLong(0);
                int boardId = st1.columnInt(1);
                String boardType = st1.columnString(2);
                String boardFirmware = st1.columnString(3);
                String boardSerial = st1.columnString(4);
                
                BoardInfo data = JsonFieldsUtils.safeCheck(new BoardInfo(boardId, timestamp, boardType, boardFirmware, boardSerial));
                
                
                result.add(data);
            }
            
        } catch (SQLiteException ex) {
            log.error("Error loading board information from database");
            throw new PersisterException(ex.getMessage());
        } finally {
            if (st1 != null) {
                st1.dispose();
            }
        }
        
        return result;        
    }
    
    @Override
    public List<SensorConfig> loadSensorInfo(long firstTimeStamp, long lastTimeStamp) throws PersisterException {
        
        List<SensorConfig> result = new ArrayList<>();
        SQLiteStatement st1 = null;
        try {
            
            //                              0               1           2            3           4         5            6          7
            st1 = getDb().prepare("SELECT `timestamp`, `channel`, `boardId`, `channelName`, `serialId`, `unit`, `samplePeriod`, `enabled` "
                            + "FROM " + SENSORS_TABLE_NAME +  " "
                            + "WHERE " + TIMESTAMP_COLUMN_NAME + " >= ? AND " + TIMESTAMP_COLUMN_NAME + "  < ?  ORDER BY " + TIMESTAMP_COLUMN_NAME + "  ASC");
            
            st1.bind(1, firstTimeStamp);
            st1.bind(2, lastTimeStamp);
            
            while (st1.step()) {
                long timestamp = st1.columnLong(0);
                int channel = st1.columnInt(1);
                int boardId = st1.columnInt(2);
                String channelName = st1.columnString(3);
                String serialId = st1.columnString(4);
                String units = st1.columnString(5);
                int samplePeriod = st1.columnInt(6);
                int enabled = st1.columnInt(7);
                
                SensorConfig data = JsonFieldsUtils.safeCheck(new SensorConfig(channelName, serialId, units, channel, boardId, samplePeriod, timestamp, (enabled != 0)));
                
                result.add(data);
            }
            
        } catch (SQLiteException ex) {
            log.error("Error loading sensor information from database");
            throw new PersisterException(ex.getMessage());
        } finally {
            if (st1 != null) {
                st1.dispose();
            }
        }
        
        return result;        
    }    
}
