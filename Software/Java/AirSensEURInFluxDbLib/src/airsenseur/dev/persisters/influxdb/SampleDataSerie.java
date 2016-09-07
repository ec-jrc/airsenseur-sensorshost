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

import airsenseur.dev.persisters.SampleDataContainer;

/**
 * Implements a serie specific for sample data container
 * @author marco
 */
public class SampleDataSerie extends Serie {

    public SampleDataSerie(String name) {
        super(name);
        
        getColumns().add("time");
        getColumns().add("name");
        getColumns().add("channel");
        getColumns().add("sampleRawVal");
        getColumns().add("sampleEvaluatedVal");
        getColumns().add("boardTimeStamp");
        getColumns().add("gpsTimestamp");
        getColumns().add("latitude");
        getColumns().add("longitude");
        getColumns().add("altitude");
    }
    
    public void addSampleData(SampleDataContainer dataContainer) {
    
        Point point = new Point();
        getPoints().add(point);
        
        point.add(dataContainer.getCollectedTimestamp());
        point.add(dataContainer.getName());
        point.add(dataContainer.getChannel());
        point.add(dataContainer.getSampleVal());
        point.add(dataContainer.getSampleEvaluatedVal());
        point.add(dataContainer.getTimeStamp());
        point.add(dataContainer.getGpsTimestamp());
        point.add(dataContainer.getLatitude());
        point.add(dataContainer.getLongitude());
        point.add(dataContainer.getAltitude());
    }
}
