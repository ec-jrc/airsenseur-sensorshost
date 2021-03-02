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
	"io"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"os/exec"
	"time"

	"airsenseur.org/AirSenServer/config"
)

// backupConfig : creates a config backup and sends it back to client
var backupConfig = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	cmd := exec.Command(config.GetPath(config.AppConfig.ScriptBackupConfig))
	output, err := cmd.StdoutPipe()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	if err := cmd.Start(); err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Disposition", "attachment; filename=airsenseur-config.tar")
	w.Header().Set("Content-Type", "application/zip")
	w.Header().Set("Last-Modified", time.Now().UTC().Format("Mon, 02 Jan 2006 15:04:05 GMT"))
	_, err = io.Copy(w, output)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}
})

// backupData : creates a data backup and sends it back to client
var backupData = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	cmd := exec.Command(config.GetPath(config.AppConfig.ScriptBackupData))
	output, err := cmd.StdoutPipe()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	if err := cmd.Start(); err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Disposition", "attachment; filename=airsenseur-data.tar")
	w.Header().Set("Content-Type", "application/zip")
	w.Header().Set("Last-Modified", time.Now().UTC().Format("Mon, 02 Jan 2006 15:04:05 GMT"))
	_, err = io.Copy(w, output)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}
})

// backupConfigRestore : restores config from file
var backupConfigRestore = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	tmpFile, err := ioutil.TempFile(os.TempDir(), "backup-")
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}
	defer os.Remove(tmpFile.Name())

	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusBadRequest)
		return
	}

	_, err = tmpFile.Write(body)
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusBadRequest)
		return
	}

	err = exec.Command(config.GetPath(config.AppConfig.ScriptConfigRestore), tmpFile.Name()).Run()
	if err != nil {
		logHTTPError(w, r, err.Error(), http.StatusInternalServerError)
		return
	}

	log.Println("Configuration restored from file")
})
