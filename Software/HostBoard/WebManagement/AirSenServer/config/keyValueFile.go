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
	"bufio"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"reflect"
	"strconv"
	"strings"
)

// keyValueFile : this struct represents a typical key=value
// configuration file
type keyValueFile struct {
	// I use a pointer since the paths in global keyValueFiles
	// change during the app initialization
	path *string

	separator   string
	boolOnlyKey bool
	silent      bool
}

// writeProperties writes on file the properties from the file structure
func (f keyValueFile) writeProperties(settingsFile interface{}) error {
	v := reflect.ValueOf(settingsFile).Elem()
	if v.Kind() != reflect.Struct {
		return fmt.Errorf("readProperties expects a pointer to struct, received %v", reflect.TypeOf(settingsFile))
	}

	t := v.Type()

	for i := 0; i < t.NumField(); i++ {
		key := t.Field(i).Tag.Get("configName")
		// If the configName key is not specified in the struct
		// the field will be skipped
		if key == "" {
			continue
		}

		obj := v.Field(i).Interface()

		var err error

		switch obj.(type) {
		case PasswordString:
			if obj.(PasswordString) == "" {
				continue
			} else {
				err = f.writeString(key, fmt.Sprintf("%v", obj))
			}
		case int:
			err = f.writeInt(key, obj.(int))
		case bool:
			err = f.writeBool(key, obj.(bool))
		case string:
			err = f.writeString(key, fmt.Sprintf("%v", obj))
		default:
			err = f.writeString(key, fmt.Sprintf("%v", obj))
		}

		if err != nil {
			log.Println(err.Error())
		}
	}

	return nil
}

// readProperties populates the file structure with the properties
// read from the corresponding files
func (f keyValueFile) readProperties(settingsFile interface{}) error {
	// https://stackoverflow.com/questions/63421976/panic-reflect-call-of-reflect-value-fieldbyname-on-interface-value
	v := reflect.ValueOf(settingsFile).Elem()
	if v.Kind() != reflect.Struct {
		return fmt.Errorf("readProperties expects a pointer to struct, received %v", reflect.TypeOf(settingsFile))
	}

	newV := reflect.New(v.Type()).Elem()
	newV.Set(v)
	newT := newV.Type()

	for i := 0; i < newV.NumField(); i++ {
		key := newT.Field(i).Tag.Get("configName")
		// If the configName key is not specified in the struct
		// the field will be skipped
		if key == "" {
			continue
		}

		field := newV.Field(i)
		obj := field.Interface()

		switch obj.(type) {
		case PasswordString:
			val, err := f.readString(key)
			if err != nil {
				if !f.silent {
					log.Println(err.Error())
				}
			} else {
				field.SetString(val)
			}
		case int:
			val, err := f.readInt(key)
			if err != nil {
				if !f.silent {
					log.Println(err.Error())
				}
			} else {
				field.SetInt(int64(val))
			}
		case bool:
			val, err := f.readBool(key)
			if err != nil {
				if !f.silent {
					log.Println(err.Error())
				}
			} else {
				field.SetBool(val)
			}
		case string:
			val, err := f.readString(key)
			if err != nil {
				if !f.silent {
					log.Println(err.Error())
				}
			} else {
				field.SetString(val)
			}
		default:
			log.Printf("Unmanaged type %T", obj)
		}
	}

	v.Set(newV)
	return nil
}

func (f keyValueFile) writeString(key string, value string) error {
	return f.writeRaw(key, value, false)
}

func (f keyValueFile) writeInt(key string, value int) error {
	return f.writeRaw(key, fmt.Sprintf("%d", value), false)
}

func (f keyValueFile) writeBool(key string, value bool) error {
	return f.writeRaw(key, fmt.Sprintf("%v", value), f.boolOnlyKey)
}

func (f keyValueFile) writeRaw(key string, value interface{}, keyOnly bool) error {
	newLine := func() string {
		if keyOnly {
			if (strings.Trim(fmt.Sprintf("%v", value), " ")) == "true" {
				return key
			}

			return ""
		}

		return key + f.separator + fmt.Sprintf("%v", value)
	}()

	configFile, _ := ioutil.ReadFile(GetPath(*f.path))
	lines := strings.Split(string(configFile), "\n")
	found := false
	duplicateLines := []int{}

	for i, line := range lines {
		// Don't include comments
		strippedLine := strings.Split(line, "#")
		prop := strings.SplitN(strings.Trim(strippedLine[0], " "), f.separator, 2)

		if prop[0] == key {
			if !found {
				found = true

				if newLine == "" {
					duplicateLines = append(duplicateLines, i)
				} else {
					lines[i] = newLine
				}
			} else {
				duplicateLines = append(duplicateLines, i)
			}
		}
	}

	// If we found the property, clean the file from duplicates of that
	// property, then save...
	if found {
		for i, lineNum := range duplicateLines {
			lines = append(lines[:(lineNum-i)], lines[(lineNum-i+1):]...)
		}

		configFile = []byte(strings.Join(lines, "\n"))
		return ioutil.WriteFile(GetPath(*f.path), configFile, 0644)
	}

	// ... otherwise just append the property at the end of file
	if newLine != "" {
		configFile = append(configFile, ([]byte("\n" + newLine))...)
	}
	return ioutil.WriteFile(GetPath(*f.path), configFile, 0644)
}

func (f keyValueFile) readString(key string) (string, error) {
	prop, err := f.readRaw(key)
	if err != nil {
		return "", err
	}

	if len(prop) == 2 {
		return prop[1], nil
	}

	return "", nil
}

func (f keyValueFile) readInt(key string) (int, error) {
	prop, err := f.readRaw(key)
	if err != nil {
		return 0, err
	}

	if len(prop) == 2 {
		int64val, err := strconv.ParseInt(prop[1], 10, 32)
		return int(int64val), err
	}

	return 0, nil
}

func (f keyValueFile) readBool(key string) (bool, error) {
	prop, err := f.readRaw(key)
	if err != nil {
		if f.boolOnlyKey {
			return false, nil
		}

		return false, err
	}

	if f.boolOnlyKey {
		return true, nil
	}

	if len(prop) == 2 {
		return strconv.ParseBool(prop[1])
	}

	return false, nil
}

func (f keyValueFile) readRaw(key string) ([]string, error) {
	file, err := os.Open(GetPath(*f.path))
	if err != nil {
		return nil, err
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)

	for scanner.Scan() {
		// Don't include comments
		strippedLine := strings.Split(scanner.Text(), "#")
		prop := strings.SplitN(strings.Trim(strippedLine[0], " "), f.separator, 2)

		if prop[0] == key {
			return prop, nil
		}
	}

	return nil, fmt.Errorf("Property '%s' not found", key)
}
