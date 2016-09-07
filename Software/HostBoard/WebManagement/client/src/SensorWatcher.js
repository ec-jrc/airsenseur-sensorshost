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

var SensorWatcher = React.createClass({
    
    getInitialState: function() {
        return this.emptyVals();
    },
    
    componentWillMount: function(){
        this.setState({
            dataValid : this.state.dataValid,
            name: this.state.name,
            rawval: this.state.rawval,
            valeval: this.state.valeval,
            rawts: this.state.rawts
        });
    },
    
    componentDidMount: function() {
        this.refresh();
    },
    
    componentWillUnmount: function() {
        clearTimeout(this.timer);
    },
    
    render: function() {
        var labelClass = "label label-warning";
        if (this.state.dataValid) {
            labelClass = "label label-info";
        }
        return (
            <div className="panel panel-default">
                <div className="panel-heading">
                    <h3 className="panel-title">Ch {this.props.channel}: {this.state.name}</h3>
                </div>
                <div className="panel-body">
                    <div className="swatcher_valeval">
                        <span className={labelClass}>{this.truncateNum(this.state.valeval)}</span>
                    </div>
                    <table className="swatcher_table">
                        <tbody>
                            <tr>
                                <td><span className="label">Raw Value:</span></td>
                                <td>{this.state.rawval}</td>
                            </tr>
                            <tr>
                                <td><span className="label">TimeStamp:</span></td>
                                <td>{this.state.rawts}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>                
        );
    },
    
    refresh: function() {
        var source = this.props.source + "?channel=" + this.props.channel;
        $.get(source, function(result) {
            var lastSample = result;
            if (this.isMounted()) {
                if (typeof(lastSample.result) !== "undefined") {
                    this.setState({
                        name: lastSample.result.name,
                        rawval: lastSample.result.value,
                        valeval: lastSample.result.evalSampleVal,
                        rawts: lastSample.result.timeStamp,
                        dataValid: true
                    });
                } else {
                    this.setState(this.emptyVals());
                }
            }
        }.bind(this));
        
        this.timer = setTimeout(this.refresh, 8356 + (Math.random() * 3000));
    },  
    
    emptyVals: function() {
        return {
            name: 'N/A',
            rawval: 'N/A',
            valeval: 'N/A',
            rawts: 0,
            dataValid: false
        };  
    },
    
    truncateNum: function(inputVal) {
      
        if (isNaN(inputVal)) {
            return "N/A";
        }
        
        if (inputVal !== Math.floor(inputVal)) {
            return inputVal.toFixed(2);
        }
        
        return inputVal;
    }
    
});

module.exports = SensorWatcher;

