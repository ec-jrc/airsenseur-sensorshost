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

package configuration

import "github.com/magiconair/properties"

// Config contains the configuration keys for this application
type Config struct {
	InfluxDBHost     string
	InfluxDBPort     uint32
	InfluxDBUsername string
	InfluxDBPassword string
	InfluxDBDatabase string
	LogInfluxQueries bool
	LogLoRaMessages  bool
}

// Main is the global main configuration object
var Main *Config

// NewMainConfig instantiates the Main Configuration object
func NewMainConfig() *Config {
	return &Config{}
}

// ReadConfiguration read configuration keys from a configuration file
func (c *Config) ReadConfiguration() {

	p := properties.MustLoadFile("influxdb.properties", properties.UTF8)

	c.InfluxDBHost = p.GetString("host", "localhost")
	c.InfluxDBPort = uint32(p.GetInt("port", 8086))
	c.InfluxDBUsername = p.GetString("user", "airsenseur")
	c.InfluxDBPassword = p.GetString("password", "airsenseur")
	c.InfluxDBDatabase = p.GetString("measurement", "airsenseur")
	c.LogInfluxQueries = p.GetBool("logInflux", false)
	c.LogLoRaMessages = p.GetBool("logLora", false)
}
