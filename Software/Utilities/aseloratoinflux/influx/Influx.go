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

package influx

import (
	"fmt"
	"log"
	"net/http"
	"strings"

	"../configuration"
)

// DB is the main sructure for the influxDB wrapper
type DB struct {
	URL      string
	UserName string
	Password string
	Client   *http.Client
}

// SensorValue is the datastruct for sample values in InfluxDB
type SensorValue struct {
	Name           string
	Channel        uint16
	EvaluatedVal   float32
	BoardTimesTamp uint32
	JavaTimeStamp  uint64
	GPSTimeStamp   uint64
	Longitude      float32
	Latitude       float32
	Elevation      float32
}

// BoardConfig is the datastruct for sensor board configuration data in InfluxDB
type BoardConfig struct {
	UnixTimeStamp uint64
	BoardID       uint32
	BoardType     string
	FwRevision    string
	Serial        string
}

// SensorConfig is the datastruct for sensor configuration data in InfluxDB
type SensorConfig struct {
	UnixTimeStamp    uint64
	Channel          uint16
	Name             string
	Serial           string
	MeasurementUnits string
}

// Influx is the main global Influx database structure
var Influx *DB

// NewDB is the default constructor
func NewDB() *DB {
	return &DB{}
}

// Open a new connection
func (db *DB) Open(host string, port uint32, userName string, password string, database string) {

	db.URL = fmt.Sprintf("http://%s:%d/write?db=%s&precision=ms", host, port, database)
	db.UserName = userName
	db.Password = password
	db.Client = &http.Client{}
}

// WriteSensorSamples writes sensor samples to InfluxDB through HTTP POSTs
func (db *DB) WriteSensorSamples(measurementName string, sensors []SensorValue) error {

	// Generate LineProtocol based write insertion
	postData := ""
	for _, sensor := range sensors {
		postData = postData + renderSensorSampleLineProtocol(measurementName, sensor)
	}

	// Push data to the influx remote database
	err := db.influxPOSTData(postData)

	return err
}

// WriteBoardsConfiguration writes boards configurations to InfluxDB through HTTP POSTs
func (db *DB) WriteBoardsConfiguration(measurementName string, boards []BoardConfig) error {

	// Generate LineProtocol based write intersion
	postData := ""
	for _, board := range boards {
		postData = postData + renderBoardConfigLineProtocol(measurementName, board)
	}

	// Push data to the influx remote database
	err := db.influxPOSTData(postData)

	return err
}

// WriteSensorsConfiguration writes sensors configurations to InfluxDB through HTTP POSTs
func (db *DB) WriteSensorsConfiguration(measurementName string, sensors []SensorConfig) error {

	// Generate LineProtocol based write intersion
	postData := ""
	for _, sensor := range sensors {
		postData = postData + renderSensorConfigLineProtocol(measurementName, sensor)
	}

	// Push data to the influx remote database
	err := db.influxPOSTData(postData)

	return err
}

func (db *DB) influxPOSTData(postData string) error {

	// Log info
	if configuration.Main.LogInfluxQueries {
		log.Println("Influx - Sending: " + postData)
	}

	req, err := http.NewRequest("POST", db.URL, strings.NewReader(postData))
	if err != nil {
		return err
	}
	req.SetBasicAuth(db.UserName, db.Password)
	req.Header.Set("content-type", "text/plain")
	resp, err := db.Client.Do(req)
	if err != nil {
		return err
	}
	if resp.StatusCode != 204 {
		return fmt.Errorf("InfluxDB server error response code: %d and status %s", resp.StatusCode, resp.Status)
	}

	return nil
}

func renderSensorSampleLineProtocol(measurementName string, sensorValue SensorValue) string {

	lineProtocol := fmt.Sprintf("%s,name=%s,channel=%d sampleEvaluatedVal=%f,boardTimeStamp=%d,gpsTimeStamp=%d,latitude=%f,longitude=%f,altitude=%f,calibrated=%f %d\n",
		measurementName, sensorValue.Name, sensorValue.Channel, sensorValue.EvaluatedVal, sensorValue.BoardTimesTamp, sensorValue.GPSTimeStamp, sensorValue.Latitude, sensorValue.Longitude, sensorValue.Elevation, sensorValue.EvaluatedVal, sensorValue.JavaTimeStamp)

	return lineProtocol
}

func renderBoardConfigLineProtocol(measurementName string, boardInfo BoardConfig) string {

	lineProtocol := fmt.Sprintf("%s_Boards,boardid=%d,boardtype=%s firmwarerev=\"%s\",serialnumber=\"%s\" %d\n",
		measurementName, boardInfo.BoardID, boardInfo.BoardType, boardInfo.FwRevision, boardInfo.Serial, boardInfo.UnixTimeStamp)

	return lineProtocol
}

func renderSensorConfigLineProtocol(measurementName string, sensorConfig SensorConfig) string {

	lineProtocol := fmt.Sprintf("%s_Sensors,name=%s,sensorid=%d serial=\"%s\",units=\"%s\",enabled=true %d\n",
		measurementName, sensorConfig.Name, sensorConfig.Channel, sensorConfig.Serial, sensorConfig.MeasurementUnits, sensorConfig.UnixTimeStamp)

	return lineProtocol
}
