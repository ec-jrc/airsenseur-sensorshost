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

import airsenseur.dev.persisters.sosdb.ConfigurationSOSDB;

/**
 *
 * @author marco
 */
public class Configuration extends ConfigurationSOSDB {
    
    public enum workingMode {
        INFLUX,
        SOSDB,
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
    
    public workingMode getWorkingMode() {
        if (getInfluxDbHost().isEmpty()) {
            return workingMode.SOSDB;
        }
        return workingMode.INFLUX;
    }
    

    /* Influx DB related configuration */
    public String getInfluxDbHost() {
        return getProperty("influxdbhost", "");
    }
    
    public int getInfluxDbPort() {
        String port = getProperty("influxdbport", "8086");
        return Integer.valueOf(port);
    }
    
    public String getInfluxDbName() {
        return getProperty("influxdbname", "database");
    }
    
    public String getInfluxDbDataSetName() {
        return getProperty("influxdbdataset", "dataset");
    }
    
    public String getInfluxDbUsername() {
        return getProperty("influxdbuser", "username");
    }
    
    public String getInfluxDbPassword() {
        return getProperty("influxdbpasswd", "password");
    }
    
    public boolean getInfluxDbUseLineProtocol() {
        String value = getProperty("uselineprotocol", "false");
        return getBooleanValue(value);
    }
    
    public boolean getUseHTTPSProtocol() {
        String value = getProperty("useHTTPS", "false");
        return getBooleanValue(value);
    }
    
    private boolean getBooleanValue(String value) {
        return ((value.compareToIgnoreCase("true") == 0) ||
                (value.compareToIgnoreCase("yes") == 0) || 
                (value.compareToIgnoreCase("on") == 0));
    }
}
