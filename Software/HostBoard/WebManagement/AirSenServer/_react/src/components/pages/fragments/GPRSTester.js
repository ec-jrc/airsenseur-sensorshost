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
import useAsyncAction from '../../hooks/useAsyncAction';

import LoaderSpinner from '../../utils/LoaderSpinner';

import './GPRSTester.css'

const GPRSTester = ({ apn, pin }) => {
    const [hostToPing, setHostToPing] = useState("");
    const [text, setText] = useState("");
    const [triggerTest, status] = useAsyncAction("/info/gprs-test", "POST");

    return (
        <div className="GPRSTester">
            <h3>Test the configuration</h3>

            <span className="hostToPingLabel">Host to Ping</span>

            <div className="hostInput">
                <div className="hostToPingInput">
                    <input type="text" label="" onChange={e => setHostToPing(e.target.value)} />
                </div>
                
                <button className="appButton" 
                        type="button" 
                        disabled={hostToPing === ""} 
                        onClick={() => triggerTest({
                                APN: apn,
                                PIN: pin,
                                HostToPing: hostToPing
                            }, r => setText(r.data))}
                >
                    Test
                </button>
            </div>

            <div className="resultContainer">
                <LoaderSpinner status={status}>
                    <textarea className="resultText" name="result" value={text} readOnly />
                </LoaderSpinner>
            </div>
        </div>
    );
}

export default GPRSTester;
