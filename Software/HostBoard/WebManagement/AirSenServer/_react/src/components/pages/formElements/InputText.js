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
import Popup from 'reactjs-popup';
import { FaInfoCircle } from 'react-icons/fa';
import { useField } from 'formik';

import InputHOC from './InputHOC';

import './InputText.css';

const InputText = ({ label, error, isPassword, helpText, ...props }) => {
    const [field] = useField(props);
    const helpEnabled = !!helpText && helpText !== ""

    const Tooltip = () => (
        <Popup
          trigger={<span><FaInfoCircle /></span>}
          on={['hover','focus','click']}
          closeOnDocumentClick
        >
          <span> {helpText} </span>
        </Popup>
      );    

    return (
        <label>
            {label}

            { helpEnabled &&
                <span className="infoIcon">
                    <Tooltip/>
                </span>                
            }

            <input 
                className={"InputText" + (error ? " error" : "") + (isPassword ? " password" : "")}
                placeholder={isPassword ? "●●●●●●●●" : ""}
                {...field} 
                {...props} 
            />
        </label>
    );
}

export default InputHOC(InputText);
