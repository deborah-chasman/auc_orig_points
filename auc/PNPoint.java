package auc;


/**
 * PNPoint written for AUCCalculator.java
 * Provides an easy way to sort the data points, first by pos,
 * then by neg, in case input file is jumbled.
 *
 * @author Mark Goadrich 2005
 * @author Kendrick Boyd 2010
 */
public class PNPoint implements Comparable{

	/**
	 * Number of true positives at the threshold
	 */
	private double pos; 

	/**
	 * Number of false positives at the threshold
	 */
	private double neg;

	/**
	 * Class constructor for a PNPoint
	 * @param pos the number of positive examples classified 
	 *  positive at this point
	 * @param neg the number of negative examples classified 
	 *  positive at this point
	 */
	public PNPoint(double pos, double neg) {
		// error checking
		if (pos < 0 || neg < 0) {
			this.pos = 0;
			this.neg = 0;
			System.err.println("ERROR: " + pos + "," + neg + " - Defaulting "
					+ "PNPoint to 0,0");
		} else {
			this.pos = pos;
			this.neg = neg;
		}
	}

	/**
	 * Data member access for pos
	 * @return the number of positive examples classified 
	 *  positive at this point
	 */
	public double getPos() {
		return pos;
	}

	/**
	 * Data member access for neg
	 * @return the number of negative examples classified 
	 *  positive at this point
	 */
	public double getNeg() {
		return neg;
	}

	/**
	 * Comparison between two objects (for the Comparable interface)
	 * @param o the Object for comparison
	 * @return -1 for "this" is earlier, 1 for "this" is later
	 */
	public int compareTo(Object o) {
		if (o instanceof PNPoint) {
			PNPoint p = (PNPoint)o;
			if (pos - p.pos > 0) {
				return 1;
			} else if (pos - p.pos < 0) { 
				return -1;
			} else {
				if (neg - p.neg > 0) {
					return 1;
				} else if (neg - p.neg < 0) {
					return -1;
				} else {
					return 0;
				}
			}
		} else {
			return -1;
		}
	}

	/**
	 * Equals method, points are equal within Point.EPSILON of each other
	 * @param o Object for comparison
	 * @return true if equal, otherwise false
	 */
	public boolean equals(Object o) {
		if (o instanceof PNPoint) {
			PNPoint p = (PNPoint)o;
			if (Math.abs(pos - p.pos) > Point.EPSILON) {
				return false;
			} else if (Math.abs(neg - p.neg) > Point.EPSILON) {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * returns a String representation of a point
	 * @return String representation
	 */
	public String toString() {
		String s = "";
		s += "(" + pos + "," + neg + ")";
		return s;
	}
}
