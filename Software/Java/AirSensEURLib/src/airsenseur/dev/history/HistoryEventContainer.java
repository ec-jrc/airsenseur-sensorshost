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

package airsenseur.dev.history;

/**
 * Basic History event container
 * @author marco
 */
public class HistoryEventContainer {
    
    public final static String EVENT_LATEST_INFLUXDB_SAMPLEPUSH_TS = "latestInfluxDbTs";
    public final static String EVENT_LATEST_INFLUXDB_BOARDINFOPUSH_TS = "latestBoardInfoDbTs";
    public final static String EVENT_LATEST_INFLUXDB_SENSORCONFIGPUSH_TS = "latestSensorConfigDbTs";
    
    public final static String EVENT_LATEST_MQTT_SAMPLEPUSH_TS = "latestMQTTTs";
    public final static String EVENT_LATEST_AWSMQTT_SAMPLEPUSH_TS = "latestAWSMQTTTs";
    
    public final static String EVENT_LATEST_IFLINK_SAMPLEPUSH_TS = "latestIFLINKTs";
    
    private final String eventName;
    private final String eventValue;
    
    private HistoryEventContainer() {
        eventName = "";
        eventValue = "";
    }

    public HistoryEventContainer(String eventName, String eventValue) {
        this.eventName = eventName;
        this.eventValue = eventValue;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventValue() {
        return eventValue;
    }
}
