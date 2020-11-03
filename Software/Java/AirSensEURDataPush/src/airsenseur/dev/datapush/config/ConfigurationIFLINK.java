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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author marco
 */
public class ConfigurationIFLINK extends ConfigurationAWSMQTT {
    
    
    // iFLINK extensions 
    public String getIFLINKHost() {
        return getProperty("iFLINKHost", "");
    }
    
    public String getIFLINKEndpoint() {
        return getProperty("iFLINKEndpoint", "");
    }
    
    public String getIFLINKSensorID() {
        return getProperty("iFLINKSensorID", "");
    }
    
    public String getIFLINKInboundSensorID() {
        return getProperty("iFLINKInboundSensorID", "");
    }

    public String getIFLINKBearerToken() {
        return getProperty("iFLINKBearerToken", "");
    }
    
    public List<String> getIFLINKSensorsList() {
        
        String sensorList = getProperty("iFLINKSensorsList", "");
        
        return new ArrayList(Arrays.asList(sensorList.split(",")));
    }
    
    public String getIFLINKDatePath() {
        return getProperty("iFLINKDatePath", "date");
    }
    
    public boolean getIFLINKUpdatePosition() {
        return getBooleanValue(getProperty("iFLINKUpdatePositions", "true"));
    }
    
    public boolean getIFLINKUseCurl() {
        return getBooleanValue(getProperty("iFLINKUseCurl", "false"));
    }
        
}
