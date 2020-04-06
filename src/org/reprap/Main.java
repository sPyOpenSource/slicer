/*
 * Created on Mar 29, 2006
 *
 */
package org.reprap;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import org.reprap.geometry.Producer;
import org.reprap.machines.MachineFactory;
import org.reprap.gui.RepRapBuild;
import org.reprap.gui.Utility;
import org.reprap.gui.botConsole.BotConsoleFrame;
import org.reprap.utilities.ExtensionFileFilter;
import org.reprap.utilities.RrDeleteOnExit;
import org.reprap.utilities.Debug;
import org.reprap.utilities.RrGraphics;
/**
 *
 * mainpage RepRap Host Controller Software
 * 
 * section overview Overview
 * 
 * Please see http://reprap.org/ for more details.
 *  
 */


public class Main {
	
    public static Main gui;
    
    private static final int localNodeNumber = 0;
	
    public static RrDeleteOnExit ftd = null;
    
    private static boolean repRapAttached = false;
    
    private Producer producer = null;
    
    private Printer printer = null;
    
    // Window to walk the file tree
    
    private JFileChooser chooser;
    private JFrame mainFrame;
    private RepRapBuild builder;

    private JCheckBoxMenuItem segmentPause;
    private JCheckBoxMenuItem layerPause;
    
    private JMenuItem cancelMenuItem;
    private JMenuItem produceProduceB;

    public void setSegmentPause(boolean state) {
        segmentPause.setState(state);
    }
    
    public void setLayerPause(boolean state) {
        layerPause.setState(state);
    }
    
    public void clickCancel() {
        cancelMenuItem.doClick();
    }
    
    private JSplitPane panel;
	
	public Main() {
		ftd = new RrDeleteOnExit();
        chooser = new JFileChooser();
 
        // Do we want just to list .stl files, or all?
        // If all, comment out the next two lines
        
        FileFilter filter = new ExtensionFileFilter("STL", new String[] { "STL" });
        chooser.setFileFilter(filter);
        
        try
        {
        	printer = MachineFactory.create();
        } catch (Exception ex)
        {
        	Debug.e("MachineFactory.create() failed.\n");
        	Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
	}

	private void createAndShowGUI() throws Exception {
        JFrame.setDefaultLookAndFeelDecorated(false);
        mainFrame = new JFrame("RepRap build bed    |     mouse:  left - rotate   middle - zoom   right - translate     |    grid: 20 mm");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Required so menus float over Java3D
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        
        // Create menus
        JMenuBar menubar = new JMenuBar();
        
        JMenu manipMenu = new JMenu("Click here for help");
        manipMenu.setMnemonic(KeyEvent.VK_M);
        menubar.add(manipMenu);

        JMenuItem manipX = new JMenuItem("Rotate X 90", KeyEvent.VK_X);
        manipX.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        manipX.addActionListener((ActionEvent arg0) -> {
            onRotateX();
        });
        manipMenu.add(manipX);

        JMenuItem manipY = new JMenuItem("Rotate Y 90", KeyEvent.VK_Y);
        manipY.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
        manipY.addActionListener((ActionEvent arg0) -> {
            onRotateY();
        });
        manipMenu.add(manipY);

        JMenuItem manipZ45 = new JMenuItem("Rotate Z 45", KeyEvent.VK_Z);
        manipZ45.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        manipZ45.addActionListener((ActionEvent arg0) -> {
            onRotateZ(45);
        });
        manipMenu.add(manipZ45);
        
        JMenuItem manipZp25 = new JMenuItem("Z Anticlockwise 2.5", KeyEvent.VK_A);
        manipZp25.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
        manipZp25.addActionListener((ActionEvent arg0) -> {
            onRotateZ(2.5);
        });
        manipMenu.add(manipZp25);
        
        JMenuItem manipZm25 = new JMenuItem("Z Clockwise 2.5", KeyEvent.VK_C);
        manipZm25.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        manipZm25.addActionListener((ActionEvent arg0) -> {
            onRotateZ(-2.5);
        });
        manipMenu.add(manipZm25);
        
        JMenuItem inToMM = new JMenuItem("Scale by 25.4 (in -> mm)", KeyEvent.VK_I);
        inToMM.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
        inToMM.addActionListener((ActionEvent arg0) -> {
            oninToMM();
        });
        manipMenu.add(inToMM);
        
        JMenuItem rescale = new JMenuItem("Scale in X, Y and Z", KeyEvent.VK_S);
        rescale.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        rescale.addActionListener((ActionEvent arg0) -> {
            onRescale();
        });
        manipMenu.add(rescale);
        
        JMenuItem changeMaterial = new JMenuItem("Change material", KeyEvent.VK_M);
        changeMaterial.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));
        changeMaterial.addActionListener((ActionEvent arg0) -> {
            onChangeMaterial();
        });
        manipMenu.add(changeMaterial);
        
        JMenuItem nextP = new JMenuItem("Select next object that will be built", KeyEvent.VK_N);
        nextP.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        nextP.addActionListener((ActionEvent arg0) -> {
            onNextPicked();
        });
        manipMenu.add(nextP);
        
        JMenuItem reorder = new JMenuItem("Reorder the building sequence", KeyEvent.VK_R);
        reorder.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
        reorder.addActionListener((ActionEvent arg0) -> {
            onReorder();
        });
        manipMenu.add(reorder);
        
        JMenuItem deleteSTL = new JMenuItem("Delete selected object", KeyEvent.VK_DELETE);
        deleteSTL.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteSTL.addActionListener((ActionEvent arg0) -> {
            onDelete();
        });
        manipMenu.add(deleteSTL);
        
        produceProduceB = new JMenuItem("Start build...", KeyEvent.VK_B);

        cancelMenuItem = new JMenuItem("Cancel", KeyEvent.VK_P);
        cancelMenuItem.setEnabled(false);

        segmentPause = new JCheckBoxMenuItem("Pause before segment");

        layerPause = new JCheckBoxMenuItem("Pause before layer");

        // Create the main window area
        // This is a horizontal box layout that includes
        // both the builder and preview screens, one of
        // which may be invisible.

        Box builderFrame = new Box(BoxLayout.Y_AXIS);
        builderFrame.add(new JLabel("Arrange items to print on the build bed"));
        builder = new RepRapBuild();
        builderFrame.setMinimumSize(new Dimension(0,0));
        builderFrame.add(builder);
        
        panel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        panel.setPreferredSize(Utility.getDefaultAppSize());
        panel.setMinimumSize(new Dimension(0, 0));
        panel.setResizeWeight(0.5);
        panel.setOneTouchExpandable(true);
        panel.setContinuousLayout(true);
        panel.setLeftComponent(builderFrame);

        panel.setDividerLocation(panel.getPreferredSize().width);
        
        mainFrame.getContentPane().add(panel);
                
        mainFrame.setJMenuBar(menubar);
        
        mainFrame.pack();
        Utility.positionWindowOnScreen(mainFrame);
        mainFrame.setVisible(true);
	}
	
    @Override
	protected void finalize() throws Throwable
	{
		Debug.d("Main finalise()");
		ftd.killThem();
	}
	
	/**
	 * @return the printer being used
	 */
	public Printer getPrinter()
	{
		return printer;
	}
	
	/**
	 * @return the printer being used
	 */
	public RepRapBuild getBuilder()
	{
		return builder;
	}
	
	/**
	 * Stop production
	 *
	 */
	public void pause()
	{
		if(producer != null)
			producer.pause();
		try
		{
			printer.stopMotor();
			printer.stopValve();
			printer.pause();
		} catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
	}
	
	/**
	 * Resume production
	 * NB: does not re-start the extruder
	 *
	 */
	public void resume()
	{
		printer.resume();
		if(producer != null)
			producer.resume();
	}
	
	public int getLayers()
	{
		if(producer == null)
			return 0;
		return producer.getLayers();
	}
	
	public int getLayer()
	{
		if(producer == null)
			return 0;
		return producer.getLayer();
	}
	
	public void onProduceB() {
        cancelMenuItem.setEnabled(true);
        produceProduceB.setEnabled(false);
		Thread t = new Thread() {
                        @Override
			public void run() {
				Thread.currentThread().setName("Producer");
				try {
					
					if(printer == null)
						Debug.e("Production attempted with null printer.");
					producer = new Producer(printer, builder);
					producer.setSegmentPause(segmentPause);
					producer.setLayerPause(layerPause);
					producer.produce();
					String usage = getResourceMessage(producer);
					producer.dispose();
					producer = null;
			        cancelMenuItem.setEnabled(false);
			        produceProduceB.setEnabled(true);
					JOptionPane.showMessageDialog(mainFrame, "Slicing complete - Exit");
					dispose();
				}
				catch (Exception ex) {
					JOptionPane.showMessageDialog(mainFrame, "Production exception: " + ex);
				}
			}
		};
		t.start();
	}
	
    public File onOpen(String description, String[] extensions, String defaultRoot) 
    {
        String result;
        File f;
        FileFilter filter = new ExtensionFileFilter(description, extensions);

        chooser.setFileFilter(filter);
        if(!defaultRoot.contentEquals("") && extensions.length == 1)
        {
        	File defaultFile = new File(defaultRoot + "." + extensions[0]);
        	chooser.setSelectedFile(defaultFile);
        }
        
        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) 
        {
            f = chooser.getSelectedFile();
            result = f.getAbsolutePath();
            if(extensions[0].toUpperCase().contentEquals("RFO"))
            	builder.addRFOFile(result);
            if(extensions[0].toUpperCase().contentEquals("STL"))
            	builder.anotherSTLFile(result, printer, true);

            return f;
        }
        return null;
    }
    
    public String saveRFO(String fileRoot)
    {
        String result;
        File f;
        FileFilter filter;
        
        
		File defaultFile = new File(fileRoot + ".rfo");
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(defaultFile);
		filter = new ExtensionFileFilter("RFO file to write to", new String[] { "rfo" });
		chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
 
        chooser.setFileFilter(filter);

        int returnVal = chooser.showSaveDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) 
        {
            f = chooser.getSelectedFile();
            result = "file:" + f.getAbsolutePath();

            	builder.saveRFOFile(result);
            return f.getName();
        }
        return "";   	
    }
    
    public String saveSCAD(String fileRoot)
    {
        String result;
        File f;
        FileFilter filter;
        
        
		File defaultFile = new File(fileRoot + ".scad");
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(defaultFile);
		filter = new ExtensionFileFilter("Directory to put OpenSCAD files in", new String[] { "" });
		chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
 
        chooser.setFileFilter(filter);

        int returnVal = chooser.showSaveDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) 
        {
            f = chooser.getSelectedFile();
            result = "file:" + f.getAbsolutePath();
            	builder.saveSCADFile(result);
            return f.getName();
        }
        return "";   	
    }
    
    public void deleteAllSTLs()
    {
    	builder.deleteAllSTLs();
    }
    
    private void onRotateX() {
    	  builder.xRotate();
    }

    private void onRotateY() {
  	  builder.yRotate();
    }

    private void onRotateZ(double angle) {
  	  builder.zRotate(angle);
    }
    
    private void oninToMM() {
    	  builder.inToMM();
      } 
    
    private void onRescale() 
    {    
  	  builder.rescale();
    } 
    
    private void onChangeMaterial() {
  	  builder.changeMaterial();
    } 
    
    private void onNextPicked()
    {
    	builder.nextPicked();
    }
    
    private void onReorder()
    {
    	builder.doReorder();
    }
    
    private void onDelete() {
    	  builder.deleteSTL();
      }
    
    public void mouseToWorld()
    {
    	builder.mouseToWorld();
    }
    
	private String getResourceMessage(Producer rProducer) {
		double moved = Math.round(rProducer.getTotalDistanceMoved() * 10.0) / 10.0;
		double extruded = Math.round(rProducer.getTotalDistanceExtruded() * 10.0) / 10.0;
		double extrudedVolume = Math.round(rProducer.getTotalVolumeExtruded() * 10.0) / 10.0;
		double time = Math.round(rProducer.getTotalElapsedTime() * 10.0) / 10.0;
		return "Total distance travelled=" + moved +
			"mm.  Total distance extruded=" + extruded +
			"mm.  Total volume extruded=" + extrudedVolume +
			"mm^3.  Elapsed time=" + time + "s";
	}
	
	public void dispose()
	{
		Debug.d("Main dispose()");
		ftd.killThem();
		/// TODO This class should be fixed so it gets the dispose on window close

		System.exit(0);
	}
	
	public static void main(String[] args) 
	{
		Thread.currentThread().setName("Main");
		javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        Thread.currentThread().setName("RepRap");
                        gui = new Main();
                        gui.createAndShowGUI();
                    }
                    catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Error in the main GUI: " + ex);
                    }
                    
                    gui.mainFrame.setFocusable(true);
                    gui.mainFrame.requestFocus();
                    BotConsoleFrame.main(null);
                });

	}
   
        
        public static void setRepRapPresent(boolean a)
        {
        	repRapAttached = a;
        }
        
        public static boolean repRapPresent()
        {
        	return repRapAttached;
        }

        
        public RrGraphics getGraphics()
        {
        	return builder.getRrGraphics();
        }

    }
