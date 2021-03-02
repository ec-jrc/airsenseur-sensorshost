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

import React, { useEffect } from 'react';
  
import useDataSource from '../hooks/useDataSource';
import LoaderSpinner from '../utils/LoaderSpinner';
import Card from './templates/Card';
import GeoMap from './fragments/GeoMap';
import Board from './fragments/Board';

import './Overview.css';
import Commands from './fragments/Commands';

const Overview = () => {
    const [machineStatus,, requestStatus, renewStatus] = useDataSource("/info/machine-status");
    const coordinates = [machineStatus.Latitude, machineStatus.Longitude];

    useEffect(() => {
        const interval = setInterval(() => {
            renewStatus(true);
        }, 60000);

        return () => clearInterval(interval);
    }, [renewStatus]);

    return (
        <div className="Overview">
            <LoaderSpinner status={requestStatus}>
                <Card>
                    <div className="commandsAndMap">
                        <div className="commands">
                            <Commands machineStatus={machineStatus} />
                        </div>

                        <div className="mapContainer">
                            <GeoMap coordinates={coordinates} />
                        </div>
                    </div>
                </Card>

                {!!machineStatus.Boards && machineStatus.Boards.length > 0 ? machineStatus.Boards.map((board, i) => 
                    <Card key={"board"+i}>
                        <Board board={board} />
                    </Card>
                ) : <Card>No boards found</Card>}
            </LoaderSpinner>
        </div>
    );
}

export default Overview;
