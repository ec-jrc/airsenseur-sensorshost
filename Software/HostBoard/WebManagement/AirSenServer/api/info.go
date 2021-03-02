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
	"encoding/hex"
	"encoding/json"
	"io/ioutil"
	"log"
	"net/http"
	"os/exec"
	"strconv"
	"strings"
	"sync"

	"airsenseur.org/AirSenServer/config"
	"airsenseur.org/AirSenServer/database"
)

// infoServerRunning : simple handler used to check if the server is running (e.g. after
// a reboot)
var infoServerRunning = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte("true"))
})

// infoMachineStatus : returns the status (boards, sensors and last measures) of the machine
var infoMachineStatus = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	machine, err := database.GetMachineStatus()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	batteryStatus, err := exec.Command(config.GetPath(config.AppConfig.ScriptBatteryStatus)).Output()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	gprsStatus, err := exec.Command(config.GetPath(config.AppConfig.ScriptGPRSStatus)).Output()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	gpsStatus, err := exec.Command(config.GetPath(config.AppConfig.ScriptGPSStatus)).Output()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	dataPushStatus, err := exec.Command(config.GetPath(config.AppConfig.ScriptDataPushStatus)).Output()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	samplingStatus, err := exec.Command(config.GetPath(config.AppConfig.ScriptSamplingStatus)).Output()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	response := struct {
		database.Machine
		BatteryStatus     string
		GPRSStatus        string
		GPSStatus         string
		DataPushStatus    bool
		SamplingStatus    bool
	}{
		Machine:           machine,
		BatteryStatus:     string(batteryStatus),
		GPRSStatus:        string(gprsStatus),
		GPSStatus:         string(gpsStatus),
		DataPushStatus:    strings.ToLower(strings.Trim(string(dataPushStatus), " \n")) == "enabled",
		SamplingStatus:    strings.ToLower(strings.Trim(string(samplingStatus), " \n")) == "enabled",
	}

	// Marshal and send to client
	output, err := json.Marshal(response)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(output)
})

// infoUniqueID : returns the unique id of the machine
var infoUniqueID = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	uuid := ""
	uuidFile, err := ioutil.ReadFile(config.GetPath(config.AppConfig.FileEEPROM))
	if err != nil {
		uuid = "Unknown"
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
	} else {
		uuid = strings.ToUpper(hex.EncodeToString(uuidFile[250:256]))
		uuid = uuid[0:2] + ":" + uuid[2:4] + ":" + uuid[4:6] + ":" + uuid[6:8] + ":" + uuid[8:10] + ":" + uuid[10:12]
	}

	// Marshal and send to client
	output, err := json.Marshal(uuid)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(output)
})

// infoGPRSTest : tests the proposed GPRS configuration
var infoGPRSTest = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusBadRequest)
		return
	}

	request := struct {
		APN, PIN, HostToPing string
	}{}

	err = json.Unmarshal(body, &request)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	output, err := exec.Command(config.GetPath(config.AppConfig.ScriptTestGPRS), request.APN, request.PIN, request.HostToPing).Output()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/text")
	w.Write(output)
})

// infoLogs : returns the machine's logs
var infoLogs = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	logs := make(map[string]string)
	logFiles := strings.Split(config.AppConfig.MaintenanceLogFiles, ":")

	for _, logFile := range logFiles {
		content, err := ioutil.ReadFile(config.GetPath(logFile))
		if err != nil {
			logs[logFile] = "File not found"
			continue
		}

		// Send only the last 50kb per file
		logMaxSize := 50000
		if len(content) > logMaxSize {
			logs[logFile] = "...\n" + string(content[len(content)-logMaxSize:])
		} else {
			logs[logFile] = string(content)
		}
	}

	// Marshal and send to client
	output, err := json.Marshal(logs)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(output)
})

// infoWifiTest : http handler that tests a wifi configuration. When the test
// is started clients will likely lose connectivity, until the test function
// reverts back to the old working configuration.
// For this reason, after launching this handler clients should cyclically
// request the infoWifiTestResult handler until they have a response.
var infoWifiTest = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	wifiConfigFile := &config.FileWifi{}

	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusBadRequest)
		return
	}

	if err := json.Unmarshal(body, &wifiConfigFile); err != nil {
		logHTTPError(w, r, err.Error(), http.StatusBadRequest)
		return
	}

	go func() {
		ipAddr, valid, err := wifiConfigFile.TestWifiConfiguration()
		if err != nil {
			log.Println(err.Error())
			return
		}

		newWifiMutex.Lock()
		newWifiIP = ipAddr
		newWifiValid = valid
		newWifiMutex.Unlock()
	}()
})

// infoWifiTestResult : this should be called cyclically after infoWifiTest until server
// finally responds to client. Provides wifi config test result and new IP address
var infoWifiTestResult = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	newWifiMutex.RLock()
	result := struct {
		Valid     bool
		IPAddress string
	}{
		Valid:     newWifiValid,
		IPAddress: newWifiIP,
	}
	newWifiMutex.RUnlock()

	output, err := json.Marshal(result)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(output)
})

// infoWifiNetworks : returns the list of wifi networks within reach
var infoWifiNetworks = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	go func() {
		output, err := exec.Command(config.GetPath(config.AppConfig.ScriptWifiScan)).Output()
		if err != nil {
			logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
			return
		}

		wifiScanMutex.Lock()
		wifiScanResult = string(output)
		wifiScanMutex.Unlock()
	}()

	w.Header().Set("Content-Type", "application/json")
})

// infoWifiNetworksResult : this should be called cyclically after infoWifiNetworks until server
// finally responds to client. Provides wifi scan result
var infoWifiNetworksResult = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	wifiScanMutex.RLock()
	result := wifiScanResult
	wifiScanMutex.RUnlock()

	type network struct {
		Name    string
		Quality int
	}

	networks := []network{}

	for _, net := range strings.Split(strings.Trim(result, " \n"), "#") {
		splitted := strings.Split(net, " ")
		quality, _ := strconv.Atoi(splitted[len(splitted)-1])

		networks = append(networks, network{
			Name:    strings.Join(splitted[:len(splitted)-1], " "),
			Quality: quality,
		})
	}

	// Marshal and send to client
	output, err := json.Marshal(networks)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(output)
})

var (
	newWifiMutex sync.RWMutex
	newWifiIP    string
	newWifiValid bool

	wifiScanMutex  sync.RWMutex
	wifiScanResult string
)
