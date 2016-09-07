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

var HourlySlot = React.createClass({
    
    getInitialState: function(){
        return null;
    },

    componentWillMount: function(){
    },    
    
    componentDidMount: function() {
    },
    
    componentWillUnmount: function() {
    },
    
    render: function() {
        var cName = "label label-default";
        if (this.props.status) {
            cName = "label label-success";
        }
        return (
                    <td key={this.props.key} onClick={this.props.onClick}>
                        <span className={cName}>&nbsp;&nbsp;</span>
                    </td>
                );
    }

});

var HourlyScheduler = React.createClass({
    
    getInitialState: function(){
        return this.emptyVals();
    },

    componentWillMount: function(){
    },    
    
    componentDidMount: function() {
    },
    
    componentWillUnmount: function() {
    },
    
    onClickHourSlot: function(slot, item) {
        if(this.props.onClick) {
            this.props.onClick(slot);
        }
    },
    
    render: function() {
        
        var hoursStatus = this.fromProps(this.props.hoursList);
        return(
            <div>
                <table style={{width: '100%'}}>
                    <tbody>
                        <tr>
                            {this.state.hoursNames.map(function(result) {
                                return <td key={result.id}>{result.value}</td>;
                            })}                            
                        </tr>
                        <tr>
                            {hoursStatus.map(function(result, i) {
                                var boundClick = this.onClickHourSlot.bind(this, result.id);
                                return <HourlySlot key={result.id} status={result.status} onClick={boundClick}/>
                            }, this)}                           
                        </tr>
                    </tbody>
                </table>
            </div>
        );
    },
    
    fromProps: function(hoursList) {
        var hoursStatus = [];
        
        for (var i = 0; i < 24; i++) {
            hoursStatus[i] = { id: i, status: false };
        }
        
        $.map(hoursList, function(value, index) {
           hoursStatus[value].status = true; 
        });
        
        return hoursStatus;
    },
    
    emptyVals: function() {
        var hoursNames = [];
        for (var i = 0; i < 24; i++) {
            hoursNames[i] = { id: i, value: ("00"+i).slice(-2) };
        }
        
        return {
            hoursNames: hoursNames
        };
    }
});

module.exports = HourlyScheduler;
