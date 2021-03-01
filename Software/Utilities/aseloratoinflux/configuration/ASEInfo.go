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

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
)

// ASESensor contains information about a single AirSensEUR sensor channel
type ASESensor struct {
	Channel          uint16 `json:"channel"`
	Name             string `json:"name"`
	Serial           string `json:"serial"`
	MeasurementUnits string `json:"units"`
}

// ASEBoard contains information about a single AirSensEUR board
type ASEBoard struct {
	UnixTimeStamp uint64
	BoardID       uint32
	BoardType     string
	FwRevision    string
	Serial        string
}

// GPSCoord contains information about position of the remote unit
type GPSCoord struct {
	Longitude     float32 `json:"lon"`
	Latitude      float32 `json:"lat"`
	Elevation     float32 `json:"ele"`
	UnixTimeStamp uint64  `json:"ts"`
}

// ASEInfo contains information about a single AirSensEUR unit
// connected through LoRa
type ASEInfo struct {
	ID      string               `json:"id"`
	Sensors map[uint16]ASESensor `json:"sensors"`
	Boards  map[uint32]ASEBoard  `json:"boards"`
	GPS     *GPSCoord            `json:"position"`
}

// ASEDb contains information on all known AirSensEUR units
type ASEDb struct {
	ASEs     map[string]ASEInfo `json:"aselist"` // Key: ASEId
	fileName string
}

// AirSensEURDB is the exported global AirSensEUR database
var AirSensEURDB *ASEDb

// NewASEDb is the ASEDb constructor
func NewASEDb() *ASEDb {
	return &ASEDb{make(map[string]ASEInfo), ""}
}

// NewASEInfo is the ASEInfo constructor
func NewASEInfo(unitID string) *ASEInfo {
	return &ASEInfo{unitID, make(map[uint16]ASESensor), make(map[uint32]ASEBoard), &GPSCoord{}}
}

// LoadFromFile loads a complete JSON database from a file
func (a *ASEDb) LoadFromFile(filename string) {

	file, _ := ioutil.ReadFile(filename)
	_ = json.Unmarshal([]byte(file), &a)
	a.fileName = filename
}

// SaveToFile saves local ASE database to a file
func (a *ASEDb) SaveToFile(filename string) {

	file, _ := json.MarshalIndent(a, "", " ")
	_ = ioutil.WriteFile(filename, file, 0644)
}

// Update to locafile
func (a *ASEDb) Update() {

	file, _ := json.MarshalIndent(a, "", " ")
	_ = ioutil.WriteFile(a.fileName, file, 0644)
}

// AddSensorToUnit add sensor information into the ASE database
func (a *ASEDb) AddSensorToUnit(unitID string, sensorInfo ASESensor) {

	// Take the unit from the database or create a new one
	ase, ok := a.ASEs[unitID]
	if !ok {
		ase = *NewASEInfo(unitID)
		a.ASEs[unitID] = ase
	}

	// Replace the sensor configuration with the new one
	ase.Sensors[sensorInfo.Channel] = sensorInfo
}

// GetSensorInfo returns a sensor information for the specified unit ID.
// Returns an error if no unitID or no sensor information is found on the database
func (a *ASEDb) GetSensorInfo(unitID string, sensorChannel uint16) (ASESensor, error) {

	// Take the unit from the database
	ase, ok := a.ASEs[unitID]
	if !ok {
		return ASESensor{}, fmt.Errorf("No ASE with unitID %s found in the database", unitID)
	}

	sensor, ok := ase.Sensors[sensorChannel]
	if !ok {
		return ASESensor{}, fmt.Errorf("No sensor with channelID %d found in ASE with unitID %s found in the database", sensorChannel, unitID)
	}

	return sensor, nil
}

// AddBoardToUnit add board information into the ASE database
func (a *ASEDb) AddBoardToUnit(unitID string, boardInfo ASEBoard) {

	// Take the unit from the database or create a new one
	ase, ok := a.ASEs[unitID]
	if !ok {
		ase = *NewASEInfo(unitID)
		a.ASEs[unitID] = ase
	}

	// Replace the board configuration with the new one
	ase.Boards[boardInfo.BoardID] = boardInfo
}

// GetLastKnownStartSamplingTimestamp retrieves the last know timestamp for a start-sampling campaign
func (a *ASEDb) GetLastKnownStartSamplingTimestamp(unitID string) uint64 {

	// Take the unit from the database
	ase, ok := a.ASEs[unitID]
	if !ok {
		log.Println(("Invalid start sampling timestamp for UnitID %s"), unitID)
		return 0
	}

	// Get boards info from the database and take the 1st available
	ts := uint64(0)
	for _, v := range ase.Boards {
		if v.UnixTimeStamp != 0 {
			ts = v.UnixTimeStamp
			break
		}
	}

	return ts
}

// AddGPSInfo include GPS information for a specific unit in local database
func (a *ASEDb) AddGPSInfo(unitID string, gpsInfo GPSCoord) error {

	// Take the unit from the database or create a new one
	ase, ok := a.ASEs[unitID]
	if !ok {
		return fmt.Errorf("ASE with ID %s not found in local database ", unitID)
	}
	ase.GPS.UnixTimeStamp = gpsInfo.UnixTimeStamp
	ase.GPS.Longitude = gpsInfo.Longitude
	ase.GPS.Latitude = gpsInfo.Latitude
	ase.GPS.Elevation = gpsInfo.Elevation

	return nil
}

// GetGPSInfo returns the latest valid GPS information
func (a *ASEDb) GetGPSInfo(unitID string) (GPSCoord, error) {

	var result GPSCoord

	// Take the unit from the database or create a new one
	ase, ok := a.ASEs[unitID]
	if !ok {
		return result, fmt.Errorf("ASE with ID %s not found in local database ", unitID)
	}
	return *ase.GPS, nil
}
