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
public class LMP9100_TIA_Gain extends RegisterDataModel {

    public LMP9100_TIA_Gain() {
        super((new ArrayList<DataItem>() {{ 
            add(new DataItem("External", 0));
            add(new DataItem("2.75kohm", 1));
            add(new DataItem("3.5kohm", 2));
            add(new DataItem("7kohm", 3));
            add(new DataItem("14kohm", 4));
            add(new DataItem("35kohm", 5));
            add(new DataItem("120kohm", 6));
            add(new DataItem("350kohm", 7));
        }}).toArray());
        
        setSelectedItem(0);
        setBase(2, 3);
    }
}
