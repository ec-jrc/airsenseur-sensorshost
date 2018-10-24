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

package airsenseur.dev.chemsensorpanel.widgets;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JTextField;

/**
 *
 * @author marco
 */
public class JOverridableTextField extends JTextField {
    
    private String boardText = "";  // Board text is the text received by 
                                    // an external read from board message

    private boolean edited = false;
    private boolean hasFocus = false;
    
    private final Color editedColor = new Color(255,127,127);
    private final Color unEditedColor = new Color(255,255,255);

    public JOverridableTextField() {
        
        super.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                edited = true;
                updateBackground();
            }
        });
        
        super.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                hasFocus = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
                hasFocus = false;
            }
        });
        
        updateBackground();
    }
    
    public void setTextOverride(String t) {
        super.setText(t);
        
        edited = true;
        updateBackground();
    }

    @Override
    public void setText(String t) {
        
        String curText = super.getText().trim();
        if (t.trim().compareTo(curText) == 0) {
            edited = false;
        }
        
        if (!edited && !hasFocus) {
            super.setText(t);
        }
        
        boardText = t;
        updateBackground();
            
    }
    
    @Override
    public String getText() {
        if (!edited) {
            return boardText;
        }
        
        return super.getText();
    }
    
    private void updateBackground() {
        setBackground((edited)? editedColor : unEditedColor);
    }
    
}
