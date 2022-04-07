/*
 
 RepRap
 ------
 
 The Replicating Rapid Prototyper Project
 
 
 Copyright (C) 2006
 Adrian Bowyer & The University of Bath
 
 http://reprap.org
 
 Principal author:
 
 Adrian Bowyer
 Department of Mechanical Engineering
 Faculty of Engineering and Design
 University of Bath
 Bath BA2 7AY
 U.K.
 
 e-mail: A.Bowyer@bath.ac.uk
 
 RepRap is free; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public
 Licence as published by the Free Software Foundation; either
 version 2 of the Licence, or (at your option) any later version.
 
 RepRap is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Library General Public Licence for more details.
 
 For this purpose the words "software" and "library" in the GNU Library
 General Public Licence are taken to mean any and all computer programs
 computer files data results documents and other copyright information
 available from the RepRap project.
 
 You should have received a copy of the GNU Library General Public
 Licence along with RepRap; if not, write to the Free
 Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA,
 or see
 
 http://www.gnu.org/
 
 =====================================================================
 
 This program loads STL files of objects, orients them, and builds them
 in the RepRap machine.
 
 It is based on one of the open-source examples in Daniel Selman's excellent
 Java3D book, and his notice is immediately below.
 
 First version 2 April 2006
 This version: 16 April 2006
 
 */

/*******************************************************************************
 * VrmlPickingTest.java Copyright (C) 2001 Daniel Selman
 * 
 * First distributed with the book "Java 3D Programming" by Daniel Selman and
 * published by Manning Publications. http://manning.com/selman
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * The license can be found on the WWW at: http://www.fsf.org/copyleft/gpl.html
 * 
 * Or by writing to: Free Software Foundation, Inc., 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA.
 * 
 * Authors can be contacted at: Daniel Selman: daniel@selman.org
 * 
 * If you make changes you think others would like, please contact one of the
 * authors or someone at the www.j3d.org web site.
 ******************************************************************************/

package org.reprap.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import javafx.geometry.Bounds;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Background;

import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Vector3d;

import org.reprap.Attributes;
import org.reprap.Printer;
import org.reprap.RFO;
import org.reprap.Preferences;
import org.reprap.geometry.polygons.Point2D;
import org.reprap.geometry.polyhedra.AllSTLsToBuild;
import org.reprap.geometry.polyhedra.STLObject;
import org.reprap.utilities.Debug;
import org.reprap.utilities.RrGraphics;

/**
 * Little class to put up a radiobutton menu so you can set
 * what material something is to be made from.
 * 
 * @author adrian
 *
 */
class MaterialRadioButtons extends JPanel {
	private static final long serialVersionUID = 1L;
	private static Attributes att;
	private static JDialog dialog;
	private static JTextField copies;
	private static RepRapBuild rrb;
	private static int stlIndex; 
	
	private MaterialRadioButtons(double volume)
	{
            super(new BorderLayout());
            JPanel radioPanel;
            ButtonGroup bGroup = new ButtonGroup();
            String[] names;
            radioPanel = new JPanel(new GridLayout(0, 1));
            radioPanel.setSize(300,200);

            JLabel jLabel0 = new JLabel();
	    radioPanel.add(jLabel0);
	    jLabel0.setText("Volume of object: " + Math.round(volume) + " mm^3");
            jLabel0.setHorizontalAlignment(SwingConstants.CENTER);
		
	    JLabel jLabel2 = new JLabel();
	    radioPanel.add(jLabel2);
	    jLabel2.setText(" Number of copies of the object just loaded to print: ");
            jLabel2.setHorizontalAlignment(SwingConstants.CENTER);
            copies = new JTextField("1");
	    copies.setSize(20, 10);
		copies.setHorizontalAlignment(SwingConstants.CENTER);
		radioPanel.add(copies);
		
		JLabel jLabel1 = new JLabel();
		radioPanel.add(jLabel1);
		jLabel1.setText(" Select the material for the object(s): ");
		jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
		
		try
		{
			names = Preferences.allMaterials();
			String matname = att.getMaterial();
			if(matname == null)
				matname = "";
			int matnumber = -1;
			for(int i = 0; i < names.length; i++)
			{
				if(matname.contentEquals(names[i]))
					matnumber = i;
				JRadioButton b = new JRadioButton(names[i]);
		        b.setActionCommand(names[i]);
		        b.addActionListener((ActionEvent e) -> {
                            att.setMaterial(e.getActionCommand());
                                });
		        if(i == matnumber)
		        	b.setSelected(true);
		        bGroup.add(b);
		        radioPanel.add(b);
			}
			if(matnumber < 0)
			{
				att.setMaterial(names[0]);
				JRadioButton b = (JRadioButton)bGroup.getElements().nextElement();
				b.setSelected(true);
			} else
				copies.setEnabled(false);  // If it's already loaded, don't make multiple copies (FUTURE: why not...?)
			
			JButton okButton = new JButton();
			radioPanel.add(okButton);
			okButton.setText("OK");
			okButton.addActionListener((ActionEvent evt) -> {
                            OKHandler();
                        });
			
			add(radioPanel, BorderLayout.LINE_START);
			setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
			
		} catch (IOException ex)
		{
			Debug.e(ex.toString());
		}	
	}
	
	public static void OKHandler()
	{
		int number = Integer.parseInt(copies.getText().trim()) - 1;
		STLObject stl = rrb.getSTLs().get(stlIndex);
		rrb.moreCopies(stl, att, number);
		dialog.dispose();
	}
    
    public static void createAndShowGUI(Attributes a, RepRapBuild r, int index, double volume) 
    {
    	att = a;
    	rrb = r;
    	stlIndex = index;
        //Create and set up the window.
    	JFrame f = new JFrame();
    	dialog = new JDialog(f, "Material selector");
        dialog.setLocation(500, 400);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new MaterialRadioButtons(volume);
        newContentPane.setOpaque(true); //content panes must be opaque
        dialog.setContentPane(newContentPane);

        //Display the window.
        dialog.pack();
        dialog.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
        dialog.setVisible(true);
    }	 
    
    public static void createAndShowGUI(Attributes a, RepRapBuild r, STLObject lastPicked) 
    {
    	if(lastPicked == null)
    		return;
    	int index = -1;
		for(int i = 0; i < r.getSTLs().size(); i++)
		{
			if(r.getSTLs().get(i) == lastPicked)
			{
				index = i;
				break;
			}
		}
		if (index >= 0) 
			createAndShowGUI(a, r, index, r.getSTLs().get(index).volume());
    }
	
}


/**
 * Little class to put up a radiobutton menu so you can set
 * what material something is to be made from.
 * 
 * @author adrian
 *
 */
class ScaleXYZ extends JPanel {
	private static final long serialVersionUID = 1L;
	private static JDialog dialog;
	private static JTextField xv, yv, zv;
	private static double x, y, z;
	private static boolean called = false;
	
	private ScaleXYZ(double xi, double yi, double zi)
	{
		super(new BorderLayout());
		if(!called)
		{
			x = xi;
			y = yi;
			z = zi;
			called = true;
		}
		JPanel radioPanel;
		radioPanel = new JPanel(new GridLayout(0, 1));
		radioPanel.setSize(300,200);
		
		JLabel jLabel0 = new JLabel();
	    radioPanel.add(jLabel0);
	    jLabel0.setText("Rescale selected object (NB Meshlab is quicker)");
		jLabel0.setHorizontalAlignment(SwingConstants.CENTER);
		
	    JLabel jLabel2 = new JLabel();
	    radioPanel.add(jLabel2);
	    jLabel2.setText(" X ");
		jLabel2.setHorizontalAlignment(SwingConstants.CENTER);
		xv = new JTextField(""+x);
	    xv.setSize(20, 10);
		xv.setHorizontalAlignment(SwingConstants.CENTER);
		radioPanel.add(xv);
		
	    JLabel jLabel3 = new JLabel();
	    radioPanel.add(jLabel3);
	    jLabel3.setText(" Y ");
		jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
		yv = new JTextField(""+y);
	    yv.setSize(20, 10);
		yv.setHorizontalAlignment(SwingConstants.CENTER);
		radioPanel.add(yv);
		
	    JLabel jLabel4 = new JLabel();
	    radioPanel.add(jLabel4);
	    jLabel4.setText(" Z ");
		jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
		zv = new JTextField(""+z);
	    zv.setSize(20, 10);
		zv.setHorizontalAlignment(SwingConstants.CENTER);
		radioPanel.add(zv);
		
		JButton okButton = new JButton();
		radioPanel.add(okButton);
		okButton.setText("OK");
		okButton.addActionListener((ActionEvent evt) -> {
                    OKHandler();
                });
		JButton cancelButton = new JButton();
		radioPanel.add(cancelButton);
		cancelButton.setText("Cancel");
		cancelButton.addActionListener((ActionEvent evt) -> {
                    cancelHandler();
                });
		
		add(radioPanel, BorderLayout.LINE_START);
		setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
	}
	
	public static double x() { return x; }
	public static double y() { return y; }
	public static double z() { return z; }
	
	public static void OKHandler()
	{
		x = Double.parseDouble(xv.getText().trim());
		y = Double.parseDouble(yv.getText().trim());
		z = Double.parseDouble(zv.getText().trim());
		dialog.dispose();
	}
	public static void cancelHandler()
	{
		x = 1;
		y = 1;
		z = 1;
		dialog.dispose();
	}
    
    public static void createAndShowGUI() 
    {
    	JFrame f = new JFrame();
    	dialog = new JDialog(f, "scale");
        dialog.setLocation(500, 400);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new ScaleXYZ(1.0, 1.0, 1.0);
        newContentPane.setOpaque(true); //content panes must be opaque
        dialog.setContentPane(newContentPane);

        //Display the window.
        dialog.pack();
        dialog.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
        dialog.setVisible(true);
    }	 
}

//************************************************************************

/**
 * This is the main public class that creates a virtual world of the RepRap
 * working volume, allows you to put STL-file objects in it, move them about
 * to arrange them, and build them in the machine.
 */

public class RepRapBuild extends Panel3D implements MouseListener {
	
	private static final long serialVersionUID = 1L;
	private MouseObject mouse = null;
	private PickCanvas pickCanvas = null; // The thing picked by a mouse click
	private STLObject lastPicked = null; // The last thing picked
	private final AllSTLsToBuild stls;
	private boolean reordering;
	private RrGraphics graphics;

	// Constructors
	public RepRapBuild() throws Exception {
		initialise();
		stls = new AllSTLsToBuild();
		reordering = false;
		graphics = null;
		setPreferredSize(new Dimension(600, 400));
	}
	
	public AllSTLsToBuild getSTLs()
	{
		return stls;
	}
	
	// Set bg light grey
        @Override
	protected Background createBackground() {
		Background back = new Background(bgColour);
		back.setApplicationBounds(createApplicationBounds());
		return back;
	}

        @Override
	protected Group createViewBranchGroup(Group[] tgArray,
			ViewPlatform vp) {
		Group vpBranchGroup = new Group();

		if (tgArray != null && tgArray.length > 0) {
			Group parentGroup = vpBranchGroup;
			Group curTg;

                    for (Group tgArray1 : tgArray) {
                        curTg = tgArray1;
                        parentGroup.addChild(curTg);
                        parentGroup = curTg;
                    }

			tgArray[tgArray.length - 1].addChild(vp);
		} else
			vpBranchGroup.addChild(vp);

		return vpBranchGroup;
	}

	// Set up the RepRap working volume

        @Override
	protected Group createSceneBranchGroup() throws Exception {
		sceneBranchGroup = new Group();

		Group objRoot = sceneBranchGroup;

		Bounds lightBounds = getApplicationBounds();

		AmbientLight ambLight = new AmbientLight(true, new Color3f(1.0f, 1.0f,
				1.0f));
		ambLight.setInfluencingBounds(lightBounds);
		objRoot.addChild(ambLight);

		DirectionalLight headLight = new DirectionalLight();
		headLight.setInfluencingBounds(lightBounds);
		objRoot.addChild(headLight);

		mouse = new MouseObject(getApplicationBounds(), mouse_tf, mouse_zf);

		wv_and_stls.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		wv_and_stls.setCapability(Group.ALLOW_CHILDREN_WRITE);
		wv_and_stls.setCapability(Group.ALLOW_CHILDREN_READ);

		// Load the STL file for the working volume

		world = new STLObject(wv_and_stls, worldName);

		String stlFile = getStlBackground();

		workingVolume = new STLObject();
		workingVolume.addSTL(stlFile, wv_offset, wv_app, null);
		wv_and_stls.addChild(workingVolume.top());

		// Set the mouse to move everything

		mouse.move(world, false);
		objRoot.addChild(world.top());

		return objRoot;
	}

	// Action on mouse click

        @Override
	public void mouseClicked(MouseEvent e) {
		pickCanvas.setShapeLocation(e);

		PickResult pickResult = pickCanvas.pickClosest();
		STLObject picked;

		if (pickResult != null) // Got anything?
		{
			Node actualNode = pickResult.getObject();

			Attributes att = (Attributes)actualNode.getUserData();
			picked = att.getParent();
			if (picked != null) // Really got something?
			{
				if (picked != workingVolume) // STL object picked?
				{
					//picked = findSTL(name);
					if (picked != null) {
						picked.setAppearance(picked_app); // Highlight it
						if (lastPicked != null  && !reordering)
							lastPicked.restoreAppearance(); // lowlight
						// the last
						// one
						if(!reordering)
							mouse.move(picked, true); // Set the mouse to move it
						lastPicked = picked; // Remember it
						reorder();
					}
				} else { // Picked the working volume - deselect all and...
					if(!reordering)
						mouseToWorld();
				}
			}
		}
	}
	
	public void mouseToWorld()
	{
		if (lastPicked != null)
			lastPicked.restoreAppearance();
		mouse.move(world, false); // ...switch the mouse to moving the world
		lastPicked = null;
	}

	// Find the stl object in the scene with the given name

        @Override
	public void mouseEntered(MouseEvent e) {
	}

        @Override
	public void mouseExited(MouseEvent e) {
	}

        @Override
	public void mousePressed(MouseEvent e) {
	}

        @Override
	public void mouseReleased(MouseEvent e) {
	}

	
	public void moreCopies(STLObject original, Attributes originalAttributes, int number)
	{
		if (number <= 0)
			return;
		String fileName = original.fileAndDirectioryItCameFrom(0);
		double increment = original.extent().x + 5;
		Vector3d offset = new Vector3d();
		offset.x = increment;
		offset.y = 0;
		offset.z = 0;
		for(int i = 0; i < number; i++)
		{
			STLObject stl = new STLObject();
			Attributes newAtt = stl.addSTL(fileName, null, original.getAppearance(), null);
			if(newAtt != null)
			{
				newAtt.setMaterial(originalAttributes.getMaterial());
				stl.translate(offset);
				if(stl.numChildren() > 0)
				{
					wv_and_stls.addChild(stl.top());
					stls.add(stl);
				}
			}
			offset.x += increment;
		}
	}
	
	// Callback for when the user selects an STL file to load

	public void anotherSTLFile(String s, Printer printer, boolean centre) 
	{
		if (s == null)
			return;
		//objectIndex++;
		STLObject stl = new STLObject();
		Attributes att = stl.addSTL(s, null, Preferences.unselectedApp(), lastPicked);
		if(lastPicked == null && centre)
		{

			Point2D middle = Point2D.mul(0.5, printer.getBedNorthEast());
			Vector3d v = new Vector3d(middle.x(), middle.y(), 0);
			Vector3d e = stl.extent();
			e.z = 0;
			e.x = -0.5*e.x;
			e.y = -0.5*e.y;
			v.add(e);
			stl.translate(v);
		}
		if(att != null)
		{
			// New separate object, or just appended to lastPicked?
			if(stl.numChildren() > 0)
			{
				wv_and_stls.addChild(stl.top());
				stls.add(stl);
			}

			MaterialRadioButtons.createAndShowGUI(att, this, stls.size() - 1, stl.volume());
		}
	}
	
	// Callback for when the user has a pre-loaded STL and attribute

	public void anotherSTL(STLObject stl, Attributes att, int index) 
	{
		if (stl == null || att == null)
			return;

		// New separate object, or just appended to lastPicked?
		if(stl.numChildren() > 0)
		{
			wv_and_stls.addChild(stl.top());
			stls.add(index, stl);
		}
	}
	
	public void changeMaterial()
	{
		if(lastPicked == null)
			return;
		MaterialRadioButtons.createAndShowGUI(lastPicked.attributes(0), this, lastPicked);
	}
	
	// Callback for when the user selects an RFO file to load

	public void addRFOFile(String s) 
	{
		if (s == null)
			return;
		//deleteAllSTLs();
		AllSTLsToBuild newStls = RFO.load(s);
		for(int i = 0; i < newStls.size(); i++)
			wv_and_stls.addChild(newStls.get(i).top());
		stls.add(newStls);
	}
	
	public void saveRFOFile(String s)
	{
		RFO.save(s, stls);
	}
	
	public void saveSCADFile(String s)
	{
		stls.saveSCAD(s);
	}
	


	public void start() throws Exception {
		if (pickCanvas == null)
			initialise();
	}

        @Override
	protected void addCanvas3D(Canvas c3d) {
		setLayout(new BorderLayout());
		add(c3d, BorderLayout.CENTER);
		doLayout();

		if (sceneBranchGroup != null) {
			c3d.addMouseListener(this);

			pickCanvas = new PickCanvas(c3d, sceneBranchGroup);
			pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
			pickCanvas.setTolerance(4.0f);
		}

		c3d.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	// Callbacks for when the user rotates the selected object

	public void xRotate() {
		if (lastPicked != null)
			lastPicked.xClick();
	}

	public void yRotate() {
		if (lastPicked != null)
			lastPicked.yClick();
	}

	public void zRotate(double angle) {
		if (lastPicked != null)
			lastPicked.zClick(angle);
	}
	
	// Callback for a request to convert units
	
	public void inToMM() {
		if (lastPicked != null)
			lastPicked.inToMM();
	}
	
	public void rescale() 
	{
		if (lastPicked == null)
			return;

		ScaleXYZ.createAndShowGUI();
		lastPicked.rescale(ScaleXYZ.x(), ScaleXYZ.y(), ScaleXYZ.z());
	}
	
	public void doReorder()
	{
		if (lastPicked != null)
		{
			lastPicked.restoreAppearance();
			mouseToWorld();
			lastPicked = null;
		}
		reordering = true;
	}
	
	/**
	 * User is reordering the list
	 */
	private void reorder()
	{
		if(!reordering)
			return;
		if(stls.reorderAdd(lastPicked))
			return;
		for(int i = 0; i < stls.size(); i++)
			stls.get(i).restoreAppearance();
		//mouseToWorld();		
		lastPicked = null;
		reordering = false;
	}
	
	// Move to the next one in the list
	
	public void nextPicked()
	{
		if (lastPicked == null)
			lastPicked = stls.get(0);
		else
		{
			lastPicked.restoreAppearance();
			lastPicked = stls.getNextOne(lastPicked);
		}
		lastPicked.setAppearance(picked_app);
		mouse.move(lastPicked, true);
	}
	
	// Callback to delete one of the loaded objects
	
	public void deleteSTL()
	{
		if (lastPicked == null)
			return;
		int index = -1;
		for(int i = 0; i < stls.size(); i++)
		{
			if(stls.get(i) == lastPicked)
			{
				index = i;
				break;
			}
		}
		if (index >= 0) 
		{
			stls.remove(index);
			index = wv_and_stls.indexOfChild(lastPicked.top());
			mouseToWorld();
			wv_and_stls.removeChild(index);
			lastPicked = null;
		}
	}
	
	public void deleteAllSTLs()
	{
		for(int i = 0; i < stls.size(); i++)
		{
			STLObject s = stls.get(i);
			stls.remove(i);
			int index = wv_and_stls.indexOfChild(s.top());
			wv_and_stls.removeChild(index);
		}
		mouseToWorld();
		lastPicked = null;
	}
	
	public void setGraphics(RrGraphics g)
	{
		graphics = g;
	}
	
	public RrGraphics getRrGraphics()
	{
		return graphics;
	}

}