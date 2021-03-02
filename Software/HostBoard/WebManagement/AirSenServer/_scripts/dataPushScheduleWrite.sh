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

# Writes a cronjob for data push
#
# PARAMS: $1: a list of the hours where data push should be scheduled (e.g. "0,2,6,12,20"),
#             this format should be convenient enough to write a cron job.
#             The parameter could be an empty string in case no hours are selected.
# OUTPUT: none

CRONTABFILE="/usr/local/etc/datapushcron"
TARGETFILE="/etc/cron.d/datapushcron"

IFS=",";
HOURS=($1)

# Generate the datapushcron file
echo "" > $CRONTABFILE 
for ((i=0; i<${#HOURS[@]}; i++)); do
	echo "0 "${HOURS[i]}" * * * root /usr/local/airsenseur/runDataPush 1>/var/log/runDataPush.log 2>&1" >> $CRONTABFILE
done

# Install the generated file
cp $CRONTABFILE $TARGETFILE
chmod 644 $TARGETFILE
chown root:root $TARGETFILE

exit 0
