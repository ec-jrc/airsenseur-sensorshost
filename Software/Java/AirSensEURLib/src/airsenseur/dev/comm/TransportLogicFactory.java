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

import airsenseur.dev.exceptions.SensorBusException;

/**
 * Defines all available transport logic typed and provide a convenient way to
 * generate an instance of that
 * @author marco
 */
public class TransportLogicFactory {
    
    public static enum transportLogicType {
        FLAT,
        POINT_TO_POINT,
        POINT_TO_MULTIPOINT,
        POINT_TO_MULTIPOINT_FWU,
    }
    
    public static TransportLogicBaseImpl getInstance(transportLogicType type, 
                                                        AppDataMessageQueue rxDataQueue, 
                                                        AppDataMessageQueue txDataQueue, 
                                                        SensorBus parent, 
                                                        CommChannel commChannel, 
                                                        boolean useCRCWhenAvailable) throws SensorBusException {
       
        switch (type) {
            
            case FLAT: {
                return new TransportLogicFlat(rxDataQueue, txDataQueue, parent, commChannel);
            }
            case POINT_TO_POINT: {
                return new TransportLogicPointToPoint(rxDataQueue, txDataQueue, parent, commChannel);
            }
            
            case POINT_TO_MULTIPOINT: {
                return new TransportLogicPointToMultipoint(rxDataQueue, txDataQueue, parent, commChannel, useCRCWhenAvailable);
            }
            
            case POINT_TO_MULTIPOINT_FWU: {
                return new TransportLogicPointToMultipointFWU(rxDataQueue, txDataQueue, parent, commChannel, useCRCWhenAvailable);
            }
            
            default: {
                throw new SensorBusException("Invalid transport logic specified");
            }
        }
    }
}
