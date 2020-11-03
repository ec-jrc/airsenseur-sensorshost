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

package airsenseur.dev.persisters.sosdb;

import airsenseur.dev.persisters.sosdb.dao.UnknownContainerSOSDB;
import airsenseur.dev.persisters.sosdb.dao.ObservationSOSDB;
import airsenseur.dev.persisters.sosdb.dao.ResultSOSDB;
import airsenseur.dev.persisters.sosdb.requests.InsertObservationSOSDB;
import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.persisters.SampleDataContainer;
import airsenseur.dev.persisters.SamplesPersister;
import airsenseur.dev.persisters.sosdb.dao.GeometryContainerSOSDB;
import airsenseur.dev.persisters.sosdb.dao.NamedValueSOSDB;
import airsenseur.dev.persisters.sosdb.dao.ParameterTypeSOSDB;
import airsenseur.dev.persisters.sosdb.dao.StaticTokenSOSDB;
import airsenseur.dev.persisters.sosdb.requests.RequestBaseSOSDB;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Implements a sample persister through SOS DB
 * @author marco
 */
public class SamplePersisterSOSDB extends SOSDBBaseService implements SamplesPersister {
    
    private final Logger log = LoggerFactory.getLogger(SamplePersisterSOSDB.class);
    private final Date dateNow = new Date();
    
    private final List<String> sensorsUom;
    private final boolean identifyObservation;
    private final boolean updateFoiLocation;
    
    public SamplePersisterSOSDB(ConfigurationSOSDB configuration) throws PersisterException {
        super(configuration);
                
        sensorsUom = configuration.getSensorsUom();
        
        identifyObservation = configuration.insertId();
        
        updateFoiLocation = configuration.updateFOILocation();
        
        if ((sensorsProcedure.size() != sensorsObservedProp.size()) ||
            (sensorsObservedProp.size() != sensorsUom.size()) ||
            (sensorsProcedure.size() != sensorsUom.size())) {
            throw new PersisterException("Invalid SODB sensors configuration");
        }
        
        // Initialize the JSON object mapper (ISO8601TimeZone)
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        mapper.setDateFormat(df);
    }
    
    @Override
    public boolean startNewLog() throws PersisterException {
        
        // Nothing to do. We suppose to start a new connection each time is required
        log.info("SamplePersisterSOSDB enabled");
        return true;
    }
    
    @Override
    public void stop() {
        log.info("SamplePersisterSOSDB stopped");
    }

    @Override
    public boolean addSample(SampleDataContainer sample) throws PersisterException {
        
        List<SampleDataContainer> samples = new ArrayList<>();
        samples.add(sample);
        
        return addSamples(samples);
    }

    @Override
    public boolean addSamples(List<SampleDataContainer> samples) throws PersisterException {
        
        InsertObservationSOSDB insertObservationSOSDB = toInsertObservation(samples);
        return sendDataToSOSDb(insertObservationSOSDB);
    }
    
    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getPersisterMarker(int channel) {
        return "" + channel;
    }
    
    /**
     * Generate the insertObservation SOS command
     * @param samples: Sample list coming from the aggregation process
     * @return 
     */
    private InsertObservationSOSDB toInsertObservation(List<SampleDataContainer> samples) throws PersisterException {
        
        InsertObservationSOSDB insertObservation = new InsertObservationSOSDB();
        
        insertObservation.setOffering(offeringName);
        
        for (SampleDataContainer sample:samples) {
            
            int sensorNum = sample.getChannel();
            if (sensorNum > sensorsProcedure.size()) {
                throw new PersisterException("Invalid sample Id found on the sample list");
            }
            
            ObservationSOSDB observation = new ObservationSOSDB();
            observation.setObservedProperty(sensorsObservedProp.get(sensorNum));
            observation.setProcedure(sensorsProcedure.get(sensorNum));
            
            observation.setPhenomenonTime(new Date(sample.getCollectedTimestamp()));
            if (sample.getGpsTimestamp()!= 0.0d) {
                observation.setResultTime(new Date((long)(sample.getGpsTimestamp()*1000)));
            } else {
                observation.setResultTime(observation.getPhenomenonTime());
            }
            
            if (identifyObservation == true) {
                UnknownContainerSOSDB identifier = new UnknownContainerSOSDB();
                identifier.setValue(getUniqueIdentifier(sample.getChannel(), sample.getTimeStamp()));
                observation.setIdentifier(identifier);
            }
            
            observation.setFeatureOfInterest(foiId);
            
            // Add GPS info if available needed
            if ((sample.getLatitude() != 0.0) || (sample.getLongitude() != 0.0)) {
                
                ParameterTypeSOSDB parameter = new ParameterTypeSOSDB();
                observation.setParameter(parameter);
                
                NamedValueSOSDB namedValue = new NamedValueSOSDB();
                parameter.setNamedValue(namedValue);

                GeometryContainerSOSDB geometry = new GeometryContainerSOSDB();
                namedValue.setName(StaticTokenSOSDB.IDENTIFIER_CODESPACE_SAMPLINGGEOMETRY);
                namedValue.setValue(geometry);

                geometry.setAsPoint();
                geometry.getCoordinates().add(sample.getLatitude());
                geometry.getCoordinates().add(sample.getLongitude());
            }
            
            ResultSOSDB result = new ResultSOSDB();
            observation.setResult(result);            
            result.setUom(sensorsUom.get(sensorNum));
            result.setValue(sample.getSampleEvaluatedVal());

            insertObservation.getObservation().add(observation);
        }
        
        return insertObservation;
    }
    
    /**
     * Send data as a JSON to the remote SOS server
     * @param observation
     * @return 
     */
    private boolean sendDataToSOSDb(InsertObservationSOSDB observation) throws PersisterException {

        RequestBaseSOSDB result = (RequestBaseSOSDB)sendToServer(observation, RequestBaseSOSDB.class);
        if (result != null) {
            return true;
        }
        
        // 400 is associated to Bad Message and 
        // We're traiting this as a "good" result so all the
        // samples will be skipped. This is a somewhat annoying 
        // situation we will change in the future.
        return (lastStatusCode == 400);
    }
    
    private String getUniqueIdentifier(int channelNum, long sampleId) {
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm");
        String uniqueBase = dateFormat.format(dateNow);
        String randomString = getSaltString(6);
        String unique = String.format("%s-%s-%02d-%010d", uniqueBase, randomString, channelNum, sampleId);
        
        return unique;
    }
    
    
    private String getSaltString(int length) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder salt = new StringBuilder(length);
        Random rnd = new Random();
        while (salt.length() < length) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }    

}
