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

package apis

import (
	"encoding/json"
	"fmt"
	"log"
	"math"
	"net/http"

	"../configuration"
	"../influx"
	"../lora"
)

// HandleUplinkData decode the incoming data from LoRa
func HandleUplinkData(w http.ResponseWriter, r *http.Request) {

	// Read a max of 4Kb of body and convert to JSON
	r.Body = http.MaxBytesReader(w, r.Body, 4096)
	dec := json.NewDecoder(r.Body)
	// dec.DisallowUnknownFields()

	httpIntegration := lora.HTTPIntegration{}
	err := dec.Decode(&httpIntegration)
	if err != nil {
		log.Println("Error decoding incoming LoRa POSTed data")
		return
	}

	// This is the AirSensEUR we're referring to
	aseID := httpIntegration.DeviceName

	// Flag to understand if we need to update the local AirSensEUR database info
	updateDatabase := false

	// Log info
	if configuration.Main.LogLoRaMessages {
		log.Println(fmt.Sprintf("Message from %s [%d]: ", aseID, httpIntegration.Port) + httpIntegration.Message.DecodeDataHex)
	}

	// Evaluate samples data. They're received in port 1
	if httpIntegration.Port == 1 {
		sensorPacket, err := lora.EvaluateSensorPacket(httpIntegration.Message.DecodeDataHex)
		if err != nil {
			return
		}

		unixTs := sensorPacket.UnixTimeStamp
		boardTs := sensorPacket.BoardTimeStamp
		sensorsData := sensorPacket.SensorValues

		// Get GPS configuration
		gpsInfo, err := configuration.AirSensEURDB.GetGPSInfo(aseID)
		if (err != nil) || (math.Abs(float64(unixTs-gpsInfo.UnixTimeStamp)) > 7200000) {

			// Discard GPS informations if not valid since last two hours
			gpsInfo = configuration.GPSCoord{}
		}

		// Loop on each sensor data and generate the structure for InfluxDB
		var influxSensors []influx.SensorValue
		for _, sensorData := range sensorsData {

			// Retrieve sensors info from the database
			sensorID := uint16(sensorData.Channel)
			aseSensorInfo, err := configuration.AirSensEURDB.GetSensorInfo(aseID, sensorID)
			if err != nil {
				log.Println(fmt.Sprintf("No sensor information found in database for unit %s and sensor channel %d. Discarding", aseID, sensorID))
				continue
			}

			influxSensor := influx.SensorValue{
				Name:           aseSensorInfo.Name,
				Channel:        aseSensorInfo.Channel,
				EvaluatedVal:   sensorData.Value,
				BoardTimesTamp: boardTs,
				JavaTimeStamp:  unixTs,
				GPSTimeStamp:   gpsInfo.UnixTimeStamp,
				Longitude:      gpsInfo.Longitude,
				Latitude:       gpsInfo.Latitude,
				Elevation:      gpsInfo.Elevation}

			influxSensors = append(influxSensors, influxSensor)
		}

		// Write data into influxDB
		err = influx.Influx.WriteSensorSamples(aseID, influxSensors)
		if err != nil {
			log.Println(err.Error())
		}

		return
	}

	// Evaluate GPS information. They're received in port 2
	if httpIntegration.Port == 2 {
		gpsInfo, err := lora.EvaluateGPSPacket(httpIntegration.Message.DecodeDataHex)
		if err != nil {
			return
		}

		// Store GPS info in current configuration
		confiGPS := configuration.GPSCoord{
			Longitude:     gpsInfo.Longitude,
			Latitude:      gpsInfo.Latitude,
			Elevation:     gpsInfo.Elevation,
			UnixTimeStamp: gpsInfo.UnixTimeStamp}
		configuration.AirSensEURDB.AddGPSInfo(aseID, confiGPS)

		updateDatabase = true
	}

	// Evaluate boards registry. They're received in port 3
	if httpIntegration.Port == 3 {

		boards, err := lora.EvaluateBoardsConfigPacket(httpIntegration.Message.DecodeDataHex)
		if err != nil {
			return
		}

		// Update the database and generate the board dataset for InfluxDB
		var influxBoards []influx.BoardConfig
		for _, board := range boards {
			configBoard := configuration.ASEBoard{
				UnixTimeStamp: board.UnixTimeStamp,
				BoardID:       board.BoardID,
				BoardType:     board.BoardType,
				FwRevision:    board.FwRevision,
				Serial:        board.Serial}
			configuration.AirSensEURDB.AddBoardToUnit(aseID, configBoard)

			influxBoard := influx.BoardConfig{
				UnixTimeStamp: board.UnixTimeStamp,
				BoardID:       board.BoardID,
				BoardType:     board.BoardType,
				FwRevision:    board.FwRevision,
				Serial:        board.Serial}

			influxBoards = append(influxBoards, influxBoard)
		}

		// Write data into influxDB
		err = influx.Influx.WriteBoardsConfiguration(aseID, influxBoards)
		if err != nil {
			log.Println(err.Error())
		}

		updateDatabase = true
	}

	// Evaluate sensors registry. They're received in port 4
	if httpIntegration.Port == 4 {
		sensors, err := lora.EvaluateSensorsConfigPacket(httpIntegration.Message.DecodeDataHex)
		if err != nil {
			return
		}

		// Take the last known sampling start-time.
		// We suppose that sampling start-time is embedded in boards configuration and
		// that boards configurations were always sent before the sensors configuration
		lastKnownSamplingStartTime := configuration.AirSensEURDB.GetLastKnownStartSamplingTimestamp(aseID)

		// Update the database and generate the sensors dataset for InfluxDB
		var influxSensors []influx.SensorConfig
		for _, sensor := range sensors {
			configSensor := configuration.ASESensor{
				Channel:          sensor.SensorID,
				Name:             sensor.Name,
				Serial:           sensor.Serial,
				MeasurementUnits: sensor.MeasurementUnits}
			configuration.AirSensEURDB.AddSensorToUnit(aseID, configSensor)

			influxSensor := influx.SensorConfig{
				UnixTimeStamp:    lastKnownSamplingStartTime,
				Channel:          sensor.SensorID,
				Name:             sensor.Name,
				Serial:           sensor.Serial,
				MeasurementUnits: sensor.MeasurementUnits}

			influxSensors = append(influxSensors, influxSensor)
		}

		// Write data into influxDB
		err = influx.Influx.WriteSensorsConfiguration(aseID, influxSensors)
		if err != nil {
			log.Println(err.Error())
		}

		updateDatabase = true
	}

	if updateDatabase {
		configuration.AirSensEURDB.Update()
	}

	// Retrieve InfluxDB configuration
	conf := configuration.Config{}
	conf.ReadConfiguration()
}
