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

package airsenseur.dev.chemsensorhost;

import airsenseur.dev.chemsensorhost.exceptions.ConfigurationException;
import airsenseur.dev.chemsensorhost.exceptions.JSONServerException;
import airsenseur.dev.chemsensorhost.json.JSONServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marcos
 */
public class ChemSensorHostMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        Logger log = LoggerFactory.getLogger(ChemSensorHostMain.class);
        
        Configuration config = Configuration.getConfig();
        
        // Take the configuration
        try {
            config.init(args[0]);
        } catch (ConfigurationException ex) {
            log.error(ex.getMessage());
            return;
        }

        // Start the sensor host manager by connecting to the external board
        ChemSensorHost chemSensorHost = new ChemSensorHost();
        if (!chemSensorHost.start(config.getPollTime(), config.getNumSensors())) {
            return;
        }
        
        // Start the JSON service
        JSONServer jsonServer = new JSONServer();
        try {
            jsonServer.init(chemSensorHost, 
                            config.getJSONBindAddress(), 
                            config.getJSONBindPort());
        } catch (JSONServerException ex) {
            log.error(ex.getMessage());
            return;
        }
        
        // Loop in background (do nothing. All operations are made through callback)
        boolean bContinue = true;
        while (bContinue) {
            try {
                Thread.sleep(60000);            
            } catch (InterruptedException ex) {
                bContinue = false;
            }
        }
        
        // Exit gracefully
        jsonServer.stop();        
        chemSensorHost.exit();
    }
}
