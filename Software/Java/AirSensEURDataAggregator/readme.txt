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
# Before R2.0.0: basic aggregation of samples and persistence to raw samples through SQLite.

# R2.0.0: 
    * added auto-discovery for sensor-bus connected boards and host battery sensors.
    * added persistency of sensors serials, measurement units, sample time, channel enabled
    * added persistency of boards serials, type, id
    * Tested with AirSensEURHost R2.0.0

# R2.1.0:
    * added association between sensor logical channel and boardId in sensors table.
      To run this version, old airsenseur.db needs to be updated manually with the following command:
      sqlite3 /usr/local/airsenseur/AirSensEURDataAggregator/airsenseur.db "ALTER TABLE sensors ADD COLUMN \`boardId\` INT NOT NULL DEFAULT 0;"
    * New: avoid to ask to the Host samples for disabled channels
    * Fix: boards with repeated zero timestamped values does no more triggers a start-sampling operation
    * Tested with AirSensEURHost R2.1.0

