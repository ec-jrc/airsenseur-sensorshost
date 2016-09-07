/* ========================================================================
 * Copyright 2016 EUROPEAN UNION
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by 
 * the European Commission - subsequent versions of the EUPL (the "Licence"); 
 * You may not use this work except in compliance with the Licence. 
 * You may obtain a copy of the Licence at: http://ec.europa.eu/idabc/eupl
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the 
 * specific language governing permissions and limitations under the Licence.
 * Date: 01/02/2016
 * Authors
 * - Marco Signorini  - marco.signorini@liberaintentio.com
 * - Michel Gerboles  - michel.gerboles@jrc.ec.europa.eu,  
 *                  			European Commission - Joint Research Centre, 
 * - Laurent Spinelle - laurent.spinelle@jrc.ec.europa.eu,
 *                  			European Commission - Joint Research Centre, 
 * 
 * ======================================================================== 
 */

package airsenseur.dev.dataaggregator.collectors;

import airsenseur.dev.json.ChemSensorClient;
import airsenseur.dev.json.FreeMemory;
import airsenseur.dev.json.SampleData;

/**
 *
 * @author marco
 */
public class SensorsDataCollectorTester {

    public static void main(String[] argc) {
        
        ChemSensorClient collector = new ChemSensorClient();
        
        if (!collector.connect("192.168.0.212", 8000)) {
            return;
        }
        
        FreeMemory freeMemory = collector.getFreeMemory();
        SampleData sample = collector.getLastSample(0);
        sample = collector.getLastSample(1);
        sample = collector.getLastSample(2);
        sample = collector.getLastSample(3);
        sample = collector.getLastSample(4);
        sample = collector.getLastSample(5);
        sample = collector.getLastSample(6);
    }
}
