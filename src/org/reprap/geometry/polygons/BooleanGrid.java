
/**
 * This class stores a rectangular grid at the same grid resolution
 * as the RepRap machine's finest resolution using the Java BitSet class.
 * 
 * There are two types of pixel: solid (or true),
 * and air (or false).
 * 
 * There are Boolean operators implemented to allow unions, intersections,
 * and differences of two bitmaps, and complements of one.
 * 
 * There are also functions to do ray-trace intersections, to find the parts
 * of lines that are solid, and outline following, to find the perimiters of
 * solid shapes as polygons.
 * 
 * The class makes extensive use of lazy evaluation.
 * 
 * @author Adrian Bowyer
 *
 */

package org.reprap.geometry.polygons;

import java.util.ArrayList;
import java.util.List;
import java.util.BitSet;
import java.io.IOException;

import org.reprap.Attributes;
import org.reprap.Preferences;
import org.reprap.utilities.Debug;


public class BooleanGrid 
{
	// Various internal classes to make things work...
	
	/**
	 * Integer 2D point
	 * @author ensab
	 *
	 */

	class iPoint
	{
		private int x, y;
		
		iPoint(int xa, int ya)
		{
			x = xa;
			y = ya;
		}
		
		/**
		 * Copy constructor
		 * @param a
		 */
		iPoint(iPoint a)
		{
			x = a.x;
			y = a.y;
		}
		
		/**
		 * Convert real-world point to integer
		 * @param a
		 */
		iPoint(Point2D a)
		{
			x = iScale(a.x()) - rec.swCorner.x;
			y = iScale(a.y()) - rec.swCorner.y;
		}
		
		/**
		 * Generate the equivalent real-world point
		 * @return
		 */
		Point2D realPoint()
		{
			return new Point2D(scale(rec.swCorner.x + x), scale(rec.swCorner.y + y));
		}
		
		/**
		 * Are two points the same?
		 * @param b
		 * @return
		 */
		boolean coincidesWith(iPoint b)
		{
			return x == b.x && y == b.y;
		}
		
		/**
		 * Vector addition
		 * @param b
		 * @return
		 */
		iPoint add(iPoint b)
		{
			return new iPoint(x + b.x, y + b.y);
		}
		
		/**
		 * Vector subtraction
		 * @param b
		 * @return
		 */
		iPoint sub(iPoint b)
		{
			return new iPoint(x - b.x, y - b.y);
		}
		
		/**
		 * Opposite direction
		 * @return
		 */
		iPoint neg()
		{
			return new iPoint(-x, -y);
		}
		
		/**
		 * Absolute value
		 * @return
		 */
		iPoint abs()
		{
			return new iPoint(Math.abs(x), Math.abs(y));
		}
		
		/**
		 * Squared length
		 * @return
		 */
		long magnitude2()
		{
			return x * x + y * y;
		}
		
		/**
		 * Scalar product
		 * @param a
		 * @return
		 */
		long scalarProduct(iPoint a)
		{
			return x * a.x + y * a.y;
		}
		
		/**
		 * For printing
		 */
                @Override
		public String toString()
		{
			return ": " + x + ", " + y + " :";
		}
	}
	
	/**
	 * Small class to hold rectangles represented by the sw point and
	 * the size.
	 * @author ensab
	 *
	 */
	class iRectangle
	{
		public iPoint swCorner;
		public iPoint size;
		
		/**
		 * Construct from the corner points
		 * @param min
		 * @param max
		 */
		public iRectangle(iPoint min, iPoint max)
		{
			swCorner = new iPoint(min);
			size = max.sub(min);
			size.x++;
			size.y++;
		}
		
		/**
		 * Copy constructor
		 * @param r
		 */
		public iRectangle(iRectangle r)
		{
			swCorner = new iPoint(r.swCorner);
			size = new iPoint(r.size);
		}
		
		/**
		 * Useful to have a single-pixel at the origin
		 *
		 */
		private iRectangle()
		{
			swCorner = new iPoint(0, 0);
			size = new iPoint(1, 1);
		}
		
		/**
		 * Are two rectangles the same?
		 * @param b
		 * @return
		 */
		public boolean coincidesWith(iRectangle b)
		{
			return swCorner.coincidesWith(b.swCorner) && size.coincidesWith(b.size);
		}
		
		/**
		 * This rectangle in the real world
		 * @return
		 */
		public Rectangle realRectangle()
		{
			return new Rectangle(swCorner.realPoint(), 
					new iPoint(swCorner.x + size.x - 1, swCorner.y + size.y - 1).realPoint());
		}
		
		/**
		 * Big rectangle containing the union of two.
		 * @param b
		 * @return
		 */
		public iRectangle union(iRectangle b)
		{
			iRectangle result = new iRectangle(this);
			result.swCorner.x = Math.min(result.swCorner.x, b.swCorner.x);
			result.swCorner.y = Math.min(result.swCorner.y, b.swCorner.y);
			int sx = result.swCorner.x + result.size.x - 1;
			sx = Math.max(sx, b.swCorner.x + b.size.x - 1) - result.swCorner.x + 1;
			int sy = result.swCorner.y + result.size.y - 1;
			sy = Math.max(sy, b.swCorner.y + b.size.y - 1) - result.swCorner.y + 1;
			result.size = new iPoint(sx, sy);			
			return result;
		}
		
		/**
		 * Rectangle containing the intersection of two.
		 * @param b
		 * @return
		 */
		public iRectangle intersection(iRectangle b)
		{
			iRectangle result = new iRectangle(this);
			result.swCorner.x = Math.max(result.swCorner.x, b.swCorner.x);
			result.swCorner.y = Math.max(result.swCorner.y, b.swCorner.y);
			int sx = result.swCorner.x + result.size.x - 1;
			sx = Math.min(sx, b.swCorner.x + b.size.x - 1) - result.swCorner.x + 1;
			int sy = result.swCorner.y + result.size.y - 1;
			sy = Math.min(sy, b.swCorner.y + b.size.y - 1) - result.swCorner.y + 1;
			result.size = new iPoint(sx, sy);			
			return result;
		}
		
		/**
		 * Grow (dist +ve) or shrink (dist -ve).
		 * @param dist
		 * @return
		 */
		public iRectangle offset(int dist)
		{
			iRectangle result = new iRectangle(this);
			result.swCorner.x = result.swCorner.x - dist;
			result.swCorner.y = result.swCorner.y - dist;
			result.size.x = result.size.x + 2*dist;
			result.size.y = result.size.y + 2*dist;
			return result;
		}
		
		/**
		 * Anything there?
		 * @return
		 */
		public boolean isEmpty()
		{
			return size.x < 0 | size.y < 0;
		}
	}
	
	/**
	 * Integer-point polygon
	 * @author ensab
	 *
	 */
	class iPolygon
	{
		/**
		 * Auto-extending list of points
		 */
		private List<iPoint> points = null;
		
		/**
		 * Does the polygon loop back on itself?
		 */
		private boolean closed;
		
		public iPolygon(boolean c)
		{
			points = new ArrayList<>();
			closed = c;
		}
		
		/**
		 * Deep copy
		 * @param a
		 */
		public iPolygon(iPolygon a)
		{
			points = new ArrayList<>();
			for(int i = 0; i < a.size(); i++)
				add(a.point(i));
			closed = a.closed;
		}
		
		/**
		 * Return the point at a given index
		 * @param i
		 * @return
		 */
		public iPoint point(int i)
		{
			return points.get(i);
		}
		
		/**
		 * How many points?
		 * @return
		 */
		public int size()
		{
			return points.size();
		}
		
		/**
		 * Add a new point on the end
		 * @param p
		 */
		public void add(iPoint p)
		{
			points.add(p);
		}
		
		/**
		 * Add a whole polygon on the end
		 * @param a
		 */
		public void add(iPolygon a)
		{
			for(int i = 0; i < a.size(); i++)
				add(a.point(i));
		}
		
		/**
		 * Delete a point and close the resulting gap
		 * @param i
		 */
		public void remove(int i)
		{
			points.remove(i);
		}
		
		/**
		 * Find the index of the point in the polygon nearest to another point as long as
		 * it's less than tooFar2.  Set that to Long.MAX_VALUE for a complete search.
		 * @param a
		 * @param tooFar2
		 * @return
		 */
		public int nearest(iPoint a, long tooFar2)
		{
			int i = 0;
			int j = -1;
			long d0 = tooFar2;
			while(i < size())
			{
				long d1 = point(i).sub(a).magnitude2();
				if(d1 < d0)
				{
					j = i;
					d0 = d1;
				}
				i++;
			}
			return j;
		}
		
		/**
		 * Negate (i.e. reverse cyclic order)
		 * @return reversed polygon object
		 */
		public iPolygon negate()
		{
			iPolygon result = new iPolygon(closed);
			for(int i = size() - 1; i >= 0; i--)
				result.add(point(i)); 
			return result;
		}
		
		/**
		 * Transtate by vector t
		 * @param t
		 * @return
		 */
		public iPolygon translate(iPoint t)
		{
			iPolygon result = new iPolygon(closed);
			for(int i = 0; i < size(); i++)
				result.add(point(i).add(t));
			return result;
		}
		
		/**
		 * Find the furthest point from point v1 on the polygon such that the polygon between
		 * the two can be approximated by a DDA straight line from v1.
		 * @param v1
		 * @return
		 */
		private int findAngleStart(int v1)
		{
			int top = size() - 1;
			int bottom = v1;
			iPoint p1 = point(v1);
			int offCount = 0;
			while(top - bottom > 1)
			{
				int middle = (bottom + top)/2;
				DDA line = new DDA(p1, point(middle));
				iPoint n = line.next();
				offCount = 0;
				int j = v1;

				while(j <= middle && n != null && offCount < 2)
				{		
					if(point(j).coincidesWith(n))
						offCount = 0;
					else
						offCount++;
					n = line.next();
					j++;
				}
				
				if(offCount < 2)
					bottom = middle;
				else
					top = middle;
			}
			if(offCount < 2)
				return top;
			else
				return bottom;
		}
		
		/**
		 * Generate an equivalent polygon with fewer vertices by removing chains of points
		 * that lie in straight lines.
		 * @return
		 */
		public iPolygon simplify()
		{
			if(size() <= 3)
				return new iPolygon(this);
			iPolygon r = new iPolygon(closed);
			int v = 0;
			do
			{
				r.add(point(v));
				v = findAngleStart(v);
			}while(v < size() - 1);
			r.add(point(v));
			return r;
		}
		
		/**
		 * Convert the polygon into a polygon in the real world.
		 * @param a
		 * @return
		 */
		public Polygon realPolygon(Attributes a)
		{
			Polygon result = new Polygon(a, closed);
			for(int i = 0; i < size(); i++)
				result.add(point(i).realPoint());
			return result;
		}
	}
	
	/**
	 * A list of polygons
	 * @author ensab
	 *
	 */
	class iPolygonList
	{
		private List<iPolygon> polygons = null;
		
		public iPolygonList()
		{
			polygons = new ArrayList<>();
		}
		
		/**
		 * Return the ith polygon
		 * @param i
		 * @return
		 */
		public iPolygon polygon(int i)
		{
			return polygons.get(i);
		}
		
		/**
		 * How many polygons are there in the list?
		 * @return
		 */
		public int size()
		{
			return polygons.size();
		}
		
		/**
		 * Add a polygon on the end
		 * @param p
		 */
		public void add(iPolygon p)
		{
			polygons.add(p);
		}
		
		/**
		 * Replace a polygon in the list
		 * @param i
		 * @param p
		 */
		public void set(int i, iPolygon p)
		{
			polygons.set(i, p);
		}
		
		/**
		 * Get rid of a polygon from the list
		 * @param i
		 */
		public void remove(int i)
		{
			polygons.remove(i);
		}
		
		/**
		 * Add another list of polygons on the end
		 * @param a
		 */
		public void add(iPolygonList a)
		{
			for(int i = 0; i < a.size(); i++)
				add(a.polygon(i));
		}
		
		/**
		 * Transtate by vector t
		 * @param t
		 * @return
		 */
		public iPolygonList translate(iPoint t)
		{
			iPolygonList result = new iPolygonList();
			for(int i = 0; i < size(); i++)
				result.add(polygon(i).translate(t));
			return result;
		}
		
		/**
		 * Turn all the polygons into real-world polygons
		 * @param a
		 * @return
		 */
		public PolygonList realPolygons(Attributes a)
		{
			PolygonList result = new PolygonList();
			for(int i = 0; i < size(); i++)
				result.add(polygon(i).realPolygon(a));
			return result;
		}
		
		/**
		 * Simplify all the polygons
		 * @return
		 */
		public iPolygonList simplify()
		{
			iPolygonList result = new iPolygonList();
			for(int i = 0; i < size(); i++)
				result.add(polygon(i).simplify());
			return result;
		}
	}
	
	/**
	 * Straight-line DDA
	 * @author ensab
	 *
	 */
	class DDA
	{
		private iPoint delta, count, p;
		private int steps, taken;
		private boolean xPlus, yPlus, finished;
		
		/**
		 * Set up the DDA between a start and an end point
		 * @param s
		 * @param e
		 */
		DDA(iPoint s, iPoint e)
		{
			delta = e.sub(s).abs();

			steps = Math.max(delta.x, delta.y);
			taken = 0;
			
			xPlus = e.x >= s.x;
			yPlus = e.y >= s.y;

			count = new iPoint(-steps / 2, -steps / 2);
			
			p = new iPoint(s);
			
			finished = false;
		}
		
		/**
		 * Return the next point along the line, or null
		 * if the last point returned was the final one.
		 * @return
		 */
		iPoint next()
		{
			if(finished)
				return null;

			iPoint result = new iPoint(p);

			finished = taken >= steps;

			if(!finished)
			{
				taken++;
				count = count.add(delta);
				
				if (count.x > 0)
				{
					count.x -= steps;
					if (xPlus)
						p.x++;
					else
						p.x--;
				}

				if (count.y > 0)
				{
					count.y -= steps;
					if (yPlus)
						p.y++;
					else
						p.y--;
				}
			}
			
			return result;
		}
	}
	
	/**
	 * Little class to hold the ends of hatching patterns.  Snakes are a combination of the hatching
	 * lines that infill a shape plus the bits of boundary that join their ends to make a zig-zag pattern.
	 * @author ensab
	 *
	 */
	class SnakeEnd
	{
		public iPolygon track;
		public int hitPlaneIndex;
		
		public SnakeEnd(iPolygon t, int h)
		{
			track = t;
			hitPlaneIndex = h;
		}
	}
	
	//**************************************************************************************************
	
	// Start of BooleanGrid proper
	
	/**
	 * The resolution of the RepRap machine
	 */
	private static final double pixSize = Preferences.machineResolution()*0.6;
	private static final double realResolution = pixSize*1.5;
	private static final double rSwell = 2.0; // mm by which to swell rectangles to give margins round stuff
	
	/**
	 * How simple does a CSG expression have to be to not be worth pruning further?
	 */
	private static final int simpleEnough = 3;
	
	private static final BooleanGrid nothingThere = new BooleanGrid();
	
	/**
	 * Run round the eight neighbours of a pixel anticlockwise from bottom left
	 */
	private final iPoint[] neighbour = 
	{
		new iPoint(-1, -1),  //0 /
		new iPoint(0, -1),   //1 V
		new iPoint(1, -1),   //2 \
		new iPoint(1, 0),    //3 ->
		new iPoint(1, 1),    //4 /
		new iPoint(0, 1),    //5 ^
		new iPoint(-1, 1),   //6 \
		new iPoint(-1, 0)    //7 <
	};
	
	// Marching squares directions.  2x2 grid bits:
	//
	//    0  1
	//
	//    2  3
	
	private final int[] march =
	{
			-1, // 0
			5,  // 1
			3,  // 2
			3,  // 3
			7,  // 4
			5,  // 5
			7,  // 6
			3,  // 7
			1,  // 8
			5,  // 9
			1,  // 10
			1,  // 11
			7,  // 12
			5,  // 13
			7,  // 14
			-1  // 15
	};
	
	/**
	 * Lookup table for whether to cull points forming thin bridges.  The index into this is the byte bitpattern
	 * implied by the neighbour array.  This table is generated interactively by the program 
	 * org.reprap.utilities.FilterGenerator.
	 */
	private static final boolean[] thinFilter = {
		true, true, true, false, true, true, false, true, true, true, true, true, false, true, false, false, 
		true, true, true, true, true, true, true, true, false, true, true, true, true, true, false, false, 
		true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, true, 
		false, true, true, true, true, true, true, true, false, true, true, true, false, true, false, false, 
		true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, 
		true, true, true, true, true, false, false, false, true, false, false, false, true, false, false, false, 
		false, true, true, true, true, false, false, false, true, false, false, false, true, false, false, false, 
		true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, 
		true, false, true, false, true, true, true, false, true, true, true, false, true, true, true, false, 
		true, true, true, true, true, false, false, false, true, false, false, false, true, false, false, false, 
		true, true, true, true, true, false, false, false, true, false, false, false, true, false, false, false, 
		true, true, true, true, true, false, false, false, false, false, false, false, true, false, false, false, 
		false, true, true, false, true, true, true, false, true, true, true, true, true, true, true, false, 
		true, true, true, true, true, false, false, false, true, false, false, false, true, false, false, false, 
		false, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false, 
		false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false	
	};
	
	/**
	 * Lookup table behaves like scalar product for two neighbours i and j; get it by neighbourProduct[Math.abs(j - i)]
	 */
	private final int[] neighbourProduct = {2, 1, 0, -1, -2, -1, 0, 1};
	
	/**
	 * The pixel map
	 */
	private BitSet bits;
	
	/**
	 * Flags for visited pixels during searches
	 */
	private BitSet visited;
	
	/**
	 * The rectangle the pixelmap covers
	 */
	private iRectangle rec;
	
	/**
	 * The attributes
	 */
	private Attributes att;
	
	private Boolean isThin = false;
	

	
	//**************************************************************************************************
	// Debugging and timing
	
	private static String stack[] = new String[20];
	private static int sp = -1;
	private static boolean debug = false;
	
	private void push(String s)
	{
		if(!debug)
			return;
		sp++;
		stack[sp] = s;
		String str = "";
		for(int i = 0; i < sp; i++)
			str += " ";
		Debug.a("{ " + str + s);
	}
	private void pop()
	{
		if(!debug)
			return;
		String str = "";
		for(int i = 0; i < sp; i++)
			str += " ";		
		Debug.a("} " + str + stack[sp]);
		sp--;
	}
	
	//**************************************************************************************************
	// Constructors and administration
	
	/**
	 * Back and forth from real to pixel/integer coordinates
	 * @param i
	 * @return
	 */
	static double scale(int i) { return i*pixSize; }
	static int iScale(double d) { return (int)Math.round(d/pixSize); }
	
	/**
	 * Build the grid from a CSG expression
	 * @param csgExp
	 */
	public BooleanGrid(CSG2D csgExp, Rectangle rectangle, Attributes a)
	{
		att = a;
		isThin = false;
		Rectangle ri = rectangle.offset(rSwell);
		rec = new iRectangle(new iPoint(0, 0), new iPoint(1, 1));  // Set the origin to (0, 0)...
		rec.swCorner = new iPoint(ri.sw());                        // That then gets subtracted by the iPoint constructor to give the true origin
		rec.size = new iPoint(ri.ne());                            // The true origin is now automatically subtracted.
		bits = new BitSet(rec.size.x*rec.size.y);
		visited = null;
		push("Build quad tree... ");
		//Debug.e("Quad start.");
		generateQuadTree(new iPoint(0, 0), new iPoint(rec.size.x - 1, rec.size.y - 1), csgExp);
		//Debug.e("Quad end.");
		pop();
		deWhisker();
	}
	
	
	/**
	 * Copy constructor
	 * N.B. attributes are _not_ deep copied
	 * @param bg
	 */
	public BooleanGrid(BooleanGrid bg)
	{
		att = bg.att;
		visited = null;
		isThin = bg.isThin;
		rec= new iRectangle(bg.rec);
		bits = (BitSet)bg.bits.clone();
	}
	
	/**
	 * Copy constructor with new rectangle
	 * N.B. attributes are _not_ deep copied
	 * @param bg
	 */
	public BooleanGrid(BooleanGrid bg, iRectangle newRec)
	{
		att = bg.att;
		visited = null;
		isThin = bg.isThin;
		rec= new iRectangle(newRec);
		bits = new BitSet(rec.size.x*rec.size.y);
		iRectangle recScan = rec.intersection(bg.rec);
		int offxOut = recScan.swCorner.x - rec.swCorner.x;
		int offyOut = recScan.swCorner.y - rec.swCorner.y;
		int offxIn = recScan.swCorner.x - bg.rec.swCorner.x;
		int offyIn = recScan.swCorner.y - bg.rec.swCorner.y;
		for(int x = 0; x < recScan.size.x; x++)
			for(int y = 0; y < recScan.size.y; y++)
				bits.set(pixI(x + offxOut, y + offyOut), bg.bits.get(bg.pixI(x + offxIn, y + offyIn)));
	}
	
	/**
     * The empty grid
	 */
	private BooleanGrid()
	{
		att = new Attributes(null, null, null, null);
		rec = new iRectangle();
		bits = new BitSet(1);
		isThin = false;
		visited = null;		
	}
	
	/**
	 * The empty set
	 * @return
	 */
	public static BooleanGrid nullBooleanGrid()
	{
		return nothingThere;
	}
	
	/**
	 * Overwrite the attributes
	 * Only to be used if you know what you're doing...
	 * @param a
	 */
	public void forceAttribute(Attributes a)
	{
		att = a;
	}
	
	/**
	 * The index of a pixel in the 1D bit array.
	 * @param x
	 * @param y
	 * @return
	 */
	private int pixI(int x, int y)
	{
		return x*rec.size.y + y;
	}
	
	/**
	 * The index of a pixel in the 1D bit array.
	 * @param p
	 * @return
	 */
	private int pixI(iPoint p)
	{
		return pixI(p.x, p.y);
	}
	
	/**
	 * The pixel corresponding to an index into the bit array
	 * @param i
	 * @return
	 */
	private iPoint pixel(int i)
	{
		return new iPoint(i/rec.size.y, i%rec.size.y);
	}
	
	/**
	 * Return the attributes
	 * @return
	 */
	public Attributes attribute()
	{
		return att;
	}
	
	/**
	 * Bitmap of thin lines?
	 * @return
	 */
	public Boolean isThin()
	{
		return isThin;
	}
	
	public void setThin(Boolean t)
	{
		isThin = t;
	}
	
	/**
	 * Any pixels set?
	 * @return
	 */
	public boolean isEmpty()
	{
		return bits.isEmpty();
	}
	
	/**
	 * Is a point inside the image?
	 * @param p
	 * @return
	 */
	private boolean inside(iPoint p)
	{
		if(p.x < 0)
			return false;
		if(p.y < 0)
			return false;
		if(p.x >= rec.size.x)
			return false;
		return p.y < rec.size.y;
	}
	
	
	/**
	 * Set pixel p to value v
	 * @param p
	 * @param v
	 */
	public void set(iPoint p, boolean v)
	{
		if(!inside(p))
		{
			Debug.e("BoolenGrid.set(): attempt to set pixel beyond boundary!");
			return;
		}
		bits.set(pixI(p), v);
	}
	
	/**
	 * Fill a disc centre c radius r with v
	 * @param c
	 * @param r
	 * @param v
	 */
	public void disc(iPoint c, int r, boolean v)
	{
		for(int x = -r; x <= r; x++)
		{
			int xp = c.x + x;
			if(xp > 0 && xp < rec.size.x)
			{
				int y = (int)Math.round(Math.sqrt((double)(r*r - x*x)));
				int yp0 = c.y - y;
				int yp1 = c.y + y;
				yp0 = Math.max(yp0, 0);
				yp1 = Math.min(yp1, rec.size.y - 1);
				if(yp0 <= yp1)
					bits.set(pixI(xp, yp0), pixI(xp, yp1) + 1, v);
			}
		}
	}
	
	/**
	 * Fill a disc centre c radius r with v
	 * @param c
	 * @param r
	 * @param v
	 */
	public void disc(Point2D c, double r, boolean v)
	{
		disc(new iPoint(c), iScale(r), v);
	}
	
	/**
	 * Fill a rectangle with centreline running from p0 to p1 of width 2r with v
	 * @param p0
	 * @param p1
	 * @param r
	 * @param v
	 */
	public void rectangle(iPoint p0, iPoint p1, int r, boolean v)
	{
		r = Math.abs(r);
		Point2D rp0 = new Point2D(p0.x, p0.y);
		Point2D rp1 = new Point2D(p1.x, p1.y);
		HalfPlane[] h = new HalfPlane[4];
		h[0] = new HalfPlane(rp0, rp1);
		h[2] = h[0].offset(r);
		h[0] = h[0].offset(-r).complement();
		h[1] = new HalfPlane(rp0, Point2D.add(rp0, h[2].normal()));
		h[3] = new HalfPlane(rp1, Point2D.add(rp1, h[0].normal()));
		double xMin = Double.MAX_VALUE;
		double xMax = Double.MIN_VALUE;
		Point2D p = null;
		for(int i = 0; i < 4; i++)
		{
			try
			{
				p = h[i].cross_point(h[(i+1)%4]);
			} catch (ParallelException e)
			{}
			xMin = Math.min(xMin, p.x());
			xMax = Math.max(xMax, p.x());
		}
		int iXMin = (int)Math.round(xMin);
		iXMin = Math.max(iXMin, 0);
		int iXMax = (int)Math.round(xMax);
		iXMax = Math.min(iXMax, rec.size.x - 1);
		for(int x = iXMin; x <= iXMax; x++)
		{
			Line yLine = new Line(new Point2D(x, 0), new Point2D(x, 1));
			Interval iv = Interval.bigInterval();
			for(int i = 0; i < 4; i++)
				iv = h[i].wipe(yLine, iv);
			if(!iv.empty())
			{
				int yLow = (int)Math.round(yLine.point(iv.low()).y());
				int yHigh = (int)Math.round(yLine.point(iv.high()).y());
				yLow = Math.max(yLow, 0);
				yHigh = Math.min(yHigh, rec.size.y - 1);
				if(yLow <= yHigh)
					bits.set(pixI(x, yLow), pixI(x, yHigh) + 1, v);
			} 
		}
	}
	
	/**
	 * Fill a rectangle with centreline running from p0 to p1 of width 2r with v
	 * @param p0
	 * @param p1
	 * @param r
	 * @param v
	 */
	public void rectangle(Point2D p0, Point2D p1, double r, boolean v)
	{
		rectangle(new iPoint(p0), new iPoint(p1), iScale(r), v);
	}
	
	/**
	 * Set a whole rectangle to one value
	 * @param ipsw
	 * @param ipne
	 * @param v
	 */
	private void homogeneous(iPoint ipsw, iPoint ipne, boolean v)
	{
		for(int x = ipsw.x; x <= ipne.x; x++)
			bits.set(pixI(x, ipsw.y), pixI(x, ipne.y) + 1, v);
	}
	
	/**
	 * Set a whole rectangle to one value
	 * @param ipsw
	 * @param ipne
	 * @param v
	 */
	public void homogeneous(Point2D ipsw, Point2D ipne, boolean v)
	{
		homogeneous(new iPoint(ipsw), new iPoint(ipne), v);
	}
	
	/**
	 * Set a whole rectangle to the right values for a CSG expression
	 * @param ipsw
	 * @param ipne
	 * @param v
	 */
	private void heterogeneous(iPoint ipsw, iPoint ipne, CSG2D csgExpression)
	{
		for(int x = ipsw.x; x <= ipne.x; x++)
			for(int y = ipsw.y; y <= ipne.y; y++)
				bits.set(pixI(x, y), csgExpression.value(new iPoint(x, y).realPoint()) <= 0);
	}
	
	/**
	 * The rectangle surrounding the set pixels in real coordinates.
	 * @return
	 */
	public Rectangle box()
	{
		return new Rectangle(new iPoint(0, 0).realPoint(), new iPoint(rec.size.x - 1, rec.size.y - 1).realPoint());
	}
	
	/**
	 * The value at a point.
	 * @param p
	 * @return
	 */
	public boolean get(iPoint p)
	{
		if(!inside(p))
			return false;
		return bits.get(pixI(p));
	}
	
	/**
	 * Get the value at the point corresponding to somewhere in the real world
	 * That is, membership test.
	 * @param p
	 * @return
	 */
	public boolean get(Point2D p)
	{
		return get(new iPoint(p));
	}
	
	/**
	 * Set a point as visited
	 * @param p
	 * @param v
	 */
	private void vSet(iPoint p, boolean v)
	{
		if(!inside(p))
		{
			Debug.e("BoolenGrid.vSet(): attempt to set pixel beyond boundary!");
			return;
		}
		if(visited == null)
			visited = new BitSet(rec.size.x*rec.size.y);
		visited.set(pixI(p), v);
	}
	
	/**
	 * Has this point been visited?
	 * @param p
	 * @return
	 */
	private boolean vGet(iPoint p)
	{
		if(visited == null)
			return false;
		if(!inside(p))
			return false;		
		return visited.get(pixI(p));
	}
	
	public long pixelCount()
	{
		return bits.cardinality();
	}
	
	/**
	 * Find a set point
	 * @return
	 */
	private iPoint findSeed_i()
	{
		for(int x = 0; x < rec.size.x; x++)
			for(int y = 0; y < rec.size.y; y++)
			{
				iPoint p = new iPoint(x, y);
				if(get(p))
					return p;
			}
		return null;
	}
	
	/**
	 * Find a set point
	 * @return
	 */
	public Point2D findSeed()
	{
		iPoint p = findSeed_i();
		if(p == null)
			return null;
		else
			return p.realPoint();
	}
	
	/**
	 * Find the centroid of the shape(s)
	 * @return
	 */
	private iPoint findCentroid_i()
	{
		iPoint sum = new iPoint(0,0);
		int points = 0;
		for(int x = 0; x < rec.size.x; x++)
			for(int y = 0; y < rec.size.y; y++)
			{
				iPoint p = new iPoint(x, y);
				if(get(p))
				{
					sum = sum.add(p);
					points++;
				}
			}
		if(points == 0)
			return null;
		return new iPoint(sum.x/points, sum.y/points);
	}
	
	/**
	 * Find the centroid of the shape(s)
	 * @return
	 */
	public Point2D findCentroid()
	{
		iPoint p = findCentroid_i();
		if(p == null)
			return null;
		else
			return p.realPoint();
	}
	
	/**
	 * Generate the entire image from a CSG experession recursively
	 * using a quad tree.
	 * @param ipsw
	 * @param ipne
	 * @param csg
	 */
	private void generateQuadTree(iPoint ipsw, iPoint ipne, CSG2D csgExpression)
	{
		Point2D inc = new Point2D(pixSize*0.5, pixSize*0.5);
		Point2D p0 = ipsw.realPoint();
		
		// Single pixel?
		
		if(ipsw.coincidesWith(ipne))
		{
			set(ipsw, csgExpression.value(p0) <= 0);
			return;
		}
		
		// Uniform rectangle?
		
		Point2D p1 = ipne.realPoint();
		Interval i = csgExpression.value(new Rectangle(Point2D.sub(p0, inc), Point2D.add(p1, inc)));
		if(!i.zero())
		{
			homogeneous(ipsw, ipne, i.high() <= 0);
			return;
		}
		
		// Non-uniform, but simple, rectangle
		
		if(csgExpression.complexity() <= simpleEnough)
		{
			heterogeneous(ipsw, ipne, csgExpression);
			return;
		}
	
		// Divide this rectangle into four (roughly) congruent quads.
		
		// Work out the corner coordinates.
		
		int x0 = ipsw.x;
		int y0 = ipsw.y;
		int x1 = ipne.x;
		int y1 = ipne.y;
		int xd = (x1 - x0 + 1);
		int yd = (y1 - y0 + 1);
		int xm = x0 + xd/2;
		if(xd == 2)
			xm--;
		int ym = y0 + yd/2;
		if(yd == 2)
			ym--;
		iPoint sw, ne;
		
		// Special case - a single vertical line of pixels
		
		if(xd <= 1)
		{
			if(yd <= 1)
				Debug.e("BooleanGrid.generateQuadTree: attempt to divide single pixel!");
			sw = new iPoint(x0, y0);
			ne = new iPoint(x0, ym);
			generateQuadTree(sw, ne, csgExpression.prune(new Rectangle(Point2D.sub(sw.realPoint(), inc), Point2D.add(ne.realPoint(), inc))));
			
			sw = new iPoint(x0, ym+1);
			ne = new iPoint(x0, y1);
			generateQuadTree(sw, ne, csgExpression.prune(new Rectangle(Point2D.sub(sw.realPoint(), inc), Point2D.add(ne.realPoint(), inc))));
			
			return;
		}
		
		// Special case - a single horizontal line of pixels
		
		if(yd <= 1)
		{
			sw = new iPoint(x0, y0);
			ne = new iPoint(xm, y0);
			generateQuadTree(sw, ne, csgExpression.prune(new Rectangle(Point2D.sub(sw.realPoint(), inc), Point2D.add(ne.realPoint(), inc))));
			
			sw = new iPoint(xm+1, y0);
			ne = new iPoint(x1, y0);
			generateQuadTree(sw, ne, csgExpression.prune(new Rectangle(Point2D.sub(sw.realPoint(), inc), Point2D.add(ne.realPoint(), inc))));
			
			return;
		}
		
		// General case - 4 quads.
		
		sw = new iPoint(x0, y0);
		ne = new iPoint(xm, ym);
		generateQuadTree(sw, ne, csgExpression.prune(new Rectangle(Point2D.sub(sw.realPoint(), inc), Point2D.add(ne.realPoint(), inc))));
		
		sw = new iPoint(x0, ym + 1);
		ne = new iPoint(xm, y1);
		generateQuadTree(sw, ne, csgExpression.prune(new Rectangle(Point2D.sub(sw.realPoint(), inc), Point2D.add(ne.realPoint(), inc))));
		
		sw = new iPoint(xm+1, ym + 1);
		ne = new iPoint(x1, y1);
		generateQuadTree(sw, ne, csgExpression.prune(new Rectangle(Point2D.sub(sw.realPoint(), inc), Point2D.add(ne.realPoint(), inc))));
		
		sw = new iPoint(xm+1, y0);
		ne = new iPoint(x1, ym);
		generateQuadTree(sw, ne, csgExpression.prune(new Rectangle(Point2D.sub(sw.realPoint(), inc), Point2D.add(ne.realPoint(), inc))));		

	}

	
	//*************************************************************************************
	
	/**
	 * Reset all the visited flags for the entire image
	 *
	 */
	public void resetVisited()
	{
		if(visited != null)
			visited.clear();
	}
	
	
	/**
	 * Is a pixel on an edge?
	 * If it is solid and there is air at at least one
	 * of north, south, east, or west, then yes; otherwise
	 * no.
	 * @param a
	 * @return
	 */
	private boolean isEdgePixel(iPoint a)
	{
		if(!get(a))
			return false;
		
		for(int i = 1; i < 8; i+=2)
			if(!get(a.add(neighbour[i])))
				return true;

		return false;
	}
	
//	/**
//	 * Sloppy edge-test for a pixel.
//	 * If it is solid and there is air at at least one
//	 * neighbour then yes; otherwise no.
//	 * @param a
//	 * @return
//	 */
//	private boolean isSloppyEdgePixel(iPoint a)
//	{
//		if(!get(a))
//			return false;
//		
//		for(int i = 0; i < 8; i++)
//			if(!get(a.add(neighbour[i])))
//				return true;
//
//		return false;
//	}
	

	/**
	 * Find the index in the bitmap of the next unvisited edge pixel after-and-including start.
	 * Return -1 if there isn't one.
	 * @param start
	 * @return
	 */
//	private int findUnvisitedEdgeIndex(int start)
//	{
////		if(visited == null)
////		{
////			int i = bits.nextSetBit(start);
////			if(i < 0)
////				return -1;
////			return i;
////		}
//
//		for(int i=bits.nextSetBit(start); i>=0; i=bits.nextSetBit(i+1)) 
//		{
//			if(visited == null)
//			{
//				if(isEdgePixel(pixel(i)))
//					return i;
//			} else
//			if(!visited.get(i))
//				if(isEdgePixel(pixel(i)))
//					return i;
//		}
//		return -1;		
//	}
	

	/**
	 * Remove whiskers (single threads of pixels) and similar nasties.
	 * TODO: also need to do the same for cracks?
	 *
	 */
	private void deWhisker()
	{
//		push("deWhisker... ");
//		
//		for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)) 
//		{
//			iPoint here = pixel(i);
//			if(neighbourCount(here) < 3)
//				set(here, false);
//		}
//		
//		for(int x = 0; x < rec.size.x - 1; x++)
//			for(int y = 0; y < rec.size.y - 1; y++)
//			{
//				iPoint start = new iPoint(x, y);
//				int m = marchPattern(start);
//				if(m == 6 || m == 9)
//				{
//					if(poll(start, 3) > 0.5)
//					{
//						set(start, true);
//						set(start.add(neighbour[1]), true);
//						set(start.add(neighbour[2]), true);
//						set(start.add(neighbour[2]), true);
//					} else
//					{
//						set(start, false);
//						set(start.add(neighbour[1]), false);
//						set(start.add(neighbour[2]), false);
//						set(start.add(neighbour[2]), false);						
//					}
//				}
//			}		
////		int n;
////		for(int passes = 0; passes < 2; passes++)
////		{
////			int i = findUnvisitedEdgeIndex(0);
////			while(i >= 0)
////			{
////				iPoint p = pixel(i);
////				int filterIndex = 0;
////				for(n = 0; n < 8; n++)
////					if(get(p.add(neighbour[n])))
////						filterIndex = filterIndex | (1<<n);
////				if(thinFilter[filterIndex])
////				{
////					//printNearby(p, 3);
////					set(p, false);
////					//printNearby(p, 3);
////				} else
////					i++;
////				
//////				for(n = 0; n < 8; n++)
//////					blockSize[n] = 0;
//////				boolean last = get(p.add(neighbour[7]));
//////				boolean here;
//////				int nCount = 0;
//////				int blockCount = 0;
//////				for(n = 0; n < 8; n++)
//////				{
//////					here = get(p.add(neighbour[n]));
//////					if(here)
//////						nCount++;
//////					if(here && !last)
//////						blockCount++;
//////					if(here && last)
//////						blockSize[n]++;
//////					last = here;
//////				}
//////
//////				if(blockCount > 1 || nCount < 2)
//////				{
//////					printNearby(p, 3);
//////					set(p, false);
//////					printNearby(p, 3);
//////				}
////				i = findUnvisitedEdgeIndex(i);
////			}
////			//System.out.println("end pass " + passes);
////		}
//		pop();
	}
	
	/**
	 * Look-up table to find the index of a neighbour point, n, from the point.
	 * @param n
	 * @return
	 */
	private int neighbourIndex(iPoint n)
	{
		switch((n.y + 1)*3 + n.x + 1)
		{
		case 0: return 0;
		case 1: return 1;
		case 2: return 2;
		case 3: return 7;
		case 5: return 3;
		case 6: return 6;
		case 7: return 5;
		case 8: return 4;
		default:
			Debug.e("BooleanGrid.neighbourIndex(): not a neighbour point!" + n.toString());	
		}
		return 0;
	}
	
	/**
	 * Count the solid neighbours of this point
	 * @param p
	 * @return
	 */
	private int neighbourCount(iPoint p)
	{
		int result = 0;
		for(int i = 0; i < 8; i++)
			if(get(p.add(neighbour[i])))
				result++;
		return result;
	}

	
	/**
	 * Find the index of the neighbouring point that's closest to a given real direction.
	 * @param p
	 * @return
	 */
	private int directionToNeighbour(Point2D p)
	{
		double score = Double.NEGATIVE_INFINITY;
		int result = -1;

		for(int i = 0; i < 8; i++)
		{
			// Can't use neighbour.realPoint as that adds swCorner...
			//  We have to normalise neighbour, to get answers proportional to cosines
			double s = Point2D.mul(p, new Point2D(neighbour[i].x, neighbour[i].y).norm()); 
			if(s > score)
			{
				result = i;
				score = s;
			}
		}
		if(result < 0)
			Debug.e("BooleanGrid.directionToNeighbour(): scalar product error!" + p.toString());
		return result;
	}
	
	
	/**
	 * Find a neighbour of a pixel that has not yet been visited, that is on an edge, and
	 * that is nearest to a given neighbour direction, nd.  If nd < 0 the first unvisited
	 * neighbour is returned.  If no valid neighbour exists, null is returned.  This prefers to
	 * visit valid pixels with few neighbours, and only after that tries to head in direction nd.
	 * @param a
	 * @param direction
	 * @return
	 */
	private iPoint findUnvisitedNeighbourOnEdgeInDirection(iPoint a, int nd)
	{
		iPoint result = null;
		int directionScore = -5;
		int neighbourScore = 9;
		for(int i = 0; i < 8; i++)
		{
			iPoint b = a.add(neighbour[i]);
			if(isEdgePixel(b))
				if(!vGet(b))
				{
					if(nd < 0)
						return b;
					int ns = neighbourCount(b);
					if(ns <= neighbourScore)
					{
						neighbourScore = ns;
						int s = neighbourProduct[Math.abs(nd - i)];
						if(s > directionScore)
						{
							directionScore = s;
							result = b;
						}
					}
				}
		}
		return result;
	}
	
	/**
	 * Useful debugging function
	 * @param p
	 * @param b
	 */
	private String printNearby(iPoint p, int b)
	{
		String op = new String();
		for(int y = p.y + b; y >= p.y - b; y--)
		{
			for(int x = p.x - b; x <= p.x + b; x++)
			{
				iPoint q = new iPoint(x, y);
				if(q.coincidesWith(p))
				{
					if(get(p))
						op += " +";
					else
						op += " o";
				}
				else if(get(q))
				{
					if(visited != null)
					{
						if(vGet(q))
							op += " v";
						else
							op += " 1";
					} else
						op += " 1";
				} else
					op += " .";
			}
			op += "\n";
		}
		return op;		
	}
	
//	/**
//	 * Recursive flood-fill of solid pixels from p to return a BooleanGrid of 
//	 * just the shape connected to that pixel.
//	 * @param p
//	 * @return
//	 */
//	private void floodCopy_r(iPoint p, BooleanGrid newGrid)
//	{
//		if(!this.get(p) || newGrid.get(p))
//			return;
//		
//		newGrid.set(p, true);
//		
//		floodCopy_r(p.add(neighbour[1]), newGrid);
//		floodCopy_r(p.add(neighbour[3]), newGrid);
//		floodCopy_r(p.add(neighbour[5]), newGrid);
//		floodCopy_r(p.add(neighbour[7]), newGrid);
//	}
	
	/**
	 * Recursive flood-fill of solid pixels from p to return a BooleanGrid of 
	 * just the shape connected to that pixel.
	 * TODO - replace with more efficient scan-line algorithm
	 * @param pp
	 * @return
	 */
	public BooleanGrid floodCopy(Point2D pp)
	{
		iPoint p = new iPoint(pp);
		if(!this.inside(p) || !this.get(p))
			return nothingThere;
		BooleanGrid result = new BooleanGrid();
		result.att = this.att;
		result.visited = null;
		result.rec= new iRectangle(this.rec);
		result.bits = new BitSet(result.rec.size.x*result.rec.size.y);
		
		// We implement our own floodfill stack, rather than using recursion to
		// avoid having to specify a big Java stack just for this one function.
		
		int top = 4000000;
		iPoint[] stack = new iPoint[top];
		int sp = 0;
		stack[sp] = p;
		iPoint q;
		
		while(sp > -1)
		{
			p = stack[sp];
			sp--;

			result.set(p, true);
			
			for(int i = 1; i < 8; i += 2)
			{
				q = p.add(neighbour[i]);
				if(this.get(q) && !result.get(q))
				{
					sp++;
					if(sp >= top)
					{
						Debug.e("BooleanGrid.floodCopy(): stack overflow!");
						return result;
					}
					stack[sp] = q;
				}
			}
		}
		
		return result;
	}
	

	
	/**
	 * Calculate the 4-bit marching squares value for a point
	 * @param ip
	 * @return
	 */
	private int marchPattern(iPoint ip)
	{
		int result = 0;
		
		if(get(ip)) result |= 1;
		if(get(ip.add(neighbour[3]))) result |= 2;
		if(get(ip.add(neighbour[1]))) result |= 4;
		if(get(ip.add(neighbour[2]))) result |= 8;
		return result;
	}
	
	//********************************************************************************
	
	// Return geometrical constructions based on the pattern
	
	/**
	 * Return all the outlines of all the solid areas as polygons consisting of
	 * all the pixels that make up the outlines.
	 * @return
	 */
	private iPolygonList iAllPerimitersRaw()
	{
		return marchAll();
	}
	
	/**
	 * Return all the outlines of all the solid areas as polygons in
	 * their simplest form.
	 * @return
	 */
	private iPolygonList iAllPerimiters()
	{
		return iAllPerimitersRaw().simplify();
	}
	
	/**
	 * Return all the outlines of all the solid areas as 
	 * real-world polygons with attributes a
	 * @param a
	 * @return
	 */
	public PolygonList allPerimiters(Attributes a)
	{
		PolygonList r = iAllPerimiters().realPolygons(a);
		r = r.simplify(realResolution);	
		return r;
	}
	
	private double poll(iPoint p, int b)
	{
		int result = 0;
		iPoint q;
		for(int y = p.y + b; y >= p.y - b; y--)
			for(int x = p.x - b; x <= p.x + b; x++)
			{
				q = new iPoint(x, y);
				if(get(q)) result++;
			}
		b++;
		return (double)result/(double)(b*b);
	}
	
	/**
	 * Run over the entire pattern removing the following two types of 2x2 cells:
	 * 
	 *    . 1     1 .
	 *    1 .     . 1
	 *    
	 * by deleting the topmost point.
	 */
	private void clearSaddles()
	{
		
	}
	
	/**
	 * Run marching squares round the polygon starting with the 2x2 march pattern at start
	 * @param start
	 * @return
	 */
//	private iPolygon marchRound(iPoint start)
//	{
//		iPolygon result = new iPolygon(true);
//		
//		iPoint here = new iPoint(start);
//		iPoint pix;
//		int m;
//		boolean step = true;
//		
//		do
//		{
//			m = marchPattern(here);
//			//pix = new iPoint(here);
//			switch(m)
//			{
//			case 1:
//				if(!vGet(here))
//				{
//					result.add(here);
//					vSet(here,true);
//				}
//				break;
//			case 2:
//				pix = here.add(neighbour[3]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}
//				break;
//			case 3:
//				if(!vGet(here))
//				{
//					result.add(here);
//					vSet(here,true);
//				}
//				pix = here.add(neighbour[3]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}					
//				break;
//			case 4:
//				pix = here.add(neighbour[1]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}				
//				break;
//			case 5:
//				if(!vGet(here))
//				{
//					result.add(here);
//					vSet(here,true);
//				}				
//				pix = here.add(neighbour[1]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}				
//				break;
//			case 6:
//				Debug.e("BooleanGrid.marchRound() - dud 2x2 grid: " + m + " at " + 
//						here.toString() + "\n" + printNearby(here,4) + "\n\n");
//				step = false;
//				pix = here.add(neighbour[3]);
//				set(pix, false);
//				vSet(pix, false);
//				pix = here.add(neighbour[1]);
//				set(pix, false);
//				vSet(pix, false);
//				here = result.point(result.size() - 1);
//				if(!get(here))
//				{
//					if(result.size() > 1)
//					{
//						result.remove(result.size() - 1);
//						here = result.point(result.size() - 1);
//						if(!get(here))
//						{
//							Debug.e("BooleanGrid.marchRound() - backtracked to an unfilled point!" + printNearby(here,4) + "\n\n");
//							result.remove(result.size() - 1);
//							here = result.point(result.size() - 1);
//						}
//					} else
//					{
//						here = start;
//					}
//				}
//				break;
//
//			case 7:
//				pix = here.add(neighbour[1]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}
//				pix = here.add(neighbour[3]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}						
//				break;
//			case 8:
//				pix = here.add(neighbour[2]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}						
//				break;
//			case 9:
//				Debug.e("BooleanGrid.marchRound() - dud 2x2 grid: " + m + " at " + 
//						here.toString() + "\n" + printNearby(here,4) + "\n\n");
//				step = false;
//				set(here, false);
//				vSet(here, false);
//				pix = here.add(neighbour[2]);
//				set(pix, false);
//				vSet(pix, false);				
//				here = result.point(result.size() - 1);
//				if(!get(here))
//				{
//					if(result.size() > 1)
//					{
//						result.remove(result.size() - 1);
//						here = result.point(result.size() - 1);
//						if(!get(here))
//						{
//							Debug.e("BooleanGrid.marchRound() - backtracked to an unfilled point!" + printNearby(here,4) + "\n\n");
//							result.remove(result.size() - 1);
//							here = result.point(result.size() - 1);
//						}
//					}else
//					{
//						here = start;
//					}
//				}
//				
//				break;
//
//			case 10:
//				pix = here.add(neighbour[3]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}
//				pix = here.add(neighbour[2]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}						
//				break;
//			case 11:
//				if(!vGet(here))
//				{
//					result.add(here);
//					vSet(here,true);
//				}
//				pix = here.add(neighbour[2]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}						
//				break;
//			case 12:
//				pix = here.add(neighbour[2]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}
//				pix = here.add(neighbour[1]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}							
//				break;
//			case 13:
//				pix = here.add(neighbour[2]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}
//				if(!vGet(here))
//				{
//					result.add(here);
//					vSet(here,true);
//				}					
//				break;
//			case 14:
//				pix = here.add(neighbour[3]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}
//				pix = here.add(neighbour[1]);
//				if(!vGet(pix))
//				{
//					result.add(pix);
//					vSet(pix,true);
//				}						
//				break;
//			
//			default:
//				Debug.e("BooleanGrid.marchRound() - dud 2x2 grid: " + m + " at " + 
//						here.toString() + "\n" + printNearby(here,4) + "\n\n");
//				return result;
//			}
//			if(step)
//				here = here.add(neighbour[march[m]]);
//			step = true;
//		} while(!here.coincidesWith(start));
//		
//		return result;
//	}
	
	/**
	 * A single step of marching squares
	 * 
	 * See this useful web page:
	 * 
	 * http://devblog.phillipspiess.com/2010/02/23/better-know-an-algorithm-1-marching-squares/
	 * 
	 * @param here
	 * @param previous
	 * @return
	 */
	private int step(iPoint here, int previous)
	{
		int m = marchPattern(here);
		int result = march[m];
		if(m == 6)
		{
			if(previous == 5)
				result = 7;
			else
				result = 3;
		}
		if(m == 9)
		{
			if(previous == 3)
				result = 5;
			else
				result = 1;
		}
		return result;
	}
	
	/**
	 * Run marching squares round the polygon starting with the 2x2 march pattern at start
	 * @param start
	 * @return
	 */
	private iPolygon marchRound(iPoint start)
	{
		iPolygon result = new iPolygon(true);
		
		iPoint here = new iPoint(start);
		int next;
		int previous = 3;
		
		do
		{
			next = step(here, previous);
			result.add(here);
			vSet(here, true);
			here = here.add(neighbour[next]);
			previous = next;
		} while(!here.coincidesWith(start));
		
		return result;
	}
	
	/**
	 * Run marching squares round all polygons in the pattern, returning a list of them all
	 * @return
	 */
	private iPolygonList marchAll()
	{
		//if(isThin)
		//	return marchLines(); // XXXXXXXXXXXXXXXXXXXXXXX
		iPolygonList result = new iPolygonList();
		if(isEmpty())
			return result;
		iPoint start;
		iPolygon p;
		int m;
		
		for(int x = 0; x < rec.size.x - 1; x++)
			for(int y = 0; y < rec.size.y - 1; y++)
			{
				start = new iPoint(x, y);
				m = marchPattern(start);
				if(m != 0 && m != 15)
				{
					if( !( vGet(start) || vGet(start.add(neighbour[1])) || vGet(start.add(neighbour[2])) || vGet(start.add(neighbour[3])) ) )
					{
						p = marchRound(start);
						if(p.size() > 2)
							result.add(p);
					}
				}
			}
		resetVisited();	
		return result;
	}
	
	/**
	 * Generate a sequence of point-pairs where the line h enters
	 * and leaves solid areas.  The point pairs are stored in a 
	 * polygon, which should consequently have an even number of points
	 * in it on return.
	 * @param h
	 * @return
	 */
	private iPolygon hatch(HalfPlane h)
	{
		iPolygon result = new iPolygon(false);
		
		Interval se = box().wipe(h.pLine(), Interval.bigInterval());
		
		if(se.empty())
			return result;
		
		iPoint s = new iPoint(h.pLine().point(se.low()));
		iPoint e = new iPoint(h.pLine().point(se.high()));
		if(get(s))
			Debug.e("BooleanGrid.hatch(): start point is in solid!");
		DDA dda = new DDA(s, e);
		
		iPoint n = dda.next();
		iPoint nOld = n;
		boolean v;
		boolean vs = false;
		while(n != null)
		{
			v = get(n);
			if(v != vs)
			{
				if(v)
					result.add(n);
				else
					result.add(nOld);
			}
			vs = v;
			nOld = n;
			n = dda.next();
		}
		
		if(get(e))
		{
			Debug.e("BooleanGrid.hatch(): end point is in solid!");
			result.add(e);
		}
		
		if(result.size()%2 != 0)
			Debug.e("BooleanGrid.hatch(): odd number of crossings: " + result.size());
		return result;
	}
	
    /**
     * Find the bit of polygon edge between start/originPlane and targetPlane
     * TODO: origin == target!!!
     * @param start
     * @param hatches
     * @param originP
     * @param targetP
     * @return polygon edge between start/originaPlane and targetPlane
     */
    private SnakeEnd goToPlane(iPoint start, List<HalfPlane> hatches, int originP, int targetP) 
    {
    	iPolygon track = new iPolygon(false);
    	
    	HalfPlane originPlane = hatches.get(originP);
    	HalfPlane targetPlane = hatches.get(targetP);
    	

    		int dir = directionToNeighbour(originPlane.normal());

    			if(originPlane.value(targetPlane.pLine().origin()) < 0)
    				dir = neighbourIndex(neighbour[dir].neg());

    	if(!get(start))
    	{
    		Debug.e("BooleanGrid.goToPlane(): start is not solid!");
    		return null;
    	}
    	
    	double vTarget = targetPlane.value(start.realPoint());
    	
    	vSet(start, true);
    	
    	iPoint p = findUnvisitedNeighbourOnEdgeInDirection(start, dir);
    	if(p == null)
    		return null;
    	
    	iPoint pNew;
    	double vOrigin = originPlane.value(p.realPoint());
    	boolean notCrossedOriginPlane = originPlane.value(p.realPoint())*vOrigin >= 0;
    	boolean notCrossedTargetPlane = targetPlane.value(p.realPoint())*vTarget >= 0;
    	while(p != null && notCrossedOriginPlane && notCrossedTargetPlane)
    	{
    		track.add(p);
    		vSet(p, true);
    		pNew = findUnvisitedNeighbourOnEdgeInDirection(p, dir);
    		if(pNew == null)
    		{
    			for(int i = 0; i < track.size(); i++)
    				vSet(track.point(i), false);
    			return null;
    		}
    		dir = neighbourIndex(pNew.sub(p));
    		p = pNew;
    		notCrossedOriginPlane = originPlane.value(p.realPoint())*vOrigin >= 0;
    		notCrossedTargetPlane = targetPlane.value(p.realPoint())*vTarget >= 0;
    	}
    	
    	if(notCrossedOriginPlane)
    		return(new SnakeEnd(track, targetP));
    	
       	if(notCrossedTargetPlane)
    		return(new SnakeEnd(track, originP));
       	
       	Debug.e("BooleanGrid.goToPlane(): invalid ending!");
       	
    	return null;
    }
    
    /**
     * Find the piece of edge between start and end (if there is one).
     * @param start
     * @param end
     * @return
     */
    private iPolygon goToPoint(iPoint start, iPoint end, HalfPlane hatch, double tooFar) 
    {
    	iPolygon track = new iPolygon(false);
    	
    	iPoint diff = end.sub(start);
    	if(diff.x == 0 && diff.y == 0)
    	{
    		track.add(start);
    		return track;
    	}

    	int dir = directionToNeighbour(new Point2D(diff.x, diff.y));

    	if(!get(start))
    	{
    		Debug.e("BooleanGrid.goToPlane(): start is not solid!");
    		return null;
    	}
    	
    	vSet(start, true);
    	
    	iPoint p = findUnvisitedNeighbourOnEdgeInDirection(start, dir);
    	if(p == null)
    		return null;

    	while(true)
    	{
    		track.add(p);
    		vSet(p, true);
    		p = findUnvisitedNeighbourOnEdgeInDirection(p, dir);
    		boolean lost = p == null;
    		if(!lost)
    			lost = Math.abs(hatch.value(p.realPoint())) > tooFar;
    		if(lost)
    		{
    			for(int i = 0; i < track.size(); i++)
    				vSet(track.point(i), false);
    			vSet(start, false);
    			return null;
    		}
    		diff = end.sub(p);
    		if(diff.magnitude2() < 3)
    			return track;
        	dir = directionToNeighbour(new Point2D(diff.x, diff.y));
    	}
    }

    /**
     * Take a list of hatch point pairs from hatch (above) and the corresponding lines
     * that created them, and stitch them together to make a weaving snake-like hatching
     * pattern for infill.
     * @param ipl
     * @param hatches
     * @param thisHatch
     * @param thisPt
     * @return
     */
	private iPolygon snakeGrow(iPolygonList ipl, List<HalfPlane> hatches, int thisHatch, int thisPt) 
	{
		iPolygon result = new iPolygon(false);
		
		iPolygon thisPolygon = ipl.polygon(thisHatch);
		iPoint pt = thisPolygon.point(thisPt);
		result.add(pt);
		SnakeEnd jump;
		do
		{
			thisPolygon.remove(thisPt);
			if(thisPt%2 != 0)
				thisPt--;
			pt = thisPolygon.point(thisPt);
			result.add(pt);
			thisHatch++;
			if(thisHatch < hatches.size())
				jump = goToPlane(pt, hatches, thisHatch - 1, thisHatch); 
			else 
				jump = null;
			thisPolygon.remove(thisPt);
			if(jump != null)
			{
				result.add(jump.track);
				thisHatch = jump.hitPlaneIndex;
				thisPolygon = ipl.polygon(thisHatch);
				thisPt = thisPolygon.nearest(jump.track.point(jump.track.size() - 1), 10);
			}
		} while(jump != null && thisPt >= 0);		
		return result;
	}
	
	/**
	 * Fine the nearest plane in the hatch to a given point
	 * @param p
	 * @param hatches
	 * @return
	 */
	HalfPlane hPlane(iPoint p, List<HalfPlane> hatches)
	{
		int bot = 0;
		int top = hatches.size() - 1;
		Point2D rp = p.realPoint();
		double dbot = Math.abs(hatches.get(bot).value(rp));
		double dtop = Math.abs(hatches.get(top).value(rp));
		while(top - bot > 1)
		{
			int mid = (top + bot)/2;
			if(dbot < dtop)
			{
				top = mid;
				dtop = Math.abs(hatches.get(top).value(rp));
			} else
			{
				bot = mid;
				dbot = Math.abs(hatches.get(bot).value(rp));				
			}
		}
		if(dtop < dbot)
			return hatches.get(top);
		else
			return hatches.get(bot);
	}
	
	/**
	 * Run through the snakes, trying to join them up to make longer snakes
	 * @param snakes
	 * @param hatches
	 * @param gap
	 */
	void joinUpSnakes(iPolygonList snakes, List<HalfPlane> hatches, double gap)
	{
		int i = 0;
		if(hatches.size() <= 0)
			return;
		Point2D n = hatches.get(0).normal();
		iPolygon track;
		while(i < snakes.size())
		{
			iPoint iStart = snakes.polygon(i).point(0);
			iPoint iEnd = snakes.polygon(i).point(snakes.polygon(i).size() - 1);
			double d;
			int j = i+1;
			boolean incrementI = true;
			while(j < snakes.size())
			{
				iPoint jStart = snakes.polygon(j).point(0);
				iPoint jEnd = snakes.polygon(j).point(snakes.polygon(j).size() - 1);
				incrementI = true;
				
				Point2D diff = Point2D.sub(jStart.realPoint(), iStart.realPoint());
				d = Point2D.mul(diff, n);
				if(Math.abs(d) < 1.5*gap)
				{
					track = goToPoint(iStart, jStart, hPlane(iStart, hatches), gap);
					if(track != null)
					{
						iPolygon p = snakes.polygon(i).negate();
						p.add(track);
						p.add(snakes.polygon(j));
						snakes.set(i, p);
						snakes.remove(j);
						incrementI = false;
						break;
					}
				}
				
				diff = Point2D.sub(jEnd.realPoint(), iStart.realPoint());
				d = Point2D.mul(diff, n);
				if(Math.abs(d) < 1.5*gap)
				{
					track = goToPoint(iStart, jEnd, hPlane(iStart, hatches), gap);
					if(track != null)
					{
						iPolygon p = snakes.polygon(j);
						p.add(track.negate());
						p.add(snakes.polygon(i));
						snakes.set(i, p);
						snakes.remove(j);
						incrementI = false;
						break;						
					}
				}
				
				diff = Point2D.sub(jStart.realPoint(), iEnd.realPoint());
				d = Point2D.mul(diff, n);
				if(Math.abs(d) < 1.5*gap)
				{
					track = goToPoint(iEnd, jStart, hPlane(iEnd, hatches), gap);
					if(track != null)
					{
						iPolygon p = snakes.polygon(i);
						p.add(track);
						p.add(snakes.polygon(j));
						snakes.set(i, p);
						snakes.remove(j);
						incrementI = false;
						break;
					}
				}
				
				diff = Point2D.sub(jEnd.realPoint(), iEnd.realPoint());
				d = Point2D.mul(diff, n);
				if(Math.abs(d) < 1.5*gap)
				{
					track = goToPoint(iEnd, jEnd, hPlane(iEnd, hatches), gap);
					if(track != null)
					{
						iPolygon p = snakes.polygon(i);
						p.add(track);
						p.add(snakes.polygon(j).negate());
						snakes.set(i, p);
						snakes.remove(j);	
						incrementI = false;
						break;
					}						
				}
				j++;
			}
			if(incrementI)
				i++;
		}
	}
	
	/**
	 * Hatch all the polygons parallel to line hp with increment gap
	 * @param hp
	 * @param gap
	 * @param a
	 * @return a polygon list of hatch lines as the result with attributes a
	 */
	public PolygonList hatch(HalfPlane hp, double gap, Attributes a) //, Rr2Point startNearHere)
	{	
		//push("Computing hatching... ");
		
		if(gap <= 0) // Means the user has turned infill off for this; return an empty list.
			return new PolygonList();
			
		Rectangle big = box().scale(1.1);
		double d = Math.sqrt(big.dSquared());
		
		Point2D orth = hp.normal();
		
		int quadPointing = (int)(2 + 2*Math.atan2(orth.y(), orth.x())/Math.PI);
		
		Point2D org = big.ne();
		
		switch(quadPointing)
		{	
		case 1:
			org = big.nw();
			break;
			
		case 2:
			org = big.sw(); 
			break;
			
		case 3:
			org = big.se();
			break;
		
		case 0:
		default:
			break;
		}
		
		double dist = Point2D.mul(org, orth)/gap;
		dist = (1 + (long)dist)*gap;
		HalfPlane hatcher = new HalfPlane(hp);
		hatcher = hatcher.offset(dist);
		
		//HalfPlane hatcher = new 
		//	HalfPlane(org, Point2D.add(org, hp.pLine().direction()));
		

		List<HalfPlane> hatches = new ArrayList<>();
		iPolygonList iHatches = new iPolygonList();
		
		double g = 0;		
		while (g < d)
		{
			iPolygon ip = hatch(hatcher);
			
			if(ip.size() > 0)
			{
				hatches.add(hatcher);
				iHatches.add(ip);
			}
			hatcher = hatcher.offset(gap);
			g += gap;
		}
		
		// Now we have the individual hatch lines, join them up
		
		iPolygonList snakes = new iPolygonList();
		int segment;
		do
		{
			segment = -1;
			for(int i = 0; i < iHatches.size(); i++)
			{
				if((iHatches.polygon(i)).size() > 0)
				{
					segment = i;
					break;
				}
			}
			if(segment >= 0)
			{
				snakes.add(snakeGrow(iHatches, hatches, segment, 0));
			}
		} while(segment >= 0);
		

		try
		{
			if(Preferences.loadGlobalBool("PathOptimise"))
				joinUpSnakes(snakes, hatches, gap);
		} catch (IOException e)
		{}
		
		resetVisited();
		
		PolygonList result = snakes.realPolygons(a).simplify(realResolution);
		//result = result.nearEnds(startNearHere);
		
		//pop();
		return result;
	}
	
	/**
	 * This assumes that shrunk is this bitmap offset by dist from a previous calculation.
	 * It grows shrunk by -dist, then subtracts that from itself.  The result is a bitmap of all the
	 * thin lines in this pattern that were discarded by the original offset (plus some noise at places
	 * square convex corners that have grown back rounded).
	 * @param shrunk
	 * @param dist
	 * @return
	 */
	public BooleanGrid lines(BooleanGrid shrunk, double dist)
	{
		if(dist >=0)
		{
			Debug.e("BooleanGrid.lines() called with non-negative offset: " + dist);
			return new BooleanGrid();
		}
		return difference(this, shrunk.offset(-dist));
	}
	
	
	/**
	 * Offset the pattern by a given real-world distance.  If the distance is
	 * negative the pattern is shrunk; if it is positive it is grown;
	 * @param dist
	 * @return
	 */
	public BooleanGrid offset(double dist)
	{
		int r = iScale(dist);
		
		BooleanGrid result = new BooleanGrid(this, rec.offset(r));
		if(r == 0)
			return result;

		iPolygonList polygons = iAllPerimiters().translate(rec.swCorner.sub(result.rec.swCorner));
		if(polygons.size() <= 0)
		{
			iRectangle newRec = new iRectangle(result.rec);
			newRec.size.x = 1;
			newRec.size.y = 1;
			return new BooleanGrid(CSG2D.nothing(), newRec.realRectangle(), att);
		}

		for(int p = 0; p < polygons.size(); p++)
		{
			iPolygon ip = polygons.polygon(p);
			for(int e = 0; e < ip.size(); e++)
			{
				iPoint p0 = ip.point(e);
				iPoint p1 = ip.point((e+1)%ip.size());
				result.rectangle(p0, p1, Math.abs(r), r > 0);
				result.disc(p1, Math.abs(r), r > 0);
			}
		}
		if(result.isEmpty())
			return nothingThere;
			result.deWhisker();
		return result;
	}
	
	//*********************************************************************************************************
	
	// Boolean operators on the bitmap
	
	
	/**
	 * Complement a grid
	 * N.B. the grid doesn't get bigger, even though the expression
	 * it contains may now fill the whole of space.
	 * @return
	 */
	public BooleanGrid complement()
	{
		BooleanGrid result = new BooleanGrid(this);
		result.bits.flip(0, result.rec.size.x*result.rec.size.y - 1);
		//result.deWhisker();
		return result;
	}
	

	/**
	 * Compute the union of two bit patterns, forcing attribute a on the result.
	 * @param d
	 * @param e
	 * @param a
	 * @return
	 */
	public static BooleanGrid union(BooleanGrid d, BooleanGrid e, Attributes a)
	{	
		BooleanGrid result;
		
		if(d == nothingThere)
		{
			if(e == nothingThere)
				return nothingThere;
			if(e.att == a)
				return e;
			result = new BooleanGrid(e);
			result.forceAttribute(a);
			return result;
		}
		
		if(e == nothingThere)
		{
			if(d.att == a)
				return d;
			result = new BooleanGrid(d);
			result.forceAttribute(a);
			return result;
		}

		if(d.rec.coincidesWith(e.rec))
		{
			result = new BooleanGrid(d);
			result.bits.or(e.bits);
		} else
		{
			iRectangle u = d.rec.union(e.rec);
			result = new BooleanGrid(d, u);
			BooleanGrid temp = new BooleanGrid(e, u);
			result.bits.or(temp.bits);
		}
		//result.deWhisker();
		result.forceAttribute(a);
		return result;
	}
	
	/**
	 * Compute the union of two bit patterns
	 * @param d
	 * @param e
	 * @return
	 */
	public static BooleanGrid union(BooleanGrid d, BooleanGrid e)
	{
		BooleanGrid result = union(d, e, d.att);
		if(result != nothingThere && !d.att.getMaterial().contentEquals(e.att.getMaterial()))
			Debug.e("BooleanGrid.union(): attempt to union two bitmaps of different materials: " +
					d.attribute().getMaterial() + " and " + e.attribute().getMaterial()	);
		return result;
	}
	
	
	/**
	 * Compute the intersection of two  bit patterns
	 * @param d
	 * @param e
	 * @return
	 */
	public static BooleanGrid intersection(BooleanGrid d, BooleanGrid e, Attributes a)
	{	
		BooleanGrid result;
		
		if(d == nothingThere || e == nothingThere)
			return nothingThere;

		if(d.rec.coincidesWith(e.rec))
		{
			result = new BooleanGrid(d);
			result.bits.and(e.bits);
		} else
		{

			iRectangle u = d.rec.intersection(e.rec);
			if(u.isEmpty())
				return nothingThere;
			result = new BooleanGrid(d, u);
			BooleanGrid temp = new BooleanGrid(e, u);
			result.bits.and(temp.bits);
		}
		if(result.isEmpty())
			return nothingThere;
		result.deWhisker();
		result.forceAttribute(a);
		return result;
	}

	/**
	 * Compute the intersection of two  bit patterns
	 * @param d
	 * @param e
	 * @return
	 */
	public static BooleanGrid intersection(BooleanGrid d, BooleanGrid e)
	{
		BooleanGrid result = intersection(d, e, d.att);
		if(result != nothingThere && !d.att.getMaterial().contentEquals(e.att.getMaterial()))
			Debug.e("BooleanGrid.intersection(): attempt to intersect two bitmaps of different materials: " +
					d.attribute().getMaterial() + " and " + e.attribute().getMaterial()	);
		return result;
	}
	
	/**
	 * Grid d - grid e, forcing attribute a on the result
	 * d's rectangle is presumed to contain the result.
	 * TODO: write a function to compute the rectangle from the bitmap
	 * @param d
	 * @param e
	 * @param a
	 * @return
	 */
	public static BooleanGrid difference(BooleanGrid d, BooleanGrid e, Attributes a)
	{
		if(d == nothingThere)
			return nothingThere;
		
		BooleanGrid result;
		
		if(e == nothingThere)
		{
			if(d.att == a)
				return d;
			result = new BooleanGrid(d);
			result.forceAttribute(a);
			return result;
		}
		
		result = new BooleanGrid(d);
		BooleanGrid temp;
		if(d.rec.coincidesWith(e.rec))
			temp = e;
		else
			temp = new BooleanGrid(e, result.rec);
		result.bits.andNot(temp.bits);
		if(result.isEmpty())
			return nothingThere;
		result.deWhisker();
		result.forceAttribute(a);
		return result;
	}
	/**
	 * Grid d - grid e
	 * d's rectangle is presumed to contain the result.
	 * TODO: write a function to compute the rectangle from the bitmap
	 * @param d
	 * @param e
	 * @return
	 */
	public static BooleanGrid difference(BooleanGrid d, BooleanGrid e)
	{
		BooleanGrid result = difference(d, e, d.att);
		if(result != nothingThere && !d.att.getMaterial().contentEquals(e.att.getMaterial()))
			Debug.e("BooleanGrid.difference(): attempt to subtract two bitmaps of different materials: " +
					d.attribute().getMaterial() + " and " + e.attribute().getMaterial()	);
		return result;
	}
}
