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

package database

import (
	"database/sql"
	"fmt"
	"time"

	// SQLite3
	_ "github.com/mattn/go-sqlite3"
	"airsenseur.org/AirSenServer/config"
)

var (
	// LastData flag: normally the sensor measures served
	// by the API are at most 30 minutes old. When this
	// option is active, the data served will be at most
	// 30 minutes older than the last received measure.
	LastData bool
)

// Machine represents the machine's state
type Machine struct {
	Latitude, Longitude float64
	Boards              []Board
}

// Board represents the state of a sensor board
type Board struct {
	ID        uint
	BoardType string
	Firmware  string
	Serial    string
	Sensors   []Sensor
}

// Sensor represents the state of single sensor
type Sensor struct {
	Channel     uint
	ChannelName string
	SerialID    string
	Unit        string
	BoardID     uint
	Enabled     bool
	Measures    []Measure
}

// Measure represents a value measured by a sensor
type Measure struct {
	Timestamp uint64
	Value     float64
}

// GetMachineStatus returns a Machine object containing all boards, sensors and
// last 5 measures for each sensor
func GetMachineStatus() (Machine, error) {
	db, err := sql.Open("sqlite3", config.GetPath(config.AppConfig.DBFile)+"?mode=ro")
	if err != nil {
		return Machine{}, err
	}
	defer db.Close()

	latitude, longitude, err := getCoordinates(db)
	if err != nil {
		return Machine{}, err
	}

	measures, err := getMeasures(db)
	if err != nil {
		return Machine{}, err
	}

	sensors, err := getSensors(db)
	if err != nil {
		return Machine{}, err
	}

	boards, err := getBoards(db)
	if err != nil {
		return Machine{}, err
	}

	for i, s := range sensors {
		sensors[i].Measures, _ = measures[s.Channel]

		for j, b := range boards {
			if b.ID == s.BoardID {
				boards[j].Sensors = append(boards[j].Sensors, sensors[i])
			}
		}
	}

	return Machine{
		Latitude:  latitude,
		Longitude: longitude,
		Boards:    boards,
	}, nil
}

func getSensors(db *sql.DB) ([]Sensor, error) {
	row, err := db.Query(`
		SELECT DISTINCT channel, channelName, serialId, unit, enabled, boardId
		FROM sensors
		WHERE (SELECT MAX(timestamp) FROM sensors) 
			BETWEEN timestamp-3000 AND timestamp
	`)
	if err != nil {
		return nil, err
	}
	defer row.Close()

	sensors := []Sensor{}
	for row.Next() {
		var enabled int
		var s Sensor

		if err := row.Scan(&s.Channel, &s.ChannelName, &s.SerialID, &s.Unit, &enabled, &s.BoardID); err != nil {
			return nil, err
		}

		s.Enabled = enabled != 0

		sensors = append(sensors, s)
	}

	return sensors, nil
}

func getBoards(db *sql.DB) ([]Board, error) {
	row, err := db.Query(`
		SELECT DISTINCT boardId, boardType, boardFirmware, boardSerial
		FROM boards
		WHERE (SELECT MAX(timestamp) FROM boards) 
			BETWEEN timestamp-3000 AND timestamp
	`)
	if err != nil {
		return nil, err
	}
	defer row.Close()

	boards := []Board{}
	for row.Next() {
		var b Board

		if err := row.Scan(&b.ID, &b.BoardType, &b.Firmware, &b.Serial); err != nil {
			return nil, err
		}

		boards = append(boards, b)
	}

	return boards, nil
}

func getMeasures(db *sql.DB) (map[uint][]Measure, error) {
	var minTime string

	if LastData {
		minTime = `(SELECT MAX(collectedts)-1800000 FROM measures)`
	} else {
		minTime = fmt.Sprint((time.Now().Unix() - 1800) * 1000)
	}

	row, err := db.Query(`
		SELECT
			channel, collectedts, evvalue
		FROM (
			SELECT
				channel, collectedts, evvalue, ROW_NUMBER() OVER (
					PARTITION BY channel
					ORDER BY collectedts DESC
				) row_num
			FROM (
				SELECT channel, collectedts, evvalue
				FROM measures
				WHERE collectedts > ` + minTime + `
			)
		)
		WHERE row_num <= 5
		ORDER BY channel, collectedts;
	`)

	if err != nil {
		return nil, err
	}
	defer row.Close()

	measures := make(map[uint][]Measure)

	for row.Next() {
		var channel uint
		var m Measure

		if err := row.Scan(&channel, &m.Timestamp, &m.Value); err != nil {
			return nil, err
		}

		if measures[channel] == nil {
			measures[channel] = []Measure{}
		}
		measures[channel] = append(measures[channel], m)
	}

	return measures, nil
}

func getCoordinates(db *sql.DB) (float64, float64, error) {
	row, err := db.Query(`
		SELECT gpslatitude, gpslongitude
		FROM measures
		ORDER BY collectedts DESC
		LIMIT 1;
	`)
	if err != nil {
		return 0, 0, err
	}
	defer row.Close()

	var latitude, longitude float64

	if row.Next() {
		if err := row.Scan(&latitude, &longitude); err != nil {
			return 0, 0, err
		}
	} else {
		return 0, 0, nil
	}

	return latitude, longitude, nil
}
