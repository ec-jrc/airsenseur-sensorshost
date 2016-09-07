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

package airsenseur.dev.chemsensorpanel.dataModel;

import javax.swing.DefaultComboBoxModel;

/**
 *
 * @author marco
 * @param <E>: A generic item
 */
public class RegisterDataModel<E> extends DefaultComboBoxModel<E> {

    public static class DataItem {
        
        protected String label;
        protected int value;
        
        DataItem(String label, int value) {
            this.label = label;
            this.value = value;
        }
    
        @Override
        public String toString() {
            if (label != null) {
                return label;
            }

            return "-";
        }
    }
    
    private int base = 1;
    private int mask = Integer.MAX_VALUE;
    
    public RegisterDataModel(final E items[]){
        super(items);
    }
    
    protected void setBase(int bit, int size) {
        base = 1<<bit;
        
        mask = 0;
        for (int n = bit; n < (bit+size); n++) {
            mask |= (1<<n);
        }
    }
    
    public void setSelectedItem(int value) {
        
        // Loop on each data in the datasource and select
        // the first that matches the masked and shifted value
        value = (value & mask) / base;
        
        for(int n = 0; n < getSize(); n++) {
            if (((DataItem)getElementAt(n)).value == value) {
                super.setSelectedItem(getElementAt(n));
            }
        }
    }
    

    public int getRegisterSelectedValue() {
        
        Object sel = getSelectedItem();
        if (sel == null) {
            return 0;
        }
        
        if (sel instanceof DataItem) {
            
            DataItem selection = (DataItem)sel;

            return selection.value * base;
        }
        
        return 0;
    }
}
