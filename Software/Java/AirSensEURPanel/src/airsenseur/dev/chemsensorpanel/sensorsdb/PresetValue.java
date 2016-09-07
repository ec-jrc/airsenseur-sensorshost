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

package airsenseur.dev.chemsensorpanel.sensorsdb;

import airsenseur.dev.chemsensorpanel.exceptions.PresetException;

/**
 * We expect to have a PresetValue for any option, identified by a unique Id.
 * @author marco
 */
public class PresetValue {
    
    private final String containerId;     // Semantic of the value
    private final String value;

    public PresetValue(String containerId, String value) {
        this.containerId = containerId;
        this.value = value;
    }
    
    public PresetValue(String containerId, int value) {
        this.containerId = containerId;
        this.value = Integer.toString(value);
    }

    /**
     * @return the containerId
     */
    public String getContainerId() {
        return containerId;
    }
    
    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
    
    public int getValueAsInteger() throws PresetException {
        
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new PresetException("Invalid value when reading from database");
        }
    }
}
