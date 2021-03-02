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

# Tests the GPRS connection with the specified parameters
#
# PARAMS: $1: APN
#         $2: PIN
#         $3: HostToPing
# OUTPUT: print in stdout debug information that will be shown to the user.
#         The user should be able to understand if everything is working or not
#         and if not useful troubleshooting info should be printed out.

ASEBASE=/usr/local/airsenseur

TARGETFILE="/etc/wvdial.conf"
BACKUPFILE=${TARGETFILE}.bak

# Backup the current configuration file
cp -a ${TARGETFILE} ${BACKUPFILE}

# Update the configuration file with the parameters to test
$ASEBASE/update_wvdial $1,$2 $TARGETFILE

# Power On the GPRS and PPP
$ASEBASE/on_gprs_ppp

# Ping the required host
ping -c5 -Ippp0 $3

# Power off PPP and the GPRS dongle
$ASEBASE/off_gprs_ppp

# Copy back the configuration file
cp -a ${BACKUPFILE} ${TARGETFILE}
rm ${BACKUPFILE}

exit 0
