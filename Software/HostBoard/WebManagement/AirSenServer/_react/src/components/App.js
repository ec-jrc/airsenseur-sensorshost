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
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import { ToastContainer } from "react-toastify"

import Maintenance from './pages/Maintenance';
import NotFound from './pages/NotFound';
import Overview from './pages/Overview';
import Settings52North from './pages/Settings52North';
import Backup from './pages/Backup';
import SettingsDateTime from './pages/SettingsDateTime';
import SettingsGPRS from './pages/SettingsGPRS';
import SettingsInflux from './pages/SettingsInflux';
import SettingsVPN from './pages/SettingsVPN';
import SettingsWifi from './pages/SettingsWifi';
import MenuPanel from './menuPanel/MenuPanel';

import './App.css';
import 'react-toastify/dist/ReactToastify.css';

const App = () =>
    <div className="App">
        <Router>
            <div className="controlContainer">
                <MenuPanel />
            </div>

            <div className="panelContainer">
                <Switch>
                    <Route exact path="/">                      <Overview />        </Route>
                    <Route exact path="/connections/influx">       <SettingsInflux />  </Route>
                    <Route exact path="/connections/52north">      <Settings52North /> </Route>
                    <Route exact path="/settings/vpn">          <SettingsVPN />     </Route>
                    <Route exact path="/settings/wifi">         <SettingsWifi />    </Route>
                    <Route exact path="/settings/gprs">         <SettingsGPRS />    </Route>
                    <Route exact path="/settings/datetime">     <SettingsDateTime /></Route>
                    <Route exact path="/settings/backup">       <Backup />  </Route>
                    <Route exact path="/settings/maintenance">  <Maintenance />     </Route>

                    <Route>                                     <NotFound />        </Route>
                </Switch>       
            </div>
        </Router>

        <ToastContainer
            position="bottom-right"
            autoClose={3000}
            hideProgressBar={false}
            newestOnTop
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
        />
    </div>      

export default App;
