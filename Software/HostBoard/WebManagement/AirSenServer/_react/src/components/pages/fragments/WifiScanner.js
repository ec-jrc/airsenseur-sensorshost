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
import { MdSignalWifi0Bar, MdSignalWifi1Bar, MdSignalWifi2Bar, MdSignalWifi3Bar, MdSignalWifi4Bar } from "react-icons/md";
import Popup from "reactjs-popup";

import LoaderSpinner from '../../utils/LoaderSpinner';

import './WifiScanner.css';
import useAsyncAction from '../../hooks/useAsyncAction';

const WifiScanner = ({ onClick }) => {
    const [scanWifi] = useAsyncAction("/info/wifi-networks");
    const [scanWifiResult] = useAsyncAction("/info/wifi-networks-result", undefined, true);

    const [popupOpen, setPopupOpen] = useState(false);
    const [scanned, setScanned] = useState(false);
    const [networks, setNetworks] = useState([]);

    // Sort by network quality desc
    const sortedNetworks = [...networks].sort((a, b) => b.Quality - a.Quality);

    const scanNetworks = () => {
        setPopupOpen(true);
        scanWifi(undefined, () => {
            setTimeout(() => {}, 1000);

            const interval = setInterval(() => {
                scanWifiResult(undefined, (response) => {
                    clearInterval(interval);
                    setPopupOpen(false);
                    setNetworks(response.data);
                    setScanned(true);
                });
            }, 2000);
        });
    };
    
    return (
        <>
            <div className="WifiScanner">
                {scanned && <div>Nearby networks (click to copy the SSID):</div>}

                <div className="networksContainer">
                    {Array.isArray(sortedNetworks) && sortedNetworks.map((network, index) =>
                        <span title="Click to select" key={"net-"+index} className="networkLabel" onClick={() => onClick(network.Name)}> 
                            {   
                                network.Quality >= 60 ? <MdSignalWifi4Bar />
                                : network.Quality >= 50 ? <MdSignalWifi3Bar />
                                : network.Quality >= 40 ? <MdSignalWifi2Bar />
                                : network.Quality >= 25 ? <MdSignalWifi1Bar />
                                : <MdSignalWifi0Bar />
                            } <div>{network.Name}</div>
                        </span>
                    )}

                    {scanned && networks.length === 0 && <span className="noNetworks">No networks found</span>}

                    <button className="appButton" type="button" onClick={() => scanNetworks()}>Scan networks</button>
                </div>
            </div>

            <Popup open={popupOpen} modal closeOnDocumentClick={false}>
                <span>Scanning wifi networks. Please wait...</span>
                <LoaderSpinner status={"pending"} />
            </Popup>
        </>
    );
}
    

export default WifiScanner;
