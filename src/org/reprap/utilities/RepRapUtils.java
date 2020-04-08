package org.reprap.utilities;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;

public class RepRapUtils 
{
	/**
	 * Copy a file from one place to another
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
	public static void copyFile(File src, File dest) throws IOException 
	{
	    if(!dest.exists()) 
	    {
	        dest.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try 
	    {
	        source = new FileInputStream(src).getChannel();
	        destination = new FileOutputStream(dest).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}

	
	/**
	 * Copy a directory tree from one place to another
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
    public static void copyTree(File src, File dest) throws IOException
    {
 
    	if(src.isDirectory())
    	{
    		//if directory doesn't exist, create it
    		if(!dest.exists())
    		{
    		   dest.mkdir();
    		   //System.out.println("Directory copied from " + src + "  to " + dest);
    		}
 
    		//list all the directory contents
    		String files[] = src.list();
 
    		for (String file : files) 
    		{
    		   //construct the src and dest file structure
    		   File srcFile = new File(src, file);
    		   File destFile = new File(dest, file);
    		   //recursive copy
    		   copyTree(srcFile,destFile);
    		}
 
    	}else
    		copyFile(src, dest);
    }
	
    /**
     * Count the number of lines in a file
     * @param f
     * @return
     * @throws IOException
     */
    public static int countLines(File f) throws IOException 
    {
    	// Thanks to Johan Nordstrom
    	FileInputStream fstream = new FileInputStream(f);
        int i;
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                i = 0;
                while (( br.readLine()) != null)
                {
                    i++;
                }
            }
		return i;
    }

}
