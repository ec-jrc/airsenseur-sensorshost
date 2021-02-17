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

Revisions:
# Before R2.0.0: influx and SOSDb basic raw data dump

# R2.0.0: 
    * added persistence for boards and sensors metadata information through influxDB
    * Tested with AirSensEURDataAggregator R2.0.0

# R2.1.4:
    * added persistence for iFLINK, MQTT and AWS MQTT protocols
    * Tested with AirSensEURDataAggregator R2.0.0

# R2.2.2:
    * added "enable" key in the configuration
    * added persistence for LoRa
    * added persistence of sensor logical channel and boardId through influxDB and LoRa
    * Tested with AirSensEURDataAggregator R2.1.0

