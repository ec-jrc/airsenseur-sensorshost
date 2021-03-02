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

var SamplingCannelSettings = React.createClass({

    getInitialState: function(){
        return null;
    },

    componentWillMount: function(){
    },    
    
    componentDidMount: function() {
    },
    
    componentWillUnmount: function() {
    },
    
    handleNameChanged: function(e) {
        if (this.props.onChange) {
            this.props.onChange({ name: e.target.value });
        }
    },
    
    handleMathChanged: function(e) {
        if (this.props.onChange){
            this.props.onChange({ math: e.target.value });
        }
    },
    
    render: function(){
        return (
            <div>
                <h4>Channel {this.props.channel}</h4>
                <InputGroupItem placeholder="Associated internal name" 
                                description = "Internal Sensor Name" 
                                value={(typeof(this.props.dataView) !== "undefined")? this.props.dataView.name : ''}
                                readOnly={this.props.readOnly}
                                onChange={this.handleNameChanged}/>
                                        
                <InputGroupItem placeholder="Evaluation Math Expression" 
                                description = "Math Expression" 
                                value={(typeof(this.props.dataView) !== "undefined")? this.props.dataView.math : ''}
                                readOnly={this.props.readOnly}
                                onChange={this.handleMathChanged}/>
            </div>
        );
    }
});

module.exports = SamplingCannelSettings;
