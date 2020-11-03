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

import airsenseur.dev.persisters.SampleDataContainer;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for values with the same topic and subtopic
 * Topic has a name, an optional list of subTopics, an optional TopicParent and an optional list of TopicValues
 * @author marco
 */
public class Topic {
    
    private final String name;
    private final String fullName;
    private final List<Topic> subTopics = new ArrayList<>();
    private Object value = null;
    
    public Topic(String name) {
        this.name = name;
        this.fullName = name;
    }
    
    private Topic(String name, Topic parent) {
        this.name = name;
        
        if (parent != null) {
            fullName = parent.getFullName() + "/" + name;
        } else {
            fullName = name;
        } 
    }
    
    private Topic addSubtopic(String subtopicName) {
        
        subTopics.add(new Topic(subtopicName, this));

        return subTopics.get(subTopics.size()-1);
    }
    
    private static final String TOPICNAME_TIME = "time";
    private static final String TOPICNAME_NAME = "name";
    private static final String TOPICNAME_CHANNEL = "channel";
    private static final String TOPICNAME_SAMPLERAWVAL = "samplerawval";
    private static final String TOPICNAME_SAMPLEEVALUATEDVAL = "sampleevaluatedval";
    private static final String TOPICNAME_BOARDTIMESTAMP = "boardtimestamp";
    private static final String TOPICNAME_GPSTIMESTAMP = "gpstimestamp";
    private static final String TOPICNAME_LATITUDE = "latitude";
    private static final String TOPICNAME_LONGITUDE = "longitude";
    private static final String TOPICNAME_ALTITUDE = "altitude";
    private static final String TOPICNAME_CALIBRATED = "calibrated";
    
    // Populate this topic with information stored into sampleDataContainer
    public Topic fromSample(SampleDataContainer source) {
        
        Topic sensorRoot = addSubtopic("" + source.getChannel());
        
        sensorRoot.addSubtopic(TOPICNAME_NAME).setValue(source.getName());
        sensorRoot.addSubtopic(TOPICNAME_TIME).setValue(source.getCollectedTimestamp());
        sensorRoot.addSubtopic(TOPICNAME_CHANNEL).setValue(source.getChannel());
        sensorRoot.addSubtopic(TOPICNAME_SAMPLERAWVAL).setValue(source.getSampleVal());
        sensorRoot.addSubtopic(TOPICNAME_SAMPLEEVALUATEDVAL).setValue(source.getSampleEvaluatedVal());
        sensorRoot.addSubtopic(TOPICNAME_BOARDTIMESTAMP).setValue(source.getTimeStamp());
        sensorRoot.addSubtopic(TOPICNAME_GPSTIMESTAMP).setValue(source.getGpsTimestamp());
        sensorRoot.addSubtopic(TOPICNAME_LATITUDE).setValue(source.getLatitude());
        sensorRoot.addSubtopic(TOPICNAME_LONGITUDE).setValue(source.getLongitude());
        sensorRoot.addSubtopic(TOPICNAME_ALTITUDE).setValue(source.getAltitude());
        sensorRoot.addSubtopic(TOPICNAME_CALIBRATED).setValue(source.getCalibratedVal());
        
        return sensorRoot;
    }

    public String getName() {
        return name;
    }
    
    public String getFullName() {
        return fullName;
    }

    public List<Topic> getSubTopics() {
        return subTopics;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }

    public String getValue() {
        
        if (value != null) {
            return value.toString();
        }

        return "";
    }
}
