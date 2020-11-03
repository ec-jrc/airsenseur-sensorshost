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
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic wrapper for MQTT transactions with eclipse.paho.client library
 * @author marco
 */
public class MQTTHelper {
    
    private final String user;
    private final String password;
    private final String baseTopic;
    private final boolean useSSL;
    private final int timeout;
    private final int qos;
    
    private final String url;
    private final static String MQTT_CLIENTID = "AirSensEUR";
    
    private final MemoryPersistence persistence = new MemoryPersistence();
    
    private MqttClient mqttClient;
    private MqttConnectOptions connOpts;
    
    private final Logger log = LoggerFactory.getLogger(MQTTHelper.class);

    public MQTTHelper(String host, String user, String password, String baseTopic, boolean useSSL, int timeout, int port, int qos) {
        this.user = user;
        this.password = password;
        this.baseTopic = baseTopic;
        this.useSSL = useSSL;
        this.timeout = timeout;
        this.qos = qos;
        
        String protocol = "tcp://";
        if (this.useSSL) {
            protocol = "ssl://";
        }
        
        this.url = protocol + host + ":" + port;
    }
    
    public String getBaseTopic() {
        return baseTopic;
    }
    
    public void openConnection() throws PersisterException {
        
        // Close any open connection, if any
        if (mqttClient != null) {
            closeConnection();
        }
        
        // Start a new connection
        try {
            mqttClient = new MqttClient(url, MQTT_CLIENTID, persistence);
            
        } catch (MqttException ex) {
            log.info("MQTT start connection failed");
            logException(ex);
        }
        
        // Generate the connection options
        connOpts = new MqttConnectOptions();        
        connOpts.setCleanSession(true);
        connOpts.setConnectionTimeout(timeout);
        connOpts.setKeepAliveInterval(60);
        connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        connOpts.setUserName(user);
        connOpts.setPassword(password.toCharArray());
        
        try {
            
            // Connect to MQTT broker
            log.info("MQTT connecting to broker");
            mqttClient.connect(connOpts);
            log.info("Connected");
            
        } catch (MqttException ex) {
            logException(ex);
        }
    }
    
    public void closeConnection() throws PersisterException {
        
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
            } catch (MqttException ex) {
                logException(ex);
            }
            mqttClient = null;
        }
    }
    
    
    public boolean sendDataToMQTT(Topic topic) throws PersisterException {
        
        // Loop on each subtopic in the topic
        for (Topic subtopic:topic.getSubTopics()) {
            
            String fullTopicName = subtopic.getFullName();
            String value = subtopic.getValue();
            
            MqttMessage message = new MqttMessage(value.getBytes());
            message.setQos(qos);
            
            try {
                mqttClient.publish(fullTopicName, message);
            } catch (MqttException ex) {
                logException(ex);
            }
        }
        
        return true;
    }
    
    private void logException(MqttException ex) throws PersisterException {
        
        log.info("MQTT exception: ");
        log.info(" message: " + ex.getMessage());
        log.info(" reason: " + ex.getReasonCode());
        
        throw new PersisterException(ex.getMessage());
    }
}
