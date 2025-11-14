/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reprap;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.stage.Stage;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;

import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;

import org.reprap.comms.GCode;
import org.reprap.utilities.StlFile;
import org.reprap.utilities.SvgFileParser;

public class Gfx3D extends Application {
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        boolean is3DSupported = Platform.isSupported(ConditionalFeature.SCENE3D);
        if(!is3DSupported) {
           System.out.println("Sorry, 3D is not supported in JavaFX on this platform.");
           return;
        }

        try {
            StlFile file = new StlFile();
            GCode reader = new GCode("/Users/xuyi/Source/GCode/test.gcode");
            SvgFileParser parser = new SvgFileParser();
            //Scene scene = reader.viewer();
            //Scene scene = file.load("/Users/xuyi/Pictures/3D/edf/files/edf120.stl");
            Scene scene = file.load("/Users/xuyi/Downloads/mendel-base.stl");
            //Scene scene = parser.buildScene("8.svg");
            
            primaryStage.setScene(scene);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StlFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        primaryStage.setTitle("3D Printer");

        primaryStage.show();
    }
    
}
