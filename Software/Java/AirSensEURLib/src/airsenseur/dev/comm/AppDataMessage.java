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

package airsenseur.dev.comm;


/**
 * AppDataMessage implements a message flowing through the interface between Application and SensorBus
 * @author marco
 */
public class AppDataMessage {

    public static final int BOARD_ID_UNDEFINED = 0xFF;

    public static final int MIN_VALID_BOARD_ID_ON_SBUS = 0x00;
    public static final int MAX_VALID_BOARD_ID_ON_SBUS = 0x0F;

    private int boardId = BOARD_ID_UNDEFINED;
    private String commandString = "";
    private String commandComment = "";
    
    private AppDataMessage() {
    }

    public AppDataMessage(String command) {
        this.commandString = command;
    }

    public AppDataMessage(String command, String comment) {
        this.commandString = command;
        this.commandComment = comment;
    }
    
    public AppDataMessage(int boardId, String command) {
        this.boardId = boardId;
        this.commandString = command;
    }

    public AppDataMessage(int boardId, String command, String comment) {
        this.boardId = boardId;
        this.commandString = command;
        this.commandComment = comment;
    }
    
    @Override
    public AppDataMessage clone() {
        return new AppDataMessage(boardId, commandString, commandComment);
    }

    public int getBoardId() {
        return boardId;
    }

    public String getCommandComment() {
        return commandComment;
    }

    public String getCommandString() {
        return commandString;
    }
    
    public void setCommandString(String commandString) {
        this.commandString = commandString;
    }

    public boolean matches(int boardId, char commandId) {
        
        
        if ((boardId != this.boardId) && !((boardId == BOARD_ID_UNDEFINED) || (this.boardId == BOARD_ID_UNDEFINED))) {
            return false;
        }
                
        if (commandString.length() > 1) {
            return (commandString.charAt(0) == commandId);
        }
        
        return false;
    }
    
    public boolean matches(int boardId, String commandId, int commandIdLength) {
        
        if ((boardId != this.boardId) && !((boardId == BOARD_ID_UNDEFINED) || (this.boardId == BOARD_ID_UNDEFINED))) {
            return false;
        }
        
        if (commandString.length() >= commandIdLength) {
            String pivot = commandString.substring(0, commandIdLength);
            return (commandId.compareTo(pivot) == 0);
        }
        return false;
    }
    
    // Check if two data messages are "compatible". 
    // Compatible means:
    //   - AppDataMessages are associated to the same board Id
    //   - AppDataMessages not associated to a specific channel "matches"
    //   - AppDataMessages "matches" and are associated to the same channel
    //   - AppDataMessages "matches" with Answer Command Strings and are associated to the same channel
    public boolean compareDataMessages(AppDataMessage r) {
        
        if (r == null) {
            return false;
        }
        
        if ((boardId != r.getBoardId()) && 
            (boardId != BOARD_ID_UNDEFINED) &&
            (r.getBoardId() != BOARD_ID_UNDEFINED)) {
            return false;
        }
        
        if (commandString.startsWith(r.getCommandString()) ||
                r.getCommandString().startsWith(commandString)) {
            return true;
        }
        
        AppDataMessage c = r.clone();
        return (commandString.startsWith(c.getCommandString()) ||
                c.getCommandString().startsWith(commandString));
    }    
};
