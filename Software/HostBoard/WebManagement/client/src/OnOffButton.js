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

var OnOffButton = React.createClass({
    
    getInitialState: function() {
        return this.emptyVals();
    },
    
    componentWillMount: function(){
        this.setState({
            turnOn : this.state.turnOn
        });
    },
    
    componentDidMount: function() {
        this.refresh();
    },
    
    componentWillUnmount: function() {
        clearTimeout(this.timer);
    },
    
    render: function() {
        
        var cName = "btn";
        if (this.state.turnOn === "true") {
            cName = cName + " active";
        }
        
        return (
                <button type='button' 
                    className={cName} 
                    data-toggle='button' 
                    aria-pressed={this.state.turnOn}
                    onClick={this.remoteToggle}
                    autoComplete="off">
                        {this.props.label}
                </button>
        );
    },
    
    refresh: function() {
        this.remoteUpdate('');
                
        this.timer = setTimeout(this.refresh, 8876 + (Math.random() * 3000));
    },
    
    remoteToggle: function() {
        this.remoteUpdate(this.props.action);
    },
    
    remoteUpdate: function(actionToken) {
        var source = this.props.source;        
        if (actionToken) {
            source = source + "?action=" + actionToken;
        }

        $.get(source, function(result) {
            if (this.isMounted()) {
                this.setState({
                    turnOn: result.result.status
                });
                this.forceUpdate();
            }
        }.bind(this));
    },
    
    emptyVals: function() {
        return {
            turnOn: false
        };  
    }
});

module.exports = OnOffButton;