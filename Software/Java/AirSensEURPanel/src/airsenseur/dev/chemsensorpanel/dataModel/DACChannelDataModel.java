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

package airsenseur.dev.chemsensorpanel.dataModel;

/**
 *
 * @author marco
 */
public class DACChannelDataModel extends SliderDataModel {
    
    private int subChannel;

    public DACChannelDataModel() {
        setMaximum(4095);
    }

    /**
     * @param subChannel the subChannel to set
     */
    public void setSubChannel(int subChannel) {
        this.subChannel = subChannel;
    }
    
    /**
     * @return the subChannel
     */
    public int getSubChannel() {
        return subChannel;
    }
    
    public boolean setValue(int value, int subChannel) {
        if (this.getSubChannel() == subChannel) {
            setValue(value);
            return true;
        }
        
        return false;
    }
}
