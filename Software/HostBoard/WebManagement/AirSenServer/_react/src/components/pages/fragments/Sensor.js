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

import React from 'react';
import Popup from 'reactjs-popup';
import { LineChart, Line, Tooltip, XAxis } from 'recharts';
import { FaInfoCircle } from 'react-icons/fa';

import './Sensor.css';

const Sensor = ({ sensor }) => {
    const values = !!sensor.Measures ? sensor.Measures.map(measure => {
        const time = new Date(measure.Timestamp)
        const hours = time.getUTCHours() < 10 ? "0"+time.getUTCHours() : time.getUTCHours();
        const minutes = time.getUTCMinutes() < 10 ? "0"+time.getUTCMinutes() : time.getUTCMinutes();

        return {
            value: Math.round((measure.Value + Number.EPSILON) * 100) / 100,
            time:  hours + ":" + minutes + " UTC" 
        }
    }) : [];

    const lastValue = values.length > 0 ? values[values.length-1].value : "-";

    sensor.Unit = sensor.Unit.trim();
    const unit =  sensor.Unit === "C"       ? "°C"
                : sensor.Unit === "ug/m3"   ? "μg/m³"
                : sensor.Unit === "um"      ? "μm"
                : sensor.Unit === "%RH"     ? "% RH"
                : sensor.Unit;

    return (
        <div className={"Sensor" + (sensor.Enabled ? "" : " disabled")}>
            <div className="topBar">
                <div className="sensorName">{sensor.ChannelName}</div>

                <div className="sensorInfo"> 
                    <Popup
                        trigger={<span><FaInfoCircle /></span>}
                        closeOnDocumentClick
                        on={['hover','focus','click']}
                        >

                        <span>
                            Channel: {sensor.Channel}<br/>
                            Serial: {sensor.SerialID}
                        </span>
                    </Popup>
                </div>
            </div>

            <div className="sensorValue">
                {sensor.Enabled ? 
                    <>
                        {lastValue}
                        <div className="unit">{values.length > 0 && unit}</div>
                    </>
                : "disabled"} 
            </div>

            {values.length > 0 ?
                <div className="chart">
                    <LineChart data={values} width={150} height={45}>
                        <Tooltip />
                        <XAxis dataKey="time" hide />
                        <Line isAnimationActive={false} activeDot={false} unit={" " + unit} type="monotone" dataKey="value" strokeWidth={2} />
                    </LineChart>
                </div>
            :
                <div className="noDataWarning">
                    No recent data
                </div>
            }
        </div>
    );
}

export default Sensor;
