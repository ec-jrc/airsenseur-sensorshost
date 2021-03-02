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
import { toast } from 'react-toastify';

import useAsyncAction from '../../hooks/useAsyncAction';
import LoaderSpinner from '../../utils/LoaderSpinner';
import Toggle from '../../utils/Toggle';

import './Commands.css';

const Commands = ({ machineStatus }) => {
    const [dataPush, setDataPush] = useState(machineStatus.DataPushStatus);
    const [dataPushEnable, dataPushEnableStatus] = useAsyncAction("/do/datapush-enable", "POST");
    const [dataPushDisable, dataPushDisableStatus] = useAsyncAction("/do/datapush-disable", "POST");
    
    const [sampling, setSampling] = useState(machineStatus.SamplingStatus);
    const [samplingEnable, samplingEnableStatus] = useAsyncAction("/do/sampling-enable", "POST");
    const [samplingDisable, samplingDisableStatus] = useAsyncAction("/do/sampling-disable", "POST");

    const [pushDataNow, pushDataNowStatus] = useAsyncAction("/do/push-data-now", "POST");
    
    const status = [
        dataPushEnableStatus,
        dataPushDisableStatus,
        samplingEnableStatus,
        samplingDisableStatus,
        pushDataNowStatus
    ].includes("pending") ? "pending" : "";
	
    return (
        <LoaderSpinner status={status} overlay>
            <div className="Commands">
                <div className="status">
                    <div className="statusItem">
                        <span className="statusName">Battery</span>
                        <span className="statusValue">{machineStatus.BatteryStatus || "Unknown" }</span>
                    </div>

                    <div className="statusItem">
                        <span className="statusName">GPRS</span>
                        <span className="statusValue">{machineStatus.GPRSStatus || "Unknown" }</span>
                    </div>

                    <div className="statusItem">
                        <span className="statusName">GPS</span>
                        <span className="statusValue">{machineStatus.GPSStatus || "Unknown" }</span>
                    </div>
                </div>

                <div className="toggles">
                    <div className="toggle">
                        Data Push Scheduler
                        <Toggle checked={dataPush} onChange={() => {
                            if(dataPush) dataPushDisable({}, () => toast.info("Data push disabled"));
                            else dataPushEnable({}, () => toast.info("Data push enabled"));
                            
                            setDataPush(!dataPush);
                        }} />
                    </div>

                    <div className="toggle">
                        Sampling Process
                        <Toggle checked={sampling} onChange={() => {
                            if(sampling) samplingDisable({}, () => toast.info("Sampling disabled"));
                            else samplingEnable({}, () => toast.info("Sampling enabled"));
                            
                            setSampling(!sampling);
                        }} />
                    </div>

                    
                    <div className="dataPushNowButton">
                        <button className="appButton" 
                            type="button" 
                            onClick={() => pushDataNow({}, () => toast.info("Data push process started in background. See logs for details."))}>
                                Force Data Push Now
                        </button>
                    </div>
                </div>
            </div>
        </LoaderSpinner>

    );
}

export default Commands;
