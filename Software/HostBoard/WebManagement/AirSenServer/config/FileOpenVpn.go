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
	"fmt"
	"io/ioutil"
	"os/exec"
	"strconv"
	"strings"
)

var kvOpenvpn = keyValueFile{
	path:        &AppConfig.ConfigOpenVpn,
	separator:   " ",
	boolOnlyKey: true,
}

// FileOpenVpn represents the local OpenVPN configuration
type FileOpenVpn struct {
	Enabled           bool   `json:"enabled"            configName:"enabled"`
	Remote            string `json:"-"                  configName:"remote"`
	Hostname          string `json:"hostname"`
	Port              int    `json:"port"`
	Protocol          string `json:"protocol"           configName:"proto"`
	NsCertType        bool   `json:"nsCertType"         configName:"ns-cert-type"`
	UseLZOCompression bool   `json:"useLZOCompression"  configName:"comp-lzo"`
	PublicServerCert  string `json:"publicServerCert"`
	PublicClientKey   string `json:"publicClientKey"`
	PublicClientCert  string `json:"publicClientCert"`
}

// WriteProperties saves OpenVPN properties on the respective files
// This is bit more complicated that usual. It writes on 4 different
// files (configuration and 3 certificates) and some properties need
// to be combined writing on file (for example "remote" is
// "hostname port").
func (f *FileOpenVpn) WriteProperties() error {
	// Writes certificate files
	if err := ioutil.WriteFile(GetPath(AppConfig.OpenVpnClientCert), []byte(f.PublicClientCert), 0644); err != nil {
		return err
	}

	if err := ioutil.WriteFile(GetPath(AppConfig.OpenVpnClientKey), []byte(f.PublicClientKey), 0644); err != nil {
		return err
	}

	if err := ioutil.WriteFile(GetPath(AppConfig.OpenVpnServerCert), []byte(f.PublicServerCert), 0644); err != nil {
		return err
	}

	// "Remote" is a combination of Hostname and Port
	f.Remote = fmt.Sprintf("%v %v", f.Hostname, f.Port)

	if err := kvOpenvpn.writeProperties(f); err != nil {
		return err
	}

	// Apply config
	return exec.Command(GetPath(AppConfig.ScriptApplyOpenVpnConfig), fmt.Sprint(f.Enabled)).Run()
}

// ReadProperties reads OpenVPN properties from files.
// Reads the configuration and certificates and obtains hostname
// and port from "remote".
func (f *FileOpenVpn) ReadProperties() error {
	if err := kvOpenvpn.readProperties(f); err != nil {
		return err
	}

	// Get Hostname and Port from Remote
	remote := strings.Split(f.Remote, " ")
	if len(remote) == 2 {
		f.Hostname = remote[0]

		port, _ := strconv.ParseInt(remote[1], 10, 32)
		f.Port = int(port)
	}

	// Retrieve certificate files
	pcc, err := ioutil.ReadFile(GetPath(AppConfig.OpenVpnClientCert))
	if err != nil {
		return err
	}

	pck, err := ioutil.ReadFile(GetPath(AppConfig.OpenVpnClientKey))
	if err != nil {
		return err
	}

	psc, err := ioutil.ReadFile(GetPath(AppConfig.OpenVpnServerCert))
	if err != nil {
		return err
	}

	f.PublicClientCert = string(pcc)
	f.PublicClientKey = string(pck)
	f.PublicServerCert = string(psc)

	return nil
}
