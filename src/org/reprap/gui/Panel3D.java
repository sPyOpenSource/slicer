package org.reprap.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GraphicsConfigTemplate;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URL;

import javax.media.j3d.Appearance;
import javax.media.j3d.AudioDevice;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.Material;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.PhysicalEnvironment;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.ViewPlatform;
import javax.media.j3d.VirtualUniverse;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.reprap.Preferences;

import com.sun.j3d.audioengines.javasound.JavaSoundMixer;
import java.io.IOException;
import java.net.MalformedURLException;

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
	protected Appearance picked_app = null; // Colour for the selected part
	protected Appearance wv_app = null; // Colour for the working volume
	protected BranchGroup wv_and_stls = new BranchGroup(); // Where in the scene

	// the
	// working volume and STLs
	// are joined on.

	protected STLObject world = null; // Everything
	protected STLObject workingVolume = null; // The RepRap machine itself.
	
	// The world in the Applet
	protected VirtualUniverse universe = null;
	protected BranchGroup sceneBranchGroup = null;
	protected Bounds applicationBounds = null;

	// Set up the RepRap working volume
	abstract protected BranchGroup createSceneBranchGroup() throws Exception;

	// Set bg light grey
	abstract protected Background createBackground();

	abstract protected BranchGroup createViewBranchGroup(
			TransformGroup[] tgArray, ViewPlatform vp);
	
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
		

		picked_app = new Appearance();
		picked_app.setMaterial(new Material(selectedColour, black, selectedColour, black, 0f));
				
		wv_app = new Appearance();
		wv_app.setMaterial(new Material(machineColour, black, machineColour, black, 0f));

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

	public VirtualUniverse getVirtualUniverse() {
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

	public javax.media.j3d.Locale getFirstLocale() {
		java.util.Enumeration<?> en = universe.getAllLocales();

		if (en.hasMoreElements() != false)
			return (javax.media.j3d.Locale) en.nextElement();

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

		javax.media.j3d.Locale locale = createLocale(universe);

		BranchGroup sceneBranchGroup = createSceneBranchGroup();

		ViewPlatform vp = createViewPlatform();
		BranchGroup viewBranchGroup = createViewBranchGroup(
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

	protected void addViewBranchGroup(javax.media.j3d.Locale locale,
			BranchGroup bg) {
		locale.addBranchGraph(bg);
	}

	protected javax.media.j3d.Locale createLocale(VirtualUniverse u) {
		return new javax.media.j3d.Locale(u);
	}

	public TransformGroup[] getViewTransformGroupArray() {
		TransformGroup[] tgArray = new TransformGroup[1];
		tgArray[0] = new TransformGroup();

		Transform3D viewTrans = new Transform3D();
		Transform3D eyeTrans = new Transform3D();

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

		tgArray[0].setTransform(viewTrans);

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