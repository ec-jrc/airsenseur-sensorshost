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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author marcos
 */
public class LineGraphPanel extends javax.swing.JPanel {
    
   private int maxScoreVal = 20;
   private boolean autoScoreOn = false;
   private int MAX_HISTORY_LENGTH = 10;
   private final boolean showScores = true;
   private String title = "";
   private String subTitle = "";
   
   private final int BORDER_GAP = 15;
   private final Color GRAPH_COLOR = Color.green;
   private final Color GRAPH_POINT_COLOR = new Color(150, 50, 50, 180);
   private final Stroke GRAPH_STROKE = new BasicStroke(1f);
   private final Font TEXT_FONT = new Font("SansSerif", Font.PLAIN, 8);
   private final int GRAPH_POINT_WIDTH = 5;
   private final int GRAPH_SCORE_DISTANCE =  3;
   private final int Y_HATCH_CNT = 10;
   private final List<Integer> scores = new ArrayList<>();

    /**
     * Creates new form LineGraph
     */
    public LineGraphPanel() {
        initComponents();
    }
    
    public void setMaxYValue(int maxScore) {
        maxScoreVal = maxScore;
        if (maxScoreVal == -1) {
            autoScoreOn = true;
        }
    }
    
    public void setMaxHistory(int maxHistory) {
        MAX_HISTORY_LENGTH = maxHistory;
    }
    
    public void addScore(Integer score) {
        scores.add(score);
        
        if (scores.size() > MAX_HISTORY_LENGTH) {
            scores.remove(0);
        }

        if (autoScoreOn) {
            getMaxScoreVal();
        }
        this.repaint();
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }
    
    private void getMaxScoreVal() {
        
        int localMaxScoreVal = 1;
        for (Integer val:scores) {
            if (val > localMaxScoreVal) {
                localMaxScoreVal = val;
            }
        }
        
        if (localMaxScoreVal >= maxScoreVal) {
            maxScoreVal = localMaxScoreVal;
        } else {
            maxScoreVal -= (maxScoreVal - localMaxScoreVal) / 3;
        }
    }
    
    
   @Override
   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D)g;
      g2.setFont(TEXT_FONT);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      double xScale = ((double) getWidth() - 2 * BORDER_GAP) / (scores.size() - 1);
      double yScale = ((double) getHeight() - 2 * BORDER_GAP) / (maxScoreVal - 1);

      List<Point> graphPoints = new ArrayList<>();
      for (int i = 0; i < scores.size(); i++) {
         int x1 = (int) (i * xScale + BORDER_GAP);
         int y1 = (int) ((maxScoreVal - scores.get(i)) * yScale + BORDER_GAP);
         graphPoints.add(new Point(x1, y1));
      }
      
      // The title
      g2.drawString(title, (getWidth() - 2 * BORDER_GAP)/2, (BORDER_GAP));
      
      // The subtitle
      g2.drawString(subTitle, (getWidth() - 2 * BORDER_GAP)/2, (2*BORDER_GAP));

      // create x and y axes 
      g2.drawLine(BORDER_GAP, getHeight() - BORDER_GAP, BORDER_GAP, BORDER_GAP);
      g2.drawLine(BORDER_GAP, getHeight() - BORDER_GAP, getWidth() - BORDER_GAP, getHeight() - BORDER_GAP);

      // create hatch marks for y axis. 
      for (int i = 0; i < Y_HATCH_CNT; i++) {
         int x0 = BORDER_GAP;
         int x1 = GRAPH_POINT_WIDTH + BORDER_GAP;
         int y0 = getHeight() - (((i + 1) * (getHeight() - BORDER_GAP * 2)) / Y_HATCH_CNT + BORDER_GAP);
         int y1 = y0;
         g2.drawLine(x0, y0, x1, y1);
      }

      // and for x axis
      for (int i = 0; i < scores.size() - 1; i++) {
         int x0 = (i + 1) * (getWidth() - BORDER_GAP * 2) / (scores.size() - 1) + BORDER_GAP;
         int x1 = x0;
         int y0 = getHeight() - BORDER_GAP;
         int y1 = y0 - GRAPH_POINT_WIDTH;
         g2.drawLine(x0, y0, x1, y1);
      }

      Stroke oldStroke = g2.getStroke();
      g2.setColor(GRAPH_COLOR);
      g2.setStroke(GRAPH_STROKE);
      for (int i = 0; i < graphPoints.size() - 1; i++) {
         int x1 = graphPoints.get(i).x;
         int y1 = graphPoints.get(i).y;
         int x2 = graphPoints.get(i + 1).x;
         int y2 = graphPoints.get(i + 1).y;
         g2.drawLine(x1, y1, x2, y2);         
      }

      g2.setStroke(oldStroke);      
      g2.setColor(GRAPH_POINT_COLOR);
      for (int i = 0; i < graphPoints.size(); i++) {
         int x = graphPoints.get(i).x - GRAPH_POINT_WIDTH / 2;
         int y = graphPoints.get(i).y - GRAPH_POINT_WIDTH / 2;
         int ovalW = GRAPH_POINT_WIDTH;
         int ovalH = GRAPH_POINT_WIDTH;
         g2.fillOval(x, y, ovalW, ovalH);
         
         if (showScores) {
            int scoreVal = scores.get(i);
            g2.drawString("" + scoreVal, x - GRAPH_SCORE_DISTANCE, y - GRAPH_SCORE_DISTANCE);
         }
      }
   }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 332, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 234, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
