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

package airsenseur.dev.dataaggregator;

import airsenseur.dev.persisters.SampleDataContainer;

/**
 * A container for channel configuration, value and channel associated meta-data
 * @author marco
 */
public class ChannelDataContainer {
    
    private final static int POLL_PERIOD_OVERSAMPLING = 3;    
    
    private final SampleDataContainer value;
    
    private final long samplingPreriod;
    private final long pollPeriod;
    private final boolean enabled;    
    private long numOfZeroTsFound;
    private long lastPollTs;

    public ChannelDataContainer(SampleDataContainer value, long samplingPreriod, boolean enabled) {
        this.value = value;
        this.samplingPreriod = samplingPreriod;
        this.enabled = enabled;
        this.pollPeriod = samplingPreriod/POLL_PERIOD_OVERSAMPLING;
        this.numOfZeroTsFound = 0;        
        this.lastPollTs = 0;
    }

    /**
     * @return the value
     */
    public SampleDataContainer getValue() {
        return value;
    }

    /**
     * @return the pollingPeriod
     */
    public long getPollPeriod() {
        return pollPeriod;
    }

    /**
     * @return the lastPollingTs
     */
    public long getLastPollTimestamp() {
        return lastPollTs;
    }

    /**
     * @param lastPollTs the lastPollingTs to set
     */
    public void setLastPollTimestamp(long lastPollTs) {
        this.lastPollTs = lastPollTs;
    }

    /**
     * @return the samplingPreriod
     */
    public long getSamplingPreriod() {
        return samplingPreriod;
    }

    /**
     * 
     * @return true if the polling resulted in a zero-timestamped samples for too long time
     */
    public boolean checkIfTooZeroTsFound() {
        numOfZeroTsFound++;
        
        if (numOfZeroTsFound > 100) {
            numOfZeroTsFound = 0;
            return true;
        }
        
        return false;
    }
    
    /**
     * @return if the channel is enabled or not
     */
    public boolean isEnabled() {
        return enabled;
    }
}
