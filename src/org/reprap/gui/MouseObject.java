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
 
Wrapper class for the mouse-driven transforms.  This makes it easier
To attach the mouse to different objects in the scene.
 
First version 14 April 2006
This version: 14 April 2006
 
 */

package org.reprap.gui;

import java.util.List;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.reprap.geometry.polyhedra.STLObject;


//************************************************************************

public class MouseObject
{
    private Group top = null;     // Attach this to the rest of the tree
    private Group free = null;    // Mouse transform with no restrictions
    private Group slide = null;   // Mouse transform that only does XY sliding
    private Group trans = null;   // Set to one of the two above   
    private STLObject movingThing = null;  // The part of the scene being moved
    
    // Constructor takes the bounds of the world and two factors for translate
    // and zoom movements
    
    public MouseObject(Bounds behaviorBounds, double mouse_tf, double mouse_zf)
    {
        // Set up the free transform that allows all movements
        
        free = new Group( );
        //free.setCapability( Group.ALLOW_TRANSFORM_READ );
        //free.setCapability( Group.ALLOW_TRANSFORM_WRITE );
        
        /*Rotate mr = new Rotate( free );
        mr.setSchedulingBounds( behaviorBounds );
        free.addChild( mr );
        
        Translate mt = new Translate( free );
        mt.setSchedulingBounds( behaviorBounds );
        mt.setFactor(mouse_tf * mt.getXFactor(), mouse_tf * mt.getYFactor());
        free.addChild( mt );
                
        Translate mz = new Translate( free );
        mz.setSchedulingBounds( behaviorBounds );
        mz.setFactor(mouse_zf * mz.getFactor());
        free.addChild( mz );*/
        
        // Set up the slide transform that only allows XY movement
        
        slide = new Group();
        //slide.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
        //slide.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
        
        /*Translate mts = new Translate( slide );
        mts.setSchedulingBounds( behaviorBounds );
        mts.setFactor(mouse_tf * mts.getXFactor(), mouse_tf * mts.getYFactor());
        slide.addChild( mts );*/
        
        // Set up the thing to attach and detach
        
        top = new Group();
        
        // We aren't attached to anything yet
        
        movingThing = null;
        
        // ALLOW_EVERYTHING would be useful...
        
        /*top.setCapability(Group.ALLOW_DETACH);
        top.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        top.setCapability(Group.ALLOW_CHILDREN_WRITE); 
        
        free.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        free.setCapability(Group.ALLOW_CHILDREN_WRITE);        
        free.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        free.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        
        slide.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        slide.setCapability(Group.ALLOW_CHILDREN_WRITE);        
        slide.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        slide.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);*/
        
        // Set the default action as unrestricted movement
        
        top.getChildren().add(free);
        trans = free;
    }
    
    // Select the STL object to control with the mouse; the boolean
    // decides whether to use free or slide.
    
    public void move(STLObject stl, boolean slideOnly)
    {
        //Transform t3d = new Transform();
        //trans.getTransform(t3d);
        
        // Detach us from whatever we were stuck to
        
        //top.detach();
        
        // Take the thing we were moving, set its transform to a fixed version
        // of what we are now, and tell it we aren't controlling it any more.
        
        if(movingThing != null)
        {
            //movingThing.setTransform(t3d);
            //movingThing.handle().detach();
            movingThing.top().getChildren().add(movingThing.handle());
            movingThing.setMouse(null);
        }
        
        // Record what we will be moving from now on.
        
        movingThing = stl;
        
        // Remove the current mouse transform from top...
        
        top.getChildren().remove(0);
        
        // ... and add the one selected by slideOnly.
        
        if(slideOnly)
            trans = slide;
        else
            trans = free;
        
        top.getChildren().add(trans);
        
        // Get the current transform of the thing being moved...
        
        //t3d = new Transform();
        //movingThing.trans().getTransform(t3d);
        
        // ...set that thing's static transform to the identity...
        
        //Transform identity = new Transform();
        //movingThing.setTransform(identity);
        
        // ...and set the mouse transform to that of the thing being moved.
        
        //trans.getTransforms().add(t3d);
        
        // Put us in the path to the thing being moved.
        
        //movingThing.handle().detach();
        trans.getChildren().add(movingThing.handle());
        movingThing.top().getChildren().add(top); 
        
        top.setUserData(movingThing);
        trans.setUserData(movingThing);
        
        // Tell it we've taken over.
        
        movingThing.setMouse(this);
    }
    
    // Allow others to apply a transform to what we're controlling
    
    public void mul(Transform t3d)
    {
        trans.getTransforms().add(t3d);
    }
    
    // Get and set our transform
    
    public List<Transform> getTransforms()
    {
        return trans.getTransforms();
    }
    
    public void setTransform(Transform t3d)
    {
        trans.getTransforms().add(t3d);
    }
}
