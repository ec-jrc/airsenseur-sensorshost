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
 * Implements a "Flat" transport logic, i.e. AppDataMessages are copied to CommChannelDataMessage and viceversa
 * @author marco
 */
public class TransportLogicFlat extends TransportLogicBaseImpl {

    public TransportLogicFlat(AppDataMessageQueue rxDataQueue, AppDataMessageQueue txDataQueue, SensorBus parent, CommChannel commChannel) {
        super(rxDataQueue, txDataQueue, parent, commChannel);
    }

    @Override
    protected char getProtocolVersion() {
        throw new UnsupportedOperationException("getProtocolVersion it's not supposed to be available on Flat Transport Logic");
    }

    @Override
    public int getBaudrate() {
        return 0;
    }

    @Override
    public AppDataMessage onRxCharReceived(byte value) {
        throw new UnsupportedOperationException("onRxCharReceived it's not supposed to be available on Flat Transport Logic");
    }

    @Override
    public CommChannelDataMessage toSerialBusFormat(AppDataMessage dataMessage) {
        return new CommChannelDataMessage(dataMessage.getBoardId(), dataMessage.getCommandString());
    }

    @Override
    public void onDataReceived(CommChannelDataMessage message) throws InterruptedException {
        AppDataMessage dataMessage = new AppDataMessage(message.getBoardId(), message.getMessage());
        getRxDataQueue().put(dataMessage);
    }
}
