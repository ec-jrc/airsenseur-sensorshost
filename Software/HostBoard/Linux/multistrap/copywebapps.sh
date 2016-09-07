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

# Copy the Web Management Console files
rm -r $TARGET_ROOTFS_DIR/var/www/*
cp -a $SOURCE_APP_DIR/AirSensEURWebManager/* $TARGET_ROOTFS_DIR/var/www/.

# Change flags and permissions
chown -R root:root $TARGET_ROOTFS_DIR/var/www/*

