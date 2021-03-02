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

import "log"

// LoadAppConfiguration loads the configuration file
func LoadAppConfiguration() {
	var kvAppConfig = keyValueFile{
		path:      &ConfigFileName,
		separator: "=",
		silent:    true,
	}

	err := kvAppConfig.readProperties(&AppConfig)
	if err != nil {
		log.Println(err.Error())
		return
	}
}

// AppConfig is the configuration of the application
// It describes where to find the configuration files and
// scripts.
var AppConfig = struct {
	DBFile              string `configName:"DBFile"`
	FileEEPROM          string `configName:"FileEEPROM"`
	MaintenanceLogFiles string `configName:"MaintenanceLogFiles"`

	Config52North string `configName:"Config52North"`
	ConfigInflux  string `configName:"ConfigInflux"`
	ConfigGPRS    string `configName:"ConfigGPRS"`

	ConfigOpenVpn     string `configName:"ConfigOpenVpn"`
	OpenVpnClientCert string `configName:"OpenVpnClientCert"`
	OpenVpnClientKey  string `configName:"OpenVpnClientKey"`
	OpenVpnServerCert string `configName:"OpenVpnServerCert"`

	ConfigWifiClient string `configName:"ConfigWifiClient"`
	ConfigWifiAP     string `configName:"ConfigWifiAP"`

	ScriptReboot             string `configName:"ScriptReboot"`
	ScriptSetTime            string `configName:"ScriptSetTime"`
	ScriptTestGPRS           string `configName:"ScriptTestGPRS"`
	ScriptBatteryStatus      string `configName:"ScriptBatteryStatus"`
	ScriptGPRSStatus         string `configName:"ScriptGPRSStatus"`
	ScriptGPSStatus          string `configName:"ScriptGPSStatus"`
	ScriptDeleteAllData      string `configName:"ScriptDeleteAllData"`
	ScriptDataPushNow        string `configName:"ScriptDataPushNow"`
	ScriptDataPushEnable     string `configName:"ScriptDataPushEnable"`
	ScriptDataPushDisable    string `configName:"ScriptDataPushDisable"`
	ScriptDataPushStatus     string `configName:"ScriptDataPushStatus"`
	ScriptSamplingEnable     string `configName:"ScriptSamplingEnable"`
	ScriptSamplingDisable    string `configName:"ScriptSamplingDisable"`
	ScriptSamplingStatus     string `configName:"ScriptSamplingStatus"`
	ScriptConfigRestore      string `configName:"ScriptConfigRestore"`
	ScriptBackupConfig       string `configName:"ScriptBackupConfig"`
	ScriptBackupData         string `configName:"ScriptBackupData"`
	ScriptApplyGPRSConfig    string `configName:"ScriptApplyGPRSConfig"`
	ScriptApplyOpenVpnConfig string `configName:"ScriptApplyOpenVpnConfig"`
	ScriptApplyWifiConfig    string `configName:"ScriptApplyWifiConfig"`
	ScriptCheckWifiMode      string `configName:"ScriptCheckWifiMode"`
	ScriptDataScheduleRead   string `configName:"ScriptDataScheduleRead"`
	ScriptDataScheduleWrite  string `configName:"ScriptDataScheduleWrite"`
	ScriptWifiScan           string `configName:"ScriptWifiScan"`
}{
	// Default values
	DBFile:              "/usr/local/airsenseur/AirSensEURDataAggregator/airsenseur.db",
	FileEEPROM:          "",
	MaintenanceLogFiles: "/var/log/runDataPush.log:/var/log/airsenseurdataaggregator.log:/var/log/airsenseurhost.log:/var/log/datawatchdog.log",

	Config52North: "/usr/local/etc/datapushsosdb.properties",
	ConfigInflux:  "/usr/local/etc/influxpush.properties",
	ConfigGPRS:    "/usr/local/etc/GPRS.properties",

	ConfigOpenVpn:     "/usr/local/etc/ovpnclient.properties",
	OpenVpnClientCert: "/etc/openvpn/client/client.crt",
	OpenVpnClientKey:  "/etc/openvpn/client/client.key",
	OpenVpnServerCert: "/etc/openvpn/client/ca.crt",

	ConfigWifiClient: "/etc/network/ifcfg-wlan0_client",
	ConfigWifiAP:     "/etc/hostapd/hostapd.conf",

	ScriptReboot:             "./_scripts/reboot.sh",
	ScriptSetTime:            "./_scripts/setTime.sh",
	ScriptTestGPRS:           "./_scripts/gprsTest.sh",
	ScriptBatteryStatus:      "./_scripts/batteryStatus.sh",
	ScriptGPRSStatus:         "./_scripts/gprsStatus.sh",
	ScriptGPSStatus:          "./_scripts/gpsStatus.sh",
	ScriptDeleteAllData:      "./_scripts/deleteAllData.sh",
	ScriptDataPushNow:        "./_scripts/pushDataNow.sh",
	ScriptDataPushEnable:     "./_scripts/dataPushEnable.sh",
	ScriptDataPushDisable:    "./_scripts/dataPushDisable.sh",
	ScriptDataPushStatus:     "./_scripts/dataPushStatus.sh",
	ScriptSamplingEnable:     "./_scripts/samplingEnable.sh",
	ScriptSamplingDisable:    "./_scripts/samplingDisable.sh",
	ScriptSamplingStatus:     "./_scripts/samplingStatus.sh",
	ScriptConfigRestore:      "./_scripts/configRestore.sh",
	ScriptBackupConfig:       "./_scripts/configBackup.sh",
	ScriptBackupData:         "./_scripts/dataBackup.sh",
	ScriptApplyGPRSConfig:    "./_scripts/applyGPRSConfig.sh",
	ScriptApplyOpenVpnConfig: "./_scripts/applyOpenVpnConfig.sh",
	ScriptApplyWifiConfig:    "./_scripts/applyWifiConfig.sh",
	ScriptCheckWifiMode:      "./_scripts/checkWifiMode.sh",
	ScriptDataScheduleRead:   "./_scripts/dataPushScheduleRead.sh",
	ScriptDataScheduleWrite:  "./_scripts/dataPushScheduleWrite.sh",
	ScriptWifiScan:           "./_scripts/wifiScan.sh",
}
