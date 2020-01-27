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

package airsenseur.dev.persisters.influxdb;

import airsenseur.dev.exceptions.PersisterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Serie is a container of points with associated metadata 
 * @author marco
 */
public abstract class Serie {
    
    private String name = "";
    private final List<String> columns = new ArrayList<>();
    private final List<Point> points = new ArrayList<>();

    public Serie() {
    }
    
    public Serie(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<Point> getPoints() {
        return points;
    }
    
    protected String safeEscape(String value) {
        value = value.replace(" ", "\\ ");
        value = value.replace(",", "\\,");
        
        return value;
    }
    
    protected String encloseInQuotation(String value) {
        return '"' + value + '"';
    }
    
    public abstract String toLineProtocol() throws PersisterException;
}
