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

package airsenseur.dev.persisters.lora;

import airsenseur.dev.exceptions.GenericException;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.json.BoardInfo;
import airsenseur.dev.persisters.BoardsPersister;
import airsenseur.dev.persisters.lora.helpers.LoraDeviceComm;
import java.util.List;

/**
 * Boards configuration persister through LoRa
 * @author marco
 */
public class BoardsPersisterLoRa implements BoardsPersister {
    
    private final LoraDeviceComm loraDevice;
    
    private static class message extends BasicLoRaMessage {
        
        public long timestamp;
        
        public message(int fPort) {
            super(fPort, false);
        }
        
        public void clear(long timestamp) {
            super.clear();
            this.timestamp = timestamp;
            super.append(timestamp);
            super.dirty = false;
        }
        
        public void append(BoardInfo board) {
            
            super.append(board.boardId);
            super.append(board.boardType);
            super.append(board.fwRevision);
            super.append(board.serial);
        }
        
        // Returns the extimated size of ASCII encoded message for the specified board
        public int extimateSize(BoardInfo board) {
            
            return extimateEncodedStringSize(board.boardType) + 
                   extimateEncodedStringSize(board.fwRevision) +
                   extimateEncodedStringSize(board.serial) + 8; 
        }
    }
    
    public BoardsPersisterLoRa(LoraDeviceComm loRaDevice) {
        this.loraDevice = loRaDevice;
        
    }

    @Override
    public boolean addBoardsInfo(List<BoardInfo> boards) throws PersisterException {
        
        // Check of there is nothing to do
        if (boards.isEmpty()) {
            return true;
        }
        
        // We send a single timestamp for all the boards found
        long timestamp = boards.get(0).timestamp;

        // Loop on boards and send LoRa messages aggregating boards where possible
        message currentMessage = new message(LoraDeviceComm.LORA_DEFAULT_BOARDSINFO_PORT);        
        currentMessage.clear(timestamp);
        try {
            for (BoardInfo board:boards) {

                int size = currentMessage.extimateSize(board);

                // It's impossible to send this information because it's more than
                // the allowed LoRa packet length. Discard it.
                if (size > loraDevice.getPacketLength()) {
                    continue;
                }

                // If we don't have enought space to append info to current message
                // send partial message
                if ((currentMessage.size() + size) > loraDevice.getPacketLength()) {

                    if (!flushCurrentMessage(currentMessage))
                        return false;
                    
                } else {

                    // Otherwise, append this board info to current message
                    currentMessage.append(board);
                }
            }
            
            // Flush partial accumulated messages
            return flushCurrentMessage(currentMessage);
        
        } catch (GenericException e) {
            return false;
        }
    }

    private boolean flushCurrentMessage(message currentMessage) throws GenericException {
        
        // Nothing to do with this message
        if (!currentMessage.dirty) {
            return true;
            
        }
        
        // Flush current message
        if (!loraDevice.sendPayload(currentMessage)) {
            return false;
        }
        
        // Prepare next buffer
        currentMessage.clear(currentMessage.timestamp);
        
        return true;
    }

    @Override
    public String getPersisterMarker(int channel) {
        return HistoryEventContainer.EVENT_LATEST_LORA_BOARDINFOPUSH_TS;
    }
}
