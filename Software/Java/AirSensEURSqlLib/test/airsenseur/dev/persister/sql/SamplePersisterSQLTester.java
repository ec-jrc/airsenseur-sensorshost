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

package airsenseur.dev.persister.sql;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.sql.SamplePersisterSQL;

/**
 *
 * @author marcos
 */
public class SamplePersisterSQLTester {
    
    public static void main(String[] args) {
        
        SamplePersisterSQL persister = new SamplePersisterSQL();
        
        try {
            persister.startNewLog();
            
            SampleDataContainer dataContainer = new SampleDataContainer(10);
            dataContainer.setName("Test");
            dataContainer.updateSample(500, 600);
            dataContainer.updateGPSValues(100.0, 200.0, 300.0, 400.0);

            for (int i = 0; i < 100; i++) {
                persister.addSample(dataContainer);
            }
            
            
        } catch (PersisterException ex) {
            System.err.println(ex.getErrorMessage());
        }
    }
    
}
