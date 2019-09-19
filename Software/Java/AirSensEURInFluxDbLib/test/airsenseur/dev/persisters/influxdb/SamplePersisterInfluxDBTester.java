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

package airsenseur.dev.persisters.influxdb;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.persisters.SampleDataContainer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author marco
 */
public class SamplePersisterInfluxDBTester {
    
    private static final String dataSetName = "AirSensEURTest";
    private static final String dbHost = "influxdbserver.liberaintentio.com";
    private static final int dbPort = 8086;
    private static final String dbName = "astestdb";
    private static final String dbUser = "asdebuguser";
    private static final String dbPassword = "verysecure";
    
    
    public static void main(String[] argc) {
        try {
            testSendWithLineProtocol();
        } catch (PersisterException ex) {
            Logger.getLogger(SamplePersisterInfluxDBTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
        
    public static void testSendWithLineProtocol() throws PersisterException {
        
        SamplePersisterInfluxDB dbTester = new SamplePersisterInfluxDB(dataSetName, dbHost, dbPort, dbName, dbUser, dbPassword, true, false, 200);
        dbTester.startNewLog();
        
        List<SampleDataContainer> samples = new ArrayList<>();
        for (int n = 0; n < 10; n++) {
            SampleDataContainer sample = new SampleDataContainer(1);
            sample.setName("SampleSensor");
            Date now = new Date();
            sample.setCollectedTimestamp(now.getTime());
            sample.updateSample((int)Math.random()*65535, Math.random(), (int)(now.getTime()/10000000));
            sample.updateGPSValues(now.getTime(), Math.random()*100, Math.random()*200, Math.random()*1000);
            
            samples.add(sample);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                dbTester.stop();
                return;
            }
        }
        
        dbTester.addSamples(samples);
        
        dbTester.stop();
    }
}
