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


    // Check if a string starts with $query substring
    function startsWith($string, $query) {
        return (substr($string, 0, strlen($query)) === $query);
    }
    
    // Check if a string contains the $query substring
    function contains($string, $query) {
        return (strpos ($string, $query) !== false);
    }
    
    // Read a text file and return each line as array. LineNum is used as key 
    // in the returned array
    function readFileAsArray($fileName) {
        $fileContents = array();
        $handle = fopen($fileName, "r");
        $numLine = 1;
        if ($handle) {
            while (($line = fgets($handle)) !== false) {
                $fileContents[$numLine] = trim($line, "\n");
                $numLine++;
            }
            
            fclose($handle);
        }
        
        return $fileContents;
    }
    
    // Save an array into a text file. Each item per line
    function saveArrayAsFile($fileName, $data) {
        
        // Check for a minimum data validity
        if (!isset($data) || (count($data) == 0)) {
            return;
        }
        
        // Open the file for writing
        $handle = fopen($fileName, "w");
        if (!$handle) {
            return;
        }
        
        // Write out all elements presents in the array
        foreach($data as $value) {
            fprintf($handle, "%s\n", $value);
        }
                
        fclose($handle);
    }
    
    // Read a configuration.properties java style file.
    function parseConfigFile($fileName) {
        $plainConfig = array();
        $handle = fopen($fileName, "r");
        if ($handle) {
            while (($line = fgets($handle)) !== false) {
                if (strlen($line) > 1) {
                    list($k, $v) = explode('=', $line);
                    
                    $key = trim($k);
                    $value = trim($v);
                    
                    $plainConfig[$key] = $value;
                }
            }

            fclose($handle);
        }
        
        return $plainConfig;
    }

?>