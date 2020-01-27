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

package airsenseur.dev.datapush.datacontainers;
import java.util.ArrayList;
import java.util.List;

/**
 * A generic data container for information to be read from local persistence an stored on the remote server
 * @author marcos
 * @param <T>
 */
public class DataPushDataContainer<T> {

    private final List<T> dataset;

    public DataPushDataContainer() {
        this.dataset = new ArrayList<>();
    }

    public DataPushDataContainer(List<T> dataset) {
        this.dataset = dataset;
    }

    public boolean isEmpty() {
        return dataset.isEmpty();
    }

    public long size() {
        return dataset.size();
    }

    public List<T> getDataSet() {
        return dataset;
    }
}