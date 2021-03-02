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

import './Settings52North.css';

const Settings52North = ({ formik }) => 
    <div className="Settings52North">
        <div className="enabler">
            <InputCheckbox label="Enable" name="enabled" />
        </div>
        <div>{/* empty cell in grid */}</div>

        {formik.values.enabled && 
            <>
                <InputText label="Hostname"         name="hostname" />
                <InputText label="Port"             name="port"     type="number"/>
                <InputText label="FOI"              name="foi" />
                <InputText label="Endpoint"         name="endpoint" />
                <InputText label="Offering Name"    name="offeringName" />

                <div className="checks">
                    <InputCheckbox label="Update location"      name="updateLocation"/>
                    <InputCheckbox label="Observation by ID"    name="observationById"/>
                </div>
            </>
        }
    </div>

export default FormContainerHOC(
    Settings52North, 
    "/settings/52north",
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
        foi:                Yup.string().when("enabled", {is: true, then: Yup.string().required("Required field")}),
        endpoint:           Yup.string().when("enabled", {is: true, then: Yup.string().required("Required field")}),
        offeringName:       Yup.string().when("enabled", {is: true, then: Yup.string().required("Required field")}),
        updateLocation:     Yup.boolean().when("enabled", {is: true, then: Yup.boolean()}),
        observationById:    Yup.boolean().when("enabled", {is: true, then: Yup.boolean()}),
    }),
    "52North Setup",
    "Configuration saved successfully. It will be effective starting from the next data push."
);
                    
