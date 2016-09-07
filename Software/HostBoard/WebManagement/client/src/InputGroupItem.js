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

var InputGroupItem = React.createClass({

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
        
        var randomId = Math.random() * 100000;
        var descId = this.props.description + randomId.toFixed(2);
        return (
                <div className="input-group">
                  <input type="text" className="form-control" value={this.props.value}
                        placeholder={this.props.placeholder} aria-describedby={descId} 
                        onChange = {this.props.onChange}
                        readOnly={this.props.readOnly}/>
                  <span className="input-group-addon" id={descId}>{this.props.description}</span>
                </div>
        );
    }
});

module.exports = InputGroupItem;
