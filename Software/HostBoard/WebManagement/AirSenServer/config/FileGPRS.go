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

import "os/exec"

var kvGPRS = keyValueFile{
	path:      &AppConfig.ConfigGPRS,
	separator: "=",
}

// FileGPRS represents the local GPRS configuration
type FileGPRS struct {
	Enabled bool   `json:"enabled"  configName:"enabled"`
	APN     string `json:"apn"      configName:"apn"`
	SimPIN  string `json:"simPin"   configName:"simPin"`
}

// WriteProperties writes the configuration in the local configuration files.
func (f *FileGPRS) WriteProperties() error {
	if err := kvGPRS.writeProperties(f); err != nil {
		return err
	}

	parameters := f.APN + "," + f.SimPIN

	// Apply config
	return exec.Command(GetPath(AppConfig.ScriptApplyGPRSConfig), parameters).Run()
}

// ReadProperties obtains the configuration from the local configuration files.
func (f *FileGPRS) ReadProperties() error {
	return kvGPRS.readProperties(f)
}
