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

package airsenseur.dev.airsenseurclient;

import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.helpers.FileConfiguration;
import airsenseur.dev.json.ChemSensorClient;
import airsenseur.dev.json.RawCommand;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send the specified configuration file to the remote host
 * @author marco
 */
public class ConfigFileSender {
    
    private final ChemSensorClient dataCollector;
    
    private static final Logger log = LoggerFactory.getLogger(ConfigFileSender.class);

    public ConfigFileSender(ChemSensorClient dataCollector) {
        this.dataCollector = dataCollector;
    }
    
    public boolean process(String configFileName, int remoteId) {
        
        log.info("Sending configuration file to the remote host");
        
        FileConfiguration file = new FileConfiguration();
        if (!file.openFile(configFileName, true)) {
            log.info("Error opening the specified configuration file. Aborting.");
            return false;
        }
        
        // Read the whole configuration file
        List<RawCommand> rawData = new ArrayList<>();
        AppDataMessage dataMessage = file.getNextCommand(0);
        while (dataMessage != null) {

            rawData.add(new RawCommand(remoteId, dataMessage.getCommandString(), 
                                                dataMessage.getCommandComment()));
            dataMessage = file.getNextCommand(0);
        }

        // Send data, if present
        if (!rawData.isEmpty()) {
            List<RawCommand> result = dataCollector.sendRawData(rawData);
            if ((result == null) || result.isEmpty() || (result.size() != rawData.size())) {
                log.info("Error writing configuration file to the remote host");
            } else {
                log.info("Configuration file sent to the remote host");
            }
        } else {
            log.info("No valid command list found in the configuration file");
        }
            
        return true;
    }
    
    
}
