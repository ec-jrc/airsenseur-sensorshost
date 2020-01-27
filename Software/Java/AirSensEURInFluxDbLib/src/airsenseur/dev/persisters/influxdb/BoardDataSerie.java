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

/**
 * Implements a series specific for board information data container
 * @author marco
 */
public class BoardDataSerie extends Serie {
    
    // We use those offset to access to some specific tokens
    // when generating the line protocol for influxdb serialization.
    // Keep the offset synchronized with the columns contents when
    // changing this class implementation.
    private final static int TIME_TOKEN_OFFSET = 0;
    private final static int BOARDID_TOKEN_OFFSET = 1;
    private final static int BOARDTYPE_TOKEN_OFFSET = 2;
    
    public BoardDataSerie(String name) {
        super(name);
        
        getColumns().add("time");
        getColumns().add("boardid");
        getColumns().add("boardtype");
        getColumns().add("firmwarerev");
        getColumns().add("serialnumber");
    }    
    
    public void addBoardInfoData(BoardInfo boardInfo) {
        Point point = new Point();
        getPoints().add(point);

        point.add(boardInfo.timestamp);
        point.add(boardInfo.boardId);
        point.add(boardInfo.boardType);
        point.add(encloseInQuotation(boardInfo.fwRevision));
        point.add(encloseInQuotation(boardInfo.serial));
    }

    @Override
    public String toLineProtocol() throws PersisterException {
        
        // We expect to have time, boardid and boardtype to these specified 
        // offsets in the column list. If not, raise an error
        if (!getColumns().get(TIME_TOKEN_OFFSET).equalsIgnoreCase("time")) {
            throw new PersisterException("Invalid time token found in board info data serie. Please alert the development team");
        }
        if (!getColumns().get(BOARDID_TOKEN_OFFSET).equalsIgnoreCase("boardid")) {
            throw new PersisterException("Invalid board id token found in board info data serie. Please alert the development team");
        }
        if (!getColumns().get(BOARDTYPE_TOKEN_OFFSET).equalsIgnoreCase("boardtype")) {
            throw new PersisterException("Invalid board type token found in board info data serie. Please alert the development team");
        }
        
        StringBuilder sb = new StringBuilder();
        for (Point point:getPoints()) {
            
            // Get the time value
            Object timeVal = point.get(TIME_TOKEN_OFFSET);

            // Measurement 
            sb.append(getName()).append(",");
            
            // - Generate the boardid string -
            // Tag: boardid
            sb.append(getColumns().get(BOARDID_TOKEN_OFFSET)).append("=").append(point.get(BOARDID_TOKEN_OFFSET)).append(",");
            
            // Tag: boardtype
            sb.append(getColumns().get(BOARDTYPE_TOKEN_OFFSET)).append("=").append(point.get(BOARDTYPE_TOKEN_OFFSET)).append(" ");
            
            // Values:
            for (int n = 0; n < getColumns().size(); n++) {

                // Skip known tokens
                if ((n == TIME_TOKEN_OFFSET) || (n == BOARDID_TOKEN_OFFSET) || (n == BOARDTYPE_TOKEN_OFFSET)) {
                    continue;
                }

                sb.append(getColumns().get(n)).append("=").append(point.get(n));
                if (n != getColumns().size()-1) {
                    sb.append(",");
                }
            }

            // Timestamp
            sb.append(" ").append(timeVal).append("\n");
        }        
        
        return sb.toString();
    }
    
}
