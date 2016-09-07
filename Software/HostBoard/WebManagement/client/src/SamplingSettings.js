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
var SamplingChannelSettings = require('./SamplingChannelSettings');
var SamplingOtherSettings = require('./SamplingOtherSettings');
var DefaultResetSaveButtons = require('./DefaultResetSaveButtons');

var SamplingSettings = React.createClass({

    getInitialState: function(){
        return this.emptyVals();
    },

    componentWillMount: function(){
        this.setState(this.emptyVals());
    },    
    
    componentDidMount: function() {
        this.refresh();
    },
    
    componentWillUnmount: function() {
    },
    
    refresh: function() {
        this.remoteUpdate('');
    },
    
    loadDefaults: function() {
        this.remoteUpdate('defaults');
    },
        
    remoteUpdate: function(actionToken) {
        
        var source = "airsenseurHostSettingsProxy.php";
        if (actionToken) {
            source = source + "?action=" + actionToken;
        }
        
        if (this.isMounted()) {
            this.setState({ feedbackStatus: "waiting" });
        }
        
        $.get(source, function(result) {
            if (this.isMounted()) {
                if (typeof(result) !== "undefined") {
                    if (result.sensors) {
                        this.setState({
                            sensors: result.sensors.slice(),
                            other: result.other,
                            feedbackStatus: "none"
                        });
                    }
                } else {
                    if (this.isMounted()) {
                        this.setState(this.emptyVals());
                        this.setState({ feedbackStatus: "error" });
                    }
                }
            }
        }.bind(this));
    },
    
    onSave: function() {
        
        this.setState({ feedbackStatus: "waiting" });        
        
        $.ajax({
          url: "airsenseurHostSettingsProxy.php",
          dataType: 'json',
          type: 'POST',
          data: { action: "save", data: { other: this.state.other, sensors: this.state.sensors.slice()} },
          success: function(data) {
            this.setState({
                sensors: data.sensors,
                other: data.other,
                feedbackStatus: "success"
            });
          }.bind(this),
          error: function(xhr, status, err) {
                console.error("airsenseurHostSettingsProxy.php", status, err.toString());
                this.setStatus({ feedbackStatus: "error"});
          }.bind(this)
        });        
    },
    
    handleSensorChange: function(channel, value) {

        var sensor = this.state.sensors[channel];
        if (value.name) {
            sensor.name = value.name;
        }
        if (value.math) {
            sensor.math = value.math;
        }
        
        this.state.sensors[channel] = sensor;
        this.setState({
            sensors: this.state.sensors
        });
    },
    
    handleOtherSettingsChange: function(value) {
        
        this.state.other[value.key] = value.value;
        this.setState({
            other: this.state.other
        });
    },
    
    render: function(){
        return (
            <div>
                <h3>Sampling Settings</h3>
                <div className="row">
                    <div className="col-md-6"><SamplingChannelSettings channel="0" onChange={this.handleSensorChange.bind(this,0)} dataView={this.state.sensors[0]}/></div>
                    <div className="col-md-6"><SamplingChannelSettings channel="1" onChange={this.handleSensorChange.bind(this,1)} dataView={this.state.sensors[1]}/></div>
                </div>
                <div className="row">
                    <div className="col-md-6"><SamplingChannelSettings channel="2" onChange={this.handleSensorChange.bind(this,2)} dataView={this.state.sensors[2]}/></div>
                    <div className="col-md-6"><SamplingChannelSettings channel="3" onChange={this.handleSensorChange.bind(this,3)} dataView={this.state.sensors[3]}/></div>
                </div>
                <div className="row">
                    <div className="col-md-4"><SamplingChannelSettings channel="4" dataView={this.state.sensors[4]} readOnly="true"/></div>
                    <div className="col-md-4"><SamplingChannelSettings channel="5" dataView={this.state.sensors[5]} readOnly="true"/></div>
                    <div className="col-md-4"><SamplingChannelSettings channel="6" dataView={this.state.sensors[6]} readOnly="true"/></div>
                </div>
                <div className="row">
                    <div className="col-md-6"><SamplingOtherSettings onChange={this.handleOtherSettingsChange.bind(this)} dataView={this.state.other}/></div>
                    <div className="col-md-6">
                        <h4>&nbsp;</h4>
                        <DefaultResetSaveButtons onLoadDefaults={this.loadDefaults} 
                                                 onReset={this.refresh}
                                                 onSave={this.onSave}
                                                 feedbackStatus={this.state.feedbackStatus}/>
                    </div>
                </div>
            </div>
        );
    },
    
    emptyVals: function() {
        return {
            sensors: {},
            other: {},
            feedbackStatus: "none"
        };  
    }
});

module.exports = SamplingSettings;
