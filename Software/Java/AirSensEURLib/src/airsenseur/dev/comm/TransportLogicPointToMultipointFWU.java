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

package airsenseur.dev.comm;

/**
 * Transport logic targeted to send firmware updated to enabled SensorBus devices 
 * @author marco
 */
public class TransportLogicPointToMultipointFWU extends TransportLogicPointToMultipoint {
    
    private final static char COMMPROTOCOL_PTM_VERSION = 'P';

    public TransportLogicPointToMultipointFWU(AppDataMessageQueue rxDataQueue, AppDataMessageQueue txDataQueue, SensorBus parent, CommChannel commChannel, boolean useCRCWhenAvailable) {
        super(rxDataQueue, txDataQueue, parent, commChannel, useCRCWhenAvailable);
    }

    @Override
    protected char getProtocolVersion() {
        return COMMPROTOCOL_PTM_VERSION;
    }
}
