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
	"log"
	"net/http"
	"strconv"
	"strings"

	rice "github.com/GeertJohan/go.rice"
	"github.com/gorilla/mux"
	"airsenseur.org/AirSenServer/config"
)

// Wake : setups and starts the http web server
func Wake() chan error {
	// API router and handler
	router := mux.NewRouter()
	router.Use(corsMiddleware)

	apiRouter := router.PathPrefix("/api/").Subrouter()

	apiRouter.Handle("/settings/{file}", settingsGet).Methods("OPTIONS", "GET")
	apiRouter.Handle("/settings/{file}", settingsSet).Methods("OPTIONS", "POST")

	apiRouter.Handle("/info/server-running", infoServerRunning).Methods("OPTIONS", "GET")
	apiRouter.Handle("/info/machine-status", infoMachineStatus).Methods("OPTIONS", "GET")
	apiRouter.Handle("/info/uuid", infoUniqueID).Methods("OPTIONS", "GET")
	apiRouter.Handle("/info/gprs-test", infoGPRSTest).Methods("OPTIONS", "POST")
	apiRouter.Handle("/info/logs", infoLogs).Methods("OPTIONS", "GET")
	apiRouter.Handle("/info/wifi-test", infoWifiTest).Methods("OPTIONS", "POST")
	apiRouter.Handle("/info/wifi-test-result", infoWifiTestResult).Methods("OPTIONS", "POST")
	apiRouter.Handle("/info/wifi-networks", infoWifiNetworks).Methods("OPTIONS", "GET")
	apiRouter.Handle("/info/wifi-networks-result", infoWifiNetworksResult).Methods("OPTIONS", "GET")

	apiRouter.Handle("/backup/data", backupData).Methods("OPTIONS", "GET")
	apiRouter.Handle("/backup/config", backupConfig).Methods("OPTIONS", "GET")
	apiRouter.Handle("/backup/config-restore", backupConfigRestore).Methods("OPTIONS", "POST")

	apiRouter.Handle("/do/delete-data", doScript(config.AppConfig.ScriptDeleteAllData, "All data deleted")).Methods("OPTIONS", "POST")
	apiRouter.Handle("/do/reboot", doScript(config.AppConfig.ScriptReboot, "Reboot requested")).Methods("OPTIONS", "POST")
	apiRouter.Handle("/do/push-data-now", doScript(config.AppConfig.ScriptDataPushNow, "Performing manual data push")).Methods("OPTIONS", "POST")
	apiRouter.Handle("/do/datapush-enable", doScript(config.AppConfig.ScriptDataPushEnable, "Data push enabled")).Methods("OPTIONS", "POST")
	apiRouter.Handle("/do/datapush-disable", doScript(config.AppConfig.ScriptDataPushDisable, "Data push disabled")).Methods("OPTIONS", "POST")
	apiRouter.Handle("/do/sampling-enable", doScript(config.AppConfig.ScriptSamplingEnable, "Sampling enabled")).Methods("OPTIONS", "POST")
	apiRouter.Handle("/do/sampling-disable", doScript(config.AppConfig.ScriptSamplingDisable, "Sampling disabled")).Methods("OPTIONS", "POST")

	apiRouter.PathPrefix("/").Handler(noAPI)

	// GUI Handler
	box := fallbackFileServer{box: rice.MustFindBox("../_react/build").HTTPBox()}
	router.PathPrefix("/").Handler(http.FileServer(box))

	http.Handle("/", router)

	// Init error channel and start server
	errChan := make(chan error)
	startHTTPServer(errChan)
	return errChan
}

// Starts the HTTP server in a new goroutine
func startHTTPServer(errChan chan error) {
	s := &http.Server{Addr: ServerName + ":" + strconv.Itoa(int(ServerPort))}
	go func() {
		server := strings.TrimPrefix(ServerName, "http://")
		if server == "" {
			server = "localhost"
		}

		port := strconv.Itoa(int(ServerPort))

		log.Printf("[SERVER] Starting server at http://%v:%v\n", server, port)

		if err := s.ListenAndServe(); err != nil {
			if strings.Contains(err.Error(), "address already in use") {
				log.Printf("Port %v appears to be already in use, please check and retry\n", ServerPort)
				errChan <- err
				return
			}

			// In case of error notify main goroutine
			errChan <- err
		}
	}()
}

// Middleware for CORS compatibility
func corsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Headers", "Origin, X-Requested-With, X-HTTP-Method-Override, Content-Type, Accept, Authorization")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}

// Rice box setup
type fallbackFileServer struct{ box http.FileSystem }

func (p fallbackFileServer) Open(path string) (http.File, error) {
	file, err := p.box.Open(path)
	if err != nil {
		return p.box.Open("/index.html")
	}

	return file, err
}
