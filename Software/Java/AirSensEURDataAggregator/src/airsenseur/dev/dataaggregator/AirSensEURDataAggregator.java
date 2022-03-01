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

package airsenseur.dev.dataaggregator;

import airsenseur.dev.exceptions.ConfigurationException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author marcos
 */
public class AirSensEURDataAggregator {
    
    static final String VERSION = "AirSensEURDataAggregator R2.1.1";
    static final Logger log = LoggerFactory.getLogger(AirSensEURDataAggregator.class);
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        log.info(VERSION + " started");

        // Load the configuration file
        Configuration config = Configuration.getConfig();
        try {
            config.init(args[0]);
        } catch (ConfigurationException ex) {
            log.error(ex.getErrorMessage() + " Continuing with default values.");
        }

        // Try to initialize
        AirSensEURDataAggregatorEngine engine = new AirSensEURDataAggregatorEngine();
        while (!engine.init()) {
            try {
                log.error("AirSensEURDataAggregator initialization failed. Retrying in 5 seconds.");
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ex) {
                return;
            }
        }
        
        // Main task loop
        int pollingTime = config.getPollTime();
        log.info("AirSensEURDataAggregator main loop started with polling time (ms): " + pollingTime);
        engine.startPeriodic(pollingTime);
        
        // Loop in background (do nothing. All operations are made through callback)
        boolean bContinue = true;
        while (bContinue) {
            try {
                bContinue = !engine.waitForTermination(6000);
            } catch (InterruptedException ex) {
                bContinue = false;
            }
        }
        
        engine.terminate();
        
        log.info("AirSensEURDataAggregator terminated");
    }
}
