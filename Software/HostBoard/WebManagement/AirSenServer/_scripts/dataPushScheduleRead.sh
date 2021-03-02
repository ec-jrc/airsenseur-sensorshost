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

# Return currently scheduled hours for data push
#
# PARAMS: none
# OUTPUT: print in stdout a list of hours separated by comma (e.g. 0,2,10,20)

CRONTABFILE="/usr/local/etc/datapushcron"
HOURS=""
while IFS= read -r line
do
  tokens=($line)
  if [ "${tokens[1]}" != "" ]; then
	  HOURS=${HOURS},${tokens[1]}
  fi
done < "$CRONTABFILE"

echo ${HOURS:1}
exit 0
