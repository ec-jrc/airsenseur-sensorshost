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
# Before R2.0.0: basic retrieval of samples and persistence to raw samples through sensor bus protocol P1.0 and P2.0

# R2.0.0: 
    * added auto-discovery for sensor-bus connected boards and host battery sensors.
    * added auto-discovery of relevant serial, name and other data for sensors connected to sensor-bus
    * added high resolution last sample inquiry for float transactions
    * compliant with sensor bus protocol P2.1
    * Tested with AirSensEURDataAggregator R2.0.0

# R2.1.0:
    * added CRC-32 on sensor-bus connected boards answers. 
      This feature is optional and needs to be enabled by the configuration key:
      useCRCInSensorBus=true
      and requires shield firmware compliant with P3.0
    * added association between sensor logical channel and boardId in sensors table.
    * Tested with AirSensEURDataAggregator R2.2.1

