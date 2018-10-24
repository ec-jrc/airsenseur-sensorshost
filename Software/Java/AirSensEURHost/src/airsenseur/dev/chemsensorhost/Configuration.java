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
import airsenseur.dev.comm.AppDataMessage;
import expr.Parser;
import expr.SyntaxException;
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
    
    public final static int DEBUG_VERBOSE_DUMP_SERIAL = 5;
    
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
            throw new ConfigurationException(propertyFile + " configuration file not found. Aborting.");
        } catch (IOException ex) {
            throw new ConfigurationException(propertyFile + " configuration file not readable. Aborting.");
        }
        
        // Check for math expressions
        for (int sensor = 0; sensor < getNumSensors(); sensor++) {
            String mathExpression = getMathExpressionForSensor(sensor);
            checkMathExpression(mathExpression);
        }
    }
    
    public boolean debugEnabled() {
        return debugVerbose() != 0;
    }
    
    public int debugVerbose() {
        
        String debug = getProperty("debug", "0");
        return Integer.valueOf(debug);
    }
    
    public String serialPort() {
        return getProperty("port", "ttyS0");
    }
    
    public int getPollTime() {
        String pollTime = getProperty("pollTime", "1000");
        return Integer.valueOf(pollTime);
    }
    
    public boolean getUseBusProtocol() {
        String useBusProtocol = getProperty("useBusProtocol", "0");
        return useBusProtocol.equalsIgnoreCase("1") || useBusProtocol.equalsIgnoreCase("Yes") || useBusProtocol.equalsIgnoreCase("true");
    }
    
    public int getNumSensors() {
        String numSensors = getProperty("numSensors", "7");
        return Integer.valueOf(numSensors);
    }
    
    public String getJSONBindAddress() {
        return getProperty("jsonHostname", "localhost");
    }
    
    public int getJSONBindPort() {
        String port = getProperty("jsonPort", "8000");
        return Integer.valueOf(port);
    }
    
    
    public String getMathExpressionForSensor(int sensor) {
        String key = String.format("sensorexpression_%02d", sensor);
        return getProperty(key, "x");
    }
    
/**
     * Check validity of a mathematical expression
     * @param expression
     * @return 
     */
    private String checkMathExpression(String expression) throws ConfigurationException {
       
        try { 
            Parser.parse(expression);
        } catch (SyntaxException ex) {
            throw new ConfigurationException("Invalid expression found: " + expression);
        }
        
        return expression;
    }
    
    public String getSensorNameForChannel(int channel) {
        String key = String.format("sensorname_%02d", channel);
        return getProperty(key, "");
    }
    
    public Integer getBoardIdForSensor(int sensor) {
        String key = String.format("sensorboarid_%02d", sensor);
        String valString = getProperty(key, "" + AppDataMessage.BOARD_ID_UNDEFINED);
        try {
            return Integer.parseInt(valString);
        } catch (NumberFormatException e) {
            return AppDataMessage.BOARD_ID_UNDEFINED;
        }
    }
    
    public Integer getChannelForSensor(int sensor) {
        String key = String.format("sensorchannel_%02d", sensor);
        String valString = getProperty(key, "" + sensor);
        try {
            return Integer.parseInt(valString);
        } catch (NumberFormatException e) {
            return sensor;
        }
    }
    
    public boolean getHiResSample(int sensor) {
        String key = String.format("sensorhires_%02d", sensor);
        String valString = getProperty(key, "false");
        
        if ((valString.compareToIgnoreCase("true") == 0) || (valString.compareToIgnoreCase("yes") == 0)) {
            return true;
        } else if ((valString.compareToIgnoreCase("false") == 0) || (valString.compareToIgnoreCase("no") == 0)){
            return false;
        }
        
        try {
            return Integer.parseInt(key) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
