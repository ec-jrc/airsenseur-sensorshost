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

var ReactDOM = require('react-dom'); 
var React = require('react');
var SensorWatcher = require('./SensorWatcher');
var OnOffButton = require('./OnOffButton');

var LivePanel = React.createClass({

    getInitialState: function(){
        return null;
    },

    componentWillMount: function(){
    },    
    
    componentDidMount: function() {
    },
    
    componentWillUnmount: function() {
    },
    
    render: function(){
        return (
            <div>
                <div className='page-header'>
                    <h1>Live View</h1>
                </div>
                <div className='row'>
                    <div className='col-md-3'><SensorWatcher channel="0" source="sensRPCProxy.php"/></div>
                    <div className='col-md-3'><SensorWatcher channel="1" source="sensRPCProxy.php"/></div>
                    <div className='col-md-3'><SensorWatcher channel="2" source="sensRPCProxy.php"/></div>
                    <div className='col-md-3'><SensorWatcher channel="3" source="sensRPCProxy.php"/></div>
                </div>
                <div className='row'>
                    <div className='col-md-3'><SensorWatcher channel="4" source="sensRPCProxy.php"/></div>
                    <div className='col-md-3'><SensorWatcher channel="5" source="sensRPCProxy.php"/></div>
                    <div className='col-md-3'><SensorWatcher channel="6" source="sensRPCProxy.php"/></div>
                    <div className='col-md-3'>
                        <div className="panel panel-default">
                            <div className="panel-heading">
                                <h3 className="panel-title">Process Control</h3>
                            </div>
                            <div className="panel-body">
                                <div className='row'>
                                    <div className='col-md-6'>Sampling Process</div>
                                    <div className='col-md-6'><OnOffButton label="Running" source="samplingProcessProxy.php" action="toggle"/></div>
                                </div>
                                <div className='row'>
                                    <div className='col-md-6'>Data push Process</div>
                                    <div className='col-md-6'><OnOffButton label="Enabled" source="datapushProcessProxy.php" action="toggle"/></div>
                                </div>
                            </div>
                        </div>                
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = LivePanel;

