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


/**
 * JSON container for SOS DB Observation
 * @author marco
 */
public class ObservationSOSDB {
    
    private String type = StaticTokenSOSDB.OBSERVATION_TYPE;
    private String procedure;
    private UnknownContainerSOSDB identifier;
    private String observedProperty;
    private ParameterTypeSOSDB parameter;
    private Object featureOfInterest;
    private Date phenomenonTime;
    private Date resultTime;
    private ResultSOSDB result = new ResultSOSDB();

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
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
     * @return the featureOfInterest
     */
    public Object getFeatureOfInterest() {
        return featureOfInterest;
    }

    /**
     * @param featureOfInterest the featureOfInterest to set
     */
    public void setFeatureOfInterest(Object featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }

    /**
     * @return the result
     */
    public ResultSOSDB getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(ResultSOSDB result) {
        this.result = result;
    }

    /**
     * @return the phenomenonTime
     */
    public Date getPhenomenonTime() {
        return phenomenonTime;
    }

    /**
     * @param phenomenonTime the phenomenonTime to set
     */
    public void setPhenomenonTime(Date phenomenonTime) {
        this.phenomenonTime = phenomenonTime;
    }

    /**
     * @return the resultTime
     */
    public Date getResultTime() {
        return resultTime;
    }

    /**
     * @param resultTime the resultTime to set
     */
    public void setResultTime(Date resultTime) {
        this.resultTime = resultTime;
    }

    /**
     * @return the identifier
     */
    public UnknownContainerSOSDB getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(UnknownContainerSOSDB identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the parameter
     */
    public ParameterTypeSOSDB getParameter() {
        return parameter;
    }

    /**
     * @param parameter the parameter to set
     */
    public void setParameter(ParameterTypeSOSDB parameter) {
        this.parameter = parameter;
    }
}
