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
	"flag"
	"log"
	"os"

	"airsenseur.org/AirSenServer/api"
	"airsenseur.org/AirSenServer/config"
	"airsenseur.org/AirSenServer/database"
	_ "airsenseur.org/AirSenServer/database"
)

func main() {
	// Parse flags
	debug := flag.Bool("debug", false, "Activates extended logs")
	flag.StringVar(&api.ServerName, "server", "", "The server name to listen to, e.g. example.com. In most cases you can leave this empty.")
	flag.UintVar(&api.ServerPort, "port", 80, "The desired port where the server will be listening.")
	flag.StringVar(&api.WlanInterface, "wlan-interface", "wlan0", "FOR DEVELOPMENT PURPOSES: the machine's wlan interface.")
	flag.StringVar(&config.ConfigFileName, "config-file", "./airsenseur.properties", "Use a different configuration file.")
	flag.StringVar(&config.RootPath, "root-path", "", "FOR DEVELOPMENT PURPOSES: the machine's root path.")
	flag.BoolVar(&database.LastData, "last-data", false, "FOR DEVELOPMENT PURPOSES: normally the sensor measures served by the API are at most 30 minutes old. When this option is active, the data served will be at most 30 minutes older than the last received measure.")

	flag.Parse()

	// Logging options
	if *debug {
		log.SetFlags(log.LstdFlags | log.Lshortfile)
	}

	// Load app configuration
	config.LoadAppConfiguration()

	// Wake up server and exit on errors
	err := <-api.Wake()
	log.Println(err.Error())
	os.Exit(1)
}
