
package org.reprap.utilities;

import java.io.File;
import java.io.IOException;
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
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Sphere;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Xuyi
 */
public class SvgFileParser {
    private static double zTrans = -3500;
    private static final int camSpeed = 100;
    
    public Scene buildScene(String filename){
        Group group = new Group();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SvgFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        for(int j = 3; j < 10; j++){
            try {
                File inputFile = new File(j + ".svg");
                
                Document doc = dBuilder.parse(inputFile);
                doc.getDocumentElement().normalize();

                NodeList nList1 = doc.getElementsByTagName("line");

                for (int temp = 0; temp < nList1.getLength(); temp++) {
                    Node nNode = nList1.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        double x1 = Double.parseDouble(eElement.getAttribute("x1")) / 1000;
                        double y1 = Double.parseDouble(eElement.getAttribute("y1")) / 1000;
                        double x2 = Double.parseDouble(eElement.getAttribute("x2")) / 1000;
                        double y2 = Double.parseDouble(eElement.getAttribute("y2")) / 1000;
                        Sphere ball1 = new Sphere(2);
                        ball1.translateXProperty().set(x1);
                        ball1.translateYProperty().set(y1);
                        ball1.translateZProperty().set(0);
                        group.getChildren().add(ball1);
                        Sphere ball2 = new Sphere(2);
                        ball2.translateXProperty().set(x2);
                        ball2.translateYProperty().set(y2);
                        ball2.translateZProperty().set(0);
                        group.getChildren().add(ball2);
                        Line line = new Line(x1, y1, x2, y2); //instantiating Line class   
                        group.getChildren().add(line);
                    }
                }

                NodeList nList2 = doc.getElementsByTagName("polygon");
                for (int temp = 0; temp < nList2.getLength(); temp++) {
                    Element polygonElement = (Element) nList2.item(temp);
                    String[] points = polygonElement.getAttribute("points").split(" ");
                    double[] p = new double[points.length * 2];
                    for(int i = 0; i < points.length; i++){
                        Sphere ball1 = new Sphere(0.5);
                        String[] xy = points[i].split(",");
                        p[i * 2] = Double.parseDouble(xy[0]);
                        p[i * 2 + 1] = Double.parseDouble(xy[1]);
                        ball1.translateXProperty().set(Double.parseDouble(xy[0]));
                        ball1.translateYProperty().set(Double.parseDouble(xy[1]));
                        ball1.translateZProperty().set(j);
                        group.getChildren().add(ball1);
                    }
                    Polygon polygon = new Polygon(p);
                    polygon.setTranslateZ(j);
                    group.getChildren().add(polygon);
                }
            } catch (IOException | SAXException ex) {
                Logger.getLogger(SvgFileParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
            Scene scene = new Scene(group, 800, 600, true, SceneAntialiasing.BALANCED);
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
    }
}
