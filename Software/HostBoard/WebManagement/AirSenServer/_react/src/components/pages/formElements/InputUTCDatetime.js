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
import { useField } from 'formik';

import InputHOC from './InputHOC';
import DateTimePicker from 'react-datetime-picker';

import './InputUTCDatetime.css';

const InputUTCDatetime = ({ label, error, ...props }) => {
    const [field,, helpers] = useField(props);

    const time = !isNaN(new Date(field.value)) ? new Date(field.value) : new Date();
    time.setMinutes(time.getMinutes() + time.getTimezoneOffset())
    
    return (
        <label>
            {label}

            <DateTimePicker
                    className="InputUTCDatetime"
                    clearIcon={null}
                    calendarIcon={null}
                    {...field} 
                    {...props} 
                    value={time}
                    onChange={d => {
                        d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
                        helpers.setValue(d)
                    }}
                    onBlur={null} />
        </label>
    );
}

export default InputHOC(InputUTCDatetime);
