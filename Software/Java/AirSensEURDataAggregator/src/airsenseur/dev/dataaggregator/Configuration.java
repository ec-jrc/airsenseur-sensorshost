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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author marco
 */
public class Configuration extends Properties{
        
    private static final Configuration singleton = new Configuration();
    
    public static Configuration getConfig() {
        return singleton;
    }
    
    public void init(String propertyFile) throws ConfigurationException {
        
        InputStream inStream;
        try {
            inStream = new FileInputStream(propertyFile);
            this.load(inStream);            
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException(propertyFile + " configuration file not found.");
        } catch (IOException ex) {
            throw new ConfigurationException(propertyFile + " configuration file not readable.");
        }
    }
    
    public boolean debugEnabled() {
        return debugVerbose() != 0;
    }
    
    public int debugVerbose() {
        
        String debug = getProperty("debug", "0");
        return Integer.valueOf(debug);
    }
    
    public int getPollTime() {
        String pollTime = getProperty("sensorPollTime", "1000");
        return Integer.valueOf(pollTime);
    }
    
    public String getSensorHostname() {
        return getProperty("sensorhost", "localhost");
    }
    
    public int getSensorPort() {
        String port = getProperty("sensorport", "8000");
        return Integer.valueOf(port);
    }
    
    public String getGPSHostname() {
        return getProperty("gpshost", "localhost");
    }

    public int getGPSPort() {
        String port = getProperty("gpsrport", "2947");
        return Integer.valueOf(port);
    }
    
    public String getPersisterPath() {
        return getProperty("datapath", "./");
    }
    
    public boolean applyTimestampCorrection() {
        String valString = getProperty("applyTimestampCorrection", "true");
        return getBooleanValue(valString);
    }
    
    private boolean getBooleanValue(String valString) {
        if ((valString.compareToIgnoreCase("true") == 0) || (valString.compareToIgnoreCase("yes") == 0)) {
            return true;
        } else if ((valString.compareToIgnoreCase("false") == 0) || (valString.compareToIgnoreCase("no") == 0)){
            return false;
        }
        
        try {
            return Integer.parseInt(valString) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }     
}
