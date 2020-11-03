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

package airsenseur.dev.persisters.mqtt;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.history.HistoryEventContainer;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SamplesPersister;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marco
 */
public class SamplePersisterMQTT extends MQTTHelper implements SamplesPersister {
    
    private final Logger log = LoggerFactory.getLogger(SamplePersisterMQTT.class);
    
    public SamplePersisterMQTT(String host, String user, String password, String baseTopic, boolean useSSL, int timeout, int port, int qos) {
        super(host, user, password, baseTopic, useSSL, timeout, port, qos);
    }

    @Override
    public boolean startNewLog() throws PersisterException {
        log.info("SamplePeristerMQTT enabled");
        
        openConnection();
        
        return true;
    }

    @Override
    public void stop() {
        try {
            log.info("SamplePeristerMQTT stopped");
            closeConnection();
        } catch (PersisterException ex) {
            log.info("Error closing connection. Warns and continue.");
        }
    }

    /**
     * Push a single sample to MQTT server
     * @param sample
     * @return
     * @throws PersisterException 
     */
    @Override
    public boolean addSample(SampleDataContainer sample) throws PersisterException {
        
        Topic baseTopic = new Topic(getBaseTopic());
        Topic sensorRootTopic = baseTopic.fromSample(sample);
        
        return sendDataToMQTT(sensorRootTopic);
    }

    /**
     * Push several samples to the MQTT server
     * @param samples
     * @return
     * @throws PersisterException 
     */
    @Override
    public boolean addSamples(List<SampleDataContainer> samples) throws PersisterException {
        
        if (samples == null) {
            throw new PersisterException(("Invalid parameter on addSamples"));
        }
        
        // Loop on samples and send all through MQTT
        for (SampleDataContainer sample:samples) {
            if (!addSample(sample)) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public String getPersisterMarker(int channel) {
        return HistoryEventContainer.EVENT_LATEST_MQTT_SAMPLEPUSH_TS;
    }
    
}
