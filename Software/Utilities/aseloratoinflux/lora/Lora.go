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

package lora

import (
	"encoding/hex"
	"fmt"
	"strconv"
	"unsafe"
)

// HTTPIntegration defines the structure of the incoming LoRa HTTP integration uplink message
//{
//	"deviceName":"AirSensEUR_ID",
//	"fPort":1,
//	"object": {
//		"DecodeDataHex":"000000076ae2b7300000187133422ea3d734433f2400004772d9000145f85800024771eb0003477fff000a43d70000",
//		}
//}
type HTTPIntegration struct {
	DeviceName string                 `json:"deviceName"`
	Port       int64                  `json:"fPort"`
	Message    HTTPIntegrationPayload `json:"object"`
}

// HTTPIntegrationPayload defines the structure of the incoming LoRa HTTP integration payload
type HTTPIntegrationPayload struct {
	DecodeDataHex string `json:"DecodeDataHex"`
}

// SensorValue contains unencoded value for a single sensor in the incoming LoRa string
type SensorValue struct {
	Channel uint8
	Value   float32
}

// GPSValue contains unencoded value for GPS information in the incoming LoRa string
type GPSValue struct {
	Longitude     float32
	Latitude      float32
	Elevation     float32
	UnixTimeStamp uint64
}

// SensorPacket contains unencoded values retrieved from the incoming LoRa string
type SensorPacket struct {
	UnixTimeStamp  uint64
	BoardTimeStamp uint32
	SensorValues   []SensorValue
}

// SensorConfigPacket contains unencoded values retrieved from the incoming LoRa string
type SensorConfigPacket struct {
	SensorID         uint16
	BoardID          uint8
	Name             string
	Serial           string
	MeasurementUnits string
}

// BoardConfigPacket contains unencoded values retrieved from the incoming LoRa string
type BoardConfigPacket struct {
	UnixTimeStamp uint64
	BoardID       uint32
	BoardType     string
	FwRevision    string
	Serial        string
}

// EvaluateSensorPacket returns the data encoded in the incoming sensor packet
// Example: 000000076AE2BB1800002BDD29000000002A000000002B000000002C000000002D000000002E000000002F3F200000
func EvaluateSensorPacket(message string) (SensorPacket, error) {

	var result SensorPacket

	// UnixTimestamp (8 bytes)
	var err error
	result.UnixTimeStamp, err = strconv.ParseUint(message[0:16], 16, 64)
	if err != nil {
		return result, err
	}

	// BoardTimeStamp (4 bytes)
	bts, err := strconv.ParseUint(message[16:24], 16, 32)
	if err != nil {
		return result, err
	}
	result.BoardTimeStamp = uint32(bts)

	sensors := message[24:]
	for n := 0; n < len(message[24:]); n += 10 {
		channel, err := strconv.ParseUint(sensors[:2], 16, 8)
		if err != nil {
			return result, err
		}

		var f float32
		f, sensors, err = decodeFloat32(sensors[2:])
		if err != nil {
			return result, err
		}

		result.SensorValues = append(result.SensorValues, SensorValue{Channel: uint8(channel), Value: f})
	}

	return result, nil
}

// EvaluateGPSPacket decodes GPS information from incoming LoRa encoded string
func EvaluateGPSPacket(message string) (GPSValue, error) {

	var result GPSValue

	// UnixTimeStamp: 8 bytes
	ts, err := strconv.ParseUint(message[0:16], 16, 64)
	if err != nil {
		return result, err
	}
	result.UnixTimeStamp = ts

	result.Longitude, message, err = decodeFloat32(message[16:])
	if err != nil {
		return result, err
	}
	result.Latitude, message, err = decodeFloat32(message)
	if err != nil {
		return result, err
	}
	result.Elevation, message, err = decodeFloat32(message)
	if err != nil {
		return result, err
	}

	return result, nil
}

// EvaluateBoardsConfigPacket decodes an array of BoardConfigPacket(s) from LoRa encoded string
func EvaluateBoardsConfigPacket(message string) ([]BoardConfigPacket, error) {

	var results []BoardConfigPacket

	// UnixTimeStamp: 8 bytes
	ts, err := strconv.ParseUint(message[0:16], 16, 64)
	if err != nil {
		return results, err
	}

	// Loop on all sensors information found in the messaged string
	msg := message[16:]
	result := BoardConfigPacket{}
	items := 0
	for {
		msg, result, err = evaluateBoardConfigPacket(ts, msg)
		if err != nil {
			return results, err
		}

		results = append(results, result)

		// We don't expect more than 100 items in a single LoRa message
		// so, if this is the case, it's an error
		if len(results) > 100 {
			return results, fmt.Errorf("Invalid message when evaluating boards information")
		}

		items++

		// We don't expect to cycle more than 200 times on this loop
		// so, if this is the case, it's an error
		if items > 200 {
			return results, fmt.Errorf("Invalid message when evaluating boards information")
		}

		// This is the end of the message
		if len(msg) == 0 {
			break
		}
	}

	return results, nil
}

// EvaluateSensorsConfigPacket decodes an array of SensorConfigPacket(s) from LoRa encoded string
func EvaluateSensorsConfigPacket(message string) ([]SensorConfigPacket, error) {

	var results []SensorConfigPacket

	msg := message
	result := SensorConfigPacket{}
	var err error
	items := 0
	for {
		msg, result, err = evaluateSensorConfigPacket(msg)
		if err != nil {
			return results, err
		}

		results = append(results, result)

		// We don't expect more than 100 items in a single LoRa message
		// so, if this is the case, it's an error
		if len(results) > 100 {
			return results, fmt.Errorf("Invalid message when evaluating sensors information")
		}

		items++

		// We don't expect to cycle more than 200 times on this loop
		// so, if this is the case, it's an error
		if items > 200 {
			return results, fmt.Errorf("Invalid message when evaluating sensors information")
		}

		// This is the end of the message
		if len(msg) == 0 {
			break
		}
	}

	return results, nil
}

func evaluateBoardConfigPacket(timestamp uint64, message string) (string, BoardConfigPacket, error) {

	result := BoardConfigPacket{UnixTimeStamp: timestamp}

	// BoardID: 4 bytes
	sID, err := strconv.ParseUint(message[0:8], 16, 32)
	result.BoardID = uint32(sID)
	if err != nil {
		return message, result, err
	}

	// BoardType
	val, message, err := decodeNextString(message[8:])
	if err != nil {
		return message, result, err
	}
	result.BoardType = val

	// Fw Revision
	val, message, err = decodeNextString(message)
	if err != nil {
		return message, result, err
	}
	result.FwRevision = val

	// Serial
	val, message, err = decodeNextString(message)
	if err != nil {
		return message, result, err
	}
	result.Serial = val

	return message, result, nil
}

func evaluateSensorConfigPacket(message string) (string, SensorConfigPacket, error) {

	var result SensorConfigPacket

	// SensorID: 2 bytes
	sID, err := strconv.ParseUint(message[0:4], 16, 16)
	result.SensorID = uint16(sID)
	if err != nil {
		return message, result, err
	}

	// BoardID: 1 byte
	bID, err := strconv.ParseUint(message[4:6], 16, 8)
	if err != nil {
		return message, result, err
	}
	result.BoardID = uint8(bID)

	// Name
	val, message, err := decodeNextString(message[6:])
	if err != nil {
		return message, result, err
	}
	result.Name = val

	// Serial
	val, message, err = decodeNextString(message)
	if err != nil {
		return message, result, err
	}
	result.Serial = val

	// Measurement Units
	val, message, err = decodeNextString(message)
	if err != nil {
		return message, result, err
	}
	result.MeasurementUnits = val

	return message, result, nil
}

func decodeFloat32(message string) (float32, string, error) {

	value, err := strconv.ParseUint(message[0:8], 16, 32)
	uint32Value := uint32(value)
	f := *(*float32)(unsafe.Pointer(&uint32Value))
	if err != nil {
		return f, message, err
	}

	return f, message[8:], nil
}

func decodeNextString(message string) (string, string, error) {

	decoded, err := hex.DecodeString(message)
	if err != nil {
		return "", message, nil
	}

	// Search the next 0 in the results
	offset := 0
	for n, v := range decoded {
		if v == 0 {
			offset = n
			break
		}
	}

	// Convert to ASCII format
	strVal := fmt.Sprintf("%s", decoded[0:offset])

	// Return the value and a stripped down message starting to next token
	return strVal, message[((offset + 1) * 2):], nil
}
