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

package airsenseur.dev.persisters;

import airsenseur.dev.exceptions.PersisterException;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

/**
 * Implements a file based data persister
 * @author marcos
 */
public class SamplePersisterFile implements SamplesPersister {
    
    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";    
    private static final String DATANAME_PREFIX = "DataSet_";
    private static final String DATANAME_EXTENSION = ".csv";
    
    private final String folderPath;
    private FileWriter fileWriter;

    public SamplePersisterFile() {
        this.folderPath = "";
    }

    public SamplePersisterFile(String folderPath) {
        this.folderPath = folderPath;
    }
    
    @Override
    public boolean startNewLog() {
        try {
            
            fileWriter = new FileWriter(generateNewFileName());
                   
        } catch (IOException ex) {
            fileWriter = null;
            return false;
        }
        
        return true;
    }

    @Override
    public void stop() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException ex) {
            }
        }
    }
    
    @Override
    public boolean addSample(SampleDataContainer sample) {
        
        if (fileWriter == null) {
            return false;
        }
        
        try {
            fileWriter.append(String.format("%012d", sample.getCollectedTimestamp())).append(COMMA_DELIMITER);            
            fileWriter.append(String.format("%02d", sample.getChannel())).append(COMMA_DELIMITER);            
            fileWriter.append(sample.getName()).append(COMMA_DELIMITER);
            fileWriter.append(sample.getSerial()).append(COMMA_DELIMITER);
            fileWriter.append(String.format("%012d", sample.getSampleVal())).append(COMMA_DELIMITER);
            fileWriter.append(String.format("%012d", sample.getTimeStamp())).append(COMMA_DELIMITER);
            fileWriter.append(String.format("%f", sample.getSampleEvaluatedVal())).append(COMMA_DELIMITER);
            fileWriter.append(String.format("%f", sample.getGpsTimestamp())).append(COMMA_DELIMITER);
            fileWriter.append(String.format("%f", sample.getLatitude())).append(COMMA_DELIMITER);
            fileWriter.append(String.format("%f", sample.getLongitude())).append(COMMA_DELIMITER);
            fileWriter.append(String.format("%f", sample.getAltitude())).append(NEW_LINE_SEPARATOR);
            
            fileWriter.flush();
        } catch (IOException e) {
            return false;
        } 
        
        return true;
    }

    @Override
    public boolean addSamples(List<SampleDataContainer> samples) throws PersisterException {
        boolean bResult = true;
        for (SampleDataContainer sample:samples) {
            bResult &= addSample(sample);
        }
        
        return bResult;
    }
    
    private String generateNewFileName() {
        
        File directory = new File(folderPath);
        
        File[] files = directory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.startsWith(DATANAME_PREFIX.toLowerCase()) && 
                        name.endsWith(DATANAME_EXTENSION.toLowerCase());
            }
        });
        
        // Retrieve the maximum number embedded in the filename
        int maxNum = 0;
        if (files != null) {
            for (File file:files) {
                if (file.isFile()) {
                    String name = file.getName().substring(DATANAME_PREFIX.length(), file.getName().length()-DATANAME_EXTENSION.length());
                    if ((name != null) && !name.isEmpty()) {
                        int num = Integer.valueOf(name);
                        if (num > maxNum) {
                            maxNum = num;
                        }
                    }
                }
            }
        }
        
        return folderPath + "/" + DATANAME_PREFIX + String.format("%06d", maxNum+1) + DATANAME_EXTENSION;
    }
}
