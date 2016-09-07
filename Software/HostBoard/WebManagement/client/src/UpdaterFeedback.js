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

var UpdaterFeedback = React.createClass({

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
        return (
            <div>
                {(() => {
                    switch (this.props.currentStatus) {
                        case "none": return <span/>;
                        case "waiting": return <span className="glyphicon glyphicon-refresh" aria-hidden="true"></span>;
                        case "success": return <span className="glyphicon glyphicon-ok" aria-hidden="true"></span>;
                        case "error": return <div className="alert alert-danger" role="alert"><span className="glyphicon glyphicon-exclamation-sign" aria-hidden="true"></span></div>;
                        default: return <LivePanel/>;
                    }
                })()}
            </div>
        );
    }
});

module.exports = UpdaterFeedback;
