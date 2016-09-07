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

package airsenseur.dev.persisters.sosdb;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.history.HistoryPersister;
import airsenseur.dev.persisters.sosdb.requests.AnsGetDataAvailabilitySOSDB;
import airsenseur.dev.persisters.sosdb.requests.GetDataAvailabilitySOSDB;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An engine to retrieve a sort of history of already
 * added observations from the SOSDB remote server
 * @author marco
 */
public class HistorySOSDB extends SOSDBBaseService implements HistoryPersister {
    
    private final Logger log = LoggerFactory.getLogger(HistorySOSDB.class);
    
    public HistorySOSDB(ConfigurationSOSDB configuration) throws PersisterException {
        super(configuration);
    }

    @Override
    public Logger getLogger() {
        return log;
    }
    
    @Override
    public boolean openLog(boolean read) throws PersisterException {
        return true;
    }

    @Override
    public void closeLog() {
    }

    @Override
    public boolean saveEvent(HistoryEventContainer event) throws PersisterException {
        return true;
    }

    /**
     * Try to retrieve the maximum timestamp already present in the
     * remote SOSDB for a given channel number (eventName)
     * @param eventName: the channel 
     * @return an history container with the result or null otherwise
     * @throws PersisterException 
     */
    @Override
    public HistoryEventContainer loadEvent(String eventName) throws PersisterException {
        
        int channelId;
        try {
            channelId = Integer.parseInt(eventName);
        } catch (NumberFormatException ex) {
            throw new PersisterException("Invalid channel specified in the history.");
        }
        if (channelId >= sensorsObservedProp.size()) {
            throw new PersisterException("Invalid channel specified in the history.");
        }
        
        GetDataAvailabilitySOSDB getDataAvailaility = new GetDataAvailabilitySOSDB();
        getDataAvailaility.setFeatureOfInterest(foiId);
        getDataAvailaility.setProcedure(sensorsProcedure.get(channelId));
        getDataAvailaility.setObservedProperty(sensorsObservedProp.get(channelId));
        
        AnsGetDataAvailabilitySOSDB result = (AnsGetDataAvailabilitySOSDB)sendToServer(getDataAvailaility, AnsGetDataAvailabilitySOSDB.class);
        if ((result != null) && (result.getDataAvailability().size()>0)) {
            if (result.getDataAvailability().get(0).getPhenomenonTime().size() > 0) {
                
                Date maxTsDate = result.getDataAvailability().get(0).getPhenomenonTime().get(1);
                return new HistoryEventContainer(eventName, "" + (maxTsDate.getTime() + 1000));
            }
        }
        
        return null;
    }

    
}
