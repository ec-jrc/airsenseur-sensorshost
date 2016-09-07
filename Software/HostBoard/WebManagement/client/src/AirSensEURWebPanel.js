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

require('bootstrap');
var ReactDOM = require('react-dom'); 
var React = require('react');
var NavBarOption = require('./NavBarOption');
var LivePanel = require('./LivePanel');
var GeneralSettingsPanel = require ('./GeneralSettingsPanel');
var GPRSSettingsPanel = require('./GPRSSettingsPanel');

var AirSensEURWebPanel = React.createClass({

    getInitialState: function(){
        return {
            currentPage: 0
        };
    },

    componentWillMount: function(){
    },    
    
    componentDidMount: function() {
    },
    
    componentWillUnmount: function() {
    },
    
    onNavOptionClick: function(panelId) {
        this.setState({currentPage: panelId});
    },
    
    render: function(){
        return (
            <div>
                <nav className="navbar navbar-default">
                    <div className="container-fluid">
                      <div className="navbar-header">
                        <button type="button" className="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
                          <span className="sr-only">Toggle navigation</span>
                          <span className="icon-bar"></span>
                          <span className="icon-bar"></span>
                          <span className="icon-bar"></span>
                        </button>
                        <a className="navbar-brand" href="#">AirSensEUR</a>
                      </div>
                      <div id="navbar" className="navbar-collapse collapse">
                        <ul className="nav navbar-nav">
                          <NavBarOption label="Live View" onclick={this.onNavOptionClick.bind(this,0)} active={this.state.currentPage === 0}/>
                          <NavBarOption label="General Settings" onclick={this.onNavOptionClick.bind(this,1)} active={(this.state.currentPage === 1)}/>
                          <NavBarOption label="GPRS Config" onclick={this.onNavOptionClick.bind(this,2)} active={(this.state.currentPage === 2)}/>
                        </ul>
                      </div>
                    </div>
                </nav>

                {(() => {
                    switch (this.state.currentPage) {
                        case 0: return <LivePanel/>;
                        case 1: return <GeneralSettingsPanel/>;
                        case 2: return <GPRSSettingsPanel/>;
                        default: return <LivePanel/>;
                    }
                })()}
            </div>
        );
    }
});

ReactDOM.render(<AirSensEURWebPanel/>, document.getElementById('airsenseurhostwebpanel'));

module.exports = AirSensEURWebPanel;

