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
# AirSensEUR - Copy host applications 

#Folder containing the target rootfs
TARGET_ROOTFS_DIR="target-rootfs"

#Folder containing the pre-built java host applications
SOURCE_APP_DIR="../hostapps"

#Folder containing other custom configuration scripts
SOURCE_SCRIPT_DIR="../custom/Scripts"

#Copy all other custom files
chown -R root:root ../custom/Scripts/.
cp -a $SOURCE_SCRIPT_DIR/* $TARGET_ROOTFS_DIR/.

# Java based applications
cp -a $SOURCE_APP_DIR/AirSensEURHost $TARGET_ROOTFS_DIR/usr/local/airsenseur/.
cp -a $SOURCE_APP_DIR/AirSensEURDataAggregator $TARGET_ROOTFS_DIR/usr/local/airsenseur/.
cp -a $SOURCE_APP_DIR/AirSensEURDataPush $TARGET_ROOTFS_DIR/usr/local/airsenseur/.

# Keyboard Event monitor
cp -a $SOURCE_APP_DIR/eventmonitor $TARGET_ROOTFS_DIR/usr/local/bin/.
chmod 755 $TARGET_ROOTFS_DIR/usr/local/bin/eventmonitor

# Battery Monitor
cp -a $SOURCE_APP_DIR/batterymonitor $TARGET_ROOTFS_DIR/usr/local/bin/.
chmod 755 $TARGET_ROOTFS_DIR/usr/local/bin/batterymonitor

# GPIO_NTP PPS handler
cp -a $SOURCE_APP_DIR/rpi_gpio_ntp $TARGET_ROOTFS_DIR/usr/local/bin/.
chmod 755 $TARGET_ROOTFS_DIR/usr/local/bin/rpi_gpio_ntp

# Crontab for DataPush
cp -a $SOURCE_SCRIPT_DIR/usr/local/etc/datapushcron $TARGET_ROOTFS_DIR/etc/cron.d/.

# Change flags and permissions
chown -R root:root $TARGET_ROOTFS_DIR/usr/local/airsenseur
chmod 755 -R $TARGET_ROOTFS_DIR/usr/local/airsenseur/

chown -R root:root $TARGET_ROOTFS_DIR/usr/local/bin

chown root:root $TARGET_ROOTFS_DIR/etc/cron.d/datapushcron
chmod 644 $TARGET_ROOTFS_DIR/etc/cron.d/datapushcron

chmod 755 $TARGET_ROOTFS_DIR/etc/eventmonitor/*
chmod 755 $TARGET_ROOTFS_DIR/etc/batterymonitor/lowvoltage_*
chmod 755 $TARGET_ROOTFS_DIR/etc/init.d/airsenseurhost
chmod 755 $TARGET_ROOTFS_DIR/etc/init.d/airsenseurdataaggregator
chmod 755 $TARGET_ROOTFS_DIR/etc/rc.local
chmod 755 $TARGET_ROOTFS_DIR/firstrun.sh
