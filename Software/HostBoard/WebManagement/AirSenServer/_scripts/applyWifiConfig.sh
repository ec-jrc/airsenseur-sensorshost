#!/bin/bash

# ===========================================================================
# Copyright 2015 EUROPEAN UNION
#
# Licensed under the EUPL, Version 1.1 or subsequent versions of the
# EUPL (the "License"); You may not use this work except in compliance
# with the License. You may obtain a copy of the License at
# http://ec.europa.eu/idabc/eupl
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" basis,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Date: 02/04/2015
# Authors:
# - Michel Gerboles, michel.gerboles@jrc.ec.europa.eu,
#   Laurent Spinelle, laurent.spinelle@jrc.ec.europa.eu and
#   Alexander Kotsev, alexander.kotsev@jrc.ec.europa.eu:
#                       European Commission - Joint Research Centre,
# - Marco Signorini, marco.signorini@liberaintentio.com
#
# ===========================================================================

# Apply and test changes in saved WiFi configuration. A parameter specifies 
# if wifi should work in Client or Access Point mode.
#
# PARAMS: $1: workingMode (ap|client)
# OUTPUT: if the new configuration works as expected, print in stdout the new IP address
#         otherwise exit with status code 1  

ASEBASESCRIPT="/usr/local/airsenseur"

# Handle client mode
if [ "$1" = "client" ]; then

	# Switch to client mode
	$ASEBASESCRIPT/config_wifi_client 1>/dev/null 2>&1

	# Turn on wlan in client mode
	$ASEBASESCRIPT/on_wlan 1>/dev/null 2>&1

	# Wait for DHCP assigned address
	sleep 10

	# Get new address
	IPADDRESS=`$ASEBASESCRIPT/check_wlan_ip wlan0`
	if [ "$IPADDRESS" = "" ]; then
		exit 1
	fi
	
	echo $IPADDRESS
	exit 0
fi

# Handle ap mode
if [ "$1" = "ap" ]; then

	# Switch to ap mode
	$ASEBASESCRIPT/config_wifi_ap 1>/dev/null 2>&1

	# Turn on wlan in ap mode
	$ASEBASESCRIPT/on_wlan 1>/dev/null 2>&1

	# Wait for some time to have the wlan up and running
	sleep 5

        # Get new address
        IPADDRESS=`$ASEBASESCRIPT/check_wlan_ip wlan0`
        if [ "$IPADDRESS" = "" ]; then
                exit 1
        fi

        echo $IPADDRESS
        exit 0	
fi

# Unknown mode. This should never happens
exit 1
