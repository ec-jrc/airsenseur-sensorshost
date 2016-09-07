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
var SamplingSettings = require('./SamplingSettings');
var EditorBox = require('./EditorBox');
var DefaultResetSaveButtons = require('./DefaultResetSaveButtons');

var GPRSSettingsPanel = React.createClass({

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
    
    render: function(){
        return (
            <div>
                <div className='page-header'>
                    <h1>GPRS Configuration</h1>
                </div>
                <div className='row'>
                    <div className='col-md-8'>
                        <EditorBox cols='50' rows='23' contents={this.state.decodedData}
                            onChange={this.onConfigChange}/>
                    </div>
                    <div className='col-md-4'>
                        <DefaultResetSaveButtons onLoadDefaults={this.onLoadDefaults} 
                                                 onReset={this.refresh}
                                                 onSave={this.onSave}
                                                 feedbackStatus={this.state.feedbackStatus}/>
                    </div>
                </div>
            </div>
        );
    },
    
    onConfigChange: function(e) {
        
        var stringContents = e.target.value;
        var data = this.fromStringToArray(stringContents);
        if (this.isMounted()) {
            this.setState({contents: { data: data },
                           decodedData: stringContents });
        }
    },
    
    onLoadDefaults: function() {
        this.remoteUpdate('defaults');
    },
    
    onSave: function() {
        this.setState({ feedbackStatus: "waiting" });        
        
        $.ajax({
          url: "gprsSettingsProxy.php",
          dataType: 'json',
          type: 'POST',
          data: { action: "save", data: this.state.contents.data },
          success: function(result) {
                this.setState({
                    contents: { data: result.data },
                    decodedData: this.fromArrayToString(result.data),
                    feedbackStatus: "none"
                });
          }.bind(this),
          error: function(xhr, status, err) {
                console.error("gprsSettingsProxy.php", status, err.toString());
                this.setStatus({ feedbackStatus: "error"});
          }.bind(this)
        });        
    },

    refresh: function() {
        this.remoteUpdate('');
    },
    
    remoteUpdate: function(actionToken) {
        
        var source = "gprsSettingsProxy.php";
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
                            contents: { data: result.data },
                            decodedData: this.fromArrayToString(result.data),
                            feedbackStatus: "none"
                        });
                    } else {
                        this.setState({
                            feedbackStatus: "error"
                        });
                    }
                } else {
                    if (this.isMounted()) {
                        this.setState(this.emptyVals());
                        this.setState({ feedbackStatus: "error" });
                    };
                }
            }
        }.bind(this));
    },
    
    fromArrayToString: function(dataSet) {
        var content = "";
        $.map(dataSet, function(value, index) {
            if (value.length !== 0) {
                content = content + value + "\n";
            }
        });
        return content;
    },
    
    fromStringToArray: function(stringContents) {
        
        return stringContents.split("\n").filter(function(el) { return el.length !== 0; });
    },
    
    emptyVals: function() {
        
        var data = {};
        return {
            contents: { data: data },
            decodedData: this.fromArrayToString(data),
            feedbackStatus: "none"
        };  
    }
});

module.exports = GPRSSettingsPanel;