/*
 * StepperPositionJPanel.java
 *
 * Created on June 30, 2008, 4:59 PM
 */

package org.reprap.gui.botConsole;

import java.io.IOException;
import javax.swing.JOptionPane;
import org.reprap.Preferences;
import org.reprap.Printer;
import org.reprap.utilities.Debug;

/**
 *
 * @author  ensab
 */
public class StepperPositionJPanel extends javax.swing.JPanel {
	private static final long serialVersionUID = 1L;
    private int motorID;
    private String axis;
    private Printer printer;

    private double currentSpeed;
    @SuppressWarnings("unused")
    private double axisLength;
    private double nudgeSize;
    private double newTargetAfterNudge;
    private BotConsoleFrame parentBotConsoleFrame = null;
    /**
     * Really set it up under control...
     * @param m
     * @throws java.io.IOException
     */
    public void postSetUp(int m) throws IOException {
        
        motorID = m;
        printer = org.reprap.Main.gui.getPrinter();

        switch(motorID)
        {
        case 1:
                axis = "X";
                axisLength = Preferences.loadGlobalDouble("WorkingX(mm)"); // TODO: Replace with Prefs when Java3D parameters work for small wv's.
                break;
        case 2:
                axis = "Y";
                axisLength = Preferences.loadGlobalDouble("WorkingY(mm)"); // TODO: Replace with Prefs when Java3D parameters work for small wv's.
                break;
        case 3:
                axis = "Z";
                axisLength = Preferences.loadGlobalDouble("WorkingZ(mm)"); // TODO: Replace with Prefs when Java3D parameters work for small wv's.
                break;
        default:
                axis = "X";
                Debug.e("StepperPanel - dud axis id:" + motorID);
        }
        
        targetPosition.setEnabled(true);
        stepDownButton.setEnabled(true);
        stepUpButton.setEnabled(true);
        axisLabel.setText(axis);
        targetPosition.setText("0");
        storedPosition.setText("0");
	
//      TODO: Activate this code when the Java3D parameters allow a small enough working volume. Currently I get a black screen.

    }
    
    public void setConsoleFrame(BotConsoleFrame b)
    {
    	parentBotConsoleFrame = b;
    }
    
    public void zeroBox()
    {
    	targetPosition.setText("0");
    }
    
    public void homeAxis() {
    	parentBotConsoleFrame.suspendPolling();
        try {
            setSpeed();
			switch(motorID)
			{
			case 1:
				printer.homeToZeroX();
				break;
			case 2:
				printer.homeToZeroY();
				break;
			case 3:
				printer.homeToZeroZ();
				break;
			default:
				Debug.e("StepperPositionPanel - homeReset.  Dud motor id: " + motorID);
			}
            zeroBox();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not home motor: " + ex);
        }
        parentBotConsoleFrame.resumePolling();
    }
    
    public void setSpeed() {
        if (motorID == 3)
        {
        	currentSpeed = Double.parseDouble(org.reprap.gui.botConsole.XYZTabPanel.zSpeedField.getText());
        } else
        {
       		currentSpeed = Double.parseDouble(org.reprap.gui.botConsole.XYZTabPanel.xySpeedField.getText());
        }
    }
    
    public double getTargetPositionInMM() {
        double targetMM = Double.parseDouble(targetPosition.getText());
        if (targetMM > axisLength) {
            targetMM = axisLength;
            targetPosition.setText("" + round(targetMM, 2));
        }
        if (targetMM < 0) {
            targetMM = 0;
            targetPosition.setText("" + round(targetMM, 2));
        }
        return targetMM;
    }
    
    public void moveToTarget() {
    	parentBotConsoleFrame.suspendPolling();
    	    setSpeed();
    		double x, y, z, p;
    		x = printer.getX();
    		y = printer.getY();
    		z = printer.getZ();
    		p = getTargetPositionInMM();
    		try
    		{
    			switch(motorID)
    			{
    			case 1:
    				x = p;
    				break;
    			case 2:
    				y = p;
    				break;
    			case 3:
    				z = p;
    				break;
    			default:
    				Debug.e("moveToTarget()  Dud motor id: " + motorID);
    			}
    			printer.singleMove(x, y, z, currentSpeed, true);
    		} catch (Exception ex)
    		{}
    		parentBotConsoleFrame.resumePolling();
    }
    
    public double round(double Rval, int r2dp) {
        double p = (Double)Math.pow(10,r2dp);
        Rval = Rval * p;
        float tmp = Math.round(Rval);
        return (double)tmp/p;
    }
    
    public void setNudgeSize(double size) {
        nudgeSize = size;
    }
    
    public void setTargetPositionField(double coord) {
    	targetPosition.setText("" + coord);
    }

    
    /** Creates new form StepperPositionJPanel */
    public StepperPositionJPanel() {

      
        initComponents();
        

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        stepUpButton = new javax.swing.JButton();
        stepDownButton = new javax.swing.JButton();
        homeButton = new javax.swing.JButton();
        mmLabel1 = new javax.swing.JLabel();
        targetPosition = new javax.swing.JTextField();
        axisLabel = new javax.swing.JLabel();
        storeButton = new javax.swing.JButton();
        recallButton = new javax.swing.JButton();
        storedPosition = new javax.swing.JLabel();
        mmLabel2 = new javax.swing.JLabel();

        stepUpButton.setText(">");
        stepUpButton.setEnabled(false);
        stepUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stepUpButtonActionPerformed(evt);
            }
        });

        stepDownButton.setText("<");
        stepDownButton.setEnabled(false);
        stepDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stepDownButtonActionPerformed(evt);
            }
        });

        homeButton.setText("Home");
        homeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homeButtonActionPerformed(evt);
            }
        });

        mmLabel1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        mmLabel1.setText("mm");

        targetPosition.setColumns(4);
        targetPosition.setFont(targetPosition.getFont().deriveFont((float)12));
        targetPosition.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        targetPosition.setText("0");
        targetPosition.setEnabled(false);

        axisLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        axisLabel.setText("X");

        storeButton.setText("Sto");
        storeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                storeButtonActionPerformed(evt);
            }
        });

        recallButton.setText("Rcl");
        recallButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recallButtonActionPerformed(evt);
            }
        });

        storedPosition.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        storedPosition.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        storedPosition.setText("(?)");
        storedPosition.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        storedPosition.setMaximumSize(new java.awt.Dimension(50, 15));
        storedPosition.setPreferredSize(new java.awt.Dimension(50, 15));

        mmLabel2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        mmLabel2.setText("mm");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(axisLabel)
                .addGap(4, 4, 4)
                .addComponent(targetPosition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(mmLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stepDownButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stepUpButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(homeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(storeButton, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(recallButton)
                .addGap(3, 3, 3)
                .addComponent(storedPosition, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mmLabel2)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(axisLabel)
                    .addComponent(targetPosition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mmLabel1)
                    .addComponent(stepDownButton)
                    .addComponent(stepUpButton)
                    .addComponent(homeButton)
                    .addComponent(storeButton)
                    .addComponent(recallButton)
                    .addComponent(storedPosition, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mmLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

private void stepUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stepUpButtonActionPerformed
        setSpeed();
        newTargetAfterNudge = getTargetPositionInMM() + nudgeSize;
        targetPosition.setText("" + round(newTargetAfterNudge, 2));
        moveToTarget();
}//GEN-LAST:event_stepUpButtonActionPerformed

private void stepDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stepDownButtonActionPerformed
        setSpeed();
        double newTargetAfterNudge = getTargetPositionInMM() - nudgeSize;
        targetPosition.setText("" + round(newTargetAfterNudge, 2));
        moveToTarget();
}//GEN-LAST:event_stepDownButtonActionPerformed

private void homeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homeButtonActionPerformed
    homeAxis();
}//GEN-LAST:event_homeButtonActionPerformed

public void store()
{
    storedPosition.setText(targetPosition.getText());    
}

private void storeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_storeButtonActionPerformed
    store();
}//GEN-LAST:event_storeButtonActionPerformed

public void recall()
{
    targetPosition.setText(storedPosition.getText());
}

private void recallButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recallButtonActionPerformed
    recall();
    
}//GEN-LAST:event_recallButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel axisLabel;
    private javax.swing.JButton homeButton;
    private javax.swing.JLabel mmLabel1;
    private javax.swing.JLabel mmLabel2;
    private javax.swing.JButton recallButton;
    private javax.swing.JButton stepDownButton;
    private javax.swing.JButton stepUpButton;
    private javax.swing.JButton storeButton;
    private javax.swing.JLabel storedPosition;
    private javax.swing.JTextField targetPosition;
    // End of variables declaration//GEN-END:variables

}
