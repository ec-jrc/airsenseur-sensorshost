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

import airsenseur.dev.comm.CommProtocolHelper;
import airsenseur.dev.helpers.FileConfiguration;
import airsenseur.dev.json.ChemSensorClient;
import airsenseur.dev.json.RawCommand;
import java.util.ArrayList;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple shell client used to trigger/test commands through JSON
 * @author marcos
 */
public class AirSensEURClient {
    
    private static final Logger log = LoggerFactory.getLogger(AirSensEURClient.class);
    
    private static class Config {
        public String host = "localhost";
        public int port = 8000;
        public String configFile = "";
    };
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // Parse command line options
        Config config = parseArguments(args);
        
        // Create JSON client object
        ChemSensorClient dataCollector = new ChemSensorClient();
        if (!dataCollector.connect(config.host, config.port)) {
            System.out.println("Aborting.");
            return;
        }
        
        if (config.configFile != null) {
            
            FileConfiguration file = new FileConfiguration();
            file.openFile(config.configFile, true);

            // Read the whole configuration file
            List<RawCommand> rawData = new ArrayList<>();
            CommProtocolHelper.DataMessage dataMessage = file.getNextCommand();
            while (dataMessage != null) {

                rawData.add(new RawCommand(dataMessage.getCommandComment(), 
                                            dataMessage.getCommandString()));
                dataMessage = file.getNextCommand();
            }
            
            // Send data, if present
            if (!rawData.isEmpty()) {
                dataCollector.sendRawData(rawData);
            }
            
            dataCollector.disconnect();
        }
    }
    
    
    private static Config parseArguments(String[] args) {
        
        OptionParser parser = new OptionParser( "hpc?" );
        OptionSpec<String> hostOption = parser.accepts("h").withRequiredArg().ofType(String.class);
        OptionSpec<Integer> portOpt = parser.accepts("p").withOptionalArg().ofType(Integer.class);
        OptionSpec<String> cFOption = parser.accepts("c").withRequiredArg().ofType(String.class);

        OptionSet options = parser.parse( args );

        Config config = new Config();
        
        if (options.has(hostOption)) {
            config.host = options.valueOf(hostOption);
        }
        
        if (options.has(portOpt)) {
            config.port = options.valueOf(portOpt).intValue();
        }
        
        if (options.has(cFOption) && options.hasArgument(cFOption)) {
            config.configFile = options.valueOf(cFOption);
        }

        return config;
    }
    
    private static void printHelp() {
    }
    
}
