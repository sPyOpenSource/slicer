package org.reprap.comms;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Sphere;

import org.reprap.utilities.Debug;
import org.reprap.utilities.RrGraphics;
import org.reprap.utilities.ExtensionFileFilter;
import org.reprap.Preferences;
import org.reprap.geometry.LayerRules;

public class GCode
{
    // Codes for responses from the machine
    // A positive number returned is a request for that line number
    // to be resent.
    private static double zTrans = -3500;
    private static final int camSpeed = 100;
    private static final long shutDown  = -3;
    private static final long allSentOK = -1;
    private double eTemp;
    private double bTemp;
    private String[] sdFiles = new String[0];
    private double x, y, z, e;
    private RrGraphics simulationPlot = null;
    private String lastResp;
    private boolean nonRunningWarn;
    /**
     * Stop sending a file (if we are).
     */
    private boolean paused = false;

    /**
     * Not quite sure why this is needed...
     */
    private boolean alreadyReversed = false;

    /**
     * The name of the port talking to the RepRap machine
     */
    String portName;

    /**
     * Flag to tell it we've finished
     */
    private boolean exhaustBuffer = false;

    /**
    * this is for doing easy serial writes
    */
    private PrintStream serialOutStream = null;

    /**
     * this is our read handle on the serial port
     */
    private InputStream serialInStream = null;

    /**
     * The root file name for output (without ".gcode" on the end)
     */
    private String opFileName;

    private String layerFileNames;
	
    /**
     * How does the first file name in a multiple set end?
     */
    private static final String gcodeExtension = ".gcode";

    /**
     * How does the first file name in a multiple set end?
     */
    private static final String firstEnding = "_prologue";

    /**
     * How does the last file name in a multiple set end?
     */
    private static final String lastEnding = "_epilogue";

    /**
     * Flag for temporary files
     */
    private static final String tmpString = "_TeMpOrArY_";

    /**
     * This is used for file input
     */
    private BufferedReader fileInStream = null;

    /**
     * How long is our G-Code input file (if any).
     */
    private long fileInStreamLength = -1;

    /**
     * This is for file output
     */
    private PrintStream fileOutStream = null;

    /**
     * The current linenumber
     */
    private long lineNumber;
	
    /**
     * The ring buffer that stores the commands for 
     * possible resend requests.
     */
    private int head, tail;
    private static final int buflen = 10; // No too long, or pause doesn't work well
    private String[] ringBuffer;
    private long[] ringLines;
    private List<Point3D> points = new ArrayList<>();
    
    /**
     * The transmission to the RepRap machine is handled by
     * a separate thread.  These control that.
     */
    private Thread bufferThread = null;
    private int myPriority;

    /**
     * Some commands (at the moment just M105 - get temperature and M114 - get coords) generate
     * a response.  Return that as a string.
     */
    private int responsesExpected = 0;

    public static void main(String[] arg){
        try {
            GCode reader = new GCode("/Users/xuyi/Source/GCode/test.gcode");
            //reader.viewer();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GCode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public GCode(String fis) throws FileNotFoundException{
        init();
        fileInStream = new BufferedReader(new FileReader(fis));
    }
    
    public GCode()
    {
        init();
    }

    /**
     * constructor for when we definitely want to send GCodes to a known file
     * @param fos
     */
    public GCode(PrintStream fos)
    {
        init();
        fileOutStream = fos;
    }
	
    private void init()
    {
        resetReceived();
        paused = false;
        alreadyReversed = false;
        ringBuffer = new String[buflen];
        ringLines = new long[buflen];
        head = 0;
        tail = 0;
        nonRunningWarn = true;
        lineNumber = 0;
        exhaustBuffer = false;
        responsesExpected = 0;

        lastResp = "";

        portName = "/dev/ttyUSB0";

        openSerialConnection(portName);

        myPriority = Thread.currentThread().getPriority();

        bufferThread = null;
    }

    private void nonRunningWarning(String s)
    {
        nonRunningWarn = false;		
    }


    public boolean buildingFromFile()
    {
        return fileInStream != null;
    }
	
    public boolean savingToFile()
    {
        return fileOutStream != null;
    }

    /**
     * Stop the printer building.
     * This shouldn't also stop it being controlled interactively.
     */
    public void pause()
    {
        paused = true;
    }

    /**
     * Resume building.
     *
     */
    public void resume()
    {
        paused = false;
    }


    /**
     * Are we paused?
     * @return
     */
    public boolean isPaused()
    {
        return paused;
    }
	
	/**
	 * Send a GCode file to the machine if that's what we have to do, and
	 * @return true.  Otherwise return false.
	 *
	 */
	public Thread filePlay()
	{
            if(fileInStream == null)
            {
                    // Not playing a file...
                    return null;
            }

            simulationPlot = null;

            if(Preferences.simulate())
                    simulationPlot = new RrGraphics("RepRap building simulation");

            Thread playFile = new Thread() 
            {
                    @Override
                    public void run() 
                    {
                            Thread.currentThread().setName("GCode file printer");
                            String line;
                            long bytes = 0;
                            try 
                            {
                                    while ((line = fileInStream.readLine()) != null) 
                                    {
                                            bufferQueue(line);
                                            bytes += line.length();
                                            double fractionDone = (double)bytes / (double)fileInStreamLength;
                                            setFractionDone(fractionDone, -1, -1);
                                            while(paused)
                                            {
                                                    //iAmPaused = true;
                                                    //Debug.e("Waiting for pause to end.");
                                                    sleep(239);
                                            }
                                            //iAmPaused = false;
                                    }
                                    fileInStream.close();
                            } catch (Exception e) {  
                                    Debug.e("Error printing file: " + e.toString());
                            }
                    }
            };

            playFile.start();

	    return playFile;
	}
	
	public void setFractionDone(double fractionDone, int layer, int outOf)
	{
		org.reprap.gui.botConsole.BotConsoleFrame.getBotConsoleFrame().setFractionDone(fractionDone, layer, outOf);
	}
	
	/**
	 * Wrapper for Thread.sleep()
	 * @param millis
	 */
	public void sleep(int millis)
	{
		try
		{
			Thread.sleep(millis);
		} catch (InterruptedException ex)
		{}		
	}
	
	/**
	 * Between layers nothing will be queued.  Use the next two
	 * functions to slow and speed the buffer's spinning.
	 *
	 */
	public void slowBufferThread()
	{
		if(bufferThread != null)
			bufferThread.setPriority(1);
	}
	
	public void speedBufferThread()
	{
		if(bufferThread != null)		
			bufferThread.setPriority(myPriority);
	}
	
	/**
	 * Force the output stream - use with caution
	 * @param fos
	 */
	public void forceOutputFile(PrintStream fos)
	{
		fileOutStream = fos;
	}
	
	/**
	 * Compute the checksum of a GCode string.
	 * @param cmd
	 * @return
	 */
	private String checkSum(String cmd)
	{
		int cs = 0;
		for(int i = 0; i < cmd.length(); i++)
			cs = cs ^ cmd.charAt(i);
		cs &= 0xff;
		return "*" + cs;
	}
	
	private void ringAdd(long ln, String cmd)
	{
		head++;
		if(head >= ringBuffer.length)
			head = 0;
		ringBuffer[head] = cmd;
		ringLines[head] = ln;
	}
	
	private String ringGet(long ln)
	{
		int h = head;
		do
		{
			if(ringLines[h] == ln)
				return ringBuffer[h];
			h--;
			if(h < 0)
				h = ringBuffer.length - 1;
		} while(h != head);
		Debug.e("ringGet: line " + ln + " not stored");
		return "";
	}
	
	/**
	 * Send a command to the machine.  Return true if a response is expected; 
	 * false if not.
	 * @param cmd
	 * @return
	 */
	private boolean sendLine(String cmd)
	{
		int com = cmd.indexOf(';');
		if(com > 0)
			cmd = cmd.substring(0, com);
		if(com != 0)
		{
			cmd = cmd.trim();
			if(cmd.length() > 0)
			{
				ringAdd(lineNumber, cmd);
				cmd = "N" + lineNumber + " " + cmd + " ";
				cmd += checkSum(cmd);
				serialOutStream.print(cmd + "\n");				
				serialOutStream.flush();
				Debug.c("G-code: " + cmd + " dequeued and sent");
				return true;
			}
			return false;
		}
	
		Debug.c("G-code: " + cmd + " not sent");
		if(cmd.startsWith(";#!LAYER:"))
		{
			int l = Integer.parseInt(cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf("/")));
			int o = Integer.parseInt(cmd.substring(cmd.indexOf("/") + 1));
			setFractionDone(-1, l, o + 1);
		}
		return false;
	}
	
	/**
	 * Queue a command.  
	 */
	private void bufferQueue(String cmd) throws Exception
	{	
		if(simulationPlot != null)
			simulationPlot.add(cmd);
		
		if(serialOutStream == null)
		{
			nonRunningWarning("queue: \"" + cmd + "\" to");
			return;
		}
		

		if(sendLine(cmd))
		{
			long resp = waitForResponse();
			if(resp == shutDown)
			{
                            throw new Exception("The RepRap machine has flagged a hard error!");
			} else if (resp == allSentOK)
			{
                            lineNumber++;
                            return;
			} else // Must be a re-send line number
			{
                            long gotTo = lineNumber;
                            lineNumber = resp;
                            String rCmd = " ";
                            while(lineNumber <= gotTo && !rCmd.contentEquals(""))
                            {
                                rCmd = ringGet(lineNumber);
                                if(sendLine(rCmd))
                                {
                                    resp = waitForResponse();
                                    if (resp == allSentOK)
                                        return;
                                    if(resp == shutDown)
                                        throw new Exception("The RepRap machine has flagged a hard error!");
                                }
                            }
                    }
		} 
		Debug.d("bufferQueue(): did not send " + cmd);	
	}
	
	private void resetReceived()
	{
		eTemp = Double.NEGATIVE_INFINITY;
		bTemp = Double.NEGATIVE_INFINITY;
		x = Double.NEGATIVE_INFINITY;
		y = Double.NEGATIVE_INFINITY;
		z = Double.NEGATIVE_INFINITY;
		e = Double.NEGATIVE_INFINITY;
	}
	
	/**
	 * Pick out a value from the returned string
	 * @param s
	 * @param coord
	 * @return
	 */
	private double parseReturnedValue(String s, String cue)
	{
		int i = s.indexOf(cue);
		if(i < 0)
			return Double.NEGATIVE_INFINITY;
		i += cue.length();
		String ss = s.substring(i);
		int j = ss.indexOf(" ");
		if(j < 0)
			return Double.parseDouble(ss);
		else
			return Double.parseDouble(ss.substring(0, j));
	}
	
	/**
	 * Pick up a list of comma-separated names and transliterate them to lower-case
	 * This code allows file names to have spaces in, but this is ***highly*** depricated.
	 * @param s
	 * @param cue
	 * @return
	 */
	private String[] parseReturnedNames(String s, String cue)
	{
		int i = s.indexOf(cue);
		if(i < 0)
			return new String[0];
		i += cue.length();
		String ss = s.substring(i);
		int j = ss.indexOf(",}");
		if(j < 0)
			ss = ss.substring(0,ss.indexOf("}"));  // Must end in "name}"
		else
			ss = ss.substring(0,j);    // Must end in "name,}"
		String[] result = ss.split(",");
		for(i = 0; i < result.length; i++)
			result[i] = result[i].toLowerCase();
		return result;
	}
	
	/**
	 * If the machine has just returned an extruder temperature, return its value
	 * @return
	 */
	public double getETemp()
	{
		if(serialOutStream == null)
		{
			nonRunningWarning("getETemp() from ");
			return 0;
		}
		if(eTemp == Double.NEGATIVE_INFINITY)
		{
			Debug.d("GCodeReaderAndWriter.getETemp() - no value stored!");
			return 0;
		}
		return eTemp;
	}
	
	/**
	 * If the machine has just returned a bed temperature, return its value
	 * @return
	 */
	public double getBTemp()
	{
		if(serialOutStream == null)
		{
			nonRunningWarning("getBTemp() from ");
			return 0;
		}
		if(bTemp == Double.NEGATIVE_INFINITY)
		{
			Debug.d("GCodeReaderAndWriter.getBTemp() - no value stored!");
			return 0;
		}
		return bTemp;
	}
	
	public String[] getSDFileNames()
	{
		if(serialOutStream == null)
		{
			nonRunningWarning("getSDFileNames() from ");
			return sdFiles;
		}
		if(sdFiles.length <= 0)
			Debug.e("GCodeReaderAndWriter.getSDFileNames() - no value stored!");
		return sdFiles;
	}	
	
	/**
	 * If the machine has just returned an x coordinate, return its value
	 * @return
	 */
	public double getX()
	{
		if(serialOutStream == null)
		{
			nonRunningWarning("getX() from ");
			return 0;
		}
		if(x == Double.NEGATIVE_INFINITY)
		{
			Debug.e("GCodeReaderAndWriter.getX() - no value stored!");
			return 0;
		}
		return x;
	}

	/**
	 * If the machine has just returned a y coordinate, return its value
	 * @return
	 */
	public double getY()
	{
		if(serialOutStream == null)
		{
			nonRunningWarning("getY() from ");
			return 0;
		}
		if(y == Double.NEGATIVE_INFINITY)
		{
			Debug.e("GCodeReaderAndWriter.getY() - no value stored!");
			return 0;
		}
		return y;
	}

	/**
	 * If the machine has just returned a z coordinate, return its value
	 * @return
	 */
	public double getZ()
	{
		if(serialOutStream == null)
		{
			nonRunningWarning("getZ() from ");
			return 0;
		}
		if(z == Double.NEGATIVE_INFINITY)
		{
			Debug.e("GCodeReaderAndWriter.getZ() - no value stored!");
			return 0;
		}
		return z;
	}

	/**
	 * If the machine has just returned an e coordinate, return its value
	 * @return
	 */
	public double getE()
	{
		if(serialOutStream == null)
		{
			nonRunningWarning("getE() from ");
			return 0;
		}
		if(e == Double.NEGATIVE_INFINITY)
		{
			Debug.e("GCodeReaderAndWriter.getE() - no value stored!");
			return 0;
		}
		return e;
	}
	
	public String lastResponse()
	{
		return lastResp;
	}

	public String toHex(String arg) 
	{
	    return String.format("%x", new BigInteger(1, arg.getBytes(/*YOUR_CHARSET?*/)));
	}

	/**
	 * Parse the string sent back from the RepRap machine.
	 *
	 */
	private long waitForResponse()
	{
		int i;
		String resp = "";
		long result = allSentOK;
		String lns;
		resetReceived();
		boolean goAgain;
		Date timer = new Date();
		long startWait = timer.getTime();
		long timeNow;
		long increment = 2000;
		long longWait = 10 * 60 * 1000; // 10 mins...
		
		for(;;)
		{
			timeNow = timer.getTime() - startWait;
			if(timeNow > increment)
			{
				Debug.d("GCodeReaderAndWriter().waitForResponse(): waited for " + timeNow / 1000 + " seconds.");
				increment = 2 * increment;
			}
			
			if(timeNow > longWait)
			{
				Debug.e("GCodeReaderAndWriter().waitForResponse(): waited for " + timeNow / 1000 + " seconds.");
				try {
					queue("M0 ;shut RepRap down");
				} catch (Exception ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}
			}
			
			try
			{
				i = serialInStream.read();
			} catch (IOException ex) {
				i = -1;
			}

			//anything found?
			if (i >= 0)
			{
				char c = (char)i;

				//is it at the end of the line?
				if (c == '\n' || c == '\r')
				{
					goAgain = false;
					if (resp.startsWith("start") || resp.contentEquals("")) // Startup or null string...
					{
						resp = "";
						goAgain = true;
					} else if (resp.startsWith("!!")) // Horrible hard fault?
					{
						result = shutDown;
						Debug.e("GCodeWriter.waitForResponse(): RepRap hard fault!  RepRap said: " + resp);
						
					} else if (resp.startsWith("//")) // immediate DEBUG "comment" from the firmware  ( like C++ )
					{
						Debug.d("GCodeWriter.waitForResponse(): " + resp);
						resp = "";
						goAgain = true;
					} else if (resp.endsWith("\\")) // lines ending in a single backslash are considered "continued" to the next line, like "C"
					{
						goAgain = true; // but do "go again"
					} else if (resp.startsWith("rs")) // Re-send request?
					{
						lns = resp.substring(3);
						int sp = lns.indexOf(" ");
						if(sp > 0)
							lns = lns.substring(0, sp);
						result = Long.parseLong(lns);
						Debug.e("GCodeWriter.waitForResponse() - request to resend from line " + result +
								".  RepRap said: " + resp);
					} else if (!resp.startsWith("ok")) // Must be "ok" if not those - check
					{
						Debug.e("GCodeWriter.waitForResponse() - dud response from RepRap:" + resp + " (hex: " + toHex(resp) + ")");
						result = lineNumber; // Try to send the last line again
					}
					
					// Have we got temperatures and/or coordinates and/or filenames?
					
					eTemp = parseReturnedValue(resp, " T:");
					bTemp = parseReturnedValue(resp, " B:");
					sdFiles = parseReturnedNames(resp, " Files: {");
					if(resp.contains(" C:"))
					{
						x = parseReturnedValue(resp, " X:");
						y = parseReturnedValue(resp, " Y:");
						z = parseReturnedValue(resp, " Z:");
						e = parseReturnedValue(resp, " E:");
					}
					
					if(!goAgain)
					{
						Debug.c("Response: " + resp);
						lastResp = resp.substring(2);
						return result;
					}
				} else
					resp += c;
			}
		}
	}
		
	/**
	 * Send a G-code command to the machine or into a file.
	 * @param cmd
         * @throws java.lang.Exception
	 */
	public void queue(String cmd) throws Exception
	{
		//trim it and cleanup.
		cmd = cmd.trim();
		cmd = cmd.replaceAll("  ", " ");
		
		if(fileOutStream != null)
		{
			String g = Debug.g();
			if(g != null)
				cmd += "; " + g;
			fileOutStream.println(cmd);
			Debug.c("G-code: " + cmd + " written to file");
		} else
			bufferQueue(cmd);
	}
	
	/**
	 * Copy a file of G Codes straight to output - generally used for canned cycles
         * @param fileName
	 */
	public void copyFile(String fileName)
	{
		File f = new File(fileName);
		if (!f.exists()) 
		{
			Debug.e("GCodeReaderAndWriter().copyFile: can't find file " + fileName);
			return;
		}
		try 
		{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String s;
			while((s = br.readLine()) != null)
			{
				queue(s);
			}
			fr.close();
		} catch (Exception ex) 
		{
			Debug.e("GCodeReaderAndWriter().copyFile: exception reading file " + fileName);
		}
	}
	

	private void openSerialConnection(String portName)
	{
	}
	
	public String loadGCodeFileForMaking()
	{
            JFileChooser chooser = new JFileChooser();
            FileFilter filter;
            filter = new ExtensionFileFilter("G Code file to be read", new String[] { "gcode" });
            chooser.setFileFilter(filter);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION)
            {
                String name = chooser.getSelectedFile().getAbsolutePath();
                try
                {
                    Debug.d("opening: " + name);
                    fileInStreamLength = chooser.getSelectedFile().length();
                    fileInStream = new BufferedReader(new FileReader(chooser.getSelectedFile()));
                    return chooser.getSelectedFile().getName();
                } catch (FileNotFoundException ex) {
                    Debug.e("Can't read file " + name);
                    fileInStream = null;
                    return null;
                }
            } else {
                Debug.e("Can't read file.");
                fileInStream = null;
            }

            return null;
	}
	
	public String setGCodeFileForOutput(boolean topDown, String fileRoot)
	{
		File defaultFile = new File(fileRoot + ".gcode");
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(defaultFile);
		FileFilter filter;
		filter = new ExtensionFileFilter("G Code file to write to", new String[] { "gcode" });
		chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		opFileName = null;
		int result = chooser.showSaveDialog(null);
		if (result == JFileChooser.APPROVE_OPTION)
		{
			opFileName = chooser.getSelectedFile().getAbsolutePath();
			if(opFileName.endsWith(gcodeExtension))
				opFileName = opFileName.substring(0, opFileName.length() - 6);

			try
			{
				boolean doe = false;
				String fn = opFileName;
				if(topDown)
				{
					fn += firstEnding;
					fn += tmpString;
					doe = true;
				}
				fn += gcodeExtension;
				
				Debug.d("opening: " + fn);
				File fl = new File(fn);
				if(doe) fl.deleteOnExit();
				FileOutputStream fileStream = new FileOutputStream(fl);
				fileOutStream = new PrintStream(fileStream);
				String shortName = chooser.getSelectedFile().getName();
				if(!shortName.endsWith(gcodeExtension))
					shortName += gcodeExtension;
				layerFileNames = System.getProperty("java.io.tmpdir") + File.separator + shortName;
				File rfod = new File(layerFileNames);
				if(!rfod.mkdir())
					throw new RuntimeException(layerFileNames);
				rfod.deleteOnExit();
				layerFileNames += File.separator;
				return shortName;
			} catch (FileNotFoundException ex) 
			{
				Debug.e("Can't write to file '" + opFileName);
				opFileName = null;
				fileOutStream = null;
			}
		} else {
			fileOutStream = null;
		}
		return null;
	}
	
	/**
	 * Start the production run
	 * (as opposed to driving the machine interactively).
	 */
	public void startRun()
	{
		if(fileOutStream != null)
		{
			// Exhause buffer before we start.
			if(bufferThread != null)
			{
				exhaustBuffer = true;
				while(exhaustBuffer) sleep(200);
			}
			bufferThread = null;
			head = 0;
			tail = 0;
		}
	}
	
	public void startingLayer(LayerRules lc)
	{
		
		lc.setLayerFileName(layerFileNames + "reprap" + lc.getMachineLayer() + tmpString + gcodeExtension);
		if(!lc.getReversing())
		try
		{
			File fl = new File(lc.getLayerFileName());
			fl.deleteOnExit();
			FileOutputStream fileStream = new FileOutputStream(fl);
			fileOutStream = new PrintStream(fileStream);
		} catch (FileNotFoundException ex)
		{
			Debug.e("Can't write to file " + lc.getLayerFileName());
		}
	}
	
	public void finishedLayer(LayerRules lc)
	{
		if(!lc.getReversing())
		{
			fileOutStream.close();
		}
	}
	
	public void startingEpilogue(LayerRules lc)
	{
	}
	
	/**
	 * All done.
	 *
         * @param lc
	 */
	public void finish(LayerRules lc)
	{
		
		Debug.d("disposing of GCodeReaderAndWriter.");

		try
		{
			if (serialInStream != null)
				serialInStream.close();

			if (serialOutStream != null)
				serialOutStream.close();

			if (fileInStream != null)
				fileInStream.close();

		} catch (IOException ex) {}

	}
	
	public String getOutputFilename()
	{
		return opFileName + gcodeExtension;
	}

    public Scene buildScene() {
        Group root = new Group();
        String line;
        try {
            while ((line = fileInStream.readLine()) != null)
            {
                if(line.startsWith("G0 ")){
                    double x = 0, y = 0, z = 0;
                    for(String command:line.split(" ")){
                        if(command.startsWith("X")){
                            x = Double.parseDouble(command.substring(1));
                        }
                        if(command.startsWith("Y")){
                            y = Double.parseDouble(command.substring(1));
                        }
                        if(command.startsWith("Z")){
                            z = Double.parseDouble(command.substring(1));
                        }
                    }
                    Point3D point = new Point3D(x,y,z);
                    points.add(point);
                }
                if(line.startsWith("G1 ")){
                    double x = 0, y = 0, z = 0;
                    line = line.split(";")[0];
                    for(String command:line.split(" ")){
                        if(command.startsWith("X")){
                            x = Double.parseDouble(command.substring(1));
                        }
                        if(command.startsWith("Y")){
                            y = Double.parseDouble(command.substring(1));
                        }
                        if(command.startsWith("Z")){
                            z = Double.parseDouble(command.substring(1));
                        }
                    }
                    Point3D point = new Point3D(x,y,z);
                    points.add(point);
                }
                System.out.println(line);
            }
            for(Point3D p : points){
                Sphere ball = new Sphere(5);
                ball.translateXProperty().set(p.getX() * 50);
                ball.translateYProperty().set(p.getY() * 50);
                ball.translateZProperty().set(p.getZ() * 50);
                root.getChildren().add(ball);
            }
            Scene scene = new Scene(root, 800, 600, true, SceneAntialiasing.BALANCED);
            scene.setFill(Color.GREEN);
            Camera camera = new PerspectiveCamera(true);
            camera.setFarClip(Integer.MAX_VALUE);
            camera.setNearClip(0.1);
            scene.setCamera(camera);
            scene.setOnScroll((ScrollEvent event) -> {
                zTrans += event.getDeltaY() * (zTrans / -50);
            });
            scene.setOnKeyPressed((KeyEvent event) -> {
                switch (event.getCode()) {
                    case RIGHT:
                        scene.getCamera().setTranslateX(camera.getTranslateX() + camSpeed);
                        break;
                    case LEFT:
                        scene.getCamera().setTranslateX(camera.getTranslateX() - camSpeed);
                        break;
                    case UP:
                        scene.getCamera().setTranslateY(camera.getTranslateY() - camSpeed);
                        break;
                    case DOWN:
                        scene.getCamera().setTranslateY(camera.getTranslateY() + camSpeed);
                        break;
                    case W:
                        scene.getCamera().setRotationAxis(new Point3D(1, 0, 0));
                        scene.getCamera().setRotate(scene.getCamera().getRotate() + 2);
                        break;
                    case S:
                        scene.getCamera().setRotationAxis(new Point3D(1, 0, 0));
                        scene.getCamera().setRotate(scene.getCamera().getRotate() - 2);
                        break;
                    case Q:
                        scene.getCamera().setRotationAxis(new Point3D(0, 0, 1));
                        scene.getCamera().setRotate(scene.getCamera().getRotate() + 2);
                        break;
                    case E:
                        scene.getCamera().setRotationAxis(new Point3D(0, 0, 1));
                        scene.getCamera().setRotate(scene.getCamera().getRotate() - 2);
                        break;
                    case D:
                        scene.getCamera().setRotationAxis(new Point3D(0, 1, 0));
                        scene.getCamera().setRotate(scene.getCamera().getRotate() + 2);
                        break;
                    case A:
                        scene.getCamera().setRotationAxis(new Point3D(0, 1, 0));
                        scene.getCamera().setRotate(scene.getCamera().getRotate() - 2);
                        break;
                }
            });
            new javafx.animation.AnimationTimer() {
                @Override
                public void handle(long now) {
                    scene.getCamera().setTranslateZ(zTrans);
                }
            }.start();
            return scene;
        } catch (IOException ex) {
            Logger.getLogger(GCode.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
