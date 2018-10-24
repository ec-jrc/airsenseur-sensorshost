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

package airsenseur.dev.chemsensorpanel.helpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author marco
 */
public class FileLogger {
    
    BufferedWriter writer = null;

    /**
     * Create a new file with name starting with as specified
     * but with appended timestamp
     * @param nameSeed the beginning of the filename
     */
    public void openFile(String nameSeed) {
        
        String fileName = getTemporaryFileName(nameSeed);
        File logFile=new File(fileName);
        
        try {
            writer = new BufferedWriter(new FileWriter(logFile));
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
        
        writer = null;
    }
    
    /**
     * Add a new sample row at the end of current line
     * @param value the sample value
     * @param sensorName
     * @param sensorSerial
     * @param boardId the board Id
     * @param channel the sensor Id
     * @param timestamp timestamp associated to the sample 
     */
    public void appendSample(double value, String sensorName, String sensorSerial, int boardId, int channel, int timestamp) {
        
        if (writer == null) {
            return;
        }
            
        StringBuilder sb = new StringBuilder();
        sb.append(boardId).append(",");
        sb.append(channel).append(",");
        sb.append(sensorName).append(",");
        sb.append(sensorSerial).append(",");
        sb.append(value).append(",");
        sb.append(timestamp);
        sb.append("\r\n");
        
        try {
            writer.write(sb.toString());
            writer.flush();
        } catch (IOException ex) {
        }  
    }
    
    private String getTemporaryFileName(String nameSeed) {
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmm");
        
        Date now = new Date();
        String fileName = nameSeed + "_" + dateFormat.format(now) + ".txt";

        return fileName;
    }
}
