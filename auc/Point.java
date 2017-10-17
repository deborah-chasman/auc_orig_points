package auc;


/**
 * Point class represents simple x,y point in space, used with the Curve
 * class.
 * @author Kendrick Boyd 2010
 */
public class Point implements Comparable {
	/**
	 * Precision value used for considering 2 doubles to be equal
	 */
	public static double EPSILON = 1e-7;

	private double x,y;

	/**
	 * Constructor takes x,y values.
	 * Once constructed point object is immutable.
	 * @param inX - x value
	 * @param inY - y value
	 */
	public Point(double inX,double inY) {
		x=inX;
		y=inY;
	}


	/**
	 * Returns x coordinate
	 * @return x value
	 */
	public double getX() {
		return x;
	}

	/**
	 * Returns y coordinate
	 * @return y value
	 */
	public double getY() {
		return y;
	}

	/**
	 * Sorts Point objects by x increasing and then by y increasing.
	 * @param o other object (Point) to compare
	 * @return -1 if this.x<p.x || (this.x==p.x && this.y<p.y), 
	 *         1 if (this.x>p.x || (this.x==p.x && this.y>p.y), 
	 *         and 0 if this.x==p.x && this.y==p.y         
	 */
	public int compareTo(Object o) {
		if (o instanceof Point) {
			Point p = (Point)o;
			if (this.x+EPSILON<p.x) return -1;
			else if (this.x-EPSILON>p.x) return 1;
			else if (this.y+EPSILON<p.y) return -1;
			else if (this.y-EPSILON>p.y) return 1;
			else return 0;
		}
		else {
			return -1;
		}
	}

	/**
	 * Returns true if points are the same within EPSILON
	 * @param o other object (Point) to compare
	 * @return true if points are the same, false otherwise
	 */
	public boolean equals(Object o) {
		if (o instanceof Point) {
			Point p = (Point)o;
			return (Math.abs(this.x-p.x)<=EPSILON &&
					Math.abs(this.y-p.y)<=EPSILON);
		}
		else {
			return false;
		}	    
	}
}
