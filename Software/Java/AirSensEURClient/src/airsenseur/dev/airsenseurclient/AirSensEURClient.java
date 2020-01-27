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

import airsenseur.dev.json.ChemSensorClient;
import airsenseur.dev.json.HostStatus;
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
    
    private static final String VERSION = "AirSensEUR Client R1.0.0";
    
    private static final int RETURN_OK = 0;
    private static final int RETURN_ERROR = 1;
    
    private static final Logger log = LoggerFactory.getLogger(AirSensEURClient.class);
    
    private static class Config {
        public String host = "localhost";
        public int port = 8000;
        public int remoteBoardId = -1;
        public String configFile;
        public String registryFile;
    };
    
    private static final ChemSensorClient dataCollector = new ChemSensorClient();
    private static boolean connected = false;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        
        // Parse command line options
        Config config = parseArguments(args);
        if ((config == null) || ((config.configFile == null) && (config.registryFile == null))){
            System.exit(RETURN_OK);
        }
        
        // Connect to the remote host
        log.info("Connecting to the remote host " + config.host);
        if (!dataCollector.connect(config.host, config.port)) {
            onError("Error connecting to the remote host. Aborting.");
        }
        
        // Mark as connected
        connected = true;
        
        // Wait for remote host availability
        // Retrieve host status
        int retry = 10;
        HostStatus hostStatus;
        do {
            log.info("Checking for server to be ready");
            hostStatus = dataCollector.getHostStatus();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            retry--;
        } while ((hostStatus.status != HostStatus.STATUS_READY) && (retry > 0)); 
        if (hostStatus.status != HostStatus.STATUS_READY) {
            onError("Host status seems busy. Aborting.");
        }
        log.info("Server ready. Proceed with the required operation.");
        
        // Taking ownership
        dataCollector.takeOwnership();
        
        // Do something
        boolean result = true;
        if (config.configFile != null) {
            
            ConfigFileSender fileSender = new ConfigFileSender(dataCollector);
            result &= fileSender.process(config.configFile, config.remoteBoardId);
        }
        
        if (config.registryFile != null) {
            RegistryInquirer inquirer = new RegistryInquirer(dataCollector);
            result &= inquirer.process(config.registryFile);
        }
        
        // Releasing ownership
        dataCollector.releaseOwnership();
        
        // Disconnect from the remote host
        dataCollector.disconnect();
        
        System.exit((!result)? RETURN_ERROR: RETURN_OK);
    }
    
    
    private static Config parseArguments(String[] args) {
        
        OptionParser parser = new OptionParser( "hpcri?" );
        OptionSpec<String> hostOption = parser.accepts("h").withRequiredArg().ofType(String.class);
        OptionSpec<Integer> portOpt = parser.accepts("p").withOptionalArg().ofType(Integer.class);
        OptionSpec<String> cFOption = parser.accepts("c").withRequiredArg().ofType(String.class);
        OptionSpec<Integer> rIdOption = parser.accepts("r").withRequiredArg().ofType(Integer.class);
        OptionSpec<String> inquiryOption = parser.accepts("i").withRequiredArg().ofType(String.class);
        OptionSpec<Void> helpOption = parser.accepts("?").forHelp();

        OptionSet options = parser.parse( args );

        Config config = new Config();
        
        if (options.has(hostOption)) {
            config.host = options.valueOf(hostOption);
        }
        
        if (options.has(portOpt)) {
            config.port = options.valueOf(portOpt);
        }
        
        if (options.has(cFOption) && options.hasArgument(cFOption)) {
            config.configFile = options.valueOf(cFOption);
            
            if (options.has(rIdOption) && options.hasArgument(rIdOption)) {
                config.remoteBoardId = options.valueOf(rIdOption);
            }

            if ((config.remoteBoardId < 0) || (config.remoteBoardId > 15)) {
                printHelp();
                return null;
            }
        }
        
        if (options.has(inquiryOption) && options.hasArgument(inquiryOption)) {
            config.registryFile = options.valueOf(inquiryOption);
        }
        
        if (options.has(helpOption) || ((config.registryFile == null) && (config.configFile == null)) ) {
            printHelp();
            return null;
        }
        
        return config;
    }
    
    private static void printHelp() {
        
        System.out.println("Copyright 2015 EUROPEAN UNION");
        System.out.println("Authors:");
        System.out.println("- Michel Gerboles, michel.gerboles@jrc.ec.europa.eu, ");
        System.out.println("  Laurent Spinelle, laurent.spinelle@jrc.ec.europa.eu and ");
        System.out.println("  Alexander Kotsev, alexander.kotsev@jrc.ec.europa.eu:");
        System.out.println("			European Commission - Joint Research Centre, ");
        System.out.println("- Marco Signorini, marco.signorini@liberaintentio.com");
        System.out.println("");

        System.out.println(VERSION);
        System.out.println("Options:");
        System.out.println("  -h: hostname or IP (Default localhost)");
        System.out.println("  -p: port (Default 8000)");
        System.out.println("  -c: configuration file to be sent to the host");
        System.out.println("  -r: remote board Id where the configuration file should be stored (0<=Id<=15)");
        System.out.println("  -i: write boards and sensors registry in a file");
    }
    
    private static void onError(String errorMessage) {
        
        log.info(errorMessage);
        
        if (connected) {    
            dataCollector.releaseOwnership();
            dataCollector.disconnect();
        }
        
        System.exit(RETURN_ERROR);
    }
}
