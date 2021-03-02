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
import { FieldArray } from 'formik';

import FormContainerHOC from './FormContainerHOC';
import InputOnOffButton from './formElements/InputOnOffButton';
import InputUTCDatetime from './formElements/InputUTCDatetime';

import './SettingsDateTime.css';

const SettingsDateTime = ({ formik }) => {
    const date = new Date(formik.values.utc)
    const localTime = !isNaN(date) ? date : new Date();

    return (
        <div className="SettingsDateTime">
            <div>
                <InputUTCDatetime name="utc"  label="UTC Time" />
                <p>This equals to {localTime.toLocaleDateString()} {localTime.toLocaleTimeString()} in local time.</p>
            </div>

            <div>
                <div className="dataPushLabel">Data push schedule</div>
                <FieldArray name="schedule">
                    <div className="scheduleContainer">
                        <InputOnOffButton name={`schedule.0`} label={"00"} />
                        <InputOnOffButton name={`schedule.1`} label={"01"} />
                        <InputOnOffButton name={`schedule.2`} label={"02"} />
                        <InputOnOffButton name={`schedule.3`} label={"03"} />
                        <InputOnOffButton name={`schedule.4`} label={"04"} />
                        <InputOnOffButton name={`schedule.5`} label={"05"} />
                        <InputOnOffButton name={`schedule.6`} label={"06"} />
                        <InputOnOffButton name={`schedule.7`} label={"07"} />
                        <InputOnOffButton name={`schedule.8`} label={"08"} />
                        <InputOnOffButton name={`schedule.9`} label={"09"} />
                        <InputOnOffButton name={`schedule.10`} label={"10"} />
                        <InputOnOffButton name={`schedule.11`} label={"11"} />
                        <InputOnOffButton name={`schedule.12`} label={"12"} />
                        <InputOnOffButton name={`schedule.13`} label={"13"} />
                        <InputOnOffButton name={`schedule.14`} label={"14"} />
                        <InputOnOffButton name={`schedule.15`} label={"15"} />
                        <InputOnOffButton name={`schedule.16`} label={"16"} />
                        <InputOnOffButton name={`schedule.17`} label={"17"} />
                        <InputOnOffButton name={`schedule.18`} label={"18"} />
                        <InputOnOffButton name={`schedule.19`} label={"19"} />
                        <InputOnOffButton name={`schedule.20`} label={"20"} />
                        <InputOnOffButton name={`schedule.21`} label={"21"} />
                        <InputOnOffButton name={`schedule.22`} label={"22"} />
                        <InputOnOffButton name={`schedule.23`} label={"23"} />
                    </div>
                </FieldArray>
            </div>
        </div>
    );
}

export default FormContainerHOC(
    SettingsDateTime, 
    "/settings/datetime",
    Yup.object({
        utc:        Yup.date().typeError("Must be a date"),
        schedule:   Yup.array().of(Yup.boolean())
    }),
    "Date and Time Setup"
);
                    
