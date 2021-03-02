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
import Popup from 'reactjs-popup';
import { FaInfoCircle } from 'react-icons/fa';

import Sensor from './Sensor';
import Toggle from '../../utils/Toggle';

import './Board.css';

const Board = ({ board }) => {
    const [showDisabled, setShowDisabled] = useState(false);
    const [collapsed, setCollapsed] = useState(false);

    const boardType = board.BoardType === "1" ? "Chemical Shield"
                    : board.BoardType === "2" ? "ExpShield1"
                    : board.BoardType === "3" ? "HostBoard"
                    : board.BoardType === "4" ? "EnvShield1"
                    : "Unknown board";
                
    const sensors = !!board.Sensors ? board.Sensors.length : 0;
    const disabledSensors = !!board.Sensors ? board.Sensors.filter(el => !el.Enabled).length : 0;
    const sensorsWithIssues = !!board.Sensors ? board.Sensors.filter(el => el.Enabled && (!el.Measures || el.Measures.length === 0)).length : 0;

    return (
        <div className="Board">
            <div className="boardInfo" >
                <span className="infoIcon">
                    <Popup
                        trigger={<span><FaInfoCircle /></span>}
                        closeOnDocumentClick
                        on={['hover','focus','click']}
                        >

                        <span>
                            ID: {board.ID}<br/>
                            Firmware: {board.Firmware}<br/>
                            Serial: {board.Serial}
                        </span>
                    </Popup>
                </span>

                <h2 className="boardName">
                    <span onClick={() => setCollapsed(!collapsed)}>
                        {boardType}
                    </span>
                </h2>

                <span className="sensorsInfo">
                    <span className="ok">{sensors-disabledSensors} sensors</span>
                    {disabledSensors > 0 && <span>+{disabledSensors} disabled</span>}
                    {sensorsWithIssues > 0 && <span className="warning">{sensorsWithIssues} with issues</span>}
                </span>

                {!collapsed && disabledSensors > 0 &&
                    <div className="showDisabledCheck">
                        <label>
                            Show disabled
                            <Toggle value={showDisabled} onChange={() => setShowDisabled(!showDisabled)} />
                        </label>
                    </div>
                }
            </div>

            {!collapsed &&
                <div className={"sensorsContainer"}>
                    {!!board.Sensors && board.Sensors.map((sensor, i) => 
                        (sensor.Enabled || showDisabled) && <Sensor key={"sensor"+i} sensor={sensor} />
                    )}
                </div>
            }
        </div>
    );
}

export default Board;
