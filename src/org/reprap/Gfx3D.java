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
import javafx.geometry.Point3D;

import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;

import org.reprap.comms.GCodeReaderAndWriter;
import org.reprap.utilities.StlFile;

public class Gfx3D extends Application {
    private static double zTrans = -3500;
    private static final int camSpeed = 100;
    
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

        Camera camera = new PerspectiveCamera(true);
        camera.setFarClip(Integer.MAX_VALUE);
        camera.setNearClip(0.1);

        StlFile file = new StlFile();
        try {
            GCodeReaderAndWriter reader = new GCodeReaderAndWriter("/Users/xuyi/Pictures/test.gcode");
            Scene scene = reader.viewer();// file.load("/Users/xuyi/Pictures/edf/files/edf120.stl");
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
            primaryStage.setScene(scene);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StlFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        primaryStage.setTitle("3D Printer");

        primaryStage.show();
    }
}
