package org.reprap.machines;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

import org.reprap.Attributes;
import org.reprap.CartesianPrinter;
import org.reprap.Preferences;
import org.reprap.RepRapException;
import org.reprap.devices.NullExtruder;
import org.reprap.devices.GenericExtruder;

import org.reprap.gui.ContinuationMesage;
import org.reprap.gui.StatusMessage;
import org.reprap.Extruder;
import org.reprap.utilities.Debug;
import org.reprap.utilities.RrGraphics;
import org.reprap.utilities.Timer;
import org.reprap.utilities.ExtensionFileFilter;

import org.reprap.geometry.LayerRules;
import org.reprap.geometry.polygons.Point2D;
import org.reprap.geometry.polygons.Rectangle;
import org.reprap.utilities.StlFile;

public abstract class GenericRepRap implements CartesianPrinter
{
	protected boolean stlLoaded = false;
	protected boolean gcodeLoaded = false;
        private JFileChooser chooser;
        protected final Stage stage;
	
	LayerRules layerRules = null;
	
	/**
	 * Force an extruder to be selected on startup
	 */
	protected boolean forceSelection;
	
	// If true this starts by drawing a rectangle round everything
	// If false the extruder purges at the purge point
	//protected boolean startRectangle = true;
	
	//protected boolean accelerating;
	
	protected boolean XYEAtZero;
	
	/**
	 * Have we actually used this extruder?
	 */
	protected boolean physicalExtruderUsed[];
	
	/**
	 * 
	 */
	protected StatusMessage statusWindow;
	
	
	/**
	 * 
	 */
	protected JCheckBoxMenuItem layerPauseCheckbox = null, segmentPauseCheckbox = null;
	
	/**
	 * This is our previewer window
	 */
	//protected Previewer previewer = null;

	/**
	 * How far have we moved, in mm.
	 */
	protected double totalDistanceMoved = 0.0;
	
	/**
	 * What distnace did we extrude, in mm.
	 */
	protected double totalDistanceExtruded = 0.0;
	
	/**
	 * The location of the place to purge extruders
	 */
	//protected double dumpX, dumpY;
	
	/**
	 * The location of the place to go at the end
	 */
	//protected double finishX, finishY;
	
	/**
	 * Rezero X and y every...
	 */
	double xYReZeroInterval = -1;
	
	/**
	 * Distance since last zero
	 */

	double distanceFromLastZero = 0;
	
	/**
	 * Distance at last call of maybeZero
	 */
	double distanceAtLastCall = 0;
	
	/**
	 * The layer at which to turn the fan on.
	 */
	int fanLayer = -1;
	
	/**
	 * Scale for each axis in steps/mm.
	 */
	//protected double scaleX, scaleY, scaleZ;
	
	/**
	 * Current X, Y and Z position of the extruder 
	 */
	protected double currentX, currentY, currentZ;
	
	/**
	 * X, Y and Z position of the extruder at the end of the topmost layer 
	 */
	protected double topX, topY, topZ;
	
	/**
	 * Maximum feedrate for Z axis
	 */
	protected double maxFeedrateZ;
	
	/**
	* Current feedrate for the machine.
	*/
	protected double currentFeedrate;
	
	/**
	* Feedrate for fast XY moves on the machine.
	*/
	protected double fastXYFeedrate;
	
	/**
	 * The fastest the machine can accelerate in X and Y
	 */
	protected double maxXYAcceleration;
	
	/**
	 * The speed from which the machine can do a standing start
	 */
	protected double slowXYFeedrate;
	
	/**
	 * The fastest the machine can accelerate in Z
	 */
	protected double maxZAcceleration;
	
	/**
	 * The speed from which the machine can do a standing start in Z
	 */
	protected double slowZFeedrate;
	
	/**
	* Feedrate for fast Z moves on the machine.
	*/
	protected double fastFeedrateZ;
	
	/**
	 * Number of extruders on the 3D printer
	 */
	//protected int extruderCount;
	
	/**
	 * Array containing the extruders on the 3D printer
	 */
	protected Extruder extruders[];

	/**
	 * Current extruder?
	 */
	protected int extruder;
	
	/**
	 * When did we start printing?
	 */
	protected long startTime;
	
	protected double startCooling;
	
	/**
	 * Do we idle the z axis?
	 */
	protected boolean idleZ;
	
	/**
	* Do we include the z axis?
	*/
	protected boolean excludeZ = false;
	
	private int foundationLayers = 0;
	
	private boolean topDown;
	
	/**
	 * The temperature to set the bed to
	 */
	protected double bedTemperatureTarget;
	
	/**
	 * The maximum X and Y point we can move to
	 */
	protected Point2D bedNorthEast;
	
	/**
	 * Stepper motors for the 3 axis 
	 */
//	public GenericStepperMotor motorX;
//	public GenericStepperMotor motorY;
//	public GenericStepperMotor motorZ;
	
	public GenericRepRap(Stage stage) throws Exception
	{
            this.stage = stage;
            chooser = new JFileChooser();

            // Do we want just to list .stl files, or all?
            // If all, comment out the next two lines

            FileFilter filter = new ExtensionFileFilter("STL", new String[] { "STL" });
            chooser.setFileFilter(filter);
		topDown = false;
		XYEAtZero = false;
		startTime = System.currentTimeMillis();
		startCooling = -1;
		statusWindow = new StatusMessage(new JFrame());
		forceSelection = true;
		
		//load extruder prefs
		int extruderCount = 2;//Preferences.loadGlobalInt("NumberOfExtruders");
		if (extruderCount < 1)
			throw new Exception("A Reprap printer must contain at least one extruder.");

		//load our actual extruders.
		extruders = new GenericExtruder[extruderCount];
		loadExtruders();
		loadMotors();
		
		//load our prefs
		refreshPreferences();

		//init our stuff.
		currentX = 0;
		currentY = 0;
		currentZ = 0;
		currentFeedrate = 0;
	}
	
        @Override
	public void setLayerRules(LayerRules l)
	{
		layerRules = l;
	}
	
	public void loadMotors()
	{
	}
	
	public void loadExtruders() throws Exception
	{
		int pe;
		int physExCount = -1;
		
		for(int i = 0; i < extruders.length; i++)
		{
			extruders[i] = extruderFactory(i);
			
			// Make sure all instances of each physical extruder share the same
			// ExtrudedLength instance
			
			pe = extruders[i].getPhysicalExtruderNumber();
			if(pe > physExCount)
				physExCount = pe;
			for(int j = 0; j < i; j++)
			{
				if(extruders[j].getPhysicalExtruderNumber() == pe)
				{
					extruders[i].setExtrudeState(extruders[j].getExtruderState());
					break;
				}
			}
			
			extruders[i].setPrinter(this);
		}
		physicalExtruderUsed = new boolean[physExCount + 1];
		for(int i = 0; i <= physExCount; i++)
			physicalExtruderUsed[i] = false;
		extruder = 0;
	}
	
	public Extruder extruderFactory(int count)
	{
		return new NullExtruder(count, this);
	}
	
        @Override
	public void refreshPreferences()
	{
		try
		{
			//load axis prefs
			int axes = 3;
			
			xYReZeroInterval =  -1;

			// TODO This should be from calibration
			
			double xNE = 300;//Preferences.loadGlobalDouble("WorkingX(mm)");
			double yNE = 300;//Preferences.loadGlobalDouble("WorkingY(mm)");
			bedNorthEast = new Point2D(xNE, yNE);

			// Load our maximum feedrate variables
			double maxFeedrateX = 0;//Preferences.loadGlobalDouble("MaximumFeedrateX(mm/minute)");
			double maxFeedrateY = 0;//Preferences.loadGlobalDouble("MaximumFeedrateY(mm/minute)");
			maxFeedrateZ = 0;//Preferences.loadGlobalDouble("MaximumFeedrateZ(mm/minute)");
			
			maxXYAcceleration = 0;//Preferences.loadGlobalDouble("MaxXYAcceleration(mm/mininute/minute)");
			slowXYFeedrate = 0;//Preferences.loadGlobalDouble("SlowXYFeedrate(mm/minute)");
			
			maxZAcceleration = 0;//Preferences.loadGlobalDouble("MaxZAcceleration(mm/mininute/minute)");
			slowZFeedrate = 0;//Preferences.loadGlobalDouble("SlowZFeedrate(mm/minute)");
			
			//set our standard feedrates.
			fastXYFeedrate = Math.min(maxFeedrateX, maxFeedrateY);
			setFastFeedrateZ(maxFeedrateZ);
			
			idleZ = true;
			
			fanLayer = 0;//Preferences.loadGlobalInt("FanLayer");
			
			foundationLayers = 0;//Preferences.loadGlobalInt("FoundationLayers");
						
			bedTemperatureTarget = 100;//Preferences.loadGlobalDouble("BedTemperature(C)");
			int extruderCount = 1;//Preferences.loadGlobalInt("NumberOfExtruders");
			if (extruderCount < 1)
				throw new Exception("A Reprap printer must contain at least one extruder.");

			//load our actual extruders.
			extruders = new GenericExtruder[extruderCount];
			loadExtruders();
		}
		catch (Exception ex)
		{
			Debug.e("Refresh Reprap preferences: " + ex.toString());
		}
		
            for (Extruder extruder1 : extruders) {
                extruder1.refreshPreferences();
            }
		
		//Debug.refreshPreferences();
	}
	
	/**
	 * Plot rectangles round the build on layer 0 or above
	 * @param lc
	 */
	private void plotOutlines(LayerRules lc)
	{
		boolean zRight = false;
		try 
		{
			Rectangle r = lc.getBox();

			for(int e = extruders.length - 1; e >= 0; e--) // Count down so we end with the one most likely to start the rest
			{
				int pe = extruders[e].getPhysicalExtruderNumber();
				if(physicalExtruderUsed[pe])
				{
					if(!zRight)
					{
						singleMove(currentX, currentY, getExtruder().getExtrusionHeight(), getFastFeedrateZ(), true);
						currentZ = lc.getMachineZ();
					}
					zRight = true;
					selectExtruder(e, true, false, new Point2D(r.x().low(), r.y().low()));
					singleMove(r.x().low(), r.y().low(), currentZ, getExtruder().getFastXYFeedrate(), true);
					printStartDelay(true);
					getExtruder().setExtrusion(getExtruder().getExtruderSpeed(), false);
					singleMove(r.x().high(), r.y().low(), currentZ, getExtruder().getFastXYFeedrate(), true);
					singleMove(r.x().high(), r.y().high(), currentZ, getExtruder().getFastXYFeedrate(), true);
					singleMove(r.x().low(), r.y().high(), currentZ, getExtruder().getFastXYFeedrate(), true);
					singleMove(r.x().low(), r.y().low(), currentZ, getExtruder().getFastXYFeedrate(), true);
					currentX = r.x().low();
					currentY = r.y().low();
					printEndReverse();
					getExtruder().stopExtruding();
					getExtruder().setValve(false);
					r = r.offset(2*getExtruder().getExtrusionSize());
					physicalExtruderUsed[pe] = false; // Stop us doing it again
				}
			}
		} catch (RepRapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/**
	 * Despite its name, this gets called at the end...
	 * That's because the layers are computed top to
	 * bottom.
	 */
	public void startRun(LayerRules lc) throws Exception
	{		
		// plot the outline
		if(Preferences.loadGlobalBool("StartRectangle"))
			plotOutlines(lc);
	}
	
	/**
	 * Just finished a layer
	 * @param lc
	 */
	public void finishedLayer(LayerRules lc) throws Exception
	{

		double coolTime = getExtruder().getCoolingPeriod();

		startCooling = -1;

		if(coolTime > 0 && !lc.notStartedYet()) 
		{
			getExtruder().setCooler(true, lc.getReversing()); //***
			Debug.d("Start of cooling period");

			// Go home. Seek (0,0) then callibrate X first
			homeToZeroXYE(lc.getReversing()); //***
			startCooling = Timer.elapsed();
		}

	}

    /**
     * Deals with all the actions that need to be done between one layer
     * and the next.THIS FUNCTION MUST NOT MAKE THE REPRAP MACHINE DO ANYTHING (LIKE MOVE).
     * @param lc
     * @throws java.lang.Exception
     */
        @Override
	public void betweenLayers(LayerRules lc) throws Exception
	{
		// Now is a good time to garbage collect

		System.gc();
	}

	/**
	 * Just about to start the next layer
	 * @param lc
	 */
        @Override
	public void startingLayer(LayerRules lc) throws Exception
	{

		lc.setFractionDone();
		
		// Turn the fan on?
		
		if((getFanLayer() == lc.getMachineLayer()) || (lc.getMachineLayer() == 1 && getFanLayer() == 0)) // 1/0 is a bit of a hack...
			fanOn();

		// Don't home the first layer
		// The startup procedure has already done that

		if(lc.getMachineLayer() > 0 && Preferences.loadGlobalBool("InterLayerCooling"))
		{
			double liftZ = -1;
                    for (Extruder extruder : extruders) {
                        if (extruder.getLift() > liftZ) {
                            liftZ = extruder.getLift();
                        }
                    }
			double currentZ = getZ();
			if(liftZ > 0)
				singleMove(getX(), getY(), currentZ + liftZ, getFastFeedrateZ(), lc.getReversing()); //***
			homeToZeroXYE(lc.getReversing()); //***
			if(liftZ > 0)
				singleMove(getX(), getY(), currentZ, getFastFeedrateZ(), lc.getReversing()); //***				
		} else {
			getExtruder().zeroExtrudedLength(lc.getReversing());
		}

		double coolTime = getExtruder().getCoolingPeriod();

		if (layerPauseCheckbox != null && layerPauseCheckbox.isSelected())
			layerPause(); //***

		if(isCancelled())
		{
			getExtruder().setCooler(false, lc.getReversing());//***
			getExtruder().stopExtruding(); // Shouldn't be needed, but no harm//***
			return;
		}
		// Cooling period

		// How long has the fan been on?

		double cool = Timer.elapsed();
		if(startCooling >= 0)
			cool = cool - startCooling;
		else
			cool = 0;

		// Wait the remainder of the cooling period

		if(startCooling >= 0)
		{	
			cool = coolTime - cool;
			// NB - if cool is -ve machineWait will return immediately
			machineWait(1000*cool, false, lc.getReversing()); //***
		}
		// Fan off

		if(coolTime > 0)
			getExtruder().setCooler(false, lc.getReversing());

		// If we were cooling, wait for warm-up

		lc.moveZAtStartOfLayer(lc.getReversing()); //***
	}
	
	/**
	 * Go to the purge point
	 */
        @Override
	public void moveToPurge(double liftZ)
	{
		if(liftZ > 0)
			singleMove(currentX, currentY, currentZ + liftZ, getFastFeedrateZ(), true);
		Point2D p = layerRules.getPurgeMiddle();
		singleMove(p.x(), p.y(), currentZ, getExtruder().getFastXYFeedrate(), true);
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#calibrate()
	 */
        @Override
	public void calibrate() {
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#calibrate()
	 */
        @Override
	public void dispose()
	{
            for (Extruder extruder : extruders) {
                extruder.dispose();
            }
	}
	
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#selectMaterial(int)
	 */
        @Override
	public void selectExtruder(int materialIndex, boolean really, boolean update, Point2D next) throws Exception
	{
		if (isCancelled())
			return;

		if(materialIndex < 0 || materialIndex >= extruders.length)
		{
			Debug.e("Selected material (" + materialIndex + ") is out of range.");
			extruder = 0;
		} else
			extruder = materialIndex;
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#selectMaterial(int)
	 */
        @Override
	public void selectExtruder(Attributes att, Point2D next) throws Exception 
	{
		for(int i = 0; i < extruders.length; i++)
		{
			if(att.getMaterial().equals(extruders[i].getMaterial()))
			{
				selectExtruder(i, true, true, next);
				return;
			}
		}
		Debug.e("selectExtruder() - extruder not found for: " + att.getMaterial());
	}
	
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getX()
	 */
        @Override
	public double getX() {
		return currentX;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getY()
	 */
	public double getY() {
		return currentY;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getZ()
	 */
	public double getZ() {
		return currentZ;
	}
	
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getTotalDistanceMoved()
	 */
	public double getTotalDistanceMoved() {
		return totalDistanceMoved;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getTotalDistanceExtruded()
	 */
	public double getTotalDistanceExtruded() {
		return totalDistanceExtruded;
	}
	
	/**
	 * @param x
	 * @param y
	 * @return segment length in millimeters
	 */
	public double segmentLength(double x, double y) {
		return Math.sqrt(x * x + y * y);
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getTotalElapsedTime()
	 */
	public double getTotalElapsedTime() {
		long now = System.currentTimeMillis();
		return (now - startTime) / 1000.0;
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getExtruder(String)
	 */
	public Extruder getExtruder(String name)
	{
            for (Extruder extruder : extruders) {
                if (name.equals(extruder.toString())) {
                    return extruder;
                }
            }
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getExtruder()
	 */
        @Override
	public Extruder getExtruder()
	{
		//System.out.println("getExtruder(), extruder: " + extruder);
		return extruders[extruder];
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getExtruder()
	 */
        @Override
	public Extruder[] getExtruders()
	{
		return extruders;
	}
	
	/**
	 * Extrude for the given time in milliseconds, so that polymer is flowing
	 * before we try to move the extruder.  But first take up the slack from any
	 * previous reverse.
	 */
        @Override
	public void printStartDelay(boolean firstOneInLayer) 
	{
		try
		{		
			double rDelay = getExtruder().getExtruderState().retraction(); 

			if(rDelay > 0)
			{
				getExtruder().setMotor(true);
				machineWait(rDelay, true, true);
				getExtruder().getExtruderState().setRetraction(0);
			}

			// Extrude motor and valve delays (ms)

			double eDelay, vDelay;

			if(firstOneInLayer)
			{
				eDelay = getExtruder().getExtrusionDelayForLayer();
				vDelay = getExtruder().getValveDelayForLayer();
			} else
			{
				eDelay = getExtruder().getExtrusionDelayForPolygon();
				vDelay = getExtruder().getValveDelayForPolygon();			
			}


			if(eDelay >= vDelay)
			{
				getExtruder().setMotor(true);
				machineWait(eDelay - vDelay, false, true);
				getExtruder().setValve(true);
				machineWait(vDelay, false, true);
			} else
			{
				getExtruder().setValve(true);
				machineWait(vDelay - eDelay, false, true);
				getExtruder().setMotor(true);
				machineWait(eDelay, false, true);
			}
		} catch(Exception e)
		{
			// If anything goes wrong, we'll let someone else catch it.
		}
	}
	
	/**
	 * Extrude backwards for the given time in milliseconds, so that polymer is stopped flowing
	 * at the end of a track.  Return the amount reversed.
	 */
        @Override
	public double printEndReverse() 
	{
		// Extrude motor and valve delays (ms)
		double delay = getExtruder().getExtrusionReverseDelay();
		
		if(delay <= 0)
			return 0;
		
		try
		{
			getExtruder().setExtrusion(getExtruder().getExtruderSpeed(), true);
			machineWait(delay, true, true);
			getExtruder().stopExtruding();
			getExtruder().getExtruderState().setRetraction(getExtruder().getExtruderState().retraction() + delay);
		} catch (Exception e)
		{}
		try {
			return getExtruder().getExtruderState().retraction();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	private void checkCoordinates(double x, double y, double z)
	{
		try
		{
		if(x > Preferences.loadGlobalDouble("WorkingX(mm)") || x < 0)
			Debug.e("Attempt to move x to " + x + " which is outside [0, " + Preferences.loadGlobalDouble("WorkingX(mm)") + "]");
		if(y > Preferences.loadGlobalDouble("WorkingY(mm)") || y < 0)
			Debug.e("Attempt to move y to " + y + " which is outside [0, " + Preferences.loadGlobalDouble("WorkingY(mm)") + "]");
		if(z > Preferences.loadGlobalDouble("WorkingZ(mm)") || z < 0)
			Debug.e("Attempt to move z to " + z + " which is outside [0, " + Preferences.loadGlobalDouble("WorkingZ(mm)") + "]");
		} catch (IOException e)
		{}
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#moveTo(double, double, double, boolean, boolean)
	 */
	public void moveTo(double x, double y, double z, double feedRate, boolean startUp, boolean endUp) throws RepRapException, IOException, Exception
	{
		if (isCancelled()) return;
		
		checkCoordinates(x, y, z);

		totalDistanceMoved += segmentLength(x - currentX, y - currentY);
		
		//TODO - next bit needs to take account of startUp and endUp
		if (z != currentZ)
			totalDistanceMoved += Math.abs(currentZ - z);

		currentX = x;
		currentY = y;
		currentZ = z;
		XYEAtZero = false;
	}
	
	public void singleMove(double x, double y, double z, double feedrate, boolean really)
	{
		try
		{
			moveTo(x, y, z, feedrate, false, false);
		} catch (Exception e)
		{
			Debug.e(e.toString());
		}
	}
	
	/**
	 * Occasionally re-zero X and Y if that option is selected (i.e. xYReZeroInterval > 0)
	 * @throws Exception 
	 */
	protected void maybeReZero() throws Exception
	{
		if(xYReZeroInterval <= 0)
			return;
		distanceFromLastZero += totalDistanceMoved - distanceAtLastCall;
		distanceAtLastCall = totalDistanceMoved;
		if(distanceFromLastZero < xYReZeroInterval)
			return;
		distanceFromLastZero = 0;

		double oldFeedrate = getFeedrate();

		getExtruder().setValve(false);
		getExtruder().setMotor(false);
		if (!excludeZ)
		{
			double liftedZ = currentZ + (getExtruder().getMinLiftedZ());

			moveTo(currentX, currentY, liftedZ, getFastFeedrateZ(), false, false);
		}
		
		double oldX = currentX;
		double oldY = currentY;
		homeToZeroX();
		homeToZeroY();
		
		moveTo(oldX, oldY, currentZ, getExtruder().getFastXYFeedrate(), false, false);
		
		if (!excludeZ)
		{
			double liftedZ = currentZ - (getExtruder().getMinLiftedZ());

			moveTo(currentX, currentY, liftedZ, getFastFeedrateZ(), false, false);
		}

		moveTo(currentX, currentY, currentZ, oldFeedrate, false, false);
		printStartDelay(false);		
	}
	
	/**
	 * @param enable
	 * @throws Exception 
	 */
	public void setCooling(boolean enable) throws Exception {
		getExtruder().setCooler(enable, true);
	}
		
	public double getFeedrate()
	{
		return currentFeedrate;
	}

	private void setFastFeedrateZ(double feedrate)
	{
		fastFeedrateZ = feedrate;
	}

        @Override
	public double getFastFeedrateZ()
	{
		return fastFeedrateZ;
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#setZManual()
	 */
        @Override
	public void setZManual() throws IOException {
		setZManual(0.0);
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#setZManual(double)
	 */
	public void setZManual(double zeroPoint) throws IOException
	{
	}	
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#homeToZeroX()
	 */
	public void homeToZeroX() throws RepRapException, IOException, Exception{
		currentX = 0.0;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#homeToZeroY()
	 */
	public void homeToZeroY() throws RepRapException, IOException, Exception {
		currentY = 0.0;
	}
	
	public void homeToZeroZ() throws RepRapException, IOException, Exception {
		currentZ = 0.0;
	}
	
	public void homeToZeroXYE(boolean really) throws RepRapException, IOException, Exception
	{}
	
	public void home() throws Exception{
		currentX = currentY = currentZ = 0.0;
	}
	
	public int getExtruderNumber()
	{
		return extruder;
	}

	public double getMaxFeedrateZ()
	{
		return maxFeedrateZ;
	}
	
	/**
	 * Display a message indicating a segment is about to be
	 * printed and wait for the user to acknowledge
	 */
	protected void segmentPause() {
		try
		{
			getExtruder().setValve(false);
			getExtruder().setMotor(false);
		} catch (Exception ex) {}
		ContinuationMesage msg =
			new ContinuationMesage(null, "A new segment is about to be produced");
					//,segmentPauseCheckbox, layerPauseCheckbox);
		msg.setVisible(true);
		try {
			synchronized(msg) {
				msg.wait();
			}
		} catch (InterruptedException ex) {
		}
		if (msg.getResult() == false)
			setCancelled(true);
		else
		{
			try
			{
				getExtruder().setExtrusion(getExtruder().getExtruderSpeed(), false);
				getExtruder().setValve(false);
			} catch (Exception ex) {}
		}
		msg.dispose();
	}

	/**
	 * Display a message indicating a layer is about to be
	 * printed and wait for the user to acknowledge
	 */
	protected void layerPause() {
		ContinuationMesage msg =
			new ContinuationMesage(null, "A new layer is about to be produced");
					//,segmentPauseCheckbox, layerPauseCheckbox);
		msg.setVisible(true);
		try {
			synchronized(msg) {
				msg.wait();
			}
		} catch (InterruptedException ex) {
		}
		if (msg.getResult() == false)
			setCancelled(true);
		msg.dispose();
	}

	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between segments.
	 * 
	 * @param segmentPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly. 
	 */
        @Override
	public void setSegmentPause(JCheckBoxMenuItem segmentPause) {
		segmentPauseCheckbox = segmentPause;
	}

	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between layers.
	 * 
	 * @param layerPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly.
	 */
	public void setLayerPause(JCheckBoxMenuItem layerPause) {
		layerPauseCheckbox = layerPause;
	}

	public void setMessage(String message) {
		if (message == null)
			statusWindow.setVisible(false);
		else {
			statusWindow.setMessage(message);
			statusWindow.setVisible(true);
		}
	}
	
	public boolean isCancelled() {
		return statusWindow.isCancelled();
	}

	public void setCancelled(boolean isCancelled) {
		statusWindow.setCancelled(isCancelled);
	}
	
	
	public int getFoundationLayers()
	{
		return foundationLayers;
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
                /*if(extensions[0].toUpperCase().contentEquals("RFO"))
                    builder.addRFOFile(result);
                if(extensions[0].toUpperCase().contentEquals("STL"))*/
                StlFile file = new StlFile();
                try {
                    Scene scene = file.load(result);
                    Platform.runLater(() -> {
                        stage.setScene(scene);
                    });
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(GenericRepRap.class.getName()).log(Level.SEVERE, null, ex);
                }

                return f;
            }
            return null;
        }
	
	/**
	 * Load an STL file to be made.
	 * @return the name of the file
	 */
        @Override
	public String addSTLFileForMaking()
	{
		gcodeLoaded = false;		
		stlLoaded = true;
		File f = onOpen("STL triangulation file", new String[] {"stl"}, "");
		if(f == null)
			return "";
		else 
			return f.getName();
	}
	
	/**
	 * Load an RFO file to be made.
	 * @return the name of the file
	 */
        @Override
	public String loadRFOFileForMaking()
	{
		gcodeLoaded = false;		
		stlLoaded = true;
		File f = org.reprap.Main.gui.onOpen("RFO multiple-object file", new String[] {"rfo"}, "");
		if(f == null)
			return "";
		else 
			return f.getName();
	}
	
	/**
	 * Load an RFO file to be made.
         * @param filerRoot
	 * @return the name of the file
	 */
        @Override
	public String saveRFOFile(String filerRoot)
	{
		return org.reprap.Main.gui.saveRFO(filerRoot);
	}

	/**
	 * Load a GCode file to be made.
	 * @return the name of the file
	 */
        @Override
	public String loadGCodeFileForMaking()
	{
		/*if(stlLoaded)
			org.reprap.Main.gui.deleteAllSTLs();*/
		stlLoaded = false;
		gcodeLoaded = true;
		return null;
	}
	
	/**
	 * Stop the printer building.
	 * This shouldn't also stop it being controlled interactively.
	 */
        @Override
	public void pause()
	{
	}
	
        @Override
	public int getFanLayer()
	{
		return fanLayer;
	}
	
	/**
	 * Resume building.
	 */
        @Override
	public void resume()
	{
	}
	
        @Override
	public void setTopDown(boolean td)
	{
		topDown = td;
	}
	
	/**
	 * @return the flag that decided which direction to compute the layers
	 */
        @Override
	public boolean getTopDown()
	{
		return topDown;
	}
	
	/**
	 * Set all the extruders' separating mode
	 * @param s
	 */
        @Override
	public void setSeparating(boolean s)
	{
            for (Extruder extruder : extruders) {
                extruder.setSeparating(s);
            }
	}
	
	/**
	 * Get the feedrate currently being used
	 * @return
	 */
        @Override
	public double getCurrentFeedrate()
	{
		return currentFeedrate;
	}
	
	/**
	 * @return slow XY movement feedrate in mm/minute
	 */
        @Override
	public double getFastXYFeedrate()
	{
		return fastXYFeedrate;
	}
	
	/**
	 * @return slow XY movement feedrate in mm/minute
	 */
        @Override
	public double getSlowXYFeedrate()
	{
		return slowXYFeedrate;
	}
	
	/**
	 * @return the fastest the machine can accelerate
	 */
        @Override
	public double getMaxXYAcceleration()
	{
		return maxXYAcceleration;
	}
	
	/**
	 * @return slow XY movement feedrate in mm/minute
	 */
        @Override
	public double getSlowZFeedrate()
	{
		return slowZFeedrate;
	}
	
	/**
	 * @return the fastest the machine can accelerate
	 */
        @Override
	public double getMaxZAcceleration()
	{
		return maxZAcceleration;
	}
	
	/**
	 * Tell the printer class it's Z position.  Only to be used if
	 * you know what you're doing...
	 * @param z
	 */
        @Override
	public void setZ(double z)
	{
		currentZ = z;
	}

	/**
	 * Set the position at the end of the topmost layer
	 * @param x
	 * @param y
	 * @param z
	 */
        @Override
	public void setTop(double x, double y, double z)
	{
		topX = x;
		topY = y;
		topZ = z;
	}
	
	/**
	 * The location of the dump for purging extruders
	 * @return
	 */
        @Override
	public double getDumpX()
	{
		return layerRules.getPurgeMiddle().x();
	}
	
        @Override
	public double getDumpY()
	{
		return layerRules.getPurgeMiddle().y();
	}
	
	/**
	 * Set the bed temperature. This value is given
	 * in centigrade, i.e. 100 equals 100 centigrade. 
	 * @param temperature The temperature of the extruder in centigrade
	 * @throws Exception 
	 */
        @Override
	public void setBedTemperature(double temperature) throws Exception
	{
		bedTemperatureTarget = temperature;
	}
		
	/**
	 * The temperature we want the bed to be at
	 * @return
	 */
        @Override
	public double getBedTemperatureTarget()
	{
		return bedTemperatureTarget;
	}
	
        @Override
	public void forceNextExtruder()
	{
		forceSelection = true;
	}
	
	/**
	 * Return the current layer rules
	 * @return
	 */
        @Override
	public LayerRules getLayerRules()
	{
		return layerRules;
	}
	
	/**
	 * Return the diagnostic graphics window (or null if there isn't one)
	 * @return
	 */
        @Override
	public RrGraphics getGraphics()
	{
		return org.reprap.Main.gui.getGraphics();
	}
	
	/**
	 * The XY point furthest from the origin
         * @return 
	 */
        @Override
	public Point2D getBedNorthEast()
	{
		return bedNorthEast;
	}
}
