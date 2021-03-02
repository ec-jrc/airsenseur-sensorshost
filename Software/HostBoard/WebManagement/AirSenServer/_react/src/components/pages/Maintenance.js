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

import React, { useEffect, useRef, useState } from 'react';
import { isString } from 'formik';
import Popup from "reactjs-popup";

import SimplePage from './templates/SimplePage';
import ActionButtonsContainer from './templates/ActionButtonsContainer';
import LoaderSpinner from '../utils/LoaderSpinner';
import useDataSource from '../hooks/useDataSource';
import useAsyncAction from '../hooks/useAsyncAction';

import './Maintenance.css';
import Card from './templates/Card';

const Maintenance = () => {
    const [triggerReboot] = useAsyncAction("/do/reboot", "POST");

    const [logs, , status, refreshData] = useDataSource("/info/logs");
    const [selectedLog, setSelectedLog] = useState(null);
    const shownLog = isString(selectedLog) ? selectedLog : Object.keys(logs)[0];

    const [runningCheck] = useAsyncAction("/info/server-running", "GET", true)
    const [isRebooting, setIsRebooting] = useState(false);

    const textAreaRef = useRef(null)

    useEffect(() => {
        if(textAreaRef.current != null) {
            textAreaRef.current.scrollTop = textAreaRef.current.scrollHeight;
        }
    })

    const confirmReboot = () => {
        if(window.confirm("Are you sure you want to reboot the machine?")) {
            triggerReboot(null, () =>{
                setIsRebooting(true);
            });
        }
    }

    return (
        <SimplePage title="Maintenance" className="Maintenance">
            <Card>
                <LoaderSpinner status={status}>
                <div>Log file:</div> 

                    <div className="logsSelector">

                        <select onChange={e => setSelectedLog(e.target.value)} defaultValue={selectedLog}>
                            {Object.keys(logs).map((k, i) => 
                                <option key={i} value={k}>{k}</option>
                            )}
                        </select>
                        
                        <button className="appButton" type="button" onClick={refreshData}>Refresh</button>
                    </div>

                    <textarea ref={textAreaRef} readOnly rows="20" value={logs[shownLog]}></textarea>
                </LoaderSpinner>
            </Card>

            <ActionButtonsContainer>
                <button className="appButton" type="button" onClick={confirmReboot}>Reboot AirSensEUR</button>
            </ActionButtonsContainer>

            <Popup  open={isRebooting} 
                    modal 
                    closeOnDocumentClick={false} 
                    onOpen={() => {
                        const interval = setInterval(() => {
                            runningCheck({}, () => {
                                setIsRebooting(false)
                                clearInterval(interval)
                            });
                        }, 5000);      
                    }}>

                <span>Rebooting AirSensEUR, please wait...</span>
                <LoaderSpinner status={"pending"} />
            </Popup>
        </SimplePage>
    );
}

export default Maintenance;
