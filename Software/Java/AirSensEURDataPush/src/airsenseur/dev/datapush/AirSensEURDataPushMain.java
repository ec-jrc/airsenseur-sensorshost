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

package airsenseur.dev.datapush;

import airsenseur.dev.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marcos
 */
public class AirSensEURDataPushMain {
    
    static final String VERSION = "AirSensEURDataPush V2.0.0";
       
    private static final Logger log = LoggerFactory.getLogger(AirSensEURDataPushMain.class);
    
    private static Configuration config;
    private final static AirSensEURDataPush dataPushEngine = new AirSensEURDataPush();
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        log.info(VERSION + " started");
        
        // Load the configuration file
        config = Configuration.getConfig();
        try {
            config.init(args[0]);
        } catch (ConfigurationException ex) {
            log.error("Please specify a valid configuration file.");
            return;
        }
        
        // This is the main engine
        dataPushEngine.start(config);

        log.info("AirSensEURDataPush terminated");
    }        
}
