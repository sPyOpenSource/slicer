/*
 * XYZTabPanel.java
 *
 * Created on June 30, 2008, 9:15 PM
 */

package org.reprap.gui.botConsole;

import java.io.IOException;
import org.reprap.Printer;
import org.reprap.utilities.Debug;
/**
 *
 * @author  ensab
 */
public class XYZTabPanel extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;

    private double XYfastSpeed;
    private double ZfastSpeed;
    private boolean firstZero = true;
    private Printer printer;
    private static double nudgeSize = 0;
    private BotConsoleFrame parentBotConsoleFrame = null;
    private Thread agitateThread = null;
	
    private void setPrefs() throws IOException {	
    	XYfastSpeed = printer.getExtruder().getFastXYFeedrate();
    	ZfastSpeed = printer.getFastFeedrateZ();
        
        xySpeedField.setText(String.valueOf(XYfastSpeed));
        zSpeedField.setText(String.valueOf(ZfastSpeed));
    }
    
    /**
     * So the BotConsoleFrame can let us know who it is
     * @param b
     */
    public void setConsoleFrame(BotConsoleFrame b)
    {
    	parentBotConsoleFrame = b;
    	/*xStepperPositionJPanel.setConsoleFrame(b);
    	yStepperPositionJPanel.setConsoleFrame(b);
    	zStepperPositionJPanel.setConsoleFrame(b);*/
    }
    
    public void setMotorSpeeds() {
        
        /*xStepperPositionJPanel.setSpeed();
        yStepperPositionJPanel.setSpeed();
        zStepperPositionJPanel.setSpeed();*/
        
    }
    
    public void checkNudgeSize() {
        if (nudgeSize == 0) {
            nudgeSizeRB1.setSelected(true);
            nudgeSize = Double.parseDouble(nudgeSizeRB1.getText());
        }
    }
    
    public void setNudgeSize(Double size) {
        
        nudgeSize = size;
        
        /*xStepperPositionJPanel.setNudgeSize(nudgeSize);
        yStepperPositionJPanel.setNudgeSize(nudgeSize);
        zStepperPositionJPanel.setNudgeSize(nudgeSize);*/
    }
    
    /** Creates new form XYZTabPanel */
    public XYZTabPanel() {
    	firstZero = true;
        printer = org.reprap.Main.gui.getPrinter();
        initComponents();
        try
        {
            setPrefs();
            /*xStepperPositionJPanel.postSetUp(1);
            yStepperPositionJPanel.postSetUp(2);
            zStepperPositionJPanel.postSetUp(3);*/
        } catch (IOException ex) {Debug.e(ex.toString());}
        setMotorSpeeds();
        setNudgeSize(Double.parseDouble(nudgeSizeRB1.getText()));
        agitate = false;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        nudgePanel = new javax.swing.JPanel();
        nudgeSizeRB1 = new javax.swing.JRadioButton();
        nudgeSizeRB2 = new javax.swing.JRadioButton();
        nudgeSizeRB3 = new javax.swing.JRadioButton();
        motorsPanel = new javax.swing.JPanel();
        homeAllButton = new javax.swing.JButton();
        storeAllButton = new javax.swing.JButton();
        recallAllButton = new javax.swing.JButton();
        goButton = new javax.swing.JButton();
        speedsPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        xySpeedField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        zSpeedField = new javax.swing.JTextField();
        plotExtruderCheck = new javax.swing.JCheckBox();
        extruderToPlotWith = new javax.swing.JTextField();

        nudgePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Nudge size (mm)"));

        buttonGroup1.add(nudgeSizeRB1);
        nudgeSizeRB1.setSelected(true);
        nudgeSizeRB1.setText("0.1");
        nudgeSizeRB1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nudgeSizeRB1ActionPerformed(evt);
            }
        });

        buttonGroup1.add(nudgeSizeRB2);
        nudgeSizeRB2.setText("1.0");
        nudgeSizeRB2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nudgeSizeRB2ActionPerformed(evt);
            }
        });

        buttonGroup1.add(nudgeSizeRB3);
        nudgeSizeRB3.setText("10.0");
        nudgeSizeRB3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nudgeSizeRB3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout nudgePanelLayout = new javax.swing.GroupLayout(nudgePanel);
        nudgePanel.setLayout(nudgePanelLayout);
        nudgePanelLayout.setHorizontalGroup(
            nudgePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(nudgePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(nudgeSizeRB1)
                .addGap(18, 18, 18)
                .addComponent(nudgeSizeRB2)
                .addGap(18, 18, 18)
                .addComponent(nudgeSizeRB3)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        nudgePanelLayout.setVerticalGroup(
            nudgePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, nudgePanelLayout.createSequentialGroup()
                .addContainerGap(14, Short.MAX_VALUE)
                .addGroup(nudgePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nudgeSizeRB1)
                    .addComponent(nudgeSizeRB2)
                    .addComponent(nudgeSizeRB3)))
        );

        motorsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Axis positions"));

        homeAllButton.setText("Home all");
        homeAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homeAllButtonActionPerformed(evt);
            }
        });

        storeAllButton.setText("Sto all");
        storeAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                storeAllButtonActionPerformed(evt);
            }
        });

        recallAllButton.setText("Rcl all");
        recallAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recallAllButtonActionPerformed(evt);
            }
        });

        goButton.setText("Go");
        goButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout motorsPanelLayout = new javax.swing.GroupLayout(motorsPanel);
        motorsPanel.setLayout(motorsPanelLayout);
        motorsPanelLayout.setHorizontalGroup(
            motorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(motorsPanelLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(goButton)
                .addGap(121, 121, 121)
                .addComponent(homeAllButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(storeAllButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(recallAllButton)
                .addContainerGap(24, Short.MAX_VALUE))
        );
        motorsPanelLayout.setVerticalGroup(
            motorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, motorsPanelLayout.createSequentialGroup()
                .addGap(75, 75, 75)
                .addGroup(motorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(goButton)
                    .addComponent(recallAllButton)
                    .addComponent(storeAllButton)
                    .addComponent(homeAllButton))
                .addContainerGap())
        );

        speedsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Axis speeds (mm/min)"));

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel2.setText("X & Y");

        xySpeedField.setColumns(4);
        xySpeedField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        xySpeedField.setText("0000");

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel3.setText("Z");

        zSpeedField.setColumns(4);
        zSpeedField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        zSpeedField.setText("0000");

        javax.swing.GroupLayout speedsPanelLayout = new javax.swing.GroupLayout(speedsPanel);
        speedsPanel.setLayout(speedsPanelLayout);
        speedsPanelLayout.setHorizontalGroup(
            speedsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(speedsPanelLayout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(xySpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(zSpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        speedsPanelLayout.setVerticalGroup(
            speedsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(speedsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel2)
                .addComponent(jLabel3)
                .addComponent(zSpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(xySpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        plotExtruderCheck.setText("Plot using Extruder #");

        extruderToPlotWith.setColumns(1);
        extruderToPlotWith.setText("0");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(nudgePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(speedsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(plotExtruderCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(extruderToPlotWith, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(235, 235, 235))
                    .addComponent(motorsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(motorsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nudgePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(speedsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(extruderToPlotWith, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(plotExtruderCheck))))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


public void refreshTemperature()
{
	double t = 0;
	try {
		t = printer.getBedTemperature();
	} catch (Exception e) {
		parentBotConsoleFrame.handleException(e);
	}
	currentTempLabel.setText("" + t);
}

private void heatButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
    	parentBotConsoleFrame.suspendPolling();
    	if (heatPushed) 
    	{
    		try {
				printer.setBedTemperature(0);
			} catch (Exception e) {
				parentBotConsoleFrame.handleException(e);
			}
    		heatButton.setText("Switch bed heat on");
    		heatPushed = false;
    	} else 
    	{
    		try {
				printer.setBedTemperature(Double.parseDouble(targetTempField.getText()));
			} catch (NumberFormatException e) {
				parentBotConsoleFrame.handleException(e);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		heatButton.setText("Switch bed heat off");
    		heatPushed = true;
    	}
    	parentBotConsoleFrame.resumePolling();
    	
}                                          

private void agitate(double a, double p)
{
	
}

private void agitateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_heatButtonActionPerformed
	parentBotConsoleFrame.suspendPolling();
	if (agitate) 
	{
		agitate = false;
		agitateButton.setText("Agitate");
		parentBotConsoleFrame.resumePolling();
	} else 
	{
		agitate = true;
		agitateButton.setText("Stop Agitating");
		try {
			agitateThread = new Thread() 
			{
                                @Override
				public void run() 
				{
					double amp = 0.5*Double.parseDouble(agitateAmplitude.getText());
					double per = Double.parseDouble(agitatePeriod.getText());
					double y0 = printer.getY() + amp;
					double inc = 0.1/per;
					double t = 0.75*per;
					double y;
					Thread.currentThread().setName("Agitate");
					try {
						printer.moveTo(printer.getX(), printer.getY(), printer.getZ(), 1, false, false);
						while(agitate)
						{
							y = y0 + amp*Math.cos(2*Math.PI*t/per);
							double speed = Math.abs(-amp*2*Math.PI*Math.sin(2*Math.PI*t/per)*60/per);
							if(speed < 1)
								speed = 1;

							printer.moveTo(printer.getX(), y, printer.getZ(), speed, false, false);

							t += inc;
							if(t >= per)
								t = 0;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};

			agitateThread.start();
		} catch (NumberFormatException e) {
			parentBotConsoleFrame.handleException(e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}//GEN-LAST:event_heatButtonActionPerformed
    
    
private void nudgeSizeRB1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nudgeSizeRB1ActionPerformed
    setNudgeSize(Double.parseDouble(nudgeSizeRB1.getText()));
}//GEN-LAST:event_nudgeSizeRB1ActionPerformed

private void nudgeSizeRB2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nudgeSizeRB2ActionPerformed
    setNudgeSize(Double.parseDouble(nudgeSizeRB2.getText()));
}//GEN-LAST:event_nudgeSizeRB2ActionPerformed

private void nudgeSizeRB3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nudgeSizeRB3ActionPerformed
    setNudgeSize(Double.parseDouble(nudgeSizeRB3.getText()));
}//GEN-LAST:event_nudgeSizeRB3ActionPerformed

public void homeAll()
{
	double ze[] = new double[4];
	parentBotConsoleFrame.suspendPolling();
	try {
		printer.home();
		if(!firstZero)
			ze = printer.getZeroError();
	} catch (Exception e) {
		parentBotConsoleFrame.handleException(e);
	}
	if(!firstZero)
		Debug.d("Zero errors (steps).  X:" + ze[0] + " Y:" + ze[1] + " Z:" + ze[2]);
	/*xStepperPositionJPanel.zeroBox();
	yStepperPositionJPanel.zeroBox();
	zStepperPositionJPanel.zeroBox();*/
	firstZero = false;
    parentBotConsoleFrame.resumePolling();
}

public void homeXY()
{
	parentBotConsoleFrame.suspendPolling();
	try
	{
		printer.homeToZeroX();
		printer.homeToZeroY();
	} catch (Exception e)
	{}
	/*xStepperPositionJPanel.zeroBox();
	yStepperPositionJPanel.zeroBox();*/
	parentBotConsoleFrame.resumePolling();
}

private void homeAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homeAllButtonActionPerformed
	homeAll();
}//GEN-LAST:event_homeAllButtonActionPerformed

public void storeAll()
{
    /*xStepperPositionJPanel.store();
    yStepperPositionJPanel.store();
    zStepperPositionJPanel.store();*/	
}

private void storeAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_storeAllButtonActionPerformed
	storeAll();
}//GEN-LAST:event_storeAllButtonActionPerformed

public void recallAll()
{
    /*xStepperPositionJPanel.recall();
    yStepperPositionJPanel.recall();
    zStepperPositionJPanel.recall(); 	*/
}

private void recallAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recallAllButtonActionPerformed
	recallAll();
}//GEN-LAST:event_recallAllButtonActionPerformed

public Printer getPrinter()
{
	return printer;
}

public void recordCurrentPosition()
{
	double cp[];
	try {
		cp = printer.getCoordinates();
		/*xStepperPositionJPanel.setTargetPositionField(cp[0]);
		yStepperPositionJPanel.setTargetPositionField(cp[1]);
		zStepperPositionJPanel.setTargetPositionField(cp[2]);*/
	} catch (Exception e) {
		parentBotConsoleFrame.handleException(e);
	}
}

public void goTo(double xTo, double yTo, double zTo)
{
	parentBotConsoleFrame.suspendPolling();
	double x = printer.getX();
	double y = printer.getY();
	double z = printer.getZ();

	try
	{
		if(plotExtruderCheck.isSelected())
		{
			int eNum = Integer.parseInt(extruderToPlotWith.getText());
			GenericExtruderTabPanel etp = BotConsoleFrame.getGenericExtruderTabPanel(eNum);
			printer.selectExtruder(eNum, true, false, null);
			printer.getExtruder().setExtrusion(etp.getExtruderSpeed(), false);
			printer.machineWait(printer.getExtruder().getExtrusionDelayForLayer(), false, true);
		}
		if(z >= zTo)
		{
			printer.singleMove(xTo, yTo, z, Double.parseDouble(xySpeedField.getText()), true);
			printer.singleMove(xTo, yTo, zTo, Double.parseDouble(zSpeedField.getText()), true);
		} else
		{
			printer.singleMove(x, y, zTo, Double.parseDouble(zSpeedField.getText()), true);
			printer.singleMove(xTo, yTo, zTo, Double.parseDouble(xySpeedField.getText()), true);	
		}
		if(plotExtruderCheck.isSelected())
			printer.getExtruder().setExtrusion(0, false);
	} catch (Exception e)
	{}
	recordCurrentPosition();
	parentBotConsoleFrame.resumePolling();
}

private void goButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goButtonActionPerformed
	/*double xTo = xStepperPositionJPanel.getTargetPositionInMM();
	double yTo = yStepperPositionJPanel.getTargetPositionInMM();
	double zTo = zStepperPositionJPanel.getTargetPositionInMM();
	goTo(xTo, yTo, zTo);*/
}//GEN-LAST:event_goButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTextField extruderToPlotWith;
    private javax.swing.JButton goButton;
    private javax.swing.JButton homeAllButton;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel motorsPanel;
    private javax.swing.JPanel nudgePanel;
    private javax.swing.JRadioButton nudgeSizeRB1;
    private javax.swing.JRadioButton nudgeSizeRB2;
    private javax.swing.JRadioButton nudgeSizeRB3;
    private javax.swing.JCheckBox plotExtruderCheck;
    private javax.swing.JButton recallAllButton;
    private javax.swing.JPanel speedsPanel;
    private javax.swing.JButton storeAllButton;
    public static javax.swing.JTextField xySpeedField;
    public static javax.swing.JTextField zSpeedField;
    // End of variables declaration//GEN-END:variables
    
    private javax.swing.JLabel currentTempLabel;
    private javax.swing.JTextField targetTempField;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel bedTempPanel;
    private javax.swing.JToggleButton heatButton;
    private boolean heatPushed;
    
    private javax.swing.JTextField agitateAmplitude;
    private javax.swing.JTextField agitatePeriod;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel agitatePanel;
    private javax.swing.JToggleButton agitateButton;
	private boolean agitate;
}
