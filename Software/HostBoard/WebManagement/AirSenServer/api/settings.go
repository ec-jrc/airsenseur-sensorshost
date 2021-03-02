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

package api

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"

	"airsenseur.org/AirSenServer/config"

	"github.com/gorilla/mux"
)

// settingsGet : http handler for getting all the known properties out of a
// configuration file
var settingsGet = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	settingsFile, err := getConfigFile(mux.Vars(r)["file"])
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusNotFound)
		return
	}

	if err := settingsFile.ReadProperties(); err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	output, err := json.Marshal(settingsFile)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(output)
})

// settingsSet : http handler that receives a json object with configuration properties
// and use it to write them in the selected resource
var settingsSet = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	settingsFile, err := getConfigFile(mux.Vars(r)["file"])
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusNotFound)
		return
	}

	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusBadRequest)
		return
	}

	if err := json.Unmarshal(body, &settingsFile); err != nil {
		logHTTPError(w, r, err.Error(), http.StatusBadRequest)
		return
	}

	if err := settingsFile.WriteProperties(); err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}
})

// getConfigFile returns the selected configuration file
func getConfigFile(name string) (config.SettingsFile, error) {
	switch name {
	case "influx":
		return &config.FileInflux{}, nil
	case "52north":
		return &config.File52North{}, nil
	case "wifi":
		return &config.FileWifi{}, nil
	case "openvpn":
		return &config.FileOpenVpn{}, nil
	case "gprs":
		return &config.FileGPRS{}, nil
	case "datetime":
		return &config.FileDateTime{}, nil
	}

	return nil, fmt.Errorf("File '%v' not found", name)
}
