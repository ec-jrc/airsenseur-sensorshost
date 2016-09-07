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

package airsenseur.dev.persisters.sosdb;

import airsenseur.dev.exceptions.ConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuration container for SOS DB related persister
 * @author marco
 */
public class ConfigurationSOSDB extends Properties {
    
    
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
    
    public String debugDumpJSON() {
        return getProperty("sos.filedump", "");
    }
    
    public int debugVerbose() {
        
        String debug = getProperty("debug", "0");
        return Integer.valueOf(debug);
    }

    public String getSOSDBHost() {
        return getProperty("sos.hostname");
    }
    
    public int getSOSDBPort() {
        String port = getProperty("sos.port", "8080");
        return Integer.valueOf(port);
    }
    
    public String getSOSDBEndpoint() {
        return getProperty("sos.endpoint", "52nSOS/sos/json");
    }
    
    public int getHTTPTimeout() {
        String timeout = getProperty("sos.timeout", "30");
        return Integer.valueOf(timeout);
    }
    
    public String getOfferingName() {
        return getProperty("sos.offering.name");
    }
    
    public String getFOIName() {
        return getProperty("sos.foi.name");
    }
    
    public String getFOIId() {
        return getProperty("sos.foi.id");
    }
    
    public boolean insertId() {
        String property = getProperty("sos.observation.byid", "false");
        return Boolean.valueOf(property);
    }
    
    public boolean updateFOILocation() {
        String update = getProperty("sos.foi.updatelocation");
        return Boolean.valueOf(update);
    }
    
    public List<String> getSensorsProcedure() {
        
        return getSensorsProperty( "procedure");
    }
    
    public List<String> getSensorsObservedProp() {
        return getSensorsProperty("observedprop");
    }
    
    public List<String> getSensorsUom() {
        return getSensorsProperty("uom");
    }

    private List<String> getSensorsProperty(String propertyName) {
        
        List<String> result = new ArrayList<>();
        int sensor = 0;
        String procedure;
        do {
            String propName = String.format("sos.sensor_%02d." + propertyName, sensor);
            sensor++;
            procedure = getProperty(propName);
            if ((procedure != null) && !procedure.isEmpty()) {
                result.add(procedure);
            }
        } while((procedure != null) && !procedure.isEmpty());
        
        return result;
    }
}
