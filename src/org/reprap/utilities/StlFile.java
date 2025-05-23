package org.reprap.utilities;

import java.net.URL;
import java.net.MalformedURLException;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

// New from JDK 1.4 for endian related problems
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;

import org.jogamp.vecmath.Vector3f;
import org.reprap.Attributes;
import org.reprap.geometry.polyhedra.AllSTLsToBuild;
import org.reprap.geometry.polyhedra.AllSTLsToBuild.LineSegment;

/**
 * Title:         STL Loader
 * Description:   STL files loader (Supports ASCII and binary files) for Java3D
 *                Needs JDK 1.4 due to endian problems
 * Company:       Universidad del Pais Vasco (UPV/EHU)
 * @author:       Carlos Pedrinaci Godoy
 * @version:      1.0
 *
 * Contact : xenicp@yahoo.es
 *
 *
 * Things TO-DO:
 *    1. We can't read binary files over the net.
 *    2. For binary files if size is lower than expected (calculated with the number of faces)
 *    the program will block.
 *    3. Improve the way for detecting the kind of stl file?
 *    Can give us problems if the comment of the binary file begins by "solid"
 *    
 *    ----
 *    
 *    Modified by Adrian Bowyer to compute normals from triangles, rather than rely
 *    on the read-in values (which seem to be all wrong...).  See the function
 *     
 *    private SceneBase makeScene()
 */

public class StlFile
{
    private static double zTrans = -3500;
    private static final int camSpeed = 100;
    private static final boolean DEBUG = false;     // Sets mode to Debug: outputs every action done

    // Maximum length (in chars) of basePath
    private static final int MAX_PATH_LENGTH = 1024;

    // Global variables
    private int flag;                   // Needed cause implements Loader

    private URL baseUrl = null;         // Reading files over Internet
    private String basePath = null;     // For local files

    private boolean fromUrl = false;    // Usefull for binary files
    private boolean Ascii = true;       // File type Ascii -> true o binary -> false
    private String fileName = null;

    // Arrays with coordinates and normals
    // Needed for reading ASCII files because its size is unknown until the end
    private final ArrayList coordList = new ArrayList();		// Holds Point3f
    private final ArrayList normList  = new ArrayList();		// Holds Vector3f

    // GeometryInfo needs Arrays
    private final List<Point3D> coordArray = new ArrayList<>();
    private Vector3f[] normArray = null;

    // Needed because TRIANGLE_STRIP_ARRAY
    // As the number of strips = the number of faces it's filled in objectToVectorArray
    private int[] stripCounts = null;

    // Default = Not available
    private String objectName = "Not available";

  public static void main(String[] args)
  {
        StlFile file = new StlFile();
        String filename = "/Users/xuyi/Pictures/edf/files/edf120.stl";
        try {
            Reader reader = new BufferedReader(new FileReader(filename));
            file.setBasePathFromFilename(filename);
            file.setFileName(filename);
            StlFileParser st = new StlFileParser(reader);

            // Initialize data
            file.setAscii(true); // Default ascii
            file.readFile(st);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StlFile.class.getName()).log(Level.SEVERE, null, ex);
        }
  }

  /**
   * Method that reads the EOL
   * Needed for verifying that the file has a correct format
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readEOL(StlFileParser parser)
  {
	do {
            try {
                parser.nextToken();
            } catch (IOException e) {
                System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
            }
	} while (parser.ttype != StlFileParser.TT_EOL);
  }

  /**
   * Method that reads the word "solid" and stores the object name.
   * It also detects what kind of file it is
   * TO-DO:
   *    1. Better way control of exceptions?
   *    2. Better way to decide between Ascii and Binary?
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readSolid(StlFileParser parser)
  {
	if(parser.sval == null)
	{
            // Added by AB
            this.setAscii(false);
            return;
	}
    if( !parser.sval.equals("solid"))
    {
      //System.out.println("Expecting solid on line " + parser.lineno());
      // If the first word is not "solid" then we consider the file is binary
      // Can give us problems if the comment of the binary file begins by "solid"
      this.setAscii(false);
    } else  // It's an ASCII file
    {
    	try{
    		parser.nextToken();
    	} catch (IOException e) {
    		System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
    	}
    	if(parser.sval.equals("binary"))  // Deal with annoying CAD systems that start files with "solid binary"
    	{
    		this.setAscii(false);
    		try {
    			parser.nextToken();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
    		}
    	} else {
    		if( parser.ttype != StlFileParser.TT_WORD)
    		{
    			// Is the object name always provided???
    			System.err.println("Format Error:expecting the object name on line " + parser.lineno());
    		} else { // Store the object Name
    			this.setObjectName(parser.sval);
    			if(DEBUG)
    			{
    				System.out.println("Object Name:" + this.getObjectName());
    			}
    			this.readEOL(parser);
    		}
    	}
    }
  }//End of readSolid

  /**
   * Method that reads a normal
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readNormal(StlFileParser parser)
  {
    Vector3f v = new Vector3f();

    if(!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("normal")))
    {
      System.err.println("Format Error:expecting 'normal' on line " + parser.lineno());
    } else {
      if (parser.getNumber())
      {
        v.x = (float)parser.nval;

        if(DEBUG)
        {
          System.out.println("Normal:");
          System.out.print("X=" + v.x + " ");
        }

        if (parser.getNumber())
        {
          v.y = (float)parser.nval;
          if(DEBUG)
            System.out.print("Y=" + v.y + " ");

	  if (parser.getNumber())
          {
            v.z = (float)parser.nval;
            if(DEBUG)
              System.out.println("Z=" + v.z);

            // We add that vector to the Normal's array
            this.normList.add(v);
            this.readEOL(parser);
	  }
          else System.err.println("Format Error:expecting coordinate on line " + parser.lineno());
        }
        else System.err.println("Format Error:expecting coordinate on line " + parser.lineno());
      }
      else System.err.println("Format Error:expecting coordinate on line " + parser.lineno());
    }
  }// End of Read Normal

  /**
   * Method that reads the coordinates of a vector
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readVertex(StlFileParser parser)
  {
    float x, y, z;

    if(!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("vertex")))
    {
      System.err.println("Format Error:expecting 'vertex' on line " + parser.lineno());
    } else {
      if (parser.getNumber())
      {
        x = (float)parser.nval;

        if(DEBUG)
        {
          System.out.println("Vertex:");
          System.out.print("X=" + x + " ");
        }

        if (parser.getNumber())
        {
          y = (float)parser.nval;
          if(DEBUG)
            System.out.print("Y=" + y + " ");

	  if (parser.getNumber())
          {
	    z = (float)parser.nval;
            if(DEBUG)
              System.out.println("Z=" + z);

            // We add that vertex to the array of vertex
            coordList.add(new Point3D(x, y, z));
            readEOL(parser);
	  } else {
              System.err.println("Format Error: expecting coordinate on line " + parser.lineno());
          }
        } else {
            System.err.println("Format Error: expecting coordinate on line " + parser.lineno());
        }
      } else {
            System.err.println("Format Error: expecting coordinate on line " + parser.lineno());
        }
    }
}//End of read vertex

  /**
   * Method that reads "outer loop" and then EOL
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readLoop(StlFileParser parser)
  {
    if(!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("outer")))
    {
      System.err.println("Format Error:expecting 'outer' on line " + parser.lineno());
    } else {
        try{
            parser.nextToken();
        } catch (IOException e) {
            System.err.println("IO error on line " + parser.lineno() + ": " + e.getMessage());
        }
        if(!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("loop")))
        {
            System.err.println("Format Error:expecting 'loop' on line " + parser.lineno());
        } else {
            readEOL(parser);
        }
    }
  }//End of readLoop

  /**
   * Method that reads "endloop" then EOL
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readEndLoop(StlFileParser parser)
  {
    if(!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("endloop")))
    {
        System.err.println("Format Error:expecting 'endloop' on line " + parser.lineno());
    } else {
        readEOL(parser);
    }
  }//End of readEndLoop

  /**
   * Method that reads "endfacet" then EOL
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readEndFacet(StlFileParser parser)
  {
    if(!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("endfacet")))
    {
        System.err.println("Format Error:expecting 'endfacet' on line " + parser.lineno());
    } else {
        readEOL(parser);
    }
  }//End of readEndFacet

  /**
   * Method that reads a face of the object
   * (Cares about the format)
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readFacet(StlFileParser parser)
  {
    if(!(parser.ttype == StlFileParser.TT_WORD && parser.sval.equals("facet")))
    {
      System.err.println("Format Error:expecting 'facet' on line " + parser.lineno());
    } else {
      try{
          parser.nextToken();
          readNormal(parser);

          parser.nextToken();
          readLoop(parser);

          parser.nextToken();
          readVertex(parser);

          parser.nextToken();
          readVertex(parser);

          parser.nextToken();
          readVertex(parser);

          parser.nextToken();
          readEndLoop(parser);

          parser.nextToken();
          readEndFacet(parser);
      } catch (IOException e) {
        System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
      }
    }
  }// End of readFacet

  /**
   * Method that reads a face in binary files
   * All binary versions of the methods end by 'B'
   * As in binary files we can read the number of faces, we don't need
   * to use coordArray and normArray (reading binary files should be faster)
   *
   * @param in The ByteBuffer with the data of the object.
   * @param index The facet index
   *
   * @throws IOException
   */
  private void readFacetB(ByteBuffer in, int index) throws IOException
  {
    //File structure: Normal Vertex1 Vertex2 Vertex3
    Vector3f normal = new Vector3f();

    if(DEBUG)
      System.out.println("Reading face number " + index);

    // Read the Normal
    normArray[index] = new Vector3f();
    normArray[index].x = in.getFloat();
    normArray[index].y = in.getFloat();
    normArray[index].z = in.getFloat();

    if(DEBUG)
      System.out.println("Normal: X=" + normArray[index].x + " Y=" + normArray[index].y + " Z=" + normArray[index].z);

    // Read vertex1
    coordArray.add(new Point3D(in.getFloat(), in.getFloat(), in.getFloat()));

    if(DEBUG)
      System.out.println("Vertex 1: X=" + coordArray.get(index * 3).getX() + 
              " Y=" + coordArray.get(index * 3).getY() + 
              " Z=" + coordArray.get(index * 3).getZ());

    // Read vertex2
    coordArray.add(new Point3D(in.getFloat(), in.getFloat(), in.getFloat()));

    if(DEBUG)
      System.out.println("Vertex 2: X=" + coordArray.get(index * 3 + 1).getX() + 
              " Y=" + coordArray.get(index * 3 + 1).getY() + 
              " Z=" + coordArray.get(index * 3 + 1).getZ());

    // Read vertex3
    coordArray.add(new Point3D(in.getFloat(), in.getFloat(), in.getFloat()));

    if(DEBUG)
      System.out.println("Vertex 3: X=" + coordArray.get(index * 3 + 2).getX() + 
              " Y=" + coordArray.get(index * 3 + 2).getY() + 
              " Z=" + coordArray.get(index * 3 + 2).getZ());

  }// End of readFacetB

  /**
   * Method for reading binary files
   * Execution is completly different
   * It uses ByteBuffer for reading data and ByteOrder for retrieving the machine's endian
   * (Needs JDK 1.4)
   *
   * TO-DO:
   *  1.-Be able to read files over Internet
   *  2.-If the amount of data expected is bigger than what is on the file then
   *  the program will block forever
   *
   * @param file The name of the file
   *
   * @throws IOException
   */
  private void readBinaryFile(String file) throws IOException
  {
    FileInputStream data;                 // For reading the file
    ByteBuffer dataBuffer;                // For reading in the correct endian
    byte[] Info = new byte[80];           // Header data
    byte[] Array_number = new byte[4];    // Holds the number of faces
    byte[] Temp_Info;                     // Intermediate array

    int Number_faces; // First info (after the header) on the file

    if(DEBUG)
      System.out.println("Machine's endian: " + ByteOrder.nativeOrder());

    // Get file's name
    if(fromUrl)
    {
      // FileInputStream can only read local files!?
      System.out.println("This version doesn't support reading binary files from internet");
    } else { // It's a local file
      data = new FileInputStream(file);

      // First 80 bytes aren't important
      if(80 != data.read(Info))
      { // File is incorrect
        //System.out.println("Format Error: 80 bytes expected");
        //throw new IncorrectFormatException();
      } else { // We must first read the number of faces -> 4 bytes int
        // It depends on the endian so..

        data.read(Array_number);                      // We get the 4 bytes
        dataBuffer = ByteBuffer.wrap(Array_number);   // ByteBuffer for reading correctly the int
        dataBuffer.order(ByteOrder.nativeOrder());    // Set the right order
        Number_faces = dataBuffer.getInt();

        Temp_Info = new byte[50 * Number_faces];        // Each face has 50 bytes of data

        data.read(Temp_Info);                         // We get the rest of the file

        dataBuffer = ByteBuffer.wrap(Temp_Info);      // Now we have all the data in this ByteBuffer
        dataBuffer.order(ByteOrder.nativeOrder());

        if(DEBUG)
          System.out.println("Number of faces= " + Number_faces);

        // We can create that array directly as we know how big it's going to be
        //coordArray = new Point3D[Number_faces * 3]; // Each face has 3 vertex
        normArray = new Vector3f[Number_faces];
        stripCounts = new int[Number_faces];

        for(int i = 0; i < Number_faces; i++)
        {
          stripCounts[i] = 3;
          try
          {
            readFacetB(dataBuffer,i);
            // After each facet there are 2 bytes without information
            // In the last iteration we dont have to skip those bytes..
            if(i != Number_faces - 1)
            {
              dataBuffer.get();
              dataBuffer.get();
            }
          } catch (IOException e) {
            // Quitar
            System.out.println("Format Error: iteration number " + i);
            //throw new IncorrectFormatException();
          }
        }//End for
      }// End file reading
    }// End else
  }// End of readBinaryFile

  /**
   * Method that reads ASCII files
   * Uses StlFileParser for correct reading and format checking
   * The beggining of that method is common to binary and ASCII files
   * We try to detect what king of file it is
   *
   * TO-DO:
   *  1.- Find a best way to decide what kind of file it is
   *  2.- Is that return (first catch) the best thing to do?
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readFile(StlFileParser parser)
  {
    try{
        parser.nextToken();
    } catch (IOException e) {
        System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
        System.err.println("File seems to be empty");
        return;         // ????? Throw ?????
    }

    // Here we try to detect what kind of file it is (see readSolid)
    readSolid(parser);

    if(getAscii())
    { // Ascii file
      try
      {
        parser.nextToken();
      } catch (IOException e) {
        System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
      }

      // Read all the facets of the object
      while (parser.ttype != StlFileParser.TT_EOF && !parser.sval.equals("endsolid"))
      {
        readFacet(parser);
        try
        {
          parser.nextToken();
        }
        catch (IOException e)
        {
          System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
        }
      }// End while

      // Why are we out of the while?: EOF or endsolid
      if(parser.ttype == StlFileParser.TT_EOF){
        System.err.println("Format Error:expecting 'endsolid', line " + parser.lineno());
      } else {
        if(DEBUG)
          System.out.println("File readed");
      }
    }//End of Ascii reading
    else
    { // Binary file
      try{
        readBinaryFile(getFileName());
      }
      catch(IOException e)
      {
        System.err.println("Format Error: reading the binary file");
      }
    }// End of binary file
  }//End of readFile

  /**
   * The Stl File is loaded from the .stl file specified by
   * the filename.
   * To attach the model to your scene, call getSceneGroup() on
   * the Scene object passed back, and attach the returned
   * BranchGroup to your scene graph.  For an example, see
   * $J3D/programs/examples/ObjLoad/ObjLoad.java.
   *
   * @param filename The name of the file with the object to load
   *
   * @return Scene The scene with the object loaded.
   *
   * @throws FileNotFoundException
   */
  public Scene load(String filename) throws FileNotFoundException
  {
    setBasePathFromFilename(filename);
    setFileName(filename); // For binary files

    Reader reader = new BufferedReader(new FileReader(filename));
    return load(reader);
  } // End of load(String)

   /**
   * The Stl file is loaded off of the web.
   * To attach the model to your scene, call getSceneGroup() on
   * the Scene object passed back, and attach the returned
   * BranchGroup to your scene graph.  For an example, see
   * $J3D/programs/examples/ObjLoad/ObjLoad.java.
   *
   * @param url The url to load the onject from
   *
   * @return Scene The scene with the object loaded.
   *
   * @throws FileNotFoundException
   */
  public Scene load(URL url) throws FileNotFoundException
  {
    BufferedReader reader;

    setBaseUrlFromUrl(url);

    try {
      reader = new BufferedReader(new InputStreamReader(url.openStream()));
    } catch (IOException e) {
      throw new FileNotFoundException();
    }
    fromUrl = true;
    return load(reader);
  } // End of load(URL)

  /**
   * The Stl File is loaded from the already opened file.
   * To attach the model to your scene, call getSceneGroup() on
   * the Scene object passed back, and attach the returned
   * BranchGroup to your scene graph.  For an example, see
   * $J3D/programs/examples/ObjLoad/ObjLoad.java.
   *
   * @param reader The reader to read the object from
   *
   * @return Scene The scene with the object loaded.
   *
   * @throws FileNotFoundException
   */
  public Scene load(Reader reader) throws FileNotFoundException
  {
    // That method calls the method that loads the file for real..
    // Even if the Stl format is not complicated I've decided to use
    // a parser as in the Obj's loader included in Java3D

    StlFileParser st = new StlFileParser(reader);

    // Initialize data
    //coordList = new ArrayList();
    //normList = new ArrayList();
    setAscii(true); // Default ascii

    readFile(st);
    return makeScene();
  }

  /**
   * Method that takes the info from an ArrayList of Point3f
   * and returns a Point3f[].
   * Needed for ASCII files as we don't know the number of facets until the end
   *
   * @param inList The list to transform into Point3f[]
   *
   * @return Point3f[] The result.
   */
  private Point3D[] objectToPoint3Array(ArrayList inList)
  {
    Point3D outList[] = new Point3D[inList.size()];

    for (int i = 0 ; i < inList.size() ; i++) {
      outList[i] = (Point3D)inList.get(i);
    }
    return outList;
  } // End of objectToPoint3Array

  /**
   * Method that takes the info from an ArrayList of Vector3f
   * and returns a Vector3f[].
   * Needed for ASCII files as we don't know the number of facets until the end
   *
   * TO-DO:
   *  1.- Here we fill stripCounts...
   *      Find a better place to do it?
   *
   * @param inList The list to transform into Point3f[]
   *
   * @return Vector3f[] The result.
   */
  private Vector3f[] objectToVectorArray(ArrayList inList)
  {
    Vector3f outList[] = new Vector3f[inList.size()];

    if(DEBUG)
      System.out.println("Number of facets of the object=" + inList.size());

    // To-do
    stripCounts = new int[inList.size()];
    for (int i = 0 ; i < inList.size() ; i++) {
      outList[i] = (Vector3f)inList.get(i);
      // To-do
      stripCounts[i]=3;
    }
    return outList;
  } // End of objectToVectorArray

  /**
   * Method that creates the SceneBase with the stl file info
   *
   * @return SceneBase The scene
   */
  private Scene makeScene()
  {
        AllSTLsToBuild test = new AllSTLsToBuild();
        // Create Scene to pass back
        Group group = new Group();
        float[] points = {
            50,  0, 0, // v0 (iv0 = 0)
            45, 10, 0, // v1 (iv1 = 1)
            55, 10, 0  // v2 (iv2 = 2)
        };
        float[] texCoords = {0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f};
        int[] faces = {0, 0, 2, 2, 1, 1, 0, 0, 1, 1, 2, 2};
        int i = 0;
        for(Point3D p : coordArray){
            /*Sphere ball = new Sphere(0.5);
            ball.translateXProperty().set(p.getX());
            ball.translateYProperty().set(p.getY());
            ball.translateZProperty().set(p.getZ());
            group.getChildren().add(ball);*/
            points[i * 3]     = (float)p.getX();
            points[i * 3 + 1] = (float)p.getY();
            points[i * 3 + 2] = (float)p.getZ();
            i++;
            if(i == 3){
                LineSegment line = test.addEdge(
                        new Point3D(points[0], points[1], points[2]), 
                        new Point3D(points[3], points[4], points[5]),
                        new Point3D(points[6], points[7], points[8]),
                        14, new Attributes(null,null,null,null));
                i = 0;
                if(line != null){
                    TriangleMesh mesh = new TriangleMesh();
                    mesh.getPoints().addAll(points);
                    mesh.getTexCoords().addAll(texCoords);
                    mesh.getFaces().addAll(faces);
                    MeshView meshView = new MeshView();
                    meshView.setMesh(mesh);
                    //group.getChildren().add(meshView);
                    Sphere ball1 = new Sphere(2);
                    ball1.translateXProperty().set(line.a.x());
                    ball1.translateYProperty().set(line.a.y());
                    ball1.translateZProperty().set(0);
                    //group.getChildren().add(ball1);
                    Sphere ball2 = new Sphere(2);
                    ball2.translateXProperty().set(line.b.x());
                    ball2.translateYProperty().set(line.b.y());
                    ball2.translateZProperty().set(0);
                    //group.getChildren().add(ball2);
                    Line lines = new Line(line.a.x(), line.a.y(), line.b.x(), line.b.y()); //instantiating Line class   
                    group.getChildren().add(lines);
                }
            }
        }
        
        Scene scene = new Scene(group, 800, 600, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.GREEN);
        
    // Store the scene info on a GeometryInfo
    /*GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);

    // Convert ArrayLists to arrays: only needed if file was not binary
    if(this.Ascii)
    {
      coordArray = objectToPoint3Array(coordList);
      normArray = objectToVectorArray(normList);
    }

    gi.setCoordinates(coordArray);
    gi.setStripCounts(stripCounts); 
    NormalGenerator ng = new NormalGenerator(); // Added by AB
    ng.generateNormals(gi);			// Added by AB

    // Put geometry into Shape3d
    Shape3D shape = new Shape3D();
    shape.setGeometry(gi.getGeometryArray());
    
    group.getChildren().add(shape);
    
    scene.addNamedObject(objectName, shape);*/
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
  } // end of makeScene

  /////////////////////// Accessors and Modifiers ///////////////////////////

  public URL getBaseUrl()
  {
    return baseUrl;
  }

  /**
   * Modifier for baseUrl, if accessing internet.
   *
   * @param url The new url
   */
  public void setBaseUrl(URL url)
  {
    baseUrl = url;
  }

  private void setBaseUrlFromUrl(URL url)
  {
    StringTokenizer stok =
      new StringTokenizer(url.toString(), "/\\", true);
    int tocount = stok.countTokens() - 1;
    StringBuilder sb = new StringBuilder(MAX_PATH_LENGTH);
    for(int i = 0; i < tocount ; i++) {
	String a = stok.nextToken();
	sb.append(a);
    }
    try {
      baseUrl = new URL(sb.toString());
    }
    catch (MalformedURLException e) {
      System.err.println("Error setting base URL: " + e.getMessage());
    }
  } // End of setBaseUrlFromUrl


  public String getBasePath()
  {
    return basePath;
  }

  /**
   * Set the path where files associated with this .stl file are
   * located.
   * Only needs to be called to set it to a different directory
   * from that containing the .stl file.
   *
   * @param pathName The new Path to the file
   */
  public void setBasePath(String pathName)
  {
    basePath = pathName;
    if (basePath == null || "".equals(basePath))
	basePath = "." + java.io.File.separator;
    basePath = basePath.replace('/', java.io.File.separatorChar);
    basePath = basePath.replace('\\', java.io.File.separatorChar);
    if (!basePath.endsWith(java.io.File.separator))
	basePath = basePath + java.io.File.separator;
  } // End of setBasePath

  /*
   * Takes a file name and sets the base path to the directory
   * containing that file.
   */
  private void setBasePathFromFilename(String fileName)
  {
    // Get ready to parse the file name
    StringTokenizer stok =
      new StringTokenizer(fileName, java.io.File.separator);

    //  Get memory in which to put the path
    StringBuilder sb = new StringBuilder(MAX_PATH_LENGTH);

    // Check for initial slash
    if (fileName!= null && fileName.startsWith(java.io.File.separator))
      sb.append(java.io.File.separator);

    // Copy everything into path except the file name
    for(int i = stok.countTokens() - 1 ; i > 0 ; i--) {
      String a = stok.nextToken();
      sb.append(a);
      sb.append(java.io.File.separator);
    }
    setBasePath(sb.toString());
  } // End of setBasePathFromFilename

  public int getFlags()
  {
    return flag;
  }

  public void setFlags(int parm)
  {
    this.flag = parm;
  }

  public boolean getAscii()
  {
    return this.Ascii;
  }

  public void setAscii(boolean tipo)
  {
    this.Ascii = tipo;
  }

  public String getFileName()
  {
    return this.fileName;
  }

  public void setFileName(String filename)
  {
    this.fileName = filename;
  }

  public String getObjectName()
  {
    return this.objectName;
  }

  public void setObjectName(String name)
  {
    this.objectName = name;
  }

} // End of package stl_loader
