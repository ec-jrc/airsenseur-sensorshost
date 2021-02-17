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

/**
 *
 * @author marco
 */
public class ConfigurationLoRa extends ConfigurationIFLINK {
    
    // LoRa extensions
    public String getLoRaAppEUI() {
        return getProperty("appeui", "");
    }
    
    public String getLoRaAppKey() {
        return getProperty("appkey", "");
    }
    
    public String getLoRaDevEUI() {
        return getProperty("deveui", "");
    }
    
    public String getLoRaEndpoint() {
        return getProperty("port", "/dev/ttyUSB0");
    }
        
    public Integer getLoRaDataRate() {
        return Integer.parseInt(getProperty("datarate", "5"));
    }
    
    public Integer getLoRaTxPower() {
        return Integer.parseInt(getProperty("txpower", "14"));
    }
    
    public Integer getLoRaMaxPacketLength() {
        return Integer.parseInt(getProperty("packetlen", "0"));
    }
    
    public Integer getLoRaSleepTime() {
        return Integer.parseInt(getProperty("sleeptime", "0"));
    }
    
    public boolean disableADR() {
        return getBooleanValue(getProperty("disableADR", "false"));
    }
    
    public boolean forceUnconfirmedMessages() {
        return getBooleanValue(getProperty("forceunconfirmed", "false"));
    }
    
    public Integer getLoRaMaxRetry() {
        return Integer.parseInt(getProperty("maxretry", "5"));
    }
    
    public String getLoRaRecipeFile() {
        return getProperty("loraRecipeFile", "");
    }
}
