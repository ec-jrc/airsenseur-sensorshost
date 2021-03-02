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

import FormContainerHOC from './FormContainerHOC';
import InputText from "./formElements/InputText";
import InputCheckbox from './formElements/InputCheckbox';

import './SettingsInflux.css';

const SettingsInflux = ({ formik }) => 
    <div className="SettingsInflux">
        <div className="enabler">
            <InputCheckbox label="Enable" name="enabled" />
        </div>
        <div>{/* empty cell in grid */}</div>

        {formik.values.enabled && 
            <>
                <InputText label="Hostname" name="hostname" helpText="InfluxDB server name or IP"/>
                <InputText label="Port"     name="port"     helpText="Usually 8086" type="number"/>
                <InputText label="Database" name="database" helpText="Your Influx database name"/>
                <InputText label="Dataset"  name="dataset"  helpText="The measurement table in the database"/>
                <InputText label="Username" name="username" helpText="Username to authenticate on InfluxDB server"/>
                <InputText label="Password" name="password" helpText="Password to authenticate on InfluxDB server" isPassword />

                <div className="checks">
                    <InputCheckbox label="Encrypt Transaction"  name="encrypt"          helpText="Use HTTPS instead of HTTP in requests"/>
                    <InputCheckbox label="Use Line Protocol"    name="uselineprotocol"  helpText="Recommended for new influxDB servers"/>
                </div>
            </>
        }
    </div>

export default FormContainerHOC(
    SettingsInflux, 
    "/settings/influx",
    Yup.object({
        enabled:            Yup.boolean(),

        // other fields are checked only if enabled is true
        hostname:           Yup.string().when("enabled", {is: true, then: Yup.string().required("Required field")}),
        port:               Yup.mixed().when("enabled", {is: true, then: 
                                Yup.number()
                                    .required("Required field")
                                    .typeError("Must be an integer number")
                                    .integer("Must be an integer number")
                                    .min(0, "Must be between 0 and 65535")
                                    .max(65535, "Must be between 0 and 65535")}),
        database:           Yup.string().when("enabled", {is: true, then: Yup.string().required("Required field")}),
        dataset:            Yup.string().when("enabled", {is: true, then: Yup.string().required("Required field")}),
        username:           Yup.string().when("enabled", {is: true, then: Yup.string().required("Required field")}),
        password:           Yup.string(),
        encrypt:            Yup.boolean().when("enabled", {is: true, then: Yup.boolean()}),
        uselineprotocol:    Yup.boolean().when("enabled", {is: true, then: Yup.boolean()}),
    }),
    "Influx Setup",
    "Configuration saved successfully. It will be effective starting from the next data push."
);
                    
