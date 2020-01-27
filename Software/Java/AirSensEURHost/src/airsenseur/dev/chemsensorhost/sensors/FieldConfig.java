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

package airsenseur.dev.chemsensorhost.sensors;

/**
 *
 * @author marco
 * @param <T>
 */
public class FieldConfig<T> {
    
    private T value = null;
    private boolean autodiscovered = false;
    private boolean overridden = false;    

    public FieldConfig() {
    }
    
    public FieldConfig(T t) {
        value = t;
    }
    
    public void markForAutodiscover() {
        if (!this.overridden) {
            this.autodiscovered = false;
        }
    }
    
    public void overrideValue(T value) {
        this.value = value;
        overridden = true;
        autodiscovered = false;
    }
    
    public void autodiscoveredValue(T value) {
        this.value = value;
        overridden = false;
        autodiscovered = true;
    }
    
    public boolean isSet() {
        return autodiscovered | overridden;
    }
    
    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        
        String result = "NA";
        
        if (isSet() && (value != null)) {
            result = value.toString();
        }
        if (overridden) {
            result = result + " (overridden)";
        }
        if (autodiscovered) {
            result = result + " (autodiscovered)";
        }
        
        return result;
    }
}
