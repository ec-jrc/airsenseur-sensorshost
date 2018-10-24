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
import airsenseur.dev.helpers.TaskScheduler;
import airsenseur.dev.json.ChemSensorClient;
import airsenseur.dev.json.RawCommand;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author marco
 */
public class CommChannelJsonRPC extends TaskScheduler implements CommChannel  {
    
    private final static int TXQUEUE_FLUSH_PERIOD = 3333;
    private final static int FORCE_FLUSH_QUEUE_SIZE = 20;
    
    private ChannelDataConsumer rxDataConsumer;
    private final ChemSensorClient sensorClient = new ChemSensorClient();
    private final static int port = 8000;
    
    private final List<RawCommand> txList = new ArrayList<>();

    @Override
    public boolean openPort(String name, int rate, ChannelDataConsumer rxDataConsumer) throws SensorBusException {
        this.rxDataConsumer = rxDataConsumer;
        if (sensorClient.connect(name, port)) {
            startPeriodic(TXQUEUE_FLUSH_PERIOD);
            return true;
        }
        
        return false;
    }

    @Override
    public boolean closePort() {
        
        if (!txList.isEmpty()) {
            try {
                flushTxQueue();
            } catch (IOException e) {
            }
        }
        
        stop();
        
        sensorClient.disconnect();
        return true;
    }

    @Override
    public void writeMessage(CommChannelDataMessage message) throws IOException {
        
        // Messages are buffered and sent in ckunks to the remote board
        synchronized (txList) {
            txList.add(new RawCommand(message.getBoardId(), message.getMessage(), ""));
            
            if (txList.size() > FORCE_FLUSH_QUEUE_SIZE) {
                flushTxQueue();
            }
        }
    }

    private void flushTxQueue() throws IOException {
        
        List<RawCommand> results = sensorClient.sendRawData(txList);
        txList.clear();
        
        // Push results in the rx queue
        if ((results != null) && (rxDataConsumer != null)){
            
            for (RawCommand result:results) {
                CommChannelDataMessage resultMessage = new CommChannelDataMessage(result.boardId, result.commandString);
                try {
                    rxDataConsumer.onDataReceived(resultMessage);
                } catch (InterruptedException ex) {
                    throw new IOException("Error receaving message from network");
                }
            }
        }
    }

    @Override
    public void takeOwnership() {
        sensorClient.takeOwnership();
    }

    @Override
    public void releaseOwnership() {
        sensorClient.releaseOwnership();
    }

    @Override
    public void taskMain() {
        synchronized (txList) {
            if (!txList.isEmpty()) {
                try{
                    flushTxQueue();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public String getTaskName() {
        return "CommChannelRPCFlusher";
    }
}
