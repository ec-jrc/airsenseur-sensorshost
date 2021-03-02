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
    
    $currentFileName = './sensor.properties';
    $defaultFileName = './sensor.defaults';
    
    // Change to the proper working folder
    chdir($asetcfolder);
    
    // Save action is handled by a POST
    $fileName = $currentFileName;
    $action = filter_input(INPUT_POST,'action');
    $data = filter_input(INPUT_POST,'data', FILTER_SANITIZE_STRING, FILTER_REQUIRE_ARRAY);
    if (isset($action) && ($action === "save") && isset($data)) {
        
        // Save incoming data
        $nextConfig = mergeConfigFile($data, $currentFileName);
        saveConfigFile($currentFileName, $nextConfig);
        
        // Force reading the file so it will put back
        $action = "";
    } else {
        
        // Check the action in a GET properties
        $action = filter_input(INPUT_GET,'action');
    }
    
    // Set the filename to be read
    if (isset($action) && ($action == "defaults")) {
        $fileName = $defaultFileName;
    }

    // Load current sensor.properties file
    $sensorData = loadConfigFile($fileName);
    
    // Send a json version of it
    echo json_encode($sensorData);
    
    
    // ------------------------------------------------------------------------
    
    function loadConfigFile($fileName) {
        
        // Read current file content
        $plainConfig = parseConfigFile($fileName, false);

        // We should have "numSensors"
        if (!isset($plainConfig['numSensors'])) {
            $plainConfig['numSensors'] = 1;
        }

        // Group all sensor data
        $sensorsConfig = array();
        for ($channel = 0; $channel < $plainConfig['numSensors']; $channel++) {
            $nameKey = sprintf("sensorname_%02d", $channel);
            $expKey = sprintf("sensorexpression_%02d", $channel);

            $sensorsConfig[$channel]['name'] = $plainConfig[$nameKey];
            $sensorsConfig[$channel]['math'] = $plainConfig[$expKey];
        }

        // Add global values
        $resultConfig["sensors"] = $sensorsConfig;
        foreach ($plainConfig as $key => $value) {
            if (!startsWith($key, "sensorname") && !startsWith($key, "sensorexpression")) {
                $resultConfig['other'][$key] = $value;
            }
        }        
        
        return $resultConfig;
    }
    
    function mergeConfigFile($data, $configFileName) {
        
        // Load current config file
        $currentConfig = loadConfigFile($configFileName);
        
        // Check for a minimum data validity
        if (!isset($data['sensors']) || !isset($data['other'])) {
            return $currentConfig;
        }
        
        // Merge new data with current configuration
        $sensors = $data['sensors'];
        foreach ($sensors as $channel => $chValue) {
            foreach($chValue as $key => $value) {
                $currentConfig['sensors'][$channel][$key] = $value;
            }
        }
        
        $other = $data['other'];
        foreach ($other as $key => $value) {
            $currentConfig['other'][$key] = $value;
        }
        
        return $currentConfig;
    }
    
    function saveConfigFile($currentFileName, $data) {
        
        // Check for a minimum data validity
        if (!isset($data['sensors']) || !isset($data['other'])) {
            return;
        }
        
        // Open the file for writing
        $handle = fopen($currentFileName, "w");
        if (!$handle) {
            return;
        }
        
        // Starts with "other" section
        foreach($data['other'] as $key => $value) {
            fprintf($handle, "%s=%s\n", $key, $value);
        }
        
        // Then each sensors property
        foreach($data['sensors'] as $channel => $chValue) {
            if (isset($chValue['name']) && isset($chValue['math'])) {
                $nameKey = sprintf("sensorname_%02d", $channel);
                $expKey = sprintf("sensorexpression_%02d", $channel);
                
                fprintf($handle, "%s=%s\n", $nameKey, $chValue['name']);
                fprintf($handle, "%s=%s\n", $expKey, $chValue['math']);
            }
        }
        
        fclose($handle);
    }
 ?>