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
import GPRSTester from './fragments/GPRSTester';

import './SettingsGPRS.css';

const SettingsGPRS = ({ formik }) => {
    return (
        <div className="SettingsGPRS">
            <div className="enabler">
                <InputCheckbox label="Enable" name="enabled" />
            </div>
            <div>{/* empty cell in grid */}</div>

            {formik.values.enabled && 
                <>
                    <InputText label="APN"      name="apn" />
                    <InputText label="SIM Pin"  name="simPin" />

                    <div className="tester">
                        <GPRSTester apn={formik.values.apn} pin={formik.values.simPin} />
                    </div>
                </>
            }
        </div>
    );
}
export default FormContainerHOC(
    SettingsGPRS, 
    "/settings/gprs",
    Yup.object({
        enabled: Yup.boolean(),

        // other fields are checked only if enabled is true
        apn:     Yup.string().when("enabled", {is: true, then: Yup.string().required("Required field")}),
        simPin:  Yup.string().when("enabled", {is: true, then: Yup.string().matches(/\b\d{4,8}\b/, "Required 4 to 8 digits, or empty")}),
    }),
    "GPRS Setup"
);
                    
