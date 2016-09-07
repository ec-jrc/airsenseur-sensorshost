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

package airsenseur.dev.persisters.sosdb.requests;

import airsenseur.dev.persisters.sosdb.dao.ObservationSOSDB;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON InsertObservation vector for SOS DB
 * @author marco
 */
public class InsertObservationSOSDB extends RequestBaseSOSDB {
    
    private String offering;
    private final List<ObservationSOSDB> observation = new ArrayList<>();

    public InsertObservationSOSDB() {
        super("InsertObservation");
    }

    /**
     * @return the offering
     */
    public String getOffering() {
        return offering;
    }

    /**
     * @param offering the offering to set
     */
    public void setOffering(String offering) {
        this.offering = offering;
    }

    /**
     * @return the observation
     */
    public List<ObservationSOSDB> getObservation() {
        return observation;
    }
}
