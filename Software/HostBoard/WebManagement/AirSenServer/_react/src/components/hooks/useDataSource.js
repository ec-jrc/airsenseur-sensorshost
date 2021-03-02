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

import { useEffect, useState } from "react";
import axios from 'axios';
import { toast } from "react-toastify";

import { apiBasePath } from "../../utils/path";

const useDataSource = (apiUrl, defaultData = {}) => {
    const [data, setData] = useState(defaultData);
    const [status, setStatus] = useState("pending");
    
    const performAsync = (silent = false) => {
        if(!silent) {
            setStatus("pending");
        }

        const source = axios.CancelToken.source();
        axios.get(apiBasePath + apiUrl, {
            cancelToken: source.token
        })
        .then((response) => {
            setData(response.data);
            setStatus("fulfilled");
        })
        .catch((error) => {
            setStatus("rejected");
            if(error.response !== undefined) {
                toast.error("Error " + error.response.status + ": " + error.response.data);
            } else {
                toast.error("Network error");
                console.log(error);
            }
        });

        return () => source.cancel();
    }

    useEffect(performAsync, []);

    return [data, setData, status, performAsync];
}

export default useDataSource;
