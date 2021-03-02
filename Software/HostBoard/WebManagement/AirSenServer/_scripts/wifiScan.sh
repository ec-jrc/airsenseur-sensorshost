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

# Scans wifi networks
#
# PARAMS: none 
# OUTPUT: Wifi networks list, as SSID-Quality pairs. SSID is separated from 
# quality with a space, and multiple network are separated by a # character.
# Quality is a 0-70 number.
# Example: wifi test 1 50#wifiTest2 60#wifi:SAs4 68 

ASEBASE=/usr/local/airsenseur

# Evaluate if we're already in client mode
CLIENTMODE=`grep client /etc/network/interfaces`

if [ "${CLIENTMODE}" = "" ]; then

	# Switch to a "fake" client mode. We don't care about SSID but it's needed in order to 
	# have the possibility to scan for surrounding WiFis
	ifconfig wlan0 down
	cp -a /etc/network/interfaces_client /etc/network/interfaces
	ifconfig wlan0 up
fi

# We're now in client mode. Search and list all the available SSIDs
LIST=`iwlist wlan0 scan`

IFS=$'\n'
ESSIDS=(`echo "$LIST" | sed -En "s/ESSID:\"([A-z0-9 _-]+)\"/\1/p" | sed -e "s/^[[:space:]]\+//g"`)
QUALITY=(`echo "$LIST" | sed -En "s/Quality=([0-9]{2})\/[0-9]{2}.+/\1/p" | sed -e "s/[[:space:]]\+//g"`)

RESULT=""
for ((i=0; i<${#ESSIDS[@]}; i++)); do
	RESULT=${RESULT}"#${ESSIDS[i]} ${QUALITY[i]}"
done

# Go back to AP mode if required
if [ "${CLIENTMODE}" = "" ]; then

	# We need the AP configuration file to properly restart services
	cp -a /etc/network/interfaces_ap /etc/network/interfaces

	$ASEBASE/off_wlan 1>/dev/null 2>&1
	sleep 1
	$ASEBASE/on_wlan 1>/dev/null 2>&1
	sleep 5
fi

# Push out the results
echo ${RESULT:1}


exit 0
