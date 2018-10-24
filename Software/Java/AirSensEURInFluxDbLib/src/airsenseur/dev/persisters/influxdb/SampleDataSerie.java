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

/**
 * Implements a series specific for sample data container
 * @author marco
 */
public class SampleDataSerie extends Serie {
    
    // We use those offset to access to some specific tokens
    // when generating the line protocol for influxdb serialization.
    // Keep the offset synchronized with the columns contents when
    // changing this class implementation.
    private final static int TIME_TOKEN_OFFSET = 0;
    private final static int NAME_TOKEN_OFFSET = 1;
    private final static int CHANNEL_TOKEN_OFFSET = 2;
    
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
        getColumns().add("calibrated");
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
        point.add(dataContainer.getCalibratedVal());
    }

    @Override
    public String toLineProtocol() throws PersisterException {
        
        // We expect to have time, name and channel to these specified 
        // offsets in the column list. If not, raise an error
        if (!getColumns().get(TIME_TOKEN_OFFSET).equalsIgnoreCase("time")) {
            throw new PersisterException("Invalid time token found in sample data serie. Please alert the development team");
        }
        if (!getColumns().get(NAME_TOKEN_OFFSET).equalsIgnoreCase("name")) {
            throw new PersisterException("Invalid name token found in sample data serie. Please alert the development team");
        }
        if (!getColumns().get(CHANNEL_TOKEN_OFFSET).equalsIgnoreCase("channel")) {
            throw new PersisterException("Invalid channel token found in sample data serie. Please alert the development team");
        }

        StringBuilder sb = new StringBuilder();
        for (Point point:getPoints()) {
            
            // Get the time value
            Object timeVal = point.get(TIME_TOKEN_OFFSET);
            
            // Skip samples with invalid name. This should never happens but it's better to check
            String name = safeEscape((String)point.get(NAME_TOKEN_OFFSET));
            if (!name.isEmpty()) {
            
                // Measurement
                sb.append(getName()).append(",");

                // - Generate the tags string -
                // Tag: Name
                sb.append(getColumns().get(NAME_TOKEN_OFFSET)).append("=").append(safeEscape((String)point.get(NAME_TOKEN_OFFSET))).append(",");

                // Tag: channel
                sb.append(getColumns().get(CHANNEL_TOKEN_OFFSET)).append("=").append(point.get(CHANNEL_TOKEN_OFFSET)).append(" ");

                // Values:
                for (int n = 0; n < getColumns().size(); n++) {

                    // Skip known tokens
                    if ((n == TIME_TOKEN_OFFSET) || (n == NAME_TOKEN_OFFSET) || (n == CHANNEL_TOKEN_OFFSET)) {
                        continue;
                    }

                    sb.append(getColumns().get(n)).append("=").append(point.get(n));
                    if (n != getColumns().size()-1) {
                        sb.append(",");
                    }
                }

                // Timestamp
                sb.append(" ").append(timeVal).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private String safeEscape(String value) {
        value = value.replace(" ", "\\ ");
        
        return value;
    }
}
