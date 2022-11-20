
package org.reprap.utilities;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
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
    public Scene buildScene(String filename){
        try {
            File inputFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("line");
            Group group = new Group();
        
            for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
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
            Scene scene = new Scene(group, 800, 600, true, SceneAntialiasing.BALANCED);
            scene.setFill(Color.GREEN);
            return scene;
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(SvgFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}