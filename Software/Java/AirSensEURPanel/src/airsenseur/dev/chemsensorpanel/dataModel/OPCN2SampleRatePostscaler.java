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
public class OPCN2SampleRatePostscaler extends RegisterDataModel {
    
    public static final int availablePostScalerFactors[] = { 0x01, 0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0C,
        0x0E,0x0F,0x10,0x12,0x14,0x15,0x18,0x19,0x1B,0x1C,0x1E,0x20,0x23,0x24,0x28, 0x2D,
    };

    public OPCN2SampleRatePostscaler() {
        
        super((new ArrayList<DataItem>() {{
            for (int n = 0; n < availablePostScalerFactors.length; n++) {
                add(new DataItem(availablePostScalerFactors[n] + " samples", availablePostScalerFactors[n]));
            }
        }}).toArray());
        
        setSelectedItem(10);
        setBase(0, 8);
    }
}
