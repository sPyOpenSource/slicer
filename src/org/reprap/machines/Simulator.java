package org.reprap.machines;

import java.io.IOException;
import java.io.PrintStream;

import org.reprap.Extruder;
import org.reprap.devices.NullExtruder;
import org.reprap.geometry.LayerRules;
import org.reprap.utilities.Debug;

/**
 *
 */
public class Simulator extends GenericRepRap {
	
    /**
     * @throws java.lang.Exception
     */
    public Simulator() throws Exception {
        super();
    }

    @Override
    public void loadMotors()
    {

    }

    @Override
    public Extruder extruderFactory(int count)
    {
        return new NullExtruder(count, this);
    }

    public void startRun()
    {

    }

    @Override
    public boolean iAmPaused()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.reprap.Printer#terminate()
     */
    @Override
    public void terminate(LayerRules lr) throws Exception
    {
        getExtruder().setMotor(false);
        getExtruder().setValve(false);
        getExtruder().setTemperature(0, false);
    }
	
	
    @Override
    public void waitTillNotBusy() throws IOException {}
    public void finishedLayer(int layerNumber) throws Exception {}
    public void betweenLayers(int layerNumber) throws Exception{}
    public void startingLayer(int layerNumber) throws Exception {}

    @Override
    public void printTo(double x, double y, double z, double feedRate, boolean stopExtruder, boolean closeValve) 
    {
            if (isCancelled())
                    return;

            double distance = segmentLength(x - currentX, y - currentY);
            if (z != currentZ)
                    distance += Math.abs(currentZ - z);

            totalDistanceExtruded += distance;
            totalDistanceMoved += distance;
            currentX = x;
            currentY = y;
            currentZ = z;
    }
	
    @Override
    public double[] getCoordinates() throws Exception
    {
            double [] result = new double[4];
            result[0] = currentX;
            result[1] = currentY;
            result[2] = currentZ;
            result[3] = getExtruder().getExtruderState().length();

            return result;
    }

    @Override
    public double[] getZeroError() throws Exception
    {
            double [] result = new double[4];
            result[0] = 0;
            result[1] = 0;
            result[2] = 0;
            result[3] = 0;

            return result;
    }

    public void delay(long millis) {}

    //TODO: make this work normally.
    @Override
    public void stopValve() throws IOException
    {
    }
	
    //TODO: make this work normally.
    @Override
    public void stopMotor() throws IOException
    {
    }

    /**
     * All machine dwells and delays are routed via this function, rather than 
     * calling Thread.sleep - this allows them to generate the right G codes (G4) etc.The RS232/USB etc comms system doesn't use this - it sets its own delays.Here do no delay; it makes no sense for the simulation machine
     * 
     * @param milliseconds
     * @param fastExtrude
     * @param really
     */
    @Override
    public void machineWait(double milliseconds, boolean fastExtrude, boolean really)
    {
    }

    @Override
    public void waitWhileBufferNotEmpty()
    {
    }

    @Override
    public void slowBuffer()
    {
    }

    @Override
    public void speedBuffer()
    {
    }
	
    /**
     * Load a GCode file to be made.
     * @return the name of the file
     */
    @Override
    public String loadGCodeFileForMaking()
    {
            Debug.e("Simulator: attempt to load GCode file.");
            return null;
    }
    /**
     * Turn the fan on
     */
    @Override
    public void fanOn()
    {
    }

    /**
     * Turn the fan off
     */
    @Override
    public void fanOff()
    {
    }
	
    /**
     * Set an output file
     * @param fileRoot
     * @return
     */
    @Override
    public String setGCodeFileForOutput(String fileRoot)
    {
            Debug.e("Simulator: cannot generate GCode file.");
            return null;		
    }

    @Override
    public Thread filePlay()
    {
            return null;
    }

    @Override
    public void stabilise()
    {}

    @Override
    public double getBedTemperature()
    {
            return bedTemperatureTarget;
    }
	
    @Override
    public void forceOutputFile(PrintStream fos)
    {

    }

    @Override
    public String getOutputFilename() { return "RepRapSimulatorOutput"; }

    @Override
    public String[] getSDFiles() 
    {
            return new String[0];
    }

    @Override
    public boolean printSDFile(String string) {
            return true;
    }
}
