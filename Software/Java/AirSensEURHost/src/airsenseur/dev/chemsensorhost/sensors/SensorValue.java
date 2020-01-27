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

package airsenseur.dev.chemsensorhost.sensors;

/**
 *
 * @author marco
 */
public class SensorValue {
    
    private int value;
    private int timeStamp;
    private double evalSampleVal;
    
    private long pollPeriod;
    private long lastPollTimestamp;
    
    public boolean load(int value, int timestamp) {
        if (this.getTimeStamp() != timestamp) {
            this.timeStamp = timestamp;
            this.value = value;
            this.setEvalSampleVal(value);
            return true;
        }

        return false;
    }

    /**
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * @return the timeStamp
     */
    public int getTimeStamp() {
        return timeStamp;
    }

    /**
     * @return the evalSampleVal
     */
    public double getEvalSampleVal() {
        return evalSampleVal;
    }

    /**
     * @return the lastPollTimestamp
     */
    public long getLastPollTimestamp() {
        return lastPollTimestamp;
    }

    /**
     * @param lastPollTimestamp the lastPollTimestamp to set
     */
    public void setLastPollTimestamp(long lastPollTimestamp) {
        this.lastPollTimestamp = lastPollTimestamp;
    }

    /**
     * @param evalSampleVal the evalSampleVal to set
     */
    public void setEvalSampleVal(double evalSampleVal) {
        this.evalSampleVal = evalSampleVal;
    }

    /**
     * @return the pollPeriod
     */
    public long getPollTime() {
        return pollPeriod;
    }

    /**
     * @param pollPeriod the pollPeriod to set
     */
    public void setPollPeriod(long pollPeriod) {
        this.pollPeriod = pollPeriod;
    }
}
