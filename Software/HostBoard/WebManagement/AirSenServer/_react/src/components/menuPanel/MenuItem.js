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

import React, { useState } from 'react';
import { Link, useLocation } from "react-router-dom";
import { MdExpandMore, MdExpandLess } from "react-icons/md"

import './MenuItem.css';

const ControlMenuItem = ({ title, to, noLink, iconComponent, submenu, children, parentPath, setMenuCollapsed }) => {
    const location = useLocation();
    const [collapsed, setCollapsed] = useState(
        !!parentPath ? !(parentPath === location.pathname.substr(0, parentPath.length)) : true
    );

    const clickAction = submenu ? () => { setCollapsed(!collapsed) } : () => setMenuCollapsed(true);
    const isCurrentPage = location.pathname === to;

    if(submenu || noLink || isCurrentPage) {
        return (
            <>
                <li className="ControlMenuItem" onClick={clickAction}>
                    <span className={"menuLink " + 
                                    (submenu ? "submenu " : "") + 
                                    (isCurrentPage && !submenu ? "selected " : "")}>
                        <span className="icon">{iconComponent}</span>
                        <span className="menuTitle">{title}</span>
                        {submenu && 
                            <span className="menuCollapse">
                                {collapsed ? <MdExpandMore /> : <MdExpandLess />}
                            </span>
                        }

                        {!!children &&isCurrentPage && !submenu &&
                            <div className="controlsWrapper">
                                {children}
                            </div>
                        }
                    </span>
                </li>
                {!!children && submenu && !collapsed && children}
            </>
        );
    }

    return (
        <li className="ControlMenuItem">
            <Link className="menuLink" to={to}>
                <span className="icon">{iconComponent}</span>
                <span className="menuTitle">{title}</span>            
            </Link>
        </li>
    );
}

export default ControlMenuItem;
