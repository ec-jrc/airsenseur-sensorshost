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

package airsenseur.dev.persisters.sosdb.dao;

import java.util.Date;
import java.util.List;

/**
 * DataAvailability JSON holder
 * @author marco
 */
public class DataAvailabilitySOSDB {
    
    private String featureOfInterest;
    private String procedure;
    private String observedProperty;
    private List<Date> phenomenonTime;
    private Integer valueCount;

    /**
     * @return the featureOfInterest
     */
    public String getFeatureOfInterest() {
        return featureOfInterest;
    }

    /**
     * @param featureOfInterest the featureOfInterest to set
     */
    public void setFeatureOfInterest(String featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }

    /**
     * @return the procedure
     */
    public String getProcedure() {
        return procedure;
    }

    /**
     * @param procedure the procedure to set
     */
    public void setProcedure(String procedure) {
        this.procedure = procedure;
    }

    /**
     * @return the observedProperty
     */
    public String getObservedProperty() {
        return observedProperty;
    }

    /**
     * @param observedProperty the observedProperty to set
     */
    public void setObservedProperty(String observedProperty) {
        this.observedProperty = observedProperty;
    }

    /**
     * @return the phenomenonTime
     */
    public List<Date> getPhenomenonTime() {
        return phenomenonTime;
    }

    /**
     * @param phenomenonTime the phenomenonTime to set
     */
    public void setPhenomenonTime(List<Date> phenomenonTime) {
        this.phenomenonTime = phenomenonTime;
    }

    /**
     * @return the valueCount
     */
    public Integer getValueCount() {
        return valueCount;
    }

    /**
     * @param valueCount the valueCount to set
     */
    public void setValueCount(Integer valueCount) {
        this.valueCount = valueCount;
    }
    
}
