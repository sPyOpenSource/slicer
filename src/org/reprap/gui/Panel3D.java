
package org.reprap.gui;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

import javafx.application.Application;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.stage.Stage;

import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

import org.jogamp.vecmath.Vector3d;

import org.reprap.geometry.polyhedra.STLObject;
import org.reprap.utilities.Debug;

abstract public class Panel3D extends Application {
	private static final long serialVersionUID = 1L;
	//-------------------------------------------------------------
	
	// What follows are defaults.  These values should be overwritten from
	// the reprap.properties file.
	
	protected String wv_location = "/Users/xuyi/Pictures/3D/edf/files/edf120.stl";

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

	protected Color bgColour = Color.color(0.9f, 0.9f, 0.9f);
	protected Color selectedColour = Color.color(0.6f, 0.2f, 0.2f);
	protected Color machineColour = Color.color(0.7f, 0.7f, 0.7f);
	protected Color unselectedColour = Color.color(0.3f, 0.3f, 0.3f);
	
	// That's the end of the configuration file data
	
	//--------------------------------------------------------------
	
	protected static final Color black = Color.color(0, 0, 0);	
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
        protected Stage stage;
	protected Group sceneBranchGroup = null;
	protected Bounds applicationBounds = null;

	// Set up the RepRap working volume
	abstract protected Scene createSceneBranchGroup() throws Exception;

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
                //wv_location = Preferences.getBasePath();

		// Translate and zoom scaling factors
		
		mouse_tf = 50; 
		mouse_zf = 50; 
		
		RadiusFactor = 0.7;
		BackFactor = 2.0;
		FrontFactor = 0.001;
		BoundFactor = 3.0;

		xwv = 400;//Preferences.loadGlobalDouble("WorkingX(mm)"); // The RepRap machine...
		ywv = 400;//Preferences.loadGlobalDouble("WorkingY(mm)"); // ...working volume in mm.
		zwv = 400;//Preferences.loadGlobalDouble("WorkingZ(mm)");

		// Factors for front and back clipping planes and so on
		
		worldName = "RepRap-World";
		wv_offset = new Vector3d(0,
				0,
				0);

		// The background, and other colours
		
		bgColour = Color.color(0.9, 0.9, 0.9);
		
		selectedColour = Color.color((float)0.6, (float)0.2, (float)0.2);

		machineColour = Color.color((float)0.3, (float)0.3, (float)0.3);
		
		unselectedColour = Color.color((float)0.3, (float)0.3, (float)0.3);
            } catch (Exception ex) {
		Debug.e("Refresh Panel3D preferences: " + ex.toString());
            }
				
		// End of stuff from the preferences file
		
		// ----------------------
	}

	protected void initialise(Stage primaryStage) throws Exception 
	{
		refreshPreferences();

		picked_app = new PhongMaterial(selectedColour);//, black, selectedColour, black, 0f);
				
		wv_app = new PhongMaterial(machineColour);//, black, machineColour, black, 0f);

		initJava3d(primaryStage);
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
		applicationBounds = new BoundingBox(xwv * 0.5,
				ywv * 0.5, zwv * 0.5, BoundFactor
				* getViewPlatformActivationRadius());
		return applicationBounds;
	}

	// (About) how big is the world?
	protected float getViewPlatformActivationRadius() {
		return (float) (RadiusFactor * Math.sqrt(xwv * xwv + ywv * ywv + zwv * zwv));
	}

	public Color getObjectColour()
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

	protected Scene createView(Stage vp) {
		Group pe = createPhysicalEnvironment();

		/*AudioDevice audioDevice = createAudioDevice(pe);

		if (audioDevice != null) {
			pe.setAudioDevice(audioDevice);
			audioDevice.initialize();
		}*/

		Scene view = new Scene(pe);

		if (vp != null)
			vp.setScene(view);

		//view.setBackClipDistance(getBackClipDistance());
		//view.setFrontClipDistance(getFrontClipDistance());

		//Canvas c3d = createCanvas3D();
		//pe.getChildren().add(c3d);
		//addCanvas3D(c3d);

		return view;
	}

	/*protected Canvas createCanvas3D() {
		GraphicsContext gc3D = new GraphicsContext();
		gc3D.setSceneAntialiasing(GraphicsConfigTemplate.PREFERRED);
		GraphicsDevice gd[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		
		Canvas c3d = new Canvas(gd[0].getBestConfiguration(gc3D));

		return c3d;
	}

	public org.jogamp.java3d.Locale getFirstLocale() {
		Iterator<Locale> en = universe.getAllLocales();

		if (en.hasNext() != false)
			return (org.jogamp.java3d.Locale) en.next();

		return null;
	}*/

	// The size of the world
	protected Bounds getApplicationBounds() {
		if (applicationBounds == null)
			applicationBounds = createApplicationBounds();

		return applicationBounds;
	}

	// Fire up Java3D
	public void initJava3d(Stage vp) throws Exception {
		//universe = createVirtualUniverse();

		//org.jogamp.java3d.Locale locale = createLocale(universe);
        Camera camera = new PerspectiveCamera(true);
        camera.setFarClip(Integer.MAX_VALUE);
        camera.setNearClip(0.1);
		Scene sceneBranchGroup = createSceneBranchGroup();
                sceneBranchGroup.setCamera(camera);
                sceneBranchGroup.getCamera().setTranslateZ(-3500);
        vp.setScene(sceneBranchGroup);
		//Stage vp = createViewPlatform();
		//Group viewBranchGroup = createViewBranchGroup(getViewTransformGroupArray());

		//createView(vp);

		Background background = createBackground();

		/*if (background != null)
			sceneBranchGroup.addChild(background);

		locale.addBranchGraph(sceneBranchGroup);
		addViewBranchGroup(locale, viewBranchGroup);*/
	}

	/*protected AudioDevice createAudioDevice(PhysicalEnvironment pe) {
		JavaSoundMixer javaSoundMixer = new JavaSoundMixer(pe);

		if (javaSoundMixer == null)
			Debug.e("create of audiodevice failed");

		return javaSoundMixer;
	}*/

	protected Group createPhysicalEnvironment() {
		return new Group();
	}

	public Stage getViewPlatform() {
		//Stage vp = new Stage();
		//vp.setViewAttachPolicy(View.RELATIVE_TO_FIELD_OF_VIEW);
		//vp.setActivationRadius(getViewPlatformActivationRadius());

		return stage;
	}

	// These two are probably wrong.

	/*protected int getCanvas3dWidth(Canvas c3d) {
		return getWidth();
	}

	protected int getCanvas3dHeight(Canvas c3d) {
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

		Transform viewTrans = new Transform();
		Transform eyeTrans = new Transform();

		BoundingBox sceneBounds = (BoundingBox) sceneBranchGroup
				.getBounds();

		// point the view at the center of the object

		Point3d center = new Point3d();
		//sceneBounds.getCenter(center);
		double radius = sceneBounds.getRadius();
		Vector3d temp = new Vector3d(sceneBounds.getCenterX(),
                        sceneBounds.getCenterY(),
                        sceneBounds.getCenterZ());
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

	protected void addCanvas3D(Canvas c3d) {
		setLayout(new BorderLayout());
		add(c3d, BorderLayout.CENTER);
		doLayout();
		c3d.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}*/
	
	protected double getScale() {
		return 1.0;
	}
	
	protected String getStlBackground() throws Exception 
	{
		return wv_location;
	}
}
