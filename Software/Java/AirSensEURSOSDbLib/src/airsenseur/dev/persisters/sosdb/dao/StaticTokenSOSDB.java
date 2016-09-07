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

/**
 * This class aggregates a set of static tokens used in the SOSDB Library
 * to represent unmodifiable parameters in the JSON vectors 
 * @author marco
 */
public class StaticTokenSOSDB {
    
    public final static String OBSERVATION_TYPE = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement";
    
    public final static String IDENTIFIER_CODESPACE_UNKNOWN = "http://www.opengis.net/def/nil/OGC/0/unknown";
    public final static String IDENTIFIER_CODESPACE_SAMPLINGGEOMETRY = "http://www.opengis.net/def/param-name/OGC-OM/2.0/samplingGeometry";
}
