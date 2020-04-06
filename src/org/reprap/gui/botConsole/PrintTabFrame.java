/*
 * PrintTabFrame.java
 *
 * Created on June 30, 2008, 1:45 PM
 */

package org.reprap.gui.botConsole;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.reprap.Extruder;
import org.reprap.Main;
import org.reprap.Printer;
import org.reprap.pcb.PCB;

/**
 *
 * @author  ensab
 */
public class PrintTabFrame extends javax.swing.JInternalFrame {//AB99
    private static final long serialVersionUID = 1L;
    private BotConsoleFrame parentBotConsoleFrame = null;
    private final Printer printer;
    private final boolean paused = false;
    private final boolean seenSNAP = false;
    private boolean seenGCode = false;
    private long startTime = -1;
    private int oldLayer = -1;
    private String loadedFiles = "";
    private boolean loadedFilesLong = false;
    private boolean stlLoaded = false;
    private boolean gcodeLoaded = false;
    private boolean slicing = false;
    private final boolean sdCard = false;
    private Thread printerFilePlay;
    
    /** Creates new form PrintTabFrame
     * @param pref */
    public PrintTabFrame(boolean pref) {
        initComponents();
    	String machine = "simulator";
    	
    	machine = org.reprap.Preferences.RepRapMachine();

    	seenGCode = true;
    	printerFilePlay = null;

    	printer = org.reprap.Main.gui.getPrinter();
    	enableSLoad();
    }
    
    /**
     * Keep the user amused.If fractionDone is negative, the function
 queries the layer statistics.If it is 0 or positive, the function uses
 it.
     * @param fractionDone
     * @param layer
     * @param layers
     */
    public void updateProgress(double fractionDone, int layer, int layers)
    {
    	if(layer >= 0){
    		currentLayerOutOfN.setText("" + layer + "/" + layers);
        }
    	
    	if(layers < 0)
    	{
    		layers = org.reprap.Main.gui.getLayers();
    	}
    	
    	if(layer < 0)
    	{
    		layer = org.reprap.Main.gui.getLayer();
    		if(layer >= 0){
        		currentLayerOutOfN.setText("" + layer + "/" + layers);
                }
    	}
    	
    	if(fractionDone < 0)
    	{
    		// Only bother if the layer has just changed

    		if(layer == oldLayer){
    			return;
                }
    		
    		boolean topDown = layer < oldLayer;

    		oldLayer = layer;

    		if(topDown){
    			fractionDone = (double)(layers - layer)/(double)layers;
                } else {
    			fractionDone = (double)layer/(double)layers;
                }
    	}
 
    	progressBar.setMinimum(0);
    	progressBar.setMaximum(100);
    	progressBar.setValue((int)(100*fractionDone));
    	
    	GregorianCalendar cal = new GregorianCalendar();
    	SimpleDateFormat dateFormat = new SimpleDateFormat("EE HH:mm");
    	Date d = cal.getTime();
    	long e = d.getTime();
    	if(startTime < 0)
    	{
    		startTime = e;
    		return;
    	}
    	
    	long f = (long)((double)(e - startTime)/fractionDone);
    	int h = (int)(f/60000)/60;
    	int m = (int)(f/60000)%60;
    	
    	if(m > 9){
    		expectedBuildTime.setText("" + h + ":" + m);
        } else {
    		expectedBuildTime.setText("" + h + ":0" + m);
        }
    	expectedFinishTime.setText(dateFormat.format(new Date(startTime + f)));
    	
    	if(printerFilePlay != null)
    	{
    		if(!printerFilePlay.isAlive()){
    			printDone();
                }
    	}
    }
    
    /**
     * So the BotConsoleFrame can let us know who it is
     * @param b
     */
    public void setConsoleFrame(BotConsoleFrame b)
    {
    	parentBotConsoleFrame = b;
    }

void pauseButtonActionPerformed(java.awt.event.ActionEvent evt){
    
}
void stopButtonActionPerformed(java.awt.event.ActionEvent evt){
    
}
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        printButton = new java.awt.Button();
        pauseButton = new java.awt.Button();
        stopButton = new java.awt.Button();
        exitButton = new java.awt.Button();
        layerPauseCheck = new javax.swing.JCheckBox();
        toSNAPRepRapRadioButton = new javax.swing.JRadioButton();
        expectedBuildTimeLabel = new javax.swing.JLabel();
        hoursMinutesLabel1 = new javax.swing.JLabel();
        expectedBuildTime = new javax.swing.JLabel();
        expectedFinishTimeLabel = new javax.swing.JLabel();
        expectedFinishTime = new javax.swing.JLabel();
        progressLabel = new javax.swing.JLabel();
        currentLayerOutOfN = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        loadSTL = new java.awt.Button();
        loadGCode = new java.awt.Button();
        gCodeToFileRadioButton = new javax.swing.JRadioButton();
        loadRFO = new java.awt.Button();
        toGCodeRepRapRadioButton = new javax.swing.JRadioButton();
        fileNameBox = new javax.swing.JLabel();
        preferencesButton = new java.awt.Button();
        saveRFO = new java.awt.Button();
        displayPathsCheck = new javax.swing.JCheckBox();

        printButton.setBackground(new java.awt.Color(51, 204, 0));
        printButton.setFont(printButton.getFont());
        printButton.setLabel("Print"); // NOI18N
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });

        pauseButton.setBackground(new java.awt.Color(255, 204, 0));
        pauseButton.setLabel("Pause"); // NOI18N
        pauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseButtonActionPerformed(evt);
            }
        });

        stopButton.setBackground(new java.awt.Color(255, 0, 0));
        stopButton.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        stopButton.setLabel("STOP !"); // NOI18N
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        exitButton.setLabel("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        layerPauseCheck.setText("Pause at end of layer"); // NOI18N
        layerPauseCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                layerPauseCheckActionPerformed(evt);
            }
        });

        buttonGroup1.add(toSNAPRepRapRadioButton);
        toSNAPRepRapRadioButton.setText("Print on SNAP RepRap");
        toSNAPRepRapRadioButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                selectorRadioButtonMousePressed(evt);
            }
        });

        expectedBuildTimeLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        expectedBuildTimeLabel.setText("Expected build time:"); // NOI18N

        hoursMinutesLabel1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        hoursMinutesLabel1.setText("(h:m)"); // NOI18N

        expectedBuildTime.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        expectedBuildTime.setText("00:00"); // NOI18N

        expectedFinishTimeLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        expectedFinishTimeLabel.setText("Expected to finsh at:"); // NOI18N

        expectedFinishTime.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        expectedFinishTime.setText("    -"); // NOI18N

        progressLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        progressLabel.setText("Layer progress:"); // NOI18N

        currentLayerOutOfN.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        currentLayerOutOfN.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        currentLayerOutOfN.setText("000/000"); // NOI18N

        loadSTL.setActionCommand("loadSTL");
        loadSTL.setBackground(new java.awt.Color(0, 204, 255));
        loadSTL.setLabel("Load STL"); // NOI18N
        loadSTL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadSTL(evt);
            }
        });

        loadGCode.setActionCommand("loadGCode");
        loadGCode.setBackground(new java.awt.Color(0, 204, 255));
        loadGCode.setLabel("Load GCode"); // NOI18N
        loadGCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoadGCode(evt);
            }
        });

        buttonGroup1.add(gCodeToFileRadioButton);
        gCodeToFileRadioButton.setText("Send GCodes to file");
        gCodeToFileRadioButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                selectorRadioButtonMousePressed(evt);
            }
        });

        loadRFO.setActionCommand("loadRFO");
        loadRFO.setBackground(new java.awt.Color(153, 153, 153));
        loadRFO.setLabel("Load RFO"); // NOI18N
        loadRFO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadRFO(evt);
            }
        });

        buttonGroup1.add(toGCodeRepRapRadioButton);
        toGCodeRepRapRadioButton.setText("Print on G-Code RepRap");
        toGCodeRepRapRadioButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                selectorRadioButtonMousePressed(evt);
            }
        });

        fileNameBox.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        fileNameBox.setText(" - ");

        preferencesButton.setActionCommand("preferences");
        preferencesButton.setBackground(new java.awt.Color(255, 102, 255));
        preferencesButton.setLabel("Preferences"); // NOI18N
        preferencesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                preferences(evt);
            }
        });

        saveRFO.setActionCommand("saveRFO");
        saveRFO.setBackground(new java.awt.Color(153, 153, 153));
        saveRFO.setLabel("Save RFO"); // NOI18N
        saveRFO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveRFO(evt);
            }
        });

        displayPathsCheck.setText("Display paths");
        displayPathsCheck.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                displayPathsCheckMouseClicked(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(saveRFO, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(loadGCode, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(loadRFO, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(loadSTL, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(toSNAPRepRapRadioButton)
                            .add(toGCodeRepRapRadioButton)
                            .add(gCodeToFileRadioButton)
                            .add(layerPauseCheck)
                            .add(displayPathsCheck))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, preferencesButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(layout.createSequentialGroup()
                                .add(printButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 72, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(pauseButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(stopButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(exitButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 62, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                    .add(layout.createSequentialGroup()
                        .add(expectedFinishTimeLabel)
                        .add(7, 7, 7)
                        .add(expectedFinishTime))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(expectedBuildTimeLabel)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(expectedBuildTime))
                            .add(layout.createSequentialGroup()
                                .add(progressLabel)
                                .add(7, 7, 7)
                                .add(currentLayerOutOfN)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(layout.createSequentialGroup()
                                .add(hoursMinutesLabel1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(fileNameBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 430, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(29, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(loadSTL, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(loadGCode, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createSequentialGroup()
                        .add(toSNAPRepRapRadioButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(toGCodeRepRapRadioButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(gCodeToFileRadioButton))
                    .add(preferencesButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layerPauseCheck)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(displayPathsCheck))
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(pauseButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 39, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(printButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 39, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(stopButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 39, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(exitButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 39, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(layout.createSequentialGroup()
                            .add(loadRFO, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(saveRFO, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(expectedBuildTimeLabel)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(expectedBuildTime)
                        .add(hoursMinutesLabel1)
                        .add(fileNameBox)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(expectedFinishTimeLabel)
                    .add(expectedFinishTime))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                        .add(progressLabel)
                        .add(currentLayerOutOfN))
                    .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(26, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
public void printLive(boolean p)
{
	slicing = true;
	/*if(p)
		sliceButton.setText("Printing...");
	else
		sliceButton.setText("Slicing...");
	sliceButton.setBackground(Color.gray);    */
}

private void restoreSliceButton()
{
	slicing = false;
	//sliceButton.setText("Slice");
	//sliceButton.setBackground(new java.awt.Color(51, 204, 0)); 
	printerFilePlay = null;
}

public void printDone()
{
	restoreSliceButton();
	String[] options = { "Exit" };
		JOptionPane.showOptionDialog(null, "The file has been processed.", "Message",
			JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
			null, options, options[0]);
	org.reprap.Main.gui.dispose();
}

private boolean worthSaving()
{
	return false;
}

private void printButtonActionPerformed(java.awt.event.ActionEvent evt) 
{//GEN-FIRST:event_printButtonActionPerformed
	if(slicing){
		return;
        }
	
	if(worthSaving())
	{
		int toDo = JOptionPane.showConfirmDialog(null, "First save the build as an RFO file?");
		switch(toDo)
		{
		case JOptionPane.YES_OPTION:
			saveRFO(null);
			break;
			
		case JOptionPane.NO_OPTION:
			break;
			
		case JOptionPane.CANCEL_OPTION:
			return;
		
		default:
			saveRFO(null);
		}
	}
	
	printLive(false);
	
	parentBotConsoleFrame.suspendPolling();
    parentBotConsoleFrame.setFractionDone(-1, -1, -1);
    org.reprap.Main.gui.mouseToWorld();
    
    	int sp = -1;
    	if(loadedFiles != null){
    		sp = loadedFiles.length();
        }
    	if(sp <= 0)
    	{
    		JOptionPane.showMessageDialog(null, "There are no STLs/RFOs loaded to slice to file.");
    		restoreSliceButton();
    		return;
    	}
    	sp = Math.max(loadedFiles.indexOf(".stl"), Math.max(loadedFiles.indexOf(".STL"), Math.max(loadedFiles.indexOf(".rfo"), loadedFiles.indexOf(".RFO"))));
    	if(sp <= 0)
       	{
    		JOptionPane.showMessageDialog(null, "The loaded file is not an STL or an RFO file.");
    	}   		
    	printer.setTopDown(true);	
    	if(printer.setGCodeFileForOutput(loadedFiles.substring(0, sp)) == null)
    	{
    		restoreSliceButton();
    		return;
    	}
    	org.reprap.Main.gui.onProduceB();
}//GEN-LAST:event_printButtonActionPerformed

private void pcbButtonActionPerformed(java.awt.event.ActionEvent evt)
{
	if(!SLoadOK){
		return;
        }
	Extruder pcbp = printer.getExtruder("PCB-pen");
	if(pcbp == null)
	{
		JOptionPane.showMessageDialog(null, "You have no PCB-pen among your extruders; see http://reprap.org/wiki/Plotting#Using_the_RepRap_Host_Software.");
		return;
	}	
	parentBotConsoleFrame.suspendPolling();
	File inputGerber = org.reprap.Main.gui.onOpen("PCB Gerber file", new String[] {"top", "bot"}, "");
	if(inputGerber == null)
	{
		JOptionPane.showMessageDialog(null, "No Gerber file was loaded.");
		return;
	}

	int sp = inputGerber.getAbsolutePath().toLowerCase().indexOf(".top");
	String drill;
	if(sp < 0)
	{
		sp = inputGerber.getAbsolutePath().toLowerCase().indexOf(".bot");
		drill = ".bdr";
	} else {
		drill = ".tdr";
	}
	String fileRoot = "";
	if(sp > 0){
		fileRoot = inputGerber.getAbsolutePath().substring(0, sp);
        }
	drill = fileRoot+drill;
	File inputDrill = new File(drill);
	if(inputDrill == null)
	{
		JOptionPane.showMessageDialog(null, "Drill file " + drill + " not found; drill centres will not be marked");
	}
	File outputGCode = org.reprap.Main.gui.onOpen("G-Code file for PCB printing", new String[] {"gcode"}, fileRoot);
	if(outputGCode == null)
	{
		JOptionPane.showMessageDialog(null, "No G-Code file was chosen.");
		return;
	}
	PCB p = new PCB(inputGerber, inputDrill, outputGCode, pcbp);
                     p.writeGCodes();
	parentBotConsoleFrame.resumePolling();
}

//private void pauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseButtonActionPerformed
//}//GEN-LAST:event_pauseButtonActionPerformed

//private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
//}//GEN-LAST:event_stopButtonActionPerformed

private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) 
{//GEN-FIRST:event_exitButtonActionPerformed
	if(worthSaving())
	{
		int toDo = JOptionPane.showConfirmDialog(null, "First save the build as an RFO file?");
		switch(toDo)
		{
		case JOptionPane.YES_OPTION:
			saveRFO(null);
			break;
			
		case JOptionPane.NO_OPTION:
			break;
			
		case JOptionPane.CANCEL_OPTION:
			return;
		
		default:
			saveRFO(null);
		}
	}
	Main.ftd.killThem();
	printer.dispose();
	System.exit(0);
}//GEN-LAST:event_exitButtonActionPerformed

private void layerPause(boolean p)
{
	org.reprap.Main.gui.setLayerPause(p);	
}

private void layerPauseCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_layerPauseCheckActionPerformed
org.reprap.Main.gui.setLayerPause(layerPauseCheck.isSelected());
}//GEN-LAST:event_layerPauseCheckActionPerformed

private void selectorRadioButtonMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectorRadioButtonMousePressed
	
	@SuppressWarnings("unused")
	String machine = "simulator";
	boolean closeMessage = false;

	machine = org.reprap.Preferences.RepRapMachine();

	try {
		org.reprap.Preferences.saveGlobal();
	} catch (IOException e) {
		Logger.getLogger(PrintTabFrame.class.getName()).log(Level.SEVERE, null, e);
	}
	printer.refreshPreferences();
	if(!closeMessage){
		return;
        }
	JOptionPane.showMessageDialog(null, "As you have changed the type of RepRap machine you are using,\nyou will have to exit this program and run it again.");

}//GEN-LAST:event_selectorRadioButtonMousePressed


private void loadSTL(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadSTL
	if(!SLoadOK){
		return;
        }
	if(gcodeLoaded)
	{
		int response = JOptionPane.showOptionDialog(
                null                       // Center in window.
                , "This will abandon the G Code file you loaded."        // Message
                , "Load STL"               // Title in titlebar
                , JOptionPane.YES_NO_OPTION  // Option type
                , JOptionPane.PLAIN_MESSAGE  // messageType
                , null                       // Icon (none)
                , new String[] {"OK", "Cancel"}                    // Button text as above.
                , ""    // Default button's label
              );
		if(response == 1){
			return;
                }
		loadedFiles = "";
	}
	String fn = printer.addSTLFileForMaking();
	if(fn.length() <= 0)
	{
		JOptionPane.showMessageDialog(null, "No STL was loaded.");
		return;
	}
	
	if(loadedFilesLong){
		return;
        }
	if(loadedFiles.length() > 50)
	{
		loadedFiles += "...";
		loadedFilesLong = true;
	} else {
		loadedFiles += fn + " ";
                     }
	fileNameBox.setText(loadedFiles);
	stlLoaded = true;
	gcodeLoaded = false;
}//GEN-LAST:event_loadSTL

private void LoadGCode(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadGCode
	if(!GLoadOK){
		return;
        }
	if(seenSNAP)
	{
		JOptionPane.showMessageDialog(null, "Sorry.  Sending G Codes to SNAP RepRap machines is not implemented.");
		return;
	}

	if(!org.reprap.Preferences.GCodeUseSerial())
	{
		JOptionPane.showMessageDialog(null, "There is no point in sending a G Code file to a G Code file.");
		return;
	}

	if(stlLoaded)
	{
		int response = JOptionPane.showOptionDialog(
                null                       // Center in window.
                , "This will abandon the STL/RFO file(s) you loaded."        // Message
                , "Load GCode"               // Title in titlebar
                , JOptionPane.YES_NO_OPTION  // Option type
                , JOptionPane.PLAIN_MESSAGE  // messageType
                , null                       // Icon (none)
                , new String[] {"OK", "Cancel"}                    // Button text as above.
                , ""    // Default button's label
              );
		if(response == 1){
			return;
                }
		org.reprap.Main.gui.deleteAllSTLs();
		loadedFiles = "";
	}
	if(gcodeLoaded)
	{
		int response = JOptionPane.showOptionDialog(
                null                       // Center in window.
                , "This will abandon the previous G Code file you loaded."        // Message
                , "Load GCode"               // Title in titlebar
                , JOptionPane.YES_NO_OPTION  // Option type
                , JOptionPane.PLAIN_MESSAGE  // messageType
                , null                       // Icon (none)
                , new String[] {"OK", "Cancel"}                    // Button text as above.
                , ""    // Default button's label
              );
		if(response == 1){
			return;
                }
		loadedFiles = "";
	}
	
	if(sdCard)
	{
		parentBotConsoleFrame.suspendPolling();
		String[] files = printer.getSDFiles();
		if(files.length > 0)
		{	
			loadedFiles = (String)JOptionPane.showInputDialog(
					this,
					"Select the SD file to print:",
					"Customized Dialog",
					JOptionPane.PLAIN_MESSAGE,
					null,
					files,
					files[0]);

			if(loadedFiles != null){
				if (loadedFiles.length() <= 0) {
					loadedFiles = null;
                                }
                        }
		} else {
			JOptionPane.showMessageDialog(null, "There are no SD files available.");
			loadedFiles = null;
		}
		parentBotConsoleFrame.resumePolling();
	} else {
		loadedFiles = printer.loadGCodeFileForMaking();
        }
	if(loadedFiles == null)
	{
		JOptionPane.showMessageDialog(null, "No GCode was loaded.");
		return;
	}
	
	fileNameBox.setText(loadedFiles);
	gcodeLoaded = true;
	stlLoaded = false;
}//GEN-LAST:event_LoadGCode

private void loadRFO(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadRFO
		if(!SLoadOK){
			return;
                }
		if(gcodeLoaded)
		{
			int response = JOptionPane.showOptionDialog(
					null                       // Center in window.
					, "This will abandon the previous GCode file you loaded."        // Message
					, "Load RFO"               // Title in titlebar
					, JOptionPane.YES_NO_OPTION  // Option type
					, JOptionPane.PLAIN_MESSAGE  // messageType
					, null                       // Icon (none)
					, new String[] {"OK", "Cancel"}                    // Button text as above.
					, ""    // Default button's label
			);
			if(response == 1){
				return;
                        }
			loadedFiles = "";
		}

		String fn = printer.loadRFOFileForMaking();
		if(fn.length() <= 0)
		{
			JOptionPane.showMessageDialog(null, "No .rfo file was loaded.");
			return;
		}

		if(loadedFilesLong){
			return;
                }
		if(loadedFiles.length() > 50)
		{
			loadedFiles += "...";
			loadedFilesLong = true;
		} else {
			loadedFiles += fn + " ";
                }
		fileNameBox.setText(loadedFiles);
		stlLoaded = true;
		gcodeLoaded = false;
}//GEN-LAST:event_loadRFO

private void preferences(java.awt.event.ActionEvent evt) {                             
	org.reprap.gui.RepRapPreferences prefs = new org.reprap.gui.RepRapPreferences();
	prefs.setVisible(true);	// prefs.show();
}                            

private void changeMachine(java.awt.event.ActionEvent evt) {                             
		
}                            

private void help(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_preferences
	try {
        URI url = new URI("http://reprap.org/wiki/RepRapPro_Slicer");
        Desktop.getDesktop().browse(url);//***AB
     } catch(IOException | URISyntaxException e) {
         Logger.getLogger(PrintTabFrame.class.getName()).log(Level.SEVERE, null, e);
     }
}//GEN-LAST:event_preferences

private void saveRFO(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveRFO
	if(!SLoadOK){
		return;
        }
	if(loadedFiles.contentEquals(""))
	{
		JOptionPane.showMessageDialog(null, "There's nothing to save...");
		return;
	}
	int sp = Math.max(loadedFiles.indexOf(".stl"), Math.max(loadedFiles.indexOf(".STL"), Math.max(loadedFiles.indexOf(".rfo"), loadedFiles.indexOf(".RFO"))));
	if(sp <= 0)
   	{
		JOptionPane.showMessageDialog(null, "The loaded file is not an STL or an RFO file.");
	} 
	printer.saveRFOFile(loadedFiles.substring(0, sp));
}//GEN-LAST:event_saveRFO

private void saveSCAD(java.awt.event.ActionEvent evt) {                          
	if(!SLoadOK){
		return;
        }
	if(loadedFiles.contentEquals(""))
	{
		JOptionPane.showMessageDialog(null, "There's nothing to save...");
		return;
	}
	int sp = Math.max(loadedFiles.indexOf(".stl"), Math.max(loadedFiles.indexOf(".STL"), Math.max(loadedFiles.indexOf(".rfo"), loadedFiles.indexOf(".RFO"))));
	if(sp <= 0)
   	{
		JOptionPane.showMessageDialog(null, "The loaded file is not an STL or an RFO file.");
	} 
	org.reprap.Main.gui.saveSCAD(loadedFiles.substring(0, sp));
}

private void displayPaths(boolean disp)
{
		org.reprap.Preferences.setSimulate(disp);
}

private void displayPathsCheckMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_displayPathsCheckMouseClicked
	displayPaths(displayPathsCheck.isSelected());
}//GEN-LAST:event_displayPathsCheckMouseClicked


private void enableSLoad()
{
	SLoadOK = true;
	GLoadOK = false;
	loadSTL.setBackground(new java.awt.Color(0, 204, 255));
	loadRFO.setBackground(new java.awt.Color(0, 204, 255));
	saveRFO.setBackground(new java.awt.Color(0, 204, 255));
	try
	{	
		org.reprap.Preferences.setRepRapMachine("GCodeRepRap");
		org.reprap.Preferences.setGCodeUseSerial(false);
	} catch (Exception e) {
		JOptionPane.showMessageDialog(null, e.toString());
	}	
}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel currentLayerOutOfN;
    private javax.swing.JCheckBox displayPathsCheck;
    private java.awt.Button exitButton;
    private javax.swing.JLabel expectedBuildTime;
    private javax.swing.JLabel expectedBuildTimeLabel;
    private javax.swing.JLabel expectedFinishTime;
    private javax.swing.JLabel expectedFinishTimeLabel;
    private javax.swing.JLabel fileNameBox;
    private javax.swing.JRadioButton gCodeToFileRadioButton;
    private javax.swing.JLabel hoursMinutesLabel1;
    private javax.swing.JCheckBox layerPauseCheck;
    private java.awt.Button loadGCode;
    private java.awt.Button loadRFO;
    private java.awt.Button loadSTL;
    private java.awt.Button pauseButton;
    private java.awt.Button preferencesButton;
    private java.awt.Button printButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressLabel;
    private java.awt.Button saveRFO;
    private java.awt.Button stopButton;
    private javax.swing.JRadioButton toGCodeRepRapRadioButton;
    private javax.swing.JRadioButton toSNAPRepRapRadioButton;
    // End of variables declaration//GEN-END:variables
    private boolean SLoadOK = false;
    private boolean GLoadOK = false;

}
