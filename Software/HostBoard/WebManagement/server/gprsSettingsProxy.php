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
    require 'utils.php';
    
    $currentFileName = './wvdial.conf';
    $defaultFileName = './wvdial.defaults';
    
    // Change to the proper working folder
    chdir($asetcfolder);
    
    // Save action is handled by a POST
    $action = filter_input(INPUT_POST,'action');
    $data = filter_input(INPUT_POST, 'data',FILTER_DEFAULT, FILTER_REQUIRE_ARRAY);
    if (isset($action) && ($action === "save") && isset($data)) {
        
        // Save incoming data
        saveArrayAsFile($currentFileName, $data);
        
        // And copy the file to /etc area
        $command = "sudo cp $currentFileName /etc/$currentFileName; sudo chmod 640 /etc/$currentFileName; chown root:dialout /etc/$currentFileName";
        exec($command, $output, $result);
        
        // Force reading the file so it will put back
        $action = "";
    } else {
        
        // Check the action in a GET properties
        $action = filter_input(INPUT_GET,'action');
    }
    
    // Set the filename to be read
    $fileName = $currentFileName;    
    if (isset($action) && ($action == "defaults")) {
        $fileName = $defaultFileName;
    } else {
        
        // Copy current configuration file from the /etc area
        $command = "sudo cp /etc/$currentFileName $currentFileName; sudo chmod 660 $currentFileName; sudo chown root:www-data $currentFileName";
        exec($command, $output, $result);
    }    

    // Load current configuration file
    $configData = array( "data" => readFileAsArray($fileName));
    
    // Send a json version of it
    echo json_encode($configData);
 ?>