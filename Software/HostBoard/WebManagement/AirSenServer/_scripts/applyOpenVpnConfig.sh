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

# Apply changes in saved OpenVPN configuration
#
# PARAMS: $1: enabled
# OUTPUT: none

ENABLED=$1

ASECONFIGFILE=/usr/local/etc/ovpnclient.properties
OVPNCONFIGFILE=/etc/openvpn/client.conf

# Apply settings on the main configuration file
REMOTE=`grep remote $ASECONFIGFILE`
sed -i "s/remote .*/${REMOTE}/" $OVPNCONFIGFILE

PROTO=`grep proto $ASECONFIGFILE`
sed -i "s/proto .*/${PROTO}/" $OVPNCONFIGFILE

COMPLZO=`grep comp-lzo $ASECONFIGFILE`
if [ "$COMPLZO" = "" ]; then
	COMPLZO="#comp-lzo"
fi
sed -i "s/comp-lzo/${COMPLZO}/" $OVPNCONFIGFILE
sed -i "s/#comp-lzo/${COMPLZO}/" $OVPNCONFIGFILE

NSCERTTYPE=`grep ns-cert-type $ASECONFIGFILE`
if [ "$NSCERTTYPE" = "" ]; then
	NSCERTTYPE="#ns-cert-type server"
else
	NSCERTTYPE="ns-cert-type server"
fi
sed -i "s/ns-cert-type server/${NSCERTTYPE}/" $OVPNCONFIGFILE
sed -i "s/#ns-cert-type server/${NSCERTTYPE}/" $OVPNCONFIGFILE

# Start/stop the service as required
if [ $ENABLED = "true" ]; then 

	# Enable daemon at startup
	systemctl daemon-reload
	systemctl enable openvpn

	# Start openvpn process
	systemctl start openvpn

else 

	# Stop openvpn process
	systemctl stop openvpn

	# Disable daemon at startup
	systemctl disable openvpn
fi

exit 0
