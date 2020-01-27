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
import airsenseur.dev.json.BoardInfo;
import java.util.List;

/**
 * Implements a board information persister through influxDB
 * @author marco
 */
public class BoardPersisterInfluxDB extends PersiterInfluxDB {
    
    public BoardPersisterInfluxDB(String dataSetName, String dbHost, int dbPort, String dbName, String dbUser, String dbPassword, boolean useLineProtocol, boolean useSSL, int timeout) {
        super(dataSetName, dbHost, dbPort, dbName, dbUser, dbPassword, useLineProtocol, useSSL, timeout);
    }
    
    
    public boolean addBoardsInfo(List<BoardInfo> boards) throws PersisterException {
        
        if (boards == null) {
            throw new PersisterException(("Invalid parameter on addBoardsInfo"));
        }
        
        BoardDataSerie serie = new BoardDataSerie(getDataSetName());
        for (BoardInfo board : boards) {
            serie.addBoardInfoData(board);
        }
        
        Series series = new Series();
        series.add(serie);
        
        boolean bResult = sendDataToInfluxDB(series);
        
        return bResult;
    }
    
}
