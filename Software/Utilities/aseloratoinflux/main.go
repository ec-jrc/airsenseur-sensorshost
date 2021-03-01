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

package main

import (
	"fmt"
	"log"
	"net/http"

	"./apis"
	"./configuration"
	"./influx"

	"github.com/gorilla/mux"
)

func main() {

	router := mux.NewRouter()

	// Apis used by the associated react pages
	router.HandleFunc("/restapi/uplink", apis.HandleUplinkData).Methods("POST")

	// A suitable log
	fmt.Println("AirSensEUR LoRa HTTP integration to InfluxDB")
	fmt.Println("Revision 1.0")
	fmt.Println("www.airsenseur.org")
	fmt.Println("Server running at port 5000")

	// Load the main configuration object
	configuration.Main = configuration.NewMainConfig()
	configuration.Main.ReadConfiguration()

	// Some useful debug information
	fmt.Println("---------------------------")
	fmt.Println(fmt.Sprintf("InfluxDB URL: http://%s:%d", configuration.Main.InfluxDBHost, configuration.Main.InfluxDBPort))
	fmt.Println(fmt.Sprintf("InfluxDB database: %s", configuration.Main.InfluxDBDatabase))

	// Load ASE Database
	configuration.AirSensEURDB = configuration.NewASEDb()
	configuration.AirSensEURDB.LoadFromFile("asedb.json")

	// Initialize InfluxDB
	influx.Influx = influx.NewDB()
	influx.Influx.Open(configuration.Main.InfluxDBHost, configuration.Main.InfluxDBPort, configuration.Main.InfluxDBUsername, configuration.Main.InfluxDBPassword, configuration.Main.InfluxDBDatabase)

	err := http.ListenAndServe(":5000", router)
	if err != nil {
		log.Println(err)
	}
}
