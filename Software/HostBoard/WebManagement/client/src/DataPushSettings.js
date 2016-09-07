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
var DefaultResetSaveButtons = require('./DefaultResetSaveButtons');
var HourlyScheduler = require('./HourlyScheduler');
var OnOffButton = require('./OnOffButton');
var RemoteLogBox = require('./RemoteLogBox');

var DataPushSettings = React.createClass({

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
        
        var source = "airsenseurDataPushCrontabProxy.php";
        if (actionToken) {
            source = source + "?action=" + actionToken;
        }
        
        if (this.isMounted()) {
            this.setState({ feedbackStatus: "waiting" });
        }
        
        $.get(source, function(result) {
            if (this.isMounted()) {
                if (typeof(result) !== "undefined") {
                    if (result.data) {
                        this.setState({
                            hoursList: result.data,
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
          url: "airsenseurDataPushCrontabProxy.php",
          dataType: 'json',
          type: 'POST',
          data: { action: "save", data: this.state.hoursList },
          success: function(result) {
            this.setState({
                hoursList: result.data,
                feedbackStatus: "success"
            });
          }.bind(this),
          error: function(xhr, status, err) {
                console.error("airsenseurDataPushCrontabProxy.php", status, err.toString());
                this.setStatus({ feedbackStatus: "error"});
          }.bind(this)
        });        
    },
    
    onClickHoursList: function(slot) {

        if ($.inArray(slot, this.state.hoursList) === -1) {
            this.state.hoursList.push(slot);
        } else {
            this.state.hoursList = $.grep(this.state.hoursList, function(value) {
              return value !== slot;
            });            
        }
        
        this.setState({hoursList: this.state.hoursList});
    },
    
    render: function(){
        return (
            <div>
                <h3>Data push process Settings</h3>
                <div className='row'>
                    <h4>Process run scheduler</h4>
                </div>
                <div className='row'>
                    <div className='col-md-12'>
                        <HourlyScheduler hoursList={this.state.hoursList} onClick={this.onClickHoursList}/>
                    </div>
                </div>
                <div className='row'>
                    <div className='col-md-6'>
                        <DefaultResetSaveButtons onLoadDefaults={this.loadDefaults} 
                                                 onReset={this.refresh}
                                                 onSave={this.onSave}
                                                 feedbackStatus={this.state.feedbackStatus}/>
                    </div>
                    <div className='col-md-6'>
                        <div className='col-md-3'>
                            <OnOffButton label="Enabled" source="datapushProcessProxy.php" action="toggle"/>
                        </div>
                        <div className='col-md-3'>
                            <OnOffButton label="Force Now" source="datapushProcessRunningProxy.php" action="start"/>
                        </div>
                    </div>
                </div>
                <div className='row'>
                    <h4>Last data push log result</h4>
                </div>
                <div className='row'>
                    <div className='col-md-12'>
                        <RemoteLogBox cols="80" rows="25" source="datapushProcessLogProxy.php"/>
                    </div>
                </div>
            </div>
        );
    },
    
    emptyVals: function() {
        
        return {
            hoursList: {},
            feedbackStatus: "none"
        };  
    }
});

module.exports = DataPushSettings;
