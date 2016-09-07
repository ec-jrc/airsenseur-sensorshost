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

package airsenseur.dev.persister.sql;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SampleLoader;
import airsenseur.dev.persisters.sql.SampleLoaderSQL;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class SampleLoaderSQLTester {

    public static void main(String[] args) {
        
        Logger log = LoggerFactory.getLogger(SampleLoaderSQLTester.class);
        
        SampleLoaderSQL loader = new SampleLoaderSQL();
        try {
            if (!loader.openLog()) {
                log.error("Impossible to open the database");
                return;
            }
            
            List<SampleDataContainer> result;
            long firstTimeStamp = 0;
            do {
                result = loader.loadSamples(SampleLoader.CHANNEL_INVALID, firstTimeStamp, 10);
                
                if (!result.isEmpty()) {
                    firstTimeStamp = (long)result.get(result.size()-1).getGpsTimestamp();
                }
                
            } while (!result.isEmpty());
            
        } catch (PersisterException ex) {
            log.error(ex.getErrorMessage());
        }
    }
}
