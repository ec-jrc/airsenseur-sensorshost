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
import java.util.List;

/**
 * SampleLoader interface. It aims to retrieve sample data from persistence tech
 * @author marco
 */
public interface SampleLoader {
    
    public static final int CHANNEL_INVALID = -1;
    
    boolean openLog() throws PersisterException;
    void stop();
    boolean supportChannels();
    long getMinimumTimestamp(int channel) throws PersisterException;
    long getMaximumTimestamp(int channel) throws PersisterException;
    List<SampleDataContainer> loadSamples(int channel, long firstTimeStamp, long lastTimeStamp) throws PersisterException;
}
