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
import * as Yup from "yup";
import Popup from "reactjs-popup";
import axios from 'axios';
import { toast } from "react-toastify";

import LoaderSpinner from '../utils/LoaderSpinner';
import FormContainerHOC from './FormContainerHOC';
import InputSelect from './formElements/InputSelect';
import WifiScanner from './fragments/WifiScanner';
import InputText from "./formElements/InputText";

import './SettingsWifi.css';
import { apiBasePath } from '../../utils/path';

const SettingsWifi = ({ formik }) => {
    const clientMode = formik.values.workingMode === "client";

    return (
        <>
            <div className="SettingsWifi">
                <label>
                    <InputSelect label="Working mode" name="workingMode">
                        <option value="ap">Access Point</option>
                        <option value="client">Client</option>
                    </InputSelect>
                </label>

                <div className="container-1">
                    {!clientMode && 
                    <>
                        <h2>Access Point Configuration</h2>
                        <InputText      label="SSID"                name="apSSID" />
                        <InputText      label="WPA Password"        name="apPassword" />
                    </>
                    }

                    {clientMode && 
                    <>
                        <h2>Client Mode Configuration</h2>
                        <InputText      label="Encryption"  name="clientEncryption"  value="WPA-PSK" disabled />
                        <InputText      label="SSID"        name="clientSSID" />

                        <WifiScanner onClick={(value) => formik.setFieldValue("clientSSID", value)} />

                        <InputText      label="Password"    name="clientPassword" isPassword />
                    </>
                    }
                </div>
            </div>

            <Popup  open={formik.isSubmitting} modal closeOnDocumentClick={false}>
                <span>Testing the proposed configuration. Please wait...</span>
                <LoaderSpinner status={"pending"} />
            </Popup>
        </>
    );
}

const preSaveCheck = values => new Promise((resolve, reject) => {
    axios.post(apiBasePath + "/info/wifi-test", values)
    .then(() => {          
        setTimeout(() => {}, 5000);

        const interval = setInterval(() => {
            axios.post(apiBasePath + "/info/wifi-test-result", undefined, {timeout: 1000})
            .then(response => {
                clearInterval(interval);

                const msg = response.data.Valid ?
                    `SUCCESS! The IP address was ${response.data.IPAddress}\n` +
                    `Depending on the network configuration this might change again after saving.\n\n` +
                    `Do you want to save the configuration?`

                  : `WARNING: the configuration test was NOT successful. Do you want to save the configuration ` +
                    `anyway? (Not recommended)`;
                    
                if (window.confirm(msg)) {
                    resolve();
                }

                reject();
            })
            .catch(() => {});
        }, 5000);
    })
    .catch(error => {
        if(error.response !== undefined) {
            toast.error("Error " + error.response.status + ": " + error.response.data);
        } else {
            toast.error("Unknown server error");
            console.log(error);
        }

        reject();
    })
});

export default FormContainerHOC(
    SettingsWifi, 
    "/settings/wifi",
    Yup.object({
        workingMode:         Yup.string().oneOf(["ap", "client"]),
        
        // check only when AP mode is selected
        apSSID:             Yup.string().when("clientMode", {is: "false", then: Yup.string().required("Required field")}),
        apPassword:         Yup.string(),
        
        // check only when client mode is selected
        clientSSID:         Yup.string().when("clientMode", {is: "true", then: Yup.string().required("Required field")}),
        clientPassword:     Yup.string(),
    }),
    "WiFi Settings",
    undefined,
    preSaveCheck
);
                    
