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

var RemoteLogBox = React.createClass({

    getInitialState: function(){
        return this.emptyVals();
    },

    componentWillMount: function(){
    },    
    
    componentDidMount: function() {
        this.refresh();
    },
    
    componentWillUnmount: function() {
        clearTimeout(this.timer);
    },
    
    render: function(){
        return (
            <textarea value={this.state.contents} rows={this.props.rows} cols={this.props.cols} className="astextarea" />
        );
    },
    
    refresh: function() {
        
        this.remoteUpdate();
        this.timer = setTimeout(this.refresh, 8876 + (Math.random() * 3000));
    },
    
    remoteUpdate: function() {
        var source = this.props.source;        

        $.get(source, function(result) {
            if (this.isMounted() && result && result.contents) {
                this.setState({
                    contents: this.fromArrayToString(result.contents)
                });
                this.forceUpdate();
            }
        }.bind(this));
    },    
    
    fromArrayToString: function(dataSet) {
        var content = "";
        $.map(dataSet, function(value, index) {
            if ((value != null) && (value.length !== 0)) {
                content = content + value + "\n";
            }
        });
        return content;
    },
   
    emptyVals: function() {
        return ({contents: ""});
    }
});

module.exports = RemoteLogBox;
