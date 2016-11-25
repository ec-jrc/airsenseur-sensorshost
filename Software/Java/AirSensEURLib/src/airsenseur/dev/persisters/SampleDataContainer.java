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

/**
 * Sample data container
 * @author marcos
 */
public class SampleDataContainer {
    
    private String name;
    private int channel;
    private long collectedTimestamp;
    
    private int sampleRawVal;
    private int sensorTimeStamp;
    private double sampleEvaluatedVal;
    private double calibratedVal;
    
    private double gpsTimestamp;
    private double latitude;
    private double longitude;
    private double altitude;
    
    public SampleDataContainer() {
        this.name = "";
        this.channel = 0;
        this.collectedTimestamp = 0;
    }

    public SampleDataContainer(int channel) {
        this.channel = channel;
    }
    
    public void reset() {
        this.sampleRawVal = 0;
        this.sampleEvaluatedVal = 0.0;
        this.sensorTimeStamp = 0;
        this.setCollectedTimestamp(0);
        updateGPSValues(0.0, 0.0, 0.0, 0.0);
    }
    
    public SampleDataContainer clone() {
        SampleDataContainer other = new SampleDataContainer(channel);
        other.name = name;
        other.sampleRawVal = sampleRawVal;
        other.sampleEvaluatedVal = sampleEvaluatedVal;
        other.sensorTimeStamp = sensorTimeStamp;
        other.calibratedVal = calibratedVal;
        other.setCollectedTimestamp(getCollectedTimestamp());
        other.updateGPSValues(gpsTimestamp, latitude, longitude, altitude);
        return other;
    }
    
    /**
     * Update the sample value only if boardTimeStamp is different
     * to what's already present in the object
     * @param sampleRawVal
     * @param timeStamp
     * @return: true if the timestamp differs from the original value
     */
    public boolean updateSample(int sampleRawVal, int timeStamp) {
        return updateSample(sampleRawVal, 0.0, timeStamp);
    }
    
    /**
     * Update the sample value only if boardTimeStamp is different
     * to what's already present in the object
     * @param sampleRawVal
     * @param sampleEvaluatedVal
     * @param timeStamp
     * @return: true if the timestamp differs from the original value
     */
    public boolean updateSample(int sampleRawVal, double sampleEvaluatedVal, int timeStamp) {
        
        if (this.getTimeStamp() != timeStamp) {
            
            this.sampleRawVal = sampleRawVal;
            this.sampleEvaluatedVal = sampleEvaluatedVal;
            this.sensorTimeStamp = timeStamp;
            
            return true;
        }
        
        return false;
    }        
    
    public boolean updateGPSValues(double gpsTimestamp, double latitude, double longitude, double altitude) {
        
        // double values can be NaN when no 3D Fix is valid
        if (!Double.isNaN(gpsTimestamp)) {
            this.gpsTimestamp = gpsTimestamp;
        } 
        if (!Double.isNaN(latitude)) {
            this.latitude = latitude;
        }
        if (!Double.isNaN(longitude)) {
            this.longitude = longitude;
        }
        if (!Double.isNaN(altitude)) {
            this.altitude = altitude;
        }
        
        return true;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @return the channel
     */
    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    /**
     * @return the sampleRawVal
     */
    public int getSampleVal() {
        return sampleRawVal;
    }
    
    /**
     * @return the collectedTimestamp
     */
    public long getCollectedTimestamp() {
        return collectedTimestamp;
    }

    /**
     * @param collectedTimestamp the collectedTimestamp to set
     */
    public void setCollectedTimestamp(long collectedTimestamp) {
        this.collectedTimestamp = collectedTimestamp;
    }

    /**
     * @return the boardTimeStamp
     */
    public int getTimeStamp() {
        return sensorTimeStamp;
    }

    /**
     * @return the gpsTimestamp
     */
    public double getGpsTimestamp() {
        return gpsTimestamp;
    }

    /**
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @return the altitude
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * @return the sampleEvaluatedVal
     */
    public double getSampleEvaluatedVal() {
        return sampleEvaluatedVal;
    }

    /**
     * Calibrated value is a placeholder for data that will be processed
     * in the future by a calibration algorithm.
     * @return the calibratedVal
     */
    public double getCalibratedVal() {
        return calibratedVal;
    }

    /**
     * Calibrated value is a placeholder for data that will be processed
     * in the future by a calibration algorithm. Populate with a dummy value.
     * @param calibratedVal the calibratedVal to set
     */
    public void setCalibratedVal(double calibratedVal) {
        this.calibratedVal = calibratedVal;
    }
}
