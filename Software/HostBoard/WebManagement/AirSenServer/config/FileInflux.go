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

var kvInflux = keyValueFile{
	path:      &AppConfig.ConfigInflux,
	separator: "=",
}

// FileInflux represents the local Influx configuration
type FileInflux struct {
	Enabled         bool           `json:"enabled"          configName:"enabled"`
	Hostname        string         `json:"hostname"         configName:"influxdbhost"`
	Port            int            `json:"port"             configName:"influxdbport"`
	Database        string         `json:"database"         configName:"influxdbname"`
	Dataset         string         `json:"dataset"          configName:"influxdbdataset"`
	Username        string         `json:"username"         configName:"influxdbuser"`
	Password        PasswordString `json:"password"         configName:"influxdbpasswd"`
	Encrypt         bool           `json:"encrypt"          configName:"useHTTPS"`
	Uselineprotocol bool           `json:"uselineprotocol"  configName:"uselineprotocol"`
}

// WriteProperties writes the configuration in the local configuration files.
func (f *FileInflux) WriteProperties() error {
	return kvInflux.writeProperties(f)
}

// ReadProperties obtains the configuration from the local configuration files.
func (f *FileInflux) ReadProperties() error {
	return kvInflux.readProperties(f)
}
