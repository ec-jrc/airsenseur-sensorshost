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

import airsenseur.dev.exceptions.ConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The "Recipe" File is a text file where single lines are treated as a set of "commands"
 * This is useful, for example, to initialize an external LoRa device, with configuration defined instructions
 * 
 * @author marco
 */
public class FileRecipe {
    
    private static final String COMMENT_LINE_TOKEN = "#";
    
    private BufferedReader reader = null;
    private final List<String> recipe = new ArrayList<>();
    
    public boolean read(String inputFileName) throws ConfigurationException {
        
        recipe.clear();
        if (inputFileName.isEmpty()) {
            return true;
        }
        
        File configFile = new File(inputFileName);
        
        try {
            reader = new BufferedReader(new FileReader(configFile));
            
            String line;
            while ((line = reader.readLine()) != null) {
                
                line = line.trim();
                line = line.replace("\n", "");
                line = line.replace("\r", "");
                
                if (!line.isEmpty() && !line.startsWith(COMMENT_LINE_TOKEN)) {
                    recipe.add(line);
                }
            }            
            reader.close();
            
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException(ex.getMessage());
        } catch (IOException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
        
        return true;
    }
    
    public Iterator<String> getRecipe() {
        return recipe.iterator();
    }
}
