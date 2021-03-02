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
	"fmt"
	"os/exec"
	"strconv"
	"strings"
	"time"
)

// FileDateTime represents the local date/time
type FileDateTime struct {
	UTC      time.Time `json:"utc"`
	Schedule [24]bool  `json:"schedule"`
}

// WriteProperties writes the configuration in the local configuration files.
func (f *FileDateTime) WriteProperties() error {
	if err := exec.Command(AppConfig.ScriptSetTime, fmt.Sprint(f.UTC.UTC().Unix())).Run(); err != nil {
		return nil
	}

	hours := []string{}
	for i, scheduled := range f.Schedule {
		if scheduled {
			hours = append(hours, fmt.Sprint(i))
		}
	}

	return exec.Command(AppConfig.ScriptDataScheduleWrite, strings.Join(hours, ",")).Run()
}

// ReadProperties obtains the configuration from the local configuration files.
func (f *FileDateTime) ReadProperties() error {
	f.UTC = time.Now().UTC()

	output, err := exec.Command(AppConfig.ScriptDataScheduleRead).Output()
	if err != nil {
		return err
	}

	hours := strings.Split(strings.Trim(string(output), " \n"), ",")

	for _, hour := range hours {
		num, err := strconv.ParseInt(hour, 10, 64)
		if err != nil {
			continue
		}

		if num < 24 {
			f.Schedule[num] = true
		}
	}

	return nil
}
