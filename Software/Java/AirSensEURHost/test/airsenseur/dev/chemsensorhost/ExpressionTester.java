/* ========================================================================
 * Copyright 2016 EUROPEAN UNION
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by 
 * the European Commission - subsequent versions of the EUPL (the "Licence"); 
 * You may not use this work except in compliance with the Licence. 
 * You may obtain a copy of the Licence at: http://ec.europa.eu/idabc/eupl
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the 
 * specific language governing permissions and limitations under the Licence.
 * Date: 01/02/2016
 * Authors
 * - Marco Signorini  - marco.signorini@liberaintentio.com
 * - Michel Gerboles  - michel.gerboles@jrc.ec.europa.eu,  
 *                  			European Commission - Joint Research Centre, 
 * - Laurent Spinelle - laurent.spinelle@jrc.ec.europa.eu,
 *                  			European Commission - Joint Research Centre, 
 * 
 * ======================================================================== 
 */

package airsenseur.dev.chemsensorhost;

import expr.Expr;
import expr.Parser;
import expr.SyntaxException;
import expr.Variable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Math Expression checker
 * @author marco
 */
public class ExpressionTester {
    
    public static void main(String[] argc) {
        
        // Check for a simple math expression
        String mathExpr = "((x/16384)*165) - 40.0";
        
        Expr expr;
        try { 
            expr = Parser.parse(mathExpr);
        } catch (SyntaxException ex) {
            Logger.getLogger(ExpressionTester.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        Variable x = Variable.make("x");
        x.setValue(5827);
        double result = expr.value();
        
        System.out.println(result);
        
        // Check for two complement expression
        mathExpr = "if(x>32767,x-32768,x+32768)";

        try { 
            expr = Parser.parse(mathExpr);
        } catch (SyntaxException ex) {
            Logger.getLogger(ExpressionTester.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        for (int i = 0; i < 65536; i++) {
            
            x.setValue(i);
            result = expr.value();
            
            if (result != convertTwoComplements(i)) {
                System.out.println("Error evaluating twocomplements of " + i);
            }
        }
        
    }
    
    public static double convertTwoComplements(double sample) {

        // Unsigned. conversion
        if (sample > 32767) {
            sample = sample - 32768;
        } else {
            sample = sample + 32768;
        }

        return sample;
    }
}
