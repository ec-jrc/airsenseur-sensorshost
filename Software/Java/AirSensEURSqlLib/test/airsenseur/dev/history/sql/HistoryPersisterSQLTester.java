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

package airsenseur.dev.history.sql;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class HistoryPersisterSQLTester {

    public static void main(String[] argc) {
        
        Logger log = LoggerFactory.getLogger(HistoryPersisterSQLTester.class);
        
        HistoryPersisterSQL history = new HistoryPersisterSQL();
        try {
            
            // Open (and create if not exists) the log
            history.openLog(true);
            
            // Read an event
            HistoryEventContainer event = history.loadEvent("myEvent");
            if (event != null) {
                log.info(event.getEventName() + ": " + event.getEventValue());
            }
            
            // Add an event
            event = new HistoryEventContainer("myEvent", "Value1");
            history.saveEvent(event);
            
            history.closeLog();
            
            // Open (and create if not exists) the log
            history.openLog(true);
            
            // Read an event
            event = history.loadEvent("myEvent");
            if (event != null) {
                log.info(event.getEventName() + ": " + event.getEventValue());
            }
            
            history.closeLog();
            
        } catch (PersisterException ex) {
            log.info(ex.getErrorMessage());
        } finally {
            history.closeLog();
        }
    }
}
