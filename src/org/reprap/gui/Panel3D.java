
package org.reprap.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GraphicsConfigTemplate;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Iterator;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.transform.Transform;

import javax.swing.JPanel;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Vector3d;

import org.reprap.Preferences;
import org.reprap.geometry.polyhedra.STLObject;
import org.reprap.utilities.Debug;

abstract public class Panel3D extends JPanel {
	private static final long serialVersionUID = 1L;
	//-------------------------------------------------------------
	
	// What follows are defaults.  These values should be overwritten from
	// the reprap.properties file.
	
	protected String wv_location = null;

	// Translate and zoom scaling factors
	
	protected double mouse_tf = 50;
	protected double mouse_zf = 50;

	protected double xwv = 300; // The RepRap machine...
	protected double ywv = 300; // ...working volume in mm.
	protected double zwv = 300;

	// Factors for front and back clipping planes and so on
	
	protected double RadiusFactor = 0.7;
	protected double BackFactor = 2.0;
	protected double FrontFactor = 0.001;
	protected double BoundFactor = 3.0;
	
	protected String worldName = "RepRap World";
	protected Vector3d wv_offset = new Vector3d(-17.3, -24.85, -2);

	// The background, and other colours

	protected Color3f bgColour = new Color3f(0.9f, 0.9f, 0.9f);
	protected Color3f selectedColour = new Color3f(0.6f, 0.2f, 0.2f);
	protected Color3f machineColour = new Color3f(0.7f, 0.7f, 0.7f);
	protected Color3f unselectedColour = new Color3f(0.3f, 0.3f, 0.3f);
	
	// That's the end of the configuration file data
	
	//--------------------------------------------------------------
	
	protected static final Color3f black = new Color3f(0, 0, 0);	
	protected Material picked_app = null; // Colour for the selected part
	protected Material wv_app = null; // Colour for the working volume
	protected Group wv_and_stls = new Group(); // Where in the scene

	// the
	// working volume and STLs
	// are joined on.

	protected STLObject world = null; // Everything
	protected STLObject workingVolume = null; // The RepRap machine itself.
	
	// The world in the Applet
	protected Scene universe = null;
	protected Group sceneBranchGroup = null;
	protected Bounds applicationBounds = null;

	// Set up the RepRap working volume
	abstract protected Group createSceneBranchGroup() throws Exception;

	// Set bg light grey
	abstract protected Background createBackground();

	abstract protected Group createViewBranchGroup(
			Group[] tgArray//, ViewPlatform vp
        );
	
	public void refreshPreferences()
	{
		// -----------------------
		
		// Set everything up from the properties file
		// All this needs to go into Preferences.java
		try
		{
                    wv_location = Preferences.getBasePath();

		// Translate and zoom scaling factors
		
		mouse_tf = 50; 
		mouse_zf = 50; 
		
		RadiusFactor = 0.7;
		BackFactor = 2.0;
		FrontFactor = 0.001;
		BoundFactor = 3.0;

		xwv = Preferences.loadGlobalDouble("WorkingX(mm)"); // The RepRap machine...
		ywv = Preferences.loadGlobalDouble("WorkingY(mm)"); // ...working volume in mm.
		zwv = Preferences.loadGlobalDouble("WorkingZ(mm)");

		// Factors for front and back clipping planes and so on
		
		worldName = "RepRap-World";
		wv_offset = new Vector3d(0,
				0,
				0);

		// The background, and other colours
		
		bgColour = new Color3f((float)0.9, (float)0.9, (float)0.9);
		
		selectedColour = new Color3f((float)0.6, (float)0.2, (float)0.2);

		machineColour = new Color3f((float)0.3, (float)0.3, (float)0.3);
		
		unselectedColour = new Color3f((float)0.3, (float)0.3, (float)0.3);
		} catch (IOException ex)
		{
			Debug.e("Refresh Panel3D preferences: " + ex.toString());
		}
				
		// End of stuff from the preferences file
		
		// ----------------------
	}


	protected void initialise() throws Exception 
	{
		
		refreshPreferences();

		picked_app = new PhongMaterial(selectedColour, black, selectedColour, black, 0f);
				
		wv_app = new PhongMaterial(machineColour, black, machineColour, black, 0f);

		initJava3d();

	}

	// How far away is the back?
	protected double getBackClipDistance() {
		return BackFactor * getViewPlatformActivationRadius();
	}

	// How close is the front?
	protected double getFrontClipDistance() {
		return FrontFactor * getViewPlatformActivationRadius();
	}
	
	// Set up the size of the world
	protected Bounds createApplicationBounds() {
		applicationBounds = new BoundingSphere(new Point3d(xwv * 0.5,
				ywv * 0.5, zwv * 0.5), BoundFactor
				* getViewPlatformActivationRadius());
		return applicationBounds;
	}

	// (About) how big is the world?
	protected float getViewPlatformActivationRadius() {
		return (float) (RadiusFactor * Math.sqrt(xwv * xwv + ywv * ywv + zwv * zwv));
	}

	public Color3f getObjectColour()
	{
		return unselectedColour;
	}
	// Where are we in the file system?

	public static URL getWorkingDirectory() {
		try {
			File file = new File(System.getProperty("user.dir"));
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			Debug.e("getWorkingDirectory( ): can't get user dir.");
		}

		return null;
	}

	// Return handles on big things above where we are interested

	public Scene getVirtualUniverse() {
		return universe;
	}

	protected View createView(ViewPlatform vp) {
		View view = new View();

		PhysicalBody pb = createPhysicalBody();
		PhysicalEnvironment pe = createPhysicalEnvironment();

		AudioDevice audioDevice = createAudioDevice(pe);

		if (audioDevice != null) {
			pe.setAudioDevice(audioDevice);
			audioDevice.initialize();
		}

		view.setPhysicalEnvironment(pe);
		view.setPhysicalBody(pb);

		if (vp != null)
			view.attachViewPlatform(vp);

		view.setBackClipDistance(getBackClipDistance());
		view.setFrontClipDistance(getFrontClipDistance());

		Canvas3D c3d = createCanvas3D();
		view.addCanvas3D(c3d);
		addCanvas3D(c3d);

		return view;
	}

	protected Canvas3D createCanvas3D() {
		GraphicsConfigTemplate3D gc3D = new GraphicsConfigTemplate3D();
		gc3D.setSceneAntialiasing(GraphicsConfigTemplate.PREFERRED);
		GraphicsDevice gd[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		
		Canvas3D c3d = new Canvas3D(gd[0].getBestConfiguration(gc3D));

		return c3d;
	}

	public org.jogamp.java3d.Locale getFirstLocale() {
		Iterator<Locale> en = universe.getAllLocales();

		if (en.hasNext() != false)
			return (org.jogamp.java3d.Locale) en.next();

		return null;
	}

	// The size of the world

	protected Bounds getApplicationBounds() {
		if (applicationBounds == null)
			applicationBounds = createApplicationBounds();

		return applicationBounds;
	}

	// Fire up Java3D

	public void initJava3d() throws Exception {
		universe = createVirtualUniverse();

		org.jogamp.java3d.Locale locale = createLocale(universe);

		Group sceneBranchGroup = createSceneBranchGroup();

		ViewPlatform vp = createViewPlatform();
		Group viewBranchGroup = createViewBranchGroup(
				getViewTransformGroupArray(), vp);

		createView(vp);

		Background background = createBackground();

		if (background != null)
			sceneBranchGroup.addChild(background);

		locale.addBranchGraph(sceneBranchGroup);
		addViewBranchGroup(locale, viewBranchGroup);

	}


	protected PhysicalBody createPhysicalBody() {
		return new PhysicalBody();
	}

	protected AudioDevice createAudioDevice(PhysicalEnvironment pe) {
		JavaSoundMixer javaSoundMixer = new JavaSoundMixer(pe);

		if (javaSoundMixer == null)
			Debug.e("create of audiodevice failed");

		return javaSoundMixer;
	}

	protected PhysicalEnvironment createPhysicalEnvironment() {
		return new PhysicalEnvironment();
	}

	protected ViewPlatform createViewPlatform() {
		ViewPlatform vp = new ViewPlatform();
		vp.setViewAttachPolicy(View.RELATIVE_TO_FIELD_OF_VIEW);
		vp.setActivationRadius(getViewPlatformActivationRadius());

		return vp;
	}

	// These two are probably wrong.

	protected int getCanvas3dWidth(Canvas3D c3d) {
		return getWidth();
	}

	protected int getCanvas3dHeight(Canvas3D c3d) {
		return getHeight();
	}

	protected VirtualUniverse createVirtualUniverse() {
		return new VirtualUniverse();
	}

	protected void addViewBranchGroup(org.jogamp.java3d.Locale locale,
			Group bg) {
		locale.addBranchGraph(bg);
	}

	protected org.jogamp.java3d.Locale createLocale(VirtualUniverse u) {
		return new org.jogamp.java3d.Locale(u);
	}

	public Group[] getViewTransformGroupArray() {
		Group[] tgArray = new Group[1];
		tgArray[0] = new Group();

		Transform viewTrans = new Transform3D();
		Transform eyeTrans = new Transform3D();

		BoundingSphere sceneBounds = (BoundingSphere) sceneBranchGroup
				.getBounds();

		// point the view at the center of the object

		Point3d center = new Point3d();
		sceneBounds.getCenter(center);
		double radius = sceneBounds.getRadius();
		Vector3d temp = new Vector3d(center);
		viewTrans.set(temp);

		// pull the eye back far enough to see the whole object

		double eyeDist = radius / Math.tan(Math.toRadians(40) / 2.0);
		temp.x = 0.0;
		temp.y = 0.0;
		temp.z = eyeDist;
		eyeTrans.set(temp);
		viewTrans.mul(eyeTrans);

		// set the view transform

		tgArray[0].getTransforms().add(viewTrans);

		return tgArray;
	}

	protected void addCanvas3D(Canvas3D c3d) {
		setLayout(new BorderLayout());
		add(c3d, BorderLayout.CENTER);
		doLayout();
		c3d.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
	
	protected double getScale() {
		return 1.0;
	}
	
	protected String getStlBackground() throws Exception 
	{
		return wv_location;
	}
	
}
