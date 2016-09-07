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

package airsenseur.dev.chemsensorpanel.sensorsdb;

import airsenseur.dev.chemsensorpanel.exceptions.PresetException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple sensor database persister through local file
 * @author marco
 */
public class SensorDbPersisterByFile implements SensorDbPersister {
    
    private final static String PRESETNAME_SEPARATOR = "::_::";
    
    String fileName;
    final Map<String,PresetDao> presets = new HashMap<>();

    @Override
    public void connect(String target) throws PresetException {
        fileName = target;
    }

    @Override
    public void disconnect() throws PresetException {
    }

    @Override
    public boolean isKnownPreset(PresetDao preset) throws PresetException {
        
        loadPresets();
        return presets.containsKey(preset.getPresetName());
    }

    @Override
    public List<PresetDao> getPresets() throws PresetException {
        
        loadPresets();
        
        List<PresetDao> result = new ArrayList<>(presets.values());
        Collections.sort(result, new Comparator<PresetDao>() {

            @Override
            public int compare(PresetDao o1, PresetDao o2) {
                return o1.getPresetName().compareTo(o2.getPresetName());
            }
        });
        return result;
    }

    @Override
    public void savePreset(PresetDao preset) throws PresetException {
        
        loadPresets();
        presets.put(preset.getPresetName(), preset);
        
        flushPresetsToFile();
    }        

    @Override
    public void deletePreset(PresetDao preset) throws PresetException {
        
        loadPresets();
        presets.remove(preset.getPresetName());
        
        flushPresetsToFile();
    }
    
    
    private void loadPresets() throws PresetException {
        
        presets.clear();
        
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException ex) {
            return;
        }
        
        try {
            PresetDao preset;
            do {
                preset = loadPreset(reader.readLine());
                if (preset != null) {
                    presets.put(preset.getPresetName(), preset);
                }
            } while (preset != null);
            
        } catch (IOException ex) {
            throw new PresetException(ex.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                throw new PresetException(ex.getMessage());
            }
        }
    }
    
    private PresetDao loadPreset(String line) throws PresetException {
        
        if ((line == null) || (line.isEmpty())) {
            return null;
        }
        
        int tokenPos = line.indexOf(PRESETNAME_SEPARATOR);
        if (tokenPos < 1) {
            throw new PresetException("Invalid sensor database file");
        }
        if (line.length() < (tokenPos + PRESETNAME_SEPARATOR.length())) {
            throw new PresetException("Invalid sensor database file");
        }
        
        String presetName = line.substring(0, tokenPos);
        PresetDao preset = new PresetDao();
        preset.setPresetName(presetName);
        
        loadValues(preset, line.substring(tokenPos+PRESETNAME_SEPARATOR.length(), line.length()));
        
        return preset;
    }
    
    
    private void loadValues(PresetDao preset, String line) throws PresetException {
        
        // Split token values
        String[] tokens = line.split("\\[");
        if (tokens.length < 1) {
            throw new PresetException("Error reading preset " + preset.getPresetName());
        }
        
        for (String token : tokens) {
            if (!token.isEmpty()) {
                loadValue(preset, token.substring(0, token.length()-1));
            }
        }
    }
    
    private void loadValue(PresetDao preset, String token) throws PresetException {
        
        String[] keyVal = token.split(":");
        if (keyVal.length != 2) {
            throw new PresetException("Error reading preset " + preset.getPresetName());
        }
        
        preset.getValues().add(new PresetValue(keyVal[0], keyVal[1]));
    }
    
    private void flushPresetsToFile() throws PresetException {
        BufferedWriter writer = null;
        try {
            
            writer = new BufferedWriter(new FileWriter(fileName));
            for (PresetDao presetDao : presets.values()) {
                
                writer.write(persistPreset(presetDao).toString());
                writer.newLine();
            }   
            
            writer.flush();
            
        } catch (IOException ex) {
            throw new PresetException("Error writing preset database file");
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                throw new PresetException("Error writing preset database file");
            }
        }
    }    
    
    private StringBuilder persistPreset(PresetDao preset) {
        
        StringBuilder sb = new StringBuilder(preset.getPresetName());
        sb.append(PRESETNAME_SEPARATOR);
        sb.append(persistValues(preset.getValues()));
        
        return sb;
    }
    
    private StringBuilder persistValues(PresetValues values) {
        
        StringBuilder sb = new StringBuilder();
        
        for (PresetValue value : values.values()) {
            sb.append(persistValue(value));
        }
        
        return sb;
    }
    
    private StringBuilder persistValue(PresetValue value) {
        
        return new StringBuilder("[").append(value.getContainerId()).append(":").append(value.getValue()).append("]");
    }
    
}
