/* ===========================================================================
 * Copyright 2015 EUROPEAN UNION
 *
 * Licensed under the EUPL, Version 1.1 or subsequent versions of the
 * EUPL (the "License"); You may not use this work except in compliance
 * with the License. You may obtain a copy of the License at
 * http://ec.europa.eu/idabc/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Date: 02/04/2015
 * Authors:
 * - Michel Gerboles, michel.gerboles@jrc.ec.europa.eu,
 *   Laurent Spinelle, laurent.spinelle@jrc.ec.europa.eu and
 *   Alexander Kotsev, alexander.kotsev@jrc.ec.europa.eu:
 *			European Commission - Joint Research Centre,
 * - Marco Signorini, marco.signorini@liberaintentio.com
 *
 * ===========================================================================
 */

import React, { useEffect, useState } from 'react';
import { MdHome, MdCloudUpload, MdSettings, MdMenu, MdExpandLess } from "react-icons/md";
import { withRouter } from "react-router-dom";

import MenuItem from './MenuItem';
import useDataSource from '../hooks/useDataSource';

import './MenuPanel.css';
import { toast } from 'react-toastify';

const MenuPanel = ({ location }) => {
    const [collapsed, setCollapsed] = useState(true);
    const [uuid] = useDataSource("/info/uuid", "");

    useEffect(() => setCollapsed(true), [location]);

    return (
        <div className={"MenuPanel" + (collapsed ? " collapsed" : "")}>
            <div className="logoContainer">
                <div className="collapseToggle" onClick={() => setCollapsed(!collapsed)}>
                    <div className="collapseIcon">
                        {collapsed ? <MdMenu /> : <MdExpandLess />}
                    </div>
                </div>

                <img src="/img/logo.png" alt="logo" />
            </div>

            <ul className="menuList main">
                {/* DASHBOARD */}
                <MenuItem to="/" iconComponent={<MdHome />} title="Overview" setMenuCollapsed={setCollapsed} />

                {/* CONNECTIONS */}
                <MenuItem parentPath="/connections" iconComponent={<MdCloudUpload />} title="Connections" submenu>
                    <MenuItem to="/connections/influx" cssName="settingsInflux" title="Influx" setMenuCollapsed={setCollapsed} />
                    <MenuItem to="/connections/52north" cssName="settings52North" title="52North" setMenuCollapsed={setCollapsed} />
                </MenuItem>

                {/* ADMINISTRATION */}
                <MenuItem parentPath="/settings" iconComponent={<MdSettings/>} title="Administration" submenu>
                    <MenuItem to="/settings/vpn" cssName="settingsVPN" title="VPN" setMenuCollapsed={setCollapsed} />
                    <MenuItem to="/settings/wifi" cssName="settingsWifi" title="WiFi" setMenuCollapsed={setCollapsed} />
                    <MenuItem to="/settings/gprs" cssName="settingsGPRS" title="GPRS" setMenuCollapsed={setCollapsed} />
                    <MenuItem to="/settings/datetime" cssName="settingsDateTime" title="Date and Time" setMenuCollapsed={setCollapsed} />
                    <MenuItem to="/settings/backup" cssName="settingsBackup" title="Backup" setMenuCollapsed={setCollapsed} />
                    <MenuItem to="/settings/maintenance" cssName="maintenance" title="Maintenance" setMenuCollapsed={setCollapsed} />
                </MenuItem>
            </ul>

            <div className="uuidContainer">
                ID: 
                {uuid !== "" ?
                    <span   className="uuid" 
                            onClick={e => {
                                e.preventDefault();
                                
                                if(!!navigator.clipboard) {
                                    navigator.clipboard.writeText(uuid)
                                    .then(() => {
                                        toast.success("UUID copied to clipboard.")
                                    })
                                    .catch(() => {
                                        toast.error("Error copying to clipboard.")
                                    })
                                } else {
                                    toast.error("Operation not supported by this browser.")
                                }
                            }}
                            title="Click to copy to cliboard">

                        {uuid}
                    </span>
                : " ---"}
            </div>
        </div>
    );
}

export default withRouter(MenuPanel);
