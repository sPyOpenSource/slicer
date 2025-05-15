package test;

import javafx.geometry.Point3D;

import org.reprap.Attributes;
import org.testng.annotations.Test;
import org.reprap.geometry.polyhedra.AllSTLsToBuild;
import org.reprap.geometry.polyhedra.AllSTLsToBuild.LineSegment;
import static org.testng.Assert.assertEquals;

/**
 *
 * @author xuyi
 */
public class STLTest {
    @Test
    public void test() {
        AllSTLsToBuild test = new AllSTLsToBuild();
        LineSegment line = test.addEdge(Point3D.ZERO, new Point3D(1,1,1), new Point3D(2,2,2), 0.5, new Attributes(null, null, null, null));
        assertEquals(line.a.x(), 0.5);
        line = test.addEdge(new Point3D(-1,-1,-1), new Point3D(1,-1,1), new Point3D(2,2,2), 0.5, new Attributes(null, null, null, null));
        assertEquals(line.b.x(), 0.5);
        line = test.addEdge(new Point3D(0,-1,0), new Point3D(1,1,1), new Point3D(2,-2,2), 0.5, new Attributes(null, null, null, null));
        assertEquals(line.b.y(), 1.25);
    }
}
