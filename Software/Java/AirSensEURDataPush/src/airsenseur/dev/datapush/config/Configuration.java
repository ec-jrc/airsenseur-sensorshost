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

package airsenseur.dev.datapush.config;

import airsenseur.dev.exceptions.ConfigurationException;

/**
 *
 * @author marco
 */
public class Configuration extends ConfigurationIFLINK {
    
    public enum workingMode {
        INFLUX,
        SOSDB,
        MQTT,
        AWS_MQTT,
        IFLINK,
    };
        
    private static final Configuration singleton = new Configuration();
    
    public static Configuration getConfig() {
        return singleton;
    }
    
    public int getPollTime() {
        String pollTime = getProperty("sensorPollTime", "1000");
        return Integer.valueOf(pollTime);
    }
    
    public String getPersisterPath() {
        return getProperty("datapath", "./");
    }
    
    public String getHistoryPath() {
        return getProperty("historypath", "./");
    }
    
    public int getMaxDatabaseRetry() {
        String maxDatabaseRetry = getProperty("maxdbretry", "10");
        return Integer.valueOf(maxDatabaseRetry);
    }
    
    public int loadDataTimeSpan() {
        String dataTimeSpan = getProperty("datatimespan", "900000");
        return Integer.valueOf(dataTimeSpan);
    }
    
    public boolean getUseHTTPSProtocol() {
        String value = getProperty("useHTTPS", "false");
        return getBooleanValue(value);
    }
    
    public int getConnectionTimeout() {
        String value = getProperty("connectionTimeout", "60");
        return Integer.valueOf(value);
    }
    
    public int getAggregationFactor() {
        String value = getProperty("aggregationFactor", "1");
        return Integer.valueOf(value);
    }
    
    public int getNumThreads() {
        String value = getProperty("numThreads", "1");
        return Integer.valueOf(value);
    }
    
    public workingMode getWorkingMode() throws ConfigurationException {
        
        if (!getInfluxDbHost().isEmpty()) {
            return workingMode.INFLUX;
        }
        
        if (!getSOSDBHost().isEmpty()) {
            return workingMode.SOSDB;
        }
                
        if (!getMQTTHost().isEmpty()) {
            return workingMode.MQTT;
        }
        
        if (!getAWSIOTKeyPath().isEmpty()) {
            return workingMode.AWS_MQTT;
        }
        
        if (!getIFLINKHost().isEmpty()) {
            return workingMode.IFLINK;
        }
        
        throw new ConfigurationException("Invalid configuration found. It's not possible to determine the working mode.");
    }
}
