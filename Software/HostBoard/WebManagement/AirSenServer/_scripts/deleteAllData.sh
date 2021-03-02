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

# Delete all sampling data
#
# PARAMS: none
# OUTPUT: none

ASEBASE=/usr/local/airsenseur

# Check if sampling is enabled
$ASEBASE/check_samplingstatus 1>/dev/null 2>&1
SAMPLINGON=$?

if [ $SAMPLINGON = 1 ]; then

        # Stop the sampling process, if on
        $ASEBASE/stop_sampling 1>/dev/null 2>&1
fi

# Remove all sampling data
rm /usr/local/airsenseur/AirSensEURDataAggregator/airsenseur.db
rm /usr/local/airsenseur/AirSensEURDataAggregator/*.csv
rm /usr/local/airsenseur/AirSensEURDataPush/history.db

# Restart the sampling process, if required
if [ $SAMPLINGON = 1 ]; then
        $ASEBASE/start_sampling 1>/dev/null 2>&1
fi

exit 0
