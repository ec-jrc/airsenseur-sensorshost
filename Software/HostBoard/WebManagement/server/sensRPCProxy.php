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
    
    // Simple XML RPC streaming proxy
    $channel = filter_input(INPUT_GET,'channel');
    if (!isset($channel)) {
        echo '{"jsonrpc":"2.0","id":null,"error":{"code":-8001,"message":"Invalid Parameters"}}';
        flush();
        return;
    }
    
    $mysocket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);

    if (!socket_connect($mysocket, '127.0.0.1', 8000)) {
      echo '{"jsonrpc":"2.0","id":null,"error":{"code":-8000,"message":"Unavailable"}}';
      flush();
      return;
    }
    socket_set_option($mysocket,SOL_SOCKET, SO_RCVTIMEO, array("sec"=>5, "usec"=>0));    

    $mystring = '{"jsonrpc":"2.0","id":"0","method":"getLastSample", "params":['.$channel.']}';
    socket_write($mysocket, $mystring, strlen($mystring));
    
    // Read the answer
    $answer = "";
    $readLen = socket_recv ($mysocket, $answer , 1024 , PHP_BINARY_READ );
    echo $answer; flush();

    socket_close($mysocket);
?>