#!/bin/sh

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
#			European Commission - Joint Research Centre, 
# - Marco Signorini, marco.signorini@liberaintentio.com
#
# ===========================================================================

# AirSensEUR - First Time run configuration file

# Configure packages that were not able to reconfigure at build time
/usr/bin/dpkg --configure -a

# Change permissions on some files
/bin/chown root:dialout /etc/wvdial.conf
/bin/chown root:www-data /usr/local/etc/sensor.properties
/bin/chown root:www-data /usr/local/etc/aggregator.properties
/bin/chown root:www-data /usr/local/etc/datapushsosdb.properties
/bin/chown root:www-data /usr/local/etc/datapushcron
/bin/chown root:staff /usr/local
/bin/chown root:staff /usr/local/airsenseur
/bin/chown root:staff /usr/local/etc

/bin/chmod 660 /usr/local/etc/datapushcron
/bin/chmod +s /usr/local/airsenseur

/bin/chown root:root /etc/sudoers
/bin/chmod 440 /etc/sudoers


# Disable some services we don't need at startup
/usr/sbin/update-rc.d hostapd remove
/usr/sbin/update-rc.d udhcpd remove

# Configure lighttpd server
/usr/sbin/lighty-enable-mod fastcgi-php
/etc/init.d/lighttpd force-reload

# Delete me so I'll never run again
rm /firstrun.sh
