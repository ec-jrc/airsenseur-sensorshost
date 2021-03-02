<?php

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

    header('Content-Type: application/json');

    require 'config.inc.php';
    
    // Change to the proper working folder
    chdir($asscriptfolder);
        
    // Check the action
    $action = filter_input(INPUT_GET,'action');
    
    $output = array();
    $result = 0;
    
    // Turn on/off the sampling process
    if (isset($action) && ($action == "toggle")) {
        $command = "sudo ./event_samplingprocess";
        exec($command, $output, $result);
    }
    
    // Check for sampling process status
    $command = "sudo ./check_hostdaemon";
    exec($command, $output, $result);

    $boolResult = ($result === 1)? "true" : "false";
    echo json_encode(array("result" => array("status" => $boolResult)));

    return;
?>
