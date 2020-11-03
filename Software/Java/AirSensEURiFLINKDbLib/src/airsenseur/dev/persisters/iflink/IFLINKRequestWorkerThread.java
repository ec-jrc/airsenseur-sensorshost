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

package airsenseur.dev.persisters.iflink;

/**
 *
 * @author marco
 */
public abstract class IFLINKRequestWorkerThread implements Runnable {
    
    private final String apiUrl;
    private final int debugVerbose;
    private final String sensorID;
    private final String bearerToken;
    private final String json;
    private final SamplePersisterIFLINKBase parent;
    
    public IFLINKRequestWorkerThread(String apiUrl, int debugVerbose, String sensorID, String bearerToken, String json, SamplePersisterIFLINKBase parent) {
        this.apiUrl = apiUrl;
        this.debugVerbose = debugVerbose;
        this.sensorID = sensorID;
        this.bearerToken = bearerToken;
        this.json = json;
        this.parent = parent;
    }
     
    @Override
    public void run() {
        
        boolean result = executeRequest(apiUrl, getDebugVerbose(), sensorID, bearerToken, json);
        getParent().setResult(result);
    }
    
    // This is where the request is done. 
    // It sould return false if any problem was found when executing the request, true otherwise.
    public abstract boolean executeRequest(String apiUrl, int debugVerbose, String sensorID, String bearerToken, String json);

    /**
     * @return the debugVerbose
     */
    public int getDebugVerbose() {
        return debugVerbose;
    }

    /**
     * @return the parent
     */
    public SamplePersisterIFLINKBase getParent() {
        return parent;
    }
}
