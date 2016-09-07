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

package airsenseur.dev.helpers;

import airsenseur.dev.comm.CommProtocolHelper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author marco
 */
public class FileConfiguration {
    
    private BufferedWriter writer = null;
    private BufferedReader reader = null;
    
    /**
     * Create or read a configuration file
     * @param fileName: the required file name
     */
    public void openFile(String fileName, boolean read) {
        
        File configFile = new File(fileName);
        openFile(configFile, read);
    }
    
    /**
     * Create or read a configuration file
     * @param configFile
     * @param read 
     */
    public void openFile(File configFile, boolean read) {
        
        if (configFile == null) {
            return;
        }

        try {
            writer = null;
            reader = null;
            if (!read) {
                writer = new BufferedWriter(new FileWriter(configFile ));
            } else {
                reader = new BufferedReader(new FileReader(configFile));
            }
        } catch (IOException ex) {
        }
    }
    
    /**
     * Close the file
     */
    public void closeFile() {
        if (writer != null) {
            
            try {
                writer.close();
            } catch(IOException ex) {
            }
        }
        
        if (reader != null) {
            
            try {
                reader.close();
            } catch(IOException ex) {
            }
        }
        
        writer = null;
        reader = null;
    }
    
    
    /** 
     * Dump all commands into a human readable text based file
     * @param commands 
     */
    public void appendCommands(List<CommProtocolHelper.DataMessage> commands) {
        
        if (writer == null) {
            return;
        }
        
        try { 
            for (CommProtocolHelper.DataMessage message : commands) {

                StringBuilder sb = new StringBuilder();
                sb.append(message.getCommandString());
                sb.append(":");
                sb.append(message.getCommandComment());

                writer.write(sb.toString());
                writer.newLine();
            }
            
            writer.flush();
        } catch (IOException e){
        } 
    }
    
    
    public CommProtocolHelper.DataMessage getNextCommand() {
        
        if (reader == null) {
            return null;
        }
        
        CommProtocolHelper.DataMessage command = null;
        try {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            
            int endCmdPos = line.indexOf("}");
            if ((endCmdPos < 0) || ((endCmdPos+2)>line.length())) {
                return null;
            }
            
            String cmd = line.substring(0, endCmdPos+1);
            String comment = line.substring(endCmdPos+2);
            
            command = new CommProtocolHelper.DataMessage(cmd, comment);
            
        } catch (IOException e) {
            return null;
        }
        
        return command;
    }
}
