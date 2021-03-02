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
import { apiBasePath } from '../../utils/path';

import SimplePage from './templates/SimplePage';
import LoaderSpinner from '../utils/LoaderSpinner';
import ActionButtonsContainer from './templates/ActionButtonsContainer';
import Card from './templates/Card';
import useAsyncAction from '../hooks/useAsyncAction';

import './Backup.css';

const Backup = () => {
    const [file, setFile] = useState("");
    const [triggerRemoveAllData, statusRemoveData] = useAsyncAction("/do/delete-data", "POST");
    const [triggerConfigRestore, statusConfigRestore] = useAsyncAction("/backup/config-restore", "POST");
    
    const status = [statusRemoveData, statusConfigRestore].includes("pending") && "pending";

    const confirmRemoveAllData = () => {
        if(window.confirm("This action will delete all sampling data from the machine forever. Do you want to continue?")) {
            triggerRemoveAllData(null, () => toast.info("Sampling data is being deleted", {autoClose: 10000}));
        }
    }

    const confirmConfigRestore = () => {
        if(window.confirm("This action will overwrite your current configuration. Make sure you made a configuration backup before proceeding. Do you want to continue?")) {
            triggerConfigRestore(file, () => toast.success("Configuration restored correctly. Please reboot the unit to apply it."));
        }
    }

    return (
        <SimplePage title="System Backup" className="Backup">
            <LoaderSpinner status={status}>
                <Card>
                    <div className="configContainer">
                        <div className="sectionBackup">
                            <h2>Backup</h2>

                            <div className="backupButtons">
                                <a target="_blank" rel="noopener noreferrer" href={apiBasePath + "/backup/config"} download>
                                    <button className="appButton" type="button">Configuration Backup</button>
                                </a>

                                <a target="_blank" rel="noopener noreferrer" href={apiBasePath + "/backup/data"} download>
                                    <button className="appButton" type="button">Data Backup</button>
                                </a>
                            </div>
                        </div>

                        <div className="sectionRestore">
                            <h2>Restore</h2>

                            <div className="restoreConfig">
                                <input type="file" accept=".tar" onChange={e => setFile(e.target.files[0])} />
                                <button className="appButton" type="button" disabled={file === ""} onClick={confirmConfigRestore}>Restore Configuration</button>
                            </div>
                        </div>
                    </div>
                </Card>


                <ActionButtonsContainer>
                    <button className="red" type="button" onClick={confirmRemoveAllData}>Remove All Data</button>
                </ActionButtonsContainer>
            </LoaderSpinner>
        </SimplePage>
    );
}
export default Backup;
