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
	"fmt"
	"log"
	"net/http"
)

// Api flags
var (
	// ServerName is the server's name.
	ServerName string

	// ServerPort is the server's port.
	ServerPort uint

	// WlanInterface is the machine's wlan interface
	WlanInterface string
)

// logHTTPError is an helper for both printing errors and sending them through http
func logHTTPError(w http.ResponseWriter, r *http.Request, text string, httpStatusCode int) {
	errMessage := "[API " + r.RequestURI + "] " + text
	log.Println(errMessage)
	http.Error(w, errMessage, httpStatusCode)
}

// noAPI : handler for unkown api method calls
var noAPI = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	logHTTPError(w, r, fmt.Sprintf("Unknown method '%s %s'", r.Method, r.RequestURI), http.StatusInternalServerError)
})
