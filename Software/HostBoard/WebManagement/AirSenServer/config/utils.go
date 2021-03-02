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

package config

import (
	"strings"
)

var (
	// ConfigFileName is the path of the configration file that the app should read
	ConfigFileName string

	// RootPath is the root path of the machine, can be changed for debug purposes
	RootPath string
)

// GetPath returns a path with the correct prefix
func GetPath(path string) string {
	path = strings.Trim(path, " ")
	runes := []rune(path)

	if len(runes) > 0 {
		switch runes[0] {
		case rune('/'):
			return RootPath + path
		case rune('.'):
			fallthrough
		default:
			return path
		}
	}

	return RootPath + path
}

// PasswordString type is used to implement a custom behaviour in json marshaling
type PasswordString string

// MarshalJSON function: marshaler ignores the field value completely.
func (PasswordString) MarshalJSON() ([]byte, error) {
	return []byte(`""`), nil
}
