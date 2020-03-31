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
 
Wrapper class for STL objects that allows them easily to be moved about
by the mouse.  The STL object itself is a Shape3D loaded by the STL loader.

First version 14 April 2006
This version: 14 April 2006
 
 */

package org.reprap.geometry.polyhedra;

import org.jogamp.java3d.loaders.IncorrectFormatException;
import org.jogamp.java3d.loaders.ParsingErrorException;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.BoundingBox;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.SceneGraphObject;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.AxisAngle4d;
import org.jogamp.vecmath.Matrix3d;
import org.jogamp.vecmath.Matrix4d;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Tuple3d;
import org.jogamp.vecmath.Vector3d;

import org.jogamp.java3d.loaders.Scene;
import org.jogamp.java3d.utils.picking.PickTool;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reprap.utilities.StlFile;
import org.reprap.Attributes;
import org.reprap.Preferences;
import org.reprap.gui.MouseObject;
import org.reprap.utilities.Debug;

/**
 * Class for holding a group (maybe just 1) of 3D objects for RepRap to make.
 * They can be moved around on the build platform en mass, but not moved
 * relative to each other, so they can represent an assembly made from several
 * different materials.
 * 
 * @author adrian
 * 
 */

public class STLObject
{
	
	/**
	 * Little class to hold offsets of loaded STL objects
	 */
	class Offsets
	{
		private Vector3d centreToOrigin;
		private Vector3d bottomLeftShift;
	}
	
	/**
	 * Little class to hold tripples of the parts of this STLObject loaded.
	 *
	 */
	class Contents
	{
	    private String sourceFile = null;   // The STL file I was loaded from
	    private BranchGroup stl = null;     // The actual STL geometry
	    private CSG3D csg = null;           // CSG if available
	    private Attributes att = null;		// The attributes associated with it
	    private final double volume;				// Useful to know
	    private int unique = 0;
	    
	    Contents(String s, BranchGroup st, CSG3D c, Attributes a, double v)
	    {
	    	sourceFile = s;
	    	stl = st;
	    	csg = c;
	    	att = a;
	    	volume = v;
	    }
	    
	    void setUnique(int i)
	    {
	    	unique = i;
	    }
	    
	    int getUnique()
	    {
	    	return unique;
	    }
	}
	
    private MouseObject mouse = null;   // The mouse, if it is controlling us
    private BranchGroup top = null;     // The thing that links us to the world
    private BranchGroup handle = null;  // Internal handle for the mouse to grab
    private TransformGroup trans = null;// Static transform for when the mouse is away
    private BranchGroup stl = null;     // The actual STL geometry; a tree duplicated flat in the list contents
    private Vector3d extent = null;     // X, Y and Z extent
    private BoundingBox bbox = null;    // Temporary storage for the bounding box while loading
    private Vector3d rootOffset = null; // Offset of the first-loaded STL under stl
    private ArrayList<Contents> contents = null;

    public STLObject()
    {
    	stl = new BranchGroup();
    	
    	contents = new ArrayList<>();
    	
        
        // No mouse yet
        
        mouse = null;
        
        // Set up our bit of the scene graph
        
        top = new BranchGroup();
        handle = new BranchGroup();
        trans = new TransformGroup();
        
        top.setCapability(BranchGroup.ALLOW_DETACH);
        top.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        top.setCapability(Group.ALLOW_CHILDREN_WRITE);
        top.setCapability(Group.ALLOW_CHILDREN_READ);
        top.setCapability(Node.ALLOW_AUTO_COMPUTE_BOUNDS_READ);
        top.setCapability(Node.ALLOW_BOUNDS_READ);
        
        handle.setCapability(BranchGroup.ALLOW_DETACH);
        handle.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        handle.setCapability(Group.ALLOW_CHILDREN_WRITE);
        handle.setCapability(Group.ALLOW_CHILDREN_READ);
        
        trans.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        trans.setCapability(Group.ALLOW_CHILDREN_WRITE);
        trans.setCapability(Group.ALLOW_CHILDREN_READ);
        trans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        trans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        
        stl.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        stl.setCapability(Group.ALLOW_CHILDREN_WRITE);
        stl.setCapability(Group.ALLOW_CHILDREN_READ);
        stl.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        stl.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        
        trans.addChild(stl);
        handle.addChild(trans);
        top.addChild(handle);
        
        Attributes nullAtt = new Attributes(null, this, null, null);
        top.setUserData(nullAtt);
        handle.setUserData(nullAtt);
        trans.setUserData(nullAtt);
        stl.setUserData(nullAtt);
        
        bbox = null;
    }
    
    /**
     * Load an STL object from a file with a known offset (set that null to put
     * the object in the middle of the bed) and set its appearance
     * 
     * @param location
     * @param offset
     * @param app
     * @param lastPicked
     * @return 
     */
    public Attributes addSTL(String location, Vector3d offset, Appearance app, STLObject lastPicked) 
    {
    	Attributes att = new Attributes(null, this, null, app);
    	Contents child = loadSingleSTL(location, att, offset, lastPicked);
    	if(child == null)
    		return null;
    	if(lastPicked == null)
    		contents.add(child);
    	else
    		lastPicked.contents.add(child);
    	return att;
    }
 

    /**
     * Actually load the stl file and set its attributes.  Offset decides where to put it relative to 
     * the origin.  If lastPicked is null, the file is loaded as a new independent STLObject; if not
     * it is added to lastPicked and subsequently is subjected to all the same transforms, so they retain
     * their relative positions.  This is how multi-material objects are loaded.
     * 
     * @param location
     * @param att
     * @param offset
     * @param lastPicked
     * @return
     */
    private Contents loadSingleSTL(String location, Attributes att, Vector3d offset, STLObject lastPicked)
    {
    	BranchGroup bgResult = null;
    	CSG3D csgResult = null;
    	
    	//STLLoader loader = new STLLoader();
    	StlFile loader = new StlFile();
    	
        Scene scene;
        double volume = 0;
        try 
        {
        	
        	//location=location.substring(5);
        	//System.out.println(location);
            scene = loader.load(location);
        	CSGReader csgr = new CSGReader(location);
        	if(csgr.csgAvailable())
        		csgResult = csgr.csg();
            if (scene != null) 
            {
                bgResult = scene.getSceneGroup();
                bgResult.setCapability(Node.ALLOW_BOUNDS_READ);
                bgResult.setCapability(Group.ALLOW_CHILDREN_READ);
                bgResult.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
                
                // Recursively add its attribute
                
                Hashtable<?,?> namedObjects = scene.getNamedObjects( );
                java.util.Enumeration<?> enumValues = namedObjects.elements( );
                
                if( enumValues != null ) 
                {
                    while(enumValues.hasMoreElements( )) 
                    {
                    	Object tt = enumValues.nextElement();
                    	if(tt instanceof Shape3D)
                    	{
                    		Shape3D value = (Shape3D)tt;
                    		volume += s3dVolume(value);
                    		bbox = (BoundingBox)value.getBounds();

                    		value.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE );
                    		GeometryArray g = (GeometryArray)value.getGeometry();
                    		g.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

                    		recursiveSetUserData(value, att);
                    	}
                    }
                }
                
                att.setPart(bgResult);
                bgResult.setUserData(att);
                Offsets off;
                if(lastPicked != null)
                {
                	// Add this object to lastPicked
                	csgResult = setOffset(bgResult, csgResult, lastPicked.rootOffset);
                	lastPicked.stl.addChild(bgResult);
                	lastPicked.setAppearance(lastPicked.getAppearance());
                	lastPicked.updateBox(bbox);
                } else
                {
                	// New independent object.
                	stl.addChild(bgResult);
                	off = getOffsets(bgResult, offset);
                	rootOffset = off.centreToOrigin;
                	csgResult = setOffset(stl, csgResult, rootOffset);
                	Transform3D temp_t = new Transform3D();
                    temp_t.set(off.bottomLeftShift);
                	trans.setTransform(temp_t);
                	restoreAppearance();
                }
            } 

        } catch ( IncorrectFormatException | ParsingErrorException | FileNotFoundException e ) 
        {
            Debug.e("loadSingelSTL(): Exception loading STL file from: " 
                    + location);
            Logger.getLogger(STLObject.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return new Contents(location, bgResult, csgResult, att, volume);
    }
    
    private void updateBox(BoundingBox bb)
    {
        org.jogamp.vecmath.Point3d pNew = new org.jogamp.vecmath.Point3d();
        org.jogamp.vecmath.Point3d pOld = new org.jogamp.vecmath.Point3d();
        bb.getLower(pNew);
        bbox.getLower(pOld);
        if(pNew.x < pOld.x)
        	pOld.x = pNew.x;
        if(pNew.y < pOld.y)
        	pOld.y = pNew.y;
        if(pNew.z < pOld.z)
        	pOld.z = pNew.z;
        bbox.setLower(pOld);
        extent = new Vector3d();
        extent.x = pOld.x;
        extent.y = pOld.y;
        extent.z = pOld.z;       

        bb.getUpper(pNew);
        bbox.getUpper(pOld);
        if(pNew.x > pOld.x)
        	pOld.x = pNew.x;
        if(pNew.y > pOld.y)
        	pOld.y = pNew.y;
        if(pNew.z > pOld.z)
        	pOld.z = pNew.z;
        bbox.setUpper(pOld);
        
        extent.x = pOld.x - extent.x;
        extent.y = pOld.y - extent.y;
        extent.z = pOld.z - extent.z;
        
    }
    
    public BranchGroup top()
    {
    	return top;
    }
    
    public TransformGroup trans()
    {
    	return trans;
    }
    
    public BranchGroup handle()
    {
    	return handle;
    }
    
    public Vector3d extent()
    {
    	return new Vector3d(extent);
    }
    
    public String fileAndDirectioryItCameFrom(int i)
    {
    	return contents.get(i).sourceFile;
    }
    
    public String fileItCameFrom(int i)
    {
    	String fn = fileAndDirectioryItCameFrom(i);
		int sepIndex = fn.lastIndexOf(File.separator);
		fn = fn.substring(sepIndex + 1, fn.length());
    	return fn;
    }
    
    public String toSCAD()
    {
    	String result = " multmatrix(m = [ [";
    	Transform3D t1 = new Transform3D();
    	Transform3D t2 = new Transform3D();
    	trans.getTransform(t1);
    	t2.set(1.0, rootOffset);
    	t1.mul(t2);
    	Matrix4d m = new Matrix4d();
    	t1.get(m);
    	result += new BigDecimal(Double.toString(m.m00)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m01)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m02)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m03)).toPlainString() + "], \n   [";
    	
    	result += new BigDecimal(Double.toString(m.m10)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m11)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m12)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m13)).toPlainString() + "], \n   [";
    	
    	result += new BigDecimal(Double.toString(m.m20)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m21)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m22)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m23)).toPlainString() + "], \n   [";
    	
    	result += new BigDecimal(Double.toString(m.m30)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m31)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m32)).toPlainString() + ", ";
    	result += new BigDecimal(Double.toString(m.m33)).toPlainString() + "]]) \n   {\n";
    	
    	for(int i = 0; i < contents.size(); i++)
    	{
    		result += "      import_stl(\"";
    		result += fileItCameFrom(i) + "\", convexity = 10);\n";
    	}
    	result += "   }\n";
    	
    	return result;
    }
    
    public Attributes attributes(int i)
    {
    	return contents.get(i).att;
    }
    
    public BranchGroup branchGroup(int i)
    {
    	return contents.get(i).stl;
    } 
    
    public int size()
    {
    	return contents.size();
    }
    
    public void setUnique(int i, int v)
    {
    	contents.get(i).setUnique(v);
    }
    
    public int getUnique(int i)
    {
    	return contents.get(i).getUnique();
    }
    
    
    
    /**
     * Find how to move the object by actually changing all its coordinates (i.e. don't just add a
     * transform).  Also record its size.
     * @param child
     * @param offset
     */
    private Offsets getOffsets(BranchGroup child, Vector3d userOffset) 
    {
    	Offsets result = new Offsets();
    	Vector3d offset = null;
    	if(userOffset != null)
    		offset = new Vector3d(userOffset);
    	
    	if(child != null && bbox != null)
    	{
            org.jogamp.vecmath.Point3d p0 = new org.jogamp.vecmath.Point3d();
            org.jogamp.vecmath.Point3d p1 = new org.jogamp.vecmath.Point3d();
            bbox.getLower(p0);
            bbox.getUpper(p1);
            
            // If no offset requested, set it to bottom-left-at-origin
            

            	if(offset == null) 
            	{
            		offset = new Vector3d();
            		offset.x = -p0.x;
            		offset.y = -p0.y;
            		offset.z = -p0.z;
            	} 
 
            // How big?
            
            extent = new Vector3d(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z);
            
            // Position us centre at origin:
            
            offset = add(offset, neg(scale(extent, 0.5)));
            
            // Recursively apply that.  N.B. we do not apply a transform to the
            // loaded object; we actually shift all its points to put it in this
            // standard place.
            
            //setOffset(offset);
            
            result.centreToOrigin = offset;
            
            //System.out.println("centreToOrigin = " + offset.toString());

            // Now shift us to have bottom left at origin using our transform.
            
            //Transform3D temp_t = new Transform3D();
            //temp_t.set(scale(size, 0.5));
            result.bottomLeftShift = scale(extent, 0.5);
            //System.out.println("half-size = " + result.bottomLeftShift.toString());
            //trans.setTransform(temp_t);
            
            //restoreAppearance();
            
        } else{
            Debug.e("applyOffset(): no bounding box or child.");
        }
    	return result;
    }
    
    
    /**
     * Make an STL object from an existing BranchGroup
     * @param s
     * @param n
     */
    public STLObject(BranchGroup s, String n) 
    {
    	this();
  
        stl.addChild(s);
        extent = new Vector3d(1, 1, 1);  // Should never be needed.
        
        Transform3D temp_t = new Transform3D();
        trans.setTransform(temp_t); 
    }


    /**
     * method to recursively set the user data for objects in the scenegraph tree
     * we also set the capabilites on Shape3D objects required by the PickTool
     * @param value
     * @param me
     */
    private void recursiveSetUserData(Object value, Object me) 
    {
        if( value instanceof SceneGraphObject  ) 
        {
            // set the user data for the item
            SceneGraphObject sg = (SceneGraphObject) value;
            sg.setUserData( me );
            
            // recursively process group
            if( sg instanceof Group ) 
            {
                Group g = (Group) sg;
                
                // recurse on child nodes
                Iterator<Node> enumKids = g.getAllChildren( );
                
                while(enumKids.hasNext( ))
                    recursiveSetUserData( enumKids.next( ), me );
            } else if ( sg instanceof Shape3D ) 
            {
                ((Shape3D)sg).setUserData(me);
                PickTool.setCapabilities( (Node) sg, PickTool.INTERSECT_FULL );
            }
        }
    }
    
    // Move the object by p permanently (i.e. don't just apply a transform).
    
    private void recursiveSetOffset(Object value, Vector3d p) 
    {
        if( value instanceof SceneGraphObject != false ) 
        {
            // set the user data for the item
            SceneGraphObject sg = (SceneGraphObject) value;
            
            // recursively process group
            if( sg instanceof Group ) 
            {
                Group g = (Group) sg;
                
                // recurse on child nodes
                Iterator<Node> enumKids = g.getAllChildren( );
                
                while(enumKids.hasNext( ))
                    recursiveSetOffset( enumKids.next( ), p );
            } else if (sg instanceof Shape3D) 
            {
                    s3dOffset((Shape3D)sg, p);
            }
        }
    }
    
    private CSG3D setOffset(BranchGroup bg, CSG3D c, Vector3d p)
    {
    	recursiveSetOffset(bg, p);
    	if(c == null)
    		return null;
    	Matrix4d m = new Matrix4d();
    	m.setIdentity();
    	m.m03 = p.x;
    	m.m13 = p.y;
    	m.m23 = p.z;
    	return c.transform(m);
    }
    
    /**
     * Move this object elsewhere
     * @param p
     */
    public void hardTranslate(Vector3d p)
    {
    	for(int i = 0; i < contents.size(); i++)
    	{
    		Contents c = contents.get(i);
    		recursiveSetOffset(c.stl, p);
    		if(c.csg != null)
    		{
    			Matrix4d m = new Matrix4d();
    			m.setIdentity();
    			m.m03 = p.x;
    			m.m13 = p.y;
    			m.m23 = p.z;
    			c.csg = c.csg.transform(m);
    		}
    	}
    }
    
    /**
     * Soft translation
     * @param p
     */
    public void translate(Vector3d p)
    {
		Transform3D t3d1 = getTransform();
		Transform3D t3d2 = new Transform3D();
		t3d2.set(p);
		t3d1.mul(t3d2);
		setTransform(t3d1);
    }
    
    // Shift a Shape3D permanently by p
    
    private void s3dOffset(Shape3D shape, Tuple3d p)
    {
        GeometryArray g = (GeometryArray)shape.getGeometry();
        Point3d p3d = new Point3d();
        if(g != null)
        {
            for(int i = 0; i < g.getVertexCount(); i++) 
            {
                g.getCoordinate(i, p3d);
                p3d.add(p);
                g.setCoordinate(i, p3d);
            }
        }
    }
    
    // Scale the object by s permanently (i.e. don't just apply a transform).
    
    private void recursiveSetScale(Object value, double x, double y, double z, boolean zOnly) 
    {
        if( value instanceof SceneGraphObject != false ) 
        {
            // set the user data for the item
            SceneGraphObject sg = (SceneGraphObject) value;
            
            // recursively process group
            if( sg instanceof Group ) 
            {
                Group g = (Group) sg;
                
                // recurse on child nodes
                Iterator<Node> enumKids = g.getAllChildren( );
                
                while(enumKids.hasNext( ))
                    recursiveSetScale( enumKids.next( ), x, y, z, zOnly );
            } else if ( sg instanceof Shape3D ) 
            {
                    s3dScale((Shape3D)sg, x, y, z, zOnly);
            }
        }
    }
    
   // Scale a Shape3D permanently by s
    
    private void s3dScale(Shape3D shape, double x, double y, double z, boolean zOnly)
    {
        GeometryArray g = (GeometryArray)shape.getGeometry();
        Point3d p3d = new Point3d();
        if(g != null)
        {
            for(int i = 0; i < g.getVertexCount(); i++) 
            {
                g.getCoordinate(i, p3d);
                if(zOnly)
                	p3d.z = z*p3d.z;
                else
                {
                	p3d.x *= x;
                	p3d.y *= y;
                	p3d.z *= z;
                }
                g.setCoordinate(i, p3d);
            }
        }
    }
    


    // Set my transform
    
    public void setTransform(Transform3D t3d)
    {
        trans.setTransform(t3d);
    }
    
    // Get my transform
    
    public Transform3D getTransform()
    {
    	Transform3D result = new Transform3D();
        trans.getTransform(result);
        return result;
    }
    
    // Get one of the the actual objects
    
    public BranchGroup getSTL()
    {
    	return stl;
    }
    
    public BranchGroup getSTL(int i)
    {
    	return contents.get(i).stl;
    }
    
    public int getCount()
    {
    	return contents.size();
    }
    
    public CSG3D getCSG(int i)
    {
    	return contents.get(i).csg;
    }
    
    // Get the number of objects
    
    public int numChildren()
    {
    	return stl.numChildren();
    }
    
    // The mouse calls this to tell us it is controlling us
    
    public void setMouse(MouseObject m)
    {
        mouse = m;
    }
    
    
    // Change colour etc. - recursive private call to walk the tree
    
    private static void setAppearance_r(Object gp, Appearance a) 
    {
        if( gp instanceof Group ) 
        {
            Group g = (Group) gp;
            
            // recurse on child nodes
            Iterator<Node> enumKids = g.getAllChildren( );
            
            while(enumKids.hasNext( )) 
            {
                Object child = enumKids.next( );
                if(child instanceof Shape3D) 
                {
                    Shape3D lf = (Shape3D) child;
                    lf.setAppearance(a);
                } else
                    setAppearance_r(child, a);
            }
        }
    }
    
    // Change colour etc. - call the internal fn to do the work.
    
    public void setAppearance(Appearance a)
    {
        setAppearance_r(stl, a);     
    }
    
    /**
     * dig down to find our appearance
     * @param gp
     * @return
     */
    private static Appearance getAppearance_r(Object gp) 
    {
        if( gp instanceof Group ) 
        {
            Group g = (Group) gp;
            
            // recurse on child nodes
            Iterator<Node> enumKids = g.getAllChildren( );
            
            while(enumKids.hasNext( )) 
            {
                Object child = enumKids.next( );
                if(child instanceof Shape3D) 
                {
                    Shape3D lf = (Shape3D) child;
                    return lf.getAppearance();
                } else
                    return getAppearance_r(child);
            }
        }
        return new Appearance();
    }
    
    public Appearance getAppearance()
    {
    	return getAppearance_r(stl);
    }
    
    /**
     * Restore the appearances to the correct colour.
     */
    public void restoreAppearance()
    {
    	    Iterator<Node> enumKids = stl.getAllChildren( );
        
        while(enumKids.hasNext( ))
        {
        	Object b = enumKids.next();
        	if(b instanceof BranchGroup)
        	{
        		Attributes att = (Attributes)((BranchGroup)b).getUserData();
        		if(att != null)
        			setAppearance_r(b, att.getAppearance());
        		else
        			Debug.e("restoreAppearance(): no Attributes!");
        	}
        }
    }
    
    // Why the !*$! aren't these in Vector3d???
    
    public static Vector3d add(Vector3d a, Vector3d b)
    {
        Vector3d result = new Vector3d();
        result.x = a.x + b.x;
        result.y = a.y + b.y;
        result.z = a.z + b.z;
        return result;
    }
    
    public static Vector3d neg(Vector3d a)
    {
        Vector3d result = new Vector3d(a);
        result.negate();
        return result;
    }
    
    public static Vector3d scale(Vector3d a, double s)
    {
        Vector3d result = new Vector3d(a);
        result.scale(s);
        return result;
    }
    
    // Put a vector in the positive octant (sort of abs for vectors)
    
    private Vector3d posOct(Vector3d v)
    {
        Vector3d result = new Vector3d();
        result.x = Math.abs(v.x);
        result.y = Math.abs(v.y);
        result.z = Math.abs(v.z);
        return result;
    }
    
    // Apply a rotating click transform about one of the coordinate axes,
    // which should be set in t.  This can only be done if we're being controlled
    // by the mouse, making us the active object.
    
    private void rClick(Transform3D t)
    {
        if(mouse == null)
            return;
        
        // Get the mouse transform and split it into a rotation and a translation
        
        Transform3D mtrans = new Transform3D();
        mouse.getTransform(mtrans);
        Vector3d mouseTranslation = new Vector3d();
        Matrix3d mouseRotation = new Matrix3d();
        mtrans.get(mouseRotation, mouseTranslation);
        
        // Subtract the part of the translation that puts the bottom left corner
        // at the origin.
        
        Vector3d zero = scale(extent, 0.5);
        mouseTranslation = add(mouseTranslation, neg(zero));       
        
        // Click the size record round by t
        
        t.transform(extent);
        extent = posOct(extent); 
        
        // Apply the new rotation to the existing one
        
        Transform3D spin = new Transform3D();
        spin.setRotation(mouseRotation);
        t.mul(spin);
        
        // Add a new translation to put the bottom left corner
        // back at the origin.
        
        zero = scale(extent, 0.5);
        mouseTranslation = add(mouseTranslation, zero);
        
        // Then slide us back where we were
        
        Transform3D fromZeroT = new Transform3D();
        fromZeroT.setTranslation(mouseTranslation);

        fromZeroT.mul(t);
        
        // Apply the whole new transformation
        
        mouse.setTransform(fromZeroT);       
    }
    
   // Rescale the STL object (for inch -> mm conversion) and stretching heights
    
    public void rScale(double x, double y, double z, boolean zOnly)
    {
        if(mouse == null && !zOnly){
            return;
        }
    	Vector3d mouseTranslation = null;
    	Matrix3d mouseRotation;
    	
        // Get the mouse transform and split it into a rotation and a translation
        
        Transform3D mtrans = new Transform3D();
        if(mouse != null)
        {
        	mouse.getTransform(mtrans);
        	mouseTranslation = new Vector3d();
        	mouseRotation = new Matrix3d();
        	mtrans.get(mouseRotation, mouseTranslation);
        }
        
        // Subtract the part of the translation that puts the bottom left corner
        // at the origin.
        
        Vector3d zero = scale(extent, 0.5);
        
        if(mouse != null)
        	mouseTranslation = add(mouseTranslation, neg(zero));       
        
        // Rescale the box
        
        if(zOnly)
        	extent.z = z*extent.z;
        else
        {
        	extent.x *= x;
        	extent.y *= y;
        	extent.z *= z;
        }
        
        // Add a new translation to put the bottom left corner
        // back at the origin.
        
        zero = scale(extent, 0.5);
        
        if(mouse != null)
        {
        	mouseTranslation = add(mouseTranslation, zero);

        	// Then slide us back where we were

        	Transform3D fromZeroT = new Transform3D();
        	fromZeroT.setTranslation(mouseTranslation);

        	// Apply the whole new transformation

        	mouse.setTransform(fromZeroT);
        }

        // Rescale the object
 
        Iterator<Node> things = stl.getAllChildren();
        while(things.hasNext()) 
        {
        	Object value = things.next();
        	recursiveSetScale(value, x, y, z, zOnly);
        }


    }
    
    public void rScale(double s, boolean zOnly)
    {
    	rScale(s, s, s, zOnly);
    }
    
    // Apply X, Y or Z 90 degree clicks to us if we're the active (i.e. mouse
    // controlled) object.
    
    public void xClick()
    {
        if(mouse == null)
            return;
        
        Transform3D x90 = new Transform3D();
        x90.set(new AxisAngle4d(1, 0, 0, 0.5*Math.PI));
        
        rClick(x90);
    }
    
    public void yClick()
    {
        if(mouse == null)
            return;
        
        Transform3D y90 = new Transform3D();
        y90.set(new AxisAngle4d(0, 1, 0, 0.5*Math.PI));
        
        rClick(y90);
    }
    
    // Do Zs by any angle
    
    public void zClick(double angle)
    {
        if(mouse == null)
            return;
        
        Transform3D zAngle = new Transform3D();
        zAngle.set(new AxisAngle4d(0, 0, 1, angle*Math.PI/180.0));
        
        rClick(zAngle);
    } 
    
    // This is called when the user wants to convert the object from
    // inches to mm.
    
    public void inToMM()
    {
        if(mouse == null)
            return;
        
        rScale(Preferences.inchesToMillimetres(), false);
    }
    
    public void rescale(double x, double y, double z)
    {
        if(mouse == null)
            return;
        
        rScale(x, y, z, false);
    }
    
    /**
     * Return the volume of the last-added item
     * @return
     */
    public double volume()
    {
    	if(contents == null)
    		return 0;
    	if(contents.size() <= 0)
    		return 0;
    	return contents.get(contents.size()-1).volume;
    }
    
    
 /*
    Project each triangle onto the XY plane and sum all the resulting prisms.  Downward-facing triangles will
    give negative volumes, upward positive.  So the result is the volume of the triangulated object.
 */
    
   /**
    * Compute the volume of a Shape3D
    * @param shape
    * @return
    */
    
    private double s3dVolume(Shape3D shape)
    {
    	double total = 0;
        GeometryArray g = (GeometryArray)shape.getGeometry();
        Point3d a = new Point3d();
        Point3d b = new Point3d();
        Point3d c = new Point3d();
        if(g != null)
        {
            for(int i = 0; i < g.getVertexCount(); i += 3) 
            {
                g.getCoordinate(i, a);
                g.getCoordinate(i+1, b);
                g.getCoordinate(i+2, c);
                total += prismVolume(a, b, c);
            }
        }
        return Math.abs(total);
    }
    
    /**
     * Compute the signed volume of a tetrahedron
     * @param a
     * @param b
     * @param c
     * @param d
     * @return
     */
    private double tetVolume(Point3d a, Point3d b, Point3d c, Point3d d)
    {
    	Matrix3d m = new Matrix3d(b.x - a.x, c.x - a.x, d.x - a.x, b.y - a.y, c.y - a.y, d.y - a.y, b.z - a.z, c.z - a.z, d.z - a.z);
    	return m.determinant()/6.0;
    }
    
    /**
     * Compute the signed volume of the prism between the XY plane and the 
     * space triangle {a, b, c}
     * 
     * @param a
     * @param b
     * @param c
     * @return
     */
    private double prismVolume(Point3d a, Point3d b, Point3d c)
    {
    	Point3d d = new Point3d(a.x, a.y, 0); 
    	Point3d e = new Point3d(b.x, b.y, 0);  
    	Point3d f = new Point3d(c.x, c.y, 0);
    	return tetVolume(a, b, c, e) +
    		tetVolume(a, e, c, d) +
    		tetVolume(e, f, c, d);
    }
    
}

//********************************************************************************
