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
# AirSensEUR Basic customization file

#Directory contains the target rootfs
TARGET_ROOTFS_DIR="target-rootfs"

#Set Board hostname
HOSTNAME=airsenseur

filename=$TARGET_ROOTFS_DIR/etc/hostname
echo $HOSTNAME > $filename

filename=$TARGET_ROOTFS_DIR/etc/hosts
echo 127.0.0.1 $HOSTNAME >> $filename

#Default name servers
filename=$TARGET_ROOTFS_DIR/etc/resolv.conf
echo nameserver 8.8.8.8 > $filename
echo nameserver 8.8.4.4 >> $filename

#Set the the debug port
filename=$TARGET_ROOTFS_DIR/etc/inittab
echo Creating $filename
echo id:2:initdefault: > $filename
echo si::sysinit:/etc/init.d/rcS >> $filename
echo ~~:S:wait:/sbin/sulogin >> $filename
echo l0:0:wait:/etc/init.d/rc 0 >> $filename
echo l1:1:wait:/etc/init.d/rc 1 >> $filename
echo l2:2:wait:/etc/init.d/rc 2 >> $filename
echo l3:3:wait:/etc/init.d/rc 3 >> $filename
echo l4:4:wait:/etc/init.d/rc 4 >> $filename
echo l5:5:wait:/etc/init.d/rc 5 >> $filename
echo l6:6:wait:/etc/init.d/rc 6 >> $filename
echo z6:6:respawn:/sbin/sulogin >> $filename
echo ca:12345:ctrlaltdel:/sbin/shutdown -t1 -a -r now >> $filename
echo T0:2345:respawn:/sbin/getty -L ttyS0 115200 vt100 >> $filename

# Other custom configurations
echo "AirSensEUR - 2015 - www.airsenseur.org" > $TARGET_ROOTFS_DIR/etc/motd

#microSD partitions mounting
filename=$TARGET_ROOTFS_DIR/etc/fstab
echo /dev/mmcblk0p1 /boot vfat noatime 0 1 > $filename
echo /dev/mmcblk0p2 / ext4 noatime 0 1 >> $filename
echo proc /proc proc defaults 0 0 >> $filename

