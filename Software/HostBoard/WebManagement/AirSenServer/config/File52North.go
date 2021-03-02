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

package config

var kv52North = keyValueFile{
	path:      &AppConfig.Config52North,
	separator: "=",
}

// File52North represents the local 52North configuration
type File52North struct {
	Enabled         bool   `json:"enabled"          configName:"enabled"`
	Hostname        string `json:"hostname"         configName:"sos.hostname"`
	Port            int    `json:"port"             configName:"sos.port"`
	Foi             string `json:"foi"              configName:"sos.foi.name"`
	Endpoint        string `json:"endpoint"         configName:"sos.endpoint"`
	OfferingName    string `json:"offeringName"     configName:"sos.offering.name"`
	UpdateLocation  bool   `json:"updateLocation"   configName:"sos.foi.updatelocation"`
	ObservationByID bool   `json:"observationById"  configName:"sos.observation.byid"`
}

// WriteProperties writes the configuration in the local configuration files.
func (f *File52North) WriteProperties() error {
	return kv52North.writeProperties(f)
}

// ReadProperties obtains the configuration from the local configuration files.
func (f *File52North) ReadProperties() error {
	return kv52North.readProperties(f)
}
