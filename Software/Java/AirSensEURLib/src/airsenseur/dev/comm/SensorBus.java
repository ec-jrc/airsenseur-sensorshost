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
 * Defines a generic interface for all commands could be performed on a physical sensor bus
 * @author marco
 */
public interface SensorBus {
    
    // Defines a callback for DataMessages received from the bus and flowing
    // to application layer
    public static interface SensorBusMessageConsumer {
        public void onNewMessageReady(AppDataMessage message);
    }
    
    public void init(SensorBusMessageConsumer messageConsumer, CommChannelFactory.commChannelType commChannelType, TransportLogicFactory.transportLogicType type) throws SensorBusException;

    public void connectToBus(String busIdentifier) throws SensorBusException;
    public void disConnectFromBus();
    public boolean reConnectToBus();
    public void takeBusOwnership() throws SensorBusException;
    public void releaseBusOwnership() throws SensorBusException;
    
    public void writeMessageToBus(AppDataMessage message) throws SensorBusException;
}

