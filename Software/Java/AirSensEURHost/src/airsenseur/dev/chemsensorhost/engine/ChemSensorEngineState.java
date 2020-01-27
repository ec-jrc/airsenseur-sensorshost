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

package airsenseur.dev.chemsensorhost.engine;

import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.exceptions.GenericException;

/**
 *
 * @author marco
 */
public interface ChemSensorEngineState {
        
    // First time initialization
    public void init(ChemSensorHostEngine parent);
    
    // Get state name
    public String getName(); 
    
    // Before entering in this state
    public boolean enter() throws GenericException;
    
    // Asks for information on sensors and/or boards
    public boolean inquirySensors() throws GenericException;
    
    // Evaluate an incoming message. Returns true if the evaluation succeded, 
    // false if no matching evaluation has been found
    public boolean evaluateAnswer(AppDataMessage rxMessage) throws GenericException;
    
    // Returns true if this state has been terminated
    public boolean terminated();
}
