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

import airsenseur.dev.comm.AppDataMessage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author marco
 */
public class FileConfiguration {
    
    private static final String BOARD_SESSION_IDENTIFIER = "####S####";

    public class SessionContent extends ArrayList<String> {
        
        private int currentOffset = 0;
        
        public String getNextLine() {
            
            if (currentOffset >= size()) {
                return null;
            }
            
            String nextLine = get(currentOffset);
            currentOffset++;
            
            return nextLine;
        }
    };
    
    private BufferedWriter writer = null;
    private BufferedReader reader = null;
    
    private final List<SessionContent> sessionContents = new ArrayList<>();
    
    /**
     * Create or read a configuration file
     * @param fileName: the required file name
     * @param read: open in read mode if true
     * @return true if success
     */
    public boolean openFile(String fileName, boolean read) {
        
        File configFile = new File(fileName);
        return openFile(configFile, read);
    }
    
    /**
     * Create or read a configuration file
     * @param configFile
     * @param read 
     * @return  true if success
     */
    public boolean openFile(File configFile, boolean read) {
        
        sessionContents.clear();        
        if (configFile == null) {
            return false;
        }

        try {
            writer = null;
            reader = null;
            if (!read) {
                writer = new BufferedWriter(new FileWriter(configFile ));
            } else {
                reader = new BufferedReader(new FileReader(configFile));
                populateSessionsContents();
                reader.close();
                reader = null;
            }
        } catch (IOException ex) {
            return false;
        }
        
        return true;
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
     * Generate a session header in the output file
     */
    public void generateBoardSession() {
        if (writer == null) {
            return;
        }
        
        try {
            
          writer.write(BOARD_SESSION_IDENTIFIER);
          writer.newLine();
          writer.flush();
          
        } catch (IOException e){
        } 
    }
    
    
    /** 
     * Dump all commands into a human readable text based file
     * @param commands 
     */
    public void appendCommands(List<AppDataMessage> commands) {
        
        if (writer == null) {
            return;
        }
        
        try { 
            for (AppDataMessage message : commands) {

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
    
    
    public AppDataMessage getNextCommand(int session) {
        
        if (session >= sessionContents.size()) {
            return null;
        }
        
        String line = sessionContents.get(session).getNextLine();
        if (line == null) {
            return null;
        }

        // Remove { and } if present 
        // (Releases Pre 0.4 generate configuration file with { and } on command strings);
        line = line.replace("{", "");
        line = line.replace("}", "");

        int endCmdPos = line.indexOf(":");
        if ((endCmdPos < 0) || ((endCmdPos+1)>line.length())) {
            return null;
        }

        String cmd = line.substring(0, endCmdPos);
        String comment = line.substring(endCmdPos+1);

        return new AppDataMessage(cmd, comment);
    }
    
    
    // Releases Post 0.4 have sessions embedded in the configuration file.
    // Each session is statically associated to a tab (i.e. board) in the java panel
    //
    // Returns: the number of sessions found or -1 if no reader is available
    private int populateSessionsContents() {
        
        if (reader == null) {
            return -1;
        }

        try {
            
            String line;
            while ((line = reader.readLine()) != null) {
            
                if (line.startsWith(BOARD_SESSION_IDENTIFIER)) {
                    sessionContents.add(new SessionContent());
                } else {

                    // Configuration files generated by version Pre 0.4 does not support sessions
                    if(sessionContents.isEmpty()) {
                        sessionContents.add(new SessionContent());
                    }

                    // Populate session content
                    SessionContent session = sessionContents.get(sessionContents.size()-1);
                    session.add(line);
                }
            }
            
        } catch (IOException e) {
            return -1;
        }
        
        return sessionContents.size();
    }
}
