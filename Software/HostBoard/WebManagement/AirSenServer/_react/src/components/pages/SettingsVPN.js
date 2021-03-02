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
import InputCheckbox from './formElements/InputCheckbox';
import InputTextArea from './formElements/InputTextArea';
import InputText from './formElements/InputText';
import InputSelect from './formElements/InputSelect';

import './SettingsVPN.css';

const SettingsVPN = ({ formik }) => 
    <div className="SettingsVPN">
        <div className="enabler">
            <InputCheckbox label="Enable" name="enabled" />
        </div>
        <div>{/* empty cell in grid */}</div>

        {formik.values.enabled && 
            <>
                <InputText label="Hostname"         name="hostname" />
                <InputText label="Port"             name="port"     type="number" />
                
                <InputSelect label="Protool"        name="protocol">
                    <option value="udp">UDP</option>
                    <option value="tcp">TCP</option>
                </InputSelect>


                <div className="checks">
                    <InputCheckbox label="Use LZO Compression"  name="useLZOCompression"/>
                    <InputCheckbox label="nsCertType"           name="nsCertType"/>
                </div>

                <div className="certs">
                    <InputTextArea label="Public server cert"   name="publicServerCert" />
                    <InputTextArea label="Public client key"    name="publicClientKey" />
                    <InputTextArea label="Public client cert"   name="publicClientCert" />
                </div>
            </>
        }
    </div>

export default FormContainerHOC(
    SettingsVPN, 
    "/settings/openvpn",
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
        useLZOCompression:  Yup.boolean(),
        nsCertType:         Yup.boolean(),
        publicServerCert:   Yup.string(),
        publicClientKey:    Yup.string(),
        publicClientCert:   Yup.string()
    }),
    "OpenVPN Setup"
);
