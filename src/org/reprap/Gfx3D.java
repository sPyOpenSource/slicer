
package org.reprap;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.stage.Stage;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Scene;

import org.reprap.comms.GCode;
import org.reprap.utilities.StlFile;
import org.reprap.utilities.SvgFileParser;
import assets.Assets;

public class Gfx3D extends Application {
    
    private final Assets assets = new Assets();
    
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
            //Scene scene = reader.buildScene();
            //Scene scene = file.load("/Users/xuyi/Pictures/3D/edf/files/edf120.stl");
            //Scene scene = file.load(assets.getURL("/assets/stl/mendel-base.stl"));
            Scene scene = parser.buildScene("8.svg");
            
            primaryStage.setScene(scene);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StlFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        primaryStage.setTitle("3D Printer");

        primaryStage.show();
    }
    
}
