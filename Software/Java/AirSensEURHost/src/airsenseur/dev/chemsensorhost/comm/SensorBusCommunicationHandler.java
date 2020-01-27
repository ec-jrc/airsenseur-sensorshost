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

package airsenseur.dev.chemsensorhost.comm;

import airsenseur.dev.comm.AppDataMessage;
import airsenseur.dev.comm.SensorBusBase;
import airsenseur.dev.exceptions.SensorBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Communication Handler. 
 * @author marcos
 */
public class SensorBusCommunicationHandler extends SensorBusBase {
    
    private final Logger log = LoggerFactory.getLogger(SensorBusCommunicationHandler.class);
    
    
    // From SensorBus
    @Override
    public void connectToBus(String busIdentifier) throws SensorBusException {
        
        log.info("Trying to connect to the serial port " + busIdentifier);
        super.connectToBus(busIdentifier);
        log.info("Connected successfully to serial port " + busIdentifier);
    }

    
    // From SensorBus
    @Override
    public boolean reConnectToBus() {
        
        log.info("Trying to reconnect to the serial port.");
        if (!super.reConnectToBus()) {
            log.error("Error when trying to reconnect to the serial port");
            return false;
        }
        
        return true;
    }

    // From SensorBus
    @Override
    public void writeMessageToBus(AppDataMessage message) throws SensorBusException {
        
        log.debug("T> [" + message.getBoardId() + "] (" + message.getCommandString() + ")" );
        super.writeMessageToBus(message);
    }
}
