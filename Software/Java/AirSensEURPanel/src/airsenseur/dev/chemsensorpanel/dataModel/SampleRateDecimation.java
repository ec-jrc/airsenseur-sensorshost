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

import java.util.ArrayList;

/**
 *
 * @author marco
 */
public class SampleRateDecimation extends RegisterDataModel {
    
    private final static int MIN_DECIMATION_VALUE = 0;
    private final static int MAX_DECIMATION_VALUE = 255;

    public SampleRateDecimation() {
        
        super((new ArrayList<DataItem>() {{ 
            for (int n = MIN_DECIMATION_VALUE; n <= MAX_DECIMATION_VALUE; n++) {
                add(new DataItem("Every " + (n+1) + " samples", n));
            }
        }}).toArray());
        
        setSelectedItem(99);
        setBase(0, 8);
    }
}
