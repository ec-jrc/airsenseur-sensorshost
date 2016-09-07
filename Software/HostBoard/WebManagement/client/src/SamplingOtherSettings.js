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
var InputGroupItem = require ('./InputGroupItem');

var SamplingOtherSettings = React.createClass({

    getInitialState: function(){
        return null;
    },

    componentWillMount: function(){
    },    
    
    componentDidMount: function() {
    },
    
    componentWillUnmount: function() {
    },
    
    handlePollChange: function(e) {
        if (this.props.onChange) {
            this.props.onChange({ key: "pollTime",
                                  value: e.target.value
                              });
        }
    },
    
    handleDebugChange: function(e) {
        if (this.props.onChange) {
            this.props.onChange({ key: "debug",
                                  value: e.target.value
                                });
        }
    },
    
    render: function(){
        return (
            <div>
                <h4>Other Settings</h4>
                <InputGroupItem placeholder="Period in milliseconds" 
                                description = "Shield Poll Period (ms)" 
                                onChange={this.handlePollChange}
                                value={(typeof(this.props.dataView) !== "undefined")? this.props.dataView.pollTime : ''}/>
                <InputGroupItem placeholder="Debug Verbosity" 
                                description = "Set debug verbosity (0 to 10)" 
                                onChange={this.handleDebugChange}
                                value={(typeof(this.props.dataView) !== "undefined")? this.props.dataView.debug : ''}/>
            </div>
        );
    }
});

module.exports = SamplingOtherSettings;
