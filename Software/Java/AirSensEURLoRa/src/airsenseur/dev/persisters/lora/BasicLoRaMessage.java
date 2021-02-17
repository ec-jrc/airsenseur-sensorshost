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

package airsenseur.dev.persisters.lora;

import airsenseur.dev.helpers.CodecHelper;

/**
 *  Implements a basic LoRa data packet message with dynamic size
 * @author marco
 */
public class BasicLoRaMessage {

    public BasicLoRaMessage(int fPort, boolean confirmed) {
        this.fPort = fPort;
        this.confirmed = confirmed;
        buffer = new StringBuilder();
        dirty = false;
    }
    
    public void clear() {
        buffer = new StringBuilder();
        dirty = false;
    }
    
    public void append(char value) {
        this.buffer.append(CodecHelper.encodeValue(value));
        this.dirty = true;
    }
    
    public void append(short value) {
        this.buffer.append(CodecHelper.encodeValue(value));
        this.dirty = true;
    }
    
    public void append(int value) {
        this.buffer.append(CodecHelper.encodeValue(value));
        this.dirty = true;
    }
    
    public void append(long value) {
        this.buffer.append(CodecHelper.encodeValue(value));
        this.dirty = true;
    }
    
    public void append(float value) {
        this.buffer.append(CodecHelper.encodeValue(value));
        this.dirty = true;
    }
    
    public void append(String value) {
        this.buffer.append(CodecHelper.encodeString(value));
        this.dirty = true;
    }
    
    public int extimateEncodedStringSize(String value) {
        
        // Two digit at char plus 2 bytes for the termination
        return (value.length()+1)*2;
    }

    public StringBuilder buffer;
    public boolean confirmed;
    public int fPort;    
    public boolean dirty;
    
    // The message is encoded in ASCII format, where a single physical byte is 
    // encoded through 2 chars/nibbles.
    public int size() { return (buffer.length() / 2); }
}

