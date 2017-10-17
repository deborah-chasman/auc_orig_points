package auc;

import java.util.*;
import java.io.*;


/**
 * Curve represents a curve in [0,1]X[0,1] space with a series of 
 * lines between the specified points and assuming a horizontal line
 * from the first and last points (sorted by x) to the respective
 * edges. Points from ROC and PR (with sufficient interpolation) can be
 * given to the Curve class to calculate area under the curve and for
 * vertical averaging.
 * @author Kendrick Boyd 2010
 */
public class Curve { 
	/**
	 * Points in the curve, in tree set for fast
	 * checking for duplicates and locating nearest
	 * values.
	 */
	private TreeSet<Point> points = null;

	/**
	 * How to sort y-value for same x-values, false sorts
	 * descending by y-value and should be used for PR curves,
	 * true sorts ascending by y-value and should be used for
	 * ROC curves
	 */
	private boolean sortAsc = true;

	/**
	 * Constructor
	 * @param sortAsc true to sort same x-values by asc y-value, 
	 *        false to sort by desc y-value
	 */
	public Curve(boolean sortAsc) {
		this.sortAsc = sortAsc;
		Comparator<Point> myComp = null;
		if (sortAsc) {

			myComp = new Comparator<Point>() {
				public int compare(Point p1, Point p2) {
					if (p1.getX()+Point.EPSILON<p2.getX()) return -1;
					else if (p1.getX()>p2.getX()+Point.EPSILON) return 1;
					else if (p1.getY()+Point.EPSILON<p2.getY()) return -1;
					else if (p1.getY()>p2.getY()+Point.EPSILON) return 1;
					else return 0;
				}
			};
		}
		else {
			myComp = new Comparator<Point>() {
				public int compare(Point p1, Point p2) {
					if (p1.getX()+Point.EPSILON<p2.getX()) return -1;
					else if (p1.getX()>p2.getX()+Point.EPSILON) return 1;
					else if (p1.getY()+Point.EPSILON<p2.getY()) return 1;
					else if (p1.getY()>p2.getY()+Point.EPSILON) return -1;
					else return 0;
				}
			};
		}
		points = new TreeSet<Point>(myComp);

	}

	/**
	 * Adds a new point to the curve
	 * @param x x value
	 * @param y y value
	 */
	public void add(double x,double y) {
		Point newP = new Point(x,y);
		if (!points.contains(newP)) {
			points.add(newP);
		}
	}


	/**
	 * Write the points defining this curve the specified
	 * file in x<tab>y<newline> format.
	 * @param filename file to write
	 */
	public void write(String filename) {
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(filename));
			write(pw);
			pw.close();
		}
		catch (IOException eIO) {
			eIO.printStackTrace();
		}
	}
	/**
	 * Write the points defining this curve to the specified
	 * PrintWriter in x<tab>y<newline> format.
	 * @param pw writer to write
	 * @throws IOException 
	 */
	public void write(PrintWriter pw) throws IOException {
		if (points.size()>0) {	    
			// write points
			for (Point p : points) {
				pw.println(p.getX() + "\t" + p.getY());
			}	    
		}
		else {
			throw new RuntimeException("Cannot write a curve with no points");
		}
	}


	/**
	 * Returns y-value at a specified x-value using
	 * linear interpolation if necessary.
	 * @param x x value
	 * @return y value
	 */
	public double getY(double x) {
		if (points.size()>0) {
			Point test = new Point(x,0.0);
			Point before = points.lower(test);
			Point after = points.ceiling(test);
			// check if exact match
			if (after!=null && Math.abs(after.getX()-x)<=Point.EPSILON) {
				// returns smallest if sorted asc
				// and largest if sorted desc ... I think that's ok
				return after.getY();
			}
			else if (before!=null && Math.abs(before.getX()-x)<=Point.EPSILON) {
				return before.getY();
			}
			else {
				// interpolate
				if (before==null) before = new Point(0.0,points.first().getY());
				if (after==null) after = new Point(1.0,points.last().getY());
				double slope = (after.getY()-before.getY())/
				(after.getX()-before.getX());
				return before.getY() + (x-before.getX())*slope;		
			}
		}
		else {
			throw new RuntimeException("Cannot obtain a y-value for a curve with no points");
		}

	}

	/**
	 * Returns area under curve from 0.0-1.0
	 * @return area
	 */
	public double getArea() {
		return getArea(0.0);
	}


	/**
	 * Returns area under the curve such that minX<=x<=1.0 using
	 * trapezoids.
	 * @return area
	 */
	public double getArea(double minX) {
		if (points.size()>0) {	    
			// find area
			double area = 0.0;
			// start at x=0.0
			Point prev = new Point(0.0,points.first().getY());
			Iterator<Point> itr = points.iterator();
			//	    for (Point p : points) {
			while (itr.hasNext()) {
				Point p = itr.next();
				if (p.getX()>=minX && prev!=null) {
					// NOTE - if prev==null i's be first point which has
					//        x=0.0 so don't need to calculate a
					//        previous trapezoid

					// include in area
					if (prev.getX()<minX) {
						// partial trapezoid

						// find y value of line at x=minX
						double slope = (p.getY()-prev.getY())/(p.getX()-prev.getX());			
						double y = prev.getY() + slope*(minX-prev.getX());

						area += 0.5 * (p.getX() - minX) * (p.getY() + y);
					}
					else {
						// full trapezoid
						area += 0.5*(p.getX()-prev.getX())*(p.getY()+prev.getY());
					}
				}
				prev = p;
			}
			// get any last bit before 1.0
			Point p = new Point(1.0,points.last().getY());
			if (p.getX()>=minX && prev!=null) {		
				// include in area
				if (prev.getX()<minX) {
					// partial trapezoid

					// find y value of line at x=minX
					double slope = (p.getY()-prev.getY())/(p.getX()-prev.getX());			
					double y = prev.getY() + slope*(minX-p.getX());

					area += 0.5 * (p.getX() - minX) * (p.getY() + y);
				}
				else {
					// full trapezoid
					area += 0.5*(p.getX()-prev.getX())*(p.getY()+prev.getY());
				}
			}

			return area;
		}
		else {
			throw new RuntimeException("Cannot find area of a curve with no points");
		}	
	}

	/**
	 * Create standardized curve with num+1 points
	 * @param num number of samples
	 * @return Curve object
	 */
	public Curve createStandardized(int num) {
		Curve ret = new Curve(sortAsc);

		for (int i=0;i<=num;i++) {
			double x = (1.0*i)/num;
			ret.add(x,getY(x));
		}
		return ret;
	}


	/**
	 * Create vertical average of the list of curves
	 * using default of 100 points
	 * @param curves list of curves to average
	 * @return new curve object representing the average at each
	 * sample point
	 */
	public static Curve createVerticalAverage(ArrayList<Curve> curves) {
		return createVerticalAverage(curves,100);
	}

	/**
	 * Create vertical average of the list of curves using
	 * specified number of averaging points.
	 * @param curves list of curves to average
	 * @param num number of sample points (really will use num+1)
	 * @return new curve object representing the average at each
	 * sample point
	 */
	public static Curve createVerticalAverage(ArrayList<Curve> curves,
			int num) {
		Curve curve = new Curve(curves.get(0).sortAsc);
		for (int i=0;i<=num;i++) {
			double x = (1.0*i)/num;
			double total = 0.0;
			for (Curve c : curves) {
				total += c.getY(x);
			}
			curve.add(x,total/curves.size());
		}
		return curve;
	}

	/**
	 * main method to run some tests
	 */
	public static void main(String args[]) {

		// create a simple curve
		Curve curve = new Curve(true);
		curve.add(0.0,0.0);
		curve.add(0.5,0.8);
		curve.add(1.0,1.0);

		System.out.println("Curve: (0.0,0.0), (0.5,0.8), (1.0,1.0)");
		System.out.println("Area: " + curve.getArea() + " (should be 0.65)");
		System.out.println("Area>=0.25: " + curve.getArea(0.25) + " (should be 0.6)");
		for (int i=0;i<11;i++) {
			double x = i/10.0;
			System.out.println("f(" + x + ") = " + curve.getY(x));
		}
		ArrayList<Curve> curves = new ArrayList<Curve>();
		curves.add(curve);

		// create a simple curve
		curve = new Curve(true);
		curve.add(0.0,0.0);
		curve.add(0.25,0.8);
		curve.add(0.5,1.0);

		System.out.println("Curve: (0.0,0.0), (0.25,0.8), (0.5,1.0)");
		System.out.println("Area: " + curve.getArea() + " (should be 0.825)");
		System.out.println("Area>=0.25: " + curve.getArea(0.25) + " (should be 0.725)");
		for (int i=0;i<11;i++) {
			double x = i/10.0;
			System.out.println("f(" + x + ") = " + curve.getY(x));
		}
		curves.add(curve);

		// average
		curve = Curve.createVerticalAverage(curves);
		System.out.println("\nVertical average:");
		System.out.println("Area: " + curve.getArea() + "(should be 0.7375)");
		System.out.println("Area>=0.25: " + curve.getArea(0.25) + " (should be 0.625)");
		for (int i=0;i<11;i++) {
			double x = i/10.0;
			System.out.println("f(" + x + ") = " + curve.getY(x));
		}

	}

}
