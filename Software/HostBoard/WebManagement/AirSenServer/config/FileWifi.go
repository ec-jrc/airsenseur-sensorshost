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

import (
	"crypto/sha1"
	"fmt"
	"os/exec"
	"strings"

	"golang.org/x/crypto/pbkdf2"
)

var kvWifiAP = keyValueFile{
	path:      &AppConfig.ConfigWifiAP,
	separator: "=",
}

var kvWifiClient = keyValueFile{
	path:      &AppConfig.ConfigWifiClient,
	separator: " ",
}

// FileWifi represents the wifi configuration of the machine.
// Since it writes to two different file it embeds two sub-structs
type FileWifi struct {
	fileWifiAP
	fileWifiClient
	WorkingMode string `json:"workingMode"`
	currentIP   string

	skipPasswordEncoding bool
}

type fileWifiAP struct {
	ApSSID     string         `json:"apSSID"            configName:"ssid"`
	ApPassword string         `json:"apPassword"        configName:"wpa_passphrase"`
}

type fileWifiClient struct {
	ClientSSID     string         `json:"clientSSID"        configName:"wpa-ssid"`
	ClientPassword PasswordString `json:"clientPassword"    configName:"wpa-psk"`
}

// WriteProperties writes the config on the machine (in two different files).
// The passphrase is obtained by encrypting the password provided by the user.
func (f *FileWifi) WriteProperties() error {
	if !f.skipPasswordEncoding {
		// Computing passphrase
		psk := pbkdf2.Key([]byte(f.ClientPassword), []byte(f.ClientSSID), 4096, 32, sha1.New)
		f.ClientPassword = ""

		for i := 0; i < 32; i++ {
			f.ClientPassword += PasswordString(fmt.Sprintf("%02x", psk[i]))
		}
	}

	if err := kvWifiAP.writeProperties(&f.fileWifiAP); err != nil {
		return err
	}

	if err := kvWifiClient.writeProperties(&f.fileWifiClient); err != nil {
		return err
	}

	output, err := exec.Command(GetPath(AppConfig.ScriptApplyWifiConfig), fmt.Sprint(f.WorkingMode)).Output()
	if err != nil {
		return err
	}

	f.currentIP = strings.Trim(string(output), " \n")

	return nil
}

// ReadProperties obtains the configuration from the local configuration files.
func (f *FileWifi) ReadProperties() error {
	if err := kvWifiAP.readProperties(&f.fileWifiAP); err != nil {
		return err
	}

	if err := kvWifiClient.readProperties(&f.fileWifiClient); err != nil {
		return err
	}

	output, err := exec.Command(GetPath(AppConfig.ScriptCheckWifiMode)).Output()
	if err != nil {
		return err
	}

	f.WorkingMode = strings.ToLower(strings.Trim(string(output), " \n"))

	return nil
}

// TestWifiConfiguration applies the proposed configuration, tests it and
// then goes back to the current one and returns the ip address got by the new
// configuration, if it works, otherwise returns an error
func (f *FileWifi) TestWifiConfiguration() (string, bool, error) {
	// Load current config
	oldConfig := &FileWifi{skipPasswordEncoding: true}
	oldConfig.ReadProperties()

	// Save and apply new config
	if err := f.WriteProperties(); err != nil {
		oldConfig.WriteProperties()
		return "", false, err
	}

	// Rollback to old configuration
	if err := oldConfig.WriteProperties(); err != nil {
		return "", false, err
	}

	return f.currentIP, true, nil
}
