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

package airsenseur.dev.helpers;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author marco
 */
public class CodecHelper {
    
    public final static void main(String[] args) {
        
        long startTime = System.currentTimeMillis();

        String dataIn = "00AB8932004500";
        for (long i = 1; i < 100000000; i++) {
            String result = decodeStringAt(dataIn, 3);
        }

        long endTime = System.currentTimeMillis();

        System.out.println("That took " + (endTime - startTime) + " milliseconds");        
    }
    
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    
    public static char[] encodeValue(char value) {
        char[] result = { hexArray[value >>> 4], hexArray[value & 0x0F] };
        return result;
    }
    
    public static char[] encodeValue(short value) {
        char[] result = { hexArray[(value >>> 12) & 0x0F], hexArray[(value >>> 8) & 0x0F], 
                            hexArray[(value >>> 4) & 0x0F], hexArray[value & 0x0F] }; 
        return result;
    }
    
    public static String encodeString(String value) {
        
        StringBuilder sb = new StringBuilder((value.length()+1)*2);
        
        for (int n = 0; n < value.length(); n++) {
            sb.append(encodeValue(value.charAt(n)));
        }
        
        sb.append(encodeValue((char)0x00));
        
        return sb.toString();
    }    
    
    public static Integer decodeChar(StringBuilder buffer) {
        
        try {
            return Integer.valueOf(buffer.toString(), 16);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }
    
    public static Integer decodeCharAt(String buffer, int start) {
        
        char[] data = { 0, 0 };
        
        buffer.getChars(start, start+2, data, 0);
        return decodeChars(data);
    }
    
    public static Integer decodeShortAt(String buffer, int start) {
        
        char[] data = { 0, 0, 0, 0 };
        
        buffer.getChars(start, start+4, data, 0);
        return decodeChars(data);
    }
    
    public static Float decodeFloatAt(String buffer, int start) {
        
        try {
            char[] inBuffer = new char[8];
            
            buffer.getChars(start, start+8, inBuffer, 0);
            
            byte[] bytes = {
                    decodeChars(inBuffer, 0, 2).byteValue(),
                    decodeChars(inBuffer, 2, 2).byteValue(),
                    decodeChars(inBuffer, 4, 2).byteValue(),
                    decodeChars(inBuffer, 6, 2).byteValue(),
            };

            float f = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
            
            return f;
            
        } catch (NullPointerException | StringIndexOutOfBoundsException | BufferUnderflowException e ) {
            return null;
        }
    }
    
    public static Integer decodeIntAt(String buffer, int start) {
        
        char[] inBuffer = new char[8];

        buffer.getChars(start, start+8, inBuffer, 0);
        return decodeChars(inBuffer);
    }
    
    public static String decodeStringAt(String buffer, int start) {
        
        Integer rxChar;
        char[] inBuffer = new char[buffer.length()];
        buffer.getChars(start, inBuffer.length, inBuffer, start);
        StringBuilder sb = new StringBuilder(buffer.length()/2);
        do {
            rxChar = decodeChars(inBuffer, start, 2);
            if (rxChar != null) {
                sb.append((char)rxChar.byteValue());
            }
            start = start+2;
        } while ((start < buffer.length()) && (rxChar != null) && (rxChar != '\0'));
        
        return sb.toString();
    }        
    
    public static Integer decodeChars(char[] data) {
        int result = 0;
        
        for (int i = 0; i < data.length; i++) {
            result <<= 4;
            if ((data[i] >= 'A') && (data[i] <='F')) {
                result += ((data[i] - 'A') + 10);
            } else if ((data[i] >= '0') && (data[i] <='9')) {
                result += (data[i] - '0');
            } else if ((data[i] >= 'a') && (data[i] <='f')) {
                result += ((data[i] - 'a') + 10);
            } else {
                return null;
            }
        }
        return result;
    }    

    public static Integer decodeChars(char[] data, int start, int length) {
        int result = 0;
        
        for (int i = start; i < start+length; i++) {
            result <<= 4;
            if ((data[i] >= 'A') && (data[i] <='F')) {
                result += ((data[i] - 'A') + 10);
            } else if ((data[i] >= 'a') && (data[i] <='f')) {
                result += ((data[i] - 'a') + 10);
            } else if ((data[i] >= '0') && (data[i] <='9')) {
                result += (data[i] - '0');
            } else {
                return null;
            }
        }
        return result;        
    }    
}
