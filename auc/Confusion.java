
package auc;

import java.util.*;
import java.io.*;


/**
 * Confusion extends Vector and keeps track of the truePos/trueNeg
 * PNPoints.  It is used to sort, interpolate, and write out to both
 * ROC and PR files, and calculate both AUC-ROC and AUC-PR scores.
 *
 * @todo What about degenerate cases when totPos or totNeg = 0.0?
 * 
 * @author Mark Goadrich 2005
 * @author Kendrick Boyd 2010
 */
public class Confusion extends Vector<PNPoint> {
	/**
	 * Number of positives in dataset
	 */
	private double totPos;

	/**
	 * Number of negatives in dataset
	 */
	private double totNeg;
	
	/**
	 * Points from original data, without interpolation.
	 */
	private Confusion orig;


	/** 
	 * Constructor using totPos and totNeg
	 * @param totPos the total number of positive examples in dataset
	 * @param totNeg the total number of negative examples in dataset
	 */
	public Confusion(double totPos, double totNeg) {
		if (totPos < 1 || totNeg < 1) {
			this.totPos = 1;
			this.totNeg = 1;
			System.err.println("ERROR: " + totPos + "," + totNeg + " - "
					+ "Defaulting Confusion to 1,1");
		} else {
			this.totPos = totPos;
			this.totNeg = totNeg;
		}	
	}

	/** 
	 * Adds a Precision-Recall point to the Confusion
	 * @param recall    the recall of this point
	 * @param precision the precision of this point
	 */
	public void addPRPoint(double recall, double precision) 
	throws NumberFormatException {

		//error checking
		if (recall > 1 || recall < 0 || precision > 1 || precision < 0) {
			throw new NumberFormatException();
		}

		double truePositive = (recall * totPos);
		double falsePositive = ((truePositive - (precision * truePositive))
				/ precision);

		PNPoint toadd = new PNPoint(truePositive, falsePositive);
		if (!contains(toadd)) {
			add(toadd);
		}
	}

	/** 
	 * Adds a ROC point to the Confusion
	 * @param fpr  the false positive rate of this point
	 * @param tpr  the true positive rate of this point
	 */
	public void addROCPoint(double fpr, double tpr) 
	throws NumberFormatException {

		//error checking
		if (fpr > 1 || fpr < 0 || tpr > 1 || tpr < 0) {
			throw new NumberFormatException();
		}

		double truePositive = (tpr * totPos);
		double falsePositive = (fpr * totNeg);

		PNPoint toadd = new PNPoint(truePositive, falsePositive);
		if (!contains(toadd)) {
			add(toadd);
		}
	}

	/** 
	 * Adds a  point to the Confusion
	 * @param pos  the positive of this point
	 * @param neg  the negative of this point
	 */
	public void addPoint(double pos, double neg) 
	throws NumberFormatException {

		//error checking
		if (pos < 0 || pos > totPos || neg < 0 || neg > totNeg) {
			throw new NumberFormatException();
		}

		PNPoint toadd = new PNPoint(pos, neg);
		if (!contains(toadd)) {
			add(toadd);
		}
	}

	/**
	 * Sorts the interpolated points from lowest pos to highest pos
	 */
	public void sort() {

		if (AUCCalculator.DEBUG) {
			System.out.println("--- Sorting the datapoints !!! ---");
		}

		// error checking
		if (size() == 0) {
			System.err.println("ERROR: No data to sort....");
			return;
		}

		// sort the vector by pos scores
		PNPoint[] temp = new PNPoint[size()];
		int i = 0;
		while (size() > 0) {
			temp[i++] = elementAt(0);
			removeElementAt(0);
		}
		Arrays.sort(temp);
		for (int j = 0; j < temp.length; j++) {
			add(temp[j]);
		}

		// ??? Removal ok???
		PNPoint first = elementAt(0);
		while(first.getPos() < 0.001 && first.getPos() > -0.001) {
			removeElementAt(0);
			first = elementAt(0);
		}

		// add in first point of 1 pos recall
		double neg = first.getNeg() / first.getPos();

		PNPoint toadd = new PNPoint(1, neg);
		if (!contains(toadd) && first.getPos() > 1) {
			insertElementAt(toadd, 0);
		}

		// add in final point of full recall, precision of %pos in dataset
		toadd = new PNPoint(totPos, totNeg);
		if (!contains(toadd)) {
			add(toadd);
		}
	}

	/**
	 * Adds interpolated points to the Confusion object.
	 * Necessary for PR curve calculations
	 */
	public void interpolate() {

		if (AUCCalculator.DEBUG) {
			System.out.println("--- Interpolating New Points ---");
		}

		// error checking
		if (size() == 0) {
			System.err.println("ERROR: No data to interpolate....");
			return;
		}

		// for each pair of points
		for (int i = 0; i < size() - 1; i++) {

			// add intermediate anchors
			PNPoint p = elementAt(i);
			PNPoint pnext = elementAt(i+1);

			// calculate ratio of negative increase to positive increase
			double pdiff = pnext.getPos() - p.getPos();
			double ndiff = pnext.getNeg() - p.getNeg();
			double margin = ndiff / pdiff;
			double initppos = p.getPos();
			double initpneg = p.getNeg();

			// as long as there is space to add new points, do so
			while (Math.abs(p.getPos() - pnext.getPos()) > 1.001) {

				double neg = initpneg + ((p.getPos() - initppos + 1 ) * margin);

				PNPoint pnp = new PNPoint(p.getPos() + 1, neg);		
				// increment i to skip all these new points
				//System.out.println(pnp);
				insertElementAt(pnp, ++i);
				p = pnp;
			}		
		}
	}


	/**
	 * Calculate the area under the precision-recall curve
	 * @param minRecall the lower bound cutoff for recall
	 * @return the AURPC to the screen
	 */
	public double calculateAUCPR(double minRecall) {
		Curve prCurve = createPRCurve();	
		double area = prCurve.getArea(minRecall);
		return area;
	}


	public Curve createPRCurve() {

		if (AUCCalculator.DEBUG) {
			System.out.println("--- Creating AUC-PR ---");
		}

		// error checking

		if (size() == 0) {
			System.err.println("ERROR: No data to calculate....");
			return null;
		}

		Curve prCurve = new Curve(false);
		for (int i=0;i<size();i++) {
			PNPoint p = elementAt(i);
			double rec = (p.getPos() / totPos);
			double prec = (p.getPos()/(p.getPos()+p.getNeg()));
			prCurve.add(rec,prec);
		}
		return prCurve;
	}


	public double calculateAUCROC() {
		Curve rocCurve = createROCCurve();
		return rocCurve.getArea();
	}



	/**
	 * Calculate the area under the ROC curve
	 * @return the AURPC to the screen
	 */
	public Curve createROCCurve() {


		if (AUCCalculator.DEBUG) {
			System.out.println("--- Calculating AUC-ROC ---");
		}


		// error checking
		if (size() == 0) {
			System.err.println("ERROR: No data to calculate....");
			return null;
		}


		Curve rocCurve = new Curve(true);
		rocCurve.add(0.0,0.0); // guarantee the point is there
		rocCurve.add(1.0,1.0);
		for (int i=0;i<size();i++) {
			PNPoint p = elementAt(i);
			double tpr = (p.getPos()/totPos);
			double fpr = (p.getNeg()/totNeg);
			rocCurve.add(fpr,tpr);

		}
		return rocCurve;
	}

	/** 
	 * Write out the PR points to fileName file
	 * @param fileName name of the file to output curve
	 */
	public void writePRFile(String fileName) {

		System.out.println("--- Writing PR file " + fileName + " ---");

		if (size() == 0) {
			System.err.println("ERROR: No data to write....");
			return;
		}

		try {
			PrintWriter fout = new PrintWriter(new FileWriter(new File(fileName)));
			Curve prCurve = createPRCurve();
			prCurve.write(fout);

			fout.close();

		} catch (IOException ioe) {
			// javac made me do it..
			System.out.println("ERROR: IO Exception in file " + 
					fileName + " - exiting...");
			System.exit(-1);
		}
	}
	
	/**
	 * Writes a file containing the original PR points, without interpolation.
	 * If multiple precision points exist for a single recall, print only the highest.
	 * @author chasman
	 * @param fileName
	 */
	public void writeOriginalPRFile(String fileName) {
		System.out.println("--- Writing original PR points " + fileName + " ---");

		if (orig.size() == 0) {
			System.err.println("ERROR: No data to write....");
			return;
		}

		try {
			PrintWriter fout = new PrintWriter(new FileWriter(new File(fileName)));
			
			// don't use a curve - will interpolate and produce two points with same recall
			//Curve prCurve = orig.createPRCurve();
			//prCurve.write(fout);
			double prevRecall=2, prevPrecision=2;
			for (PNPoint p : orig) {
				// recall: pos / totpos
				// precision: pos / pos + neg
				double recall = p.getPos() / orig.totPos;
				double precision = p.getPos() / (p.getPos() + p.getNeg());
				
				// same recall, lower precision? don't print.
				if (Math.abs(recall-prevRecall)<Point.EPSILON && precision < prevPrecision) {
					
				} else {
					fout.format("%.10f\t%.10f\n", recall, precision);
				}
				prevRecall=recall;
				prevPrecision=precision;
				
			}

			fout.close();

		} catch (IOException ioe) {
			// javac made me do it..
			System.out.println("ERROR: IO Exception in file " + 
					fileName + " - exiting...");
			System.exit(-1);
		}
	}

	/** 
	 * Write out 100 standardized PR points to fileName file
	 * @param fileName name of the file to output curve
	 */
	public void writeStandardPRFile(String fileName) {

		System.out.println("--- Writing standardized PR file " + fileName + " ---");

		if (size() == 0) {
			System.err.println("ERROR: No data to write....");
			return;
		}

		try {
			PrintWriter fout = new PrintWriter(new FileWriter(new File(fileName)));

			Curve sprCurve = createPRCurve().createStandardized(100);
			sprCurve.write(fout);


			fout.close();

		} catch (IOException ioe) {
			// javac made me do it..
			System.out.println("ERROR: IO Exception in file " + 
					fileName + " - exiting...");
			System.exit(-1);
		}
	}

	/** 
	 * Write out the ROC points to fileName file
	 * @param fileName name of the file to output curve
	 */
	public void writeROCFile(String fileName) {

		System.out.println("--- Writing ROC file " + fileName + " ---");

		if (size() == 0) {
			System.err.println("ERROR: No data to write....");
			return;
		}

		try {
			PrintWriter fout = new PrintWriter(new FileWriter(new File(fileName)));

			Curve rocCurve = createROCCurve();
			rocCurve.write(fout);

			fout.close();

		} catch (IOException ioe) {
			// javac made me do it..
			System.out.println("ERROR: IO Exception in file " + 
					fileName + " - exiting...");
			System.exit(-1);
		}
	}

	/** 
	 * Returns a String represenation of the Confusion Object
	 * @return String representation
	 */
	public String toString() {
		String s = "";
		s += "TotPos: " + totPos + ", TotNeg: " + totNeg + "\n";
		for (int i = 0; i < size(); i++) {
			s += elementAt(i) + "\n";
		}
		return s;
	}



	public static Confusion createConfusion(ArrayList<ClassSort> list) {
		// sort predictions
		double posCount = 0;
		double negCount = 0;
		double total = 0;

		Collections.sort(list);

		ArrayList<PNPoint> list2 = new ArrayList<PNPoint>();

		ClassSort cur = list.get(list.size()-1);
		double prevprob = cur.getProb();
		if (cur.getClassification() == 1) {
			posCount += cur.getWeight();
		} else {
			negCount += cur.getWeight();
		}
		total += cur.getWeight();
		for (int i = list.size()-2; i >=0; i--) {
			cur = list.get(i);
			double prob = cur.getProb();

			int outcome = cur.getClassification();
			//		System.out.println(prob + " " + outcome);

			// don't use equals with double	    
			if (Math.abs(prob-prevprob)>Point.EPSILON) {
				//System.out.println(posCount+ " " + negCount + " " + prob);
				list2.add(new PNPoint(posCount,negCount));
			}
			prevprob = prob;

			if (outcome == 1) {
				posCount += cur.getWeight();
			} else {
				negCount += cur.getWeight();
			}
			total += cur.getWeight();
		}
		list2.add(new PNPoint(posCount, negCount));



		Confusion points = new Confusion(posCount, negCount);
		points.orig = new Confusion(posCount, negCount);
		
		for (PNPoint p : list2) {
			points.addPoint(p.getPos(), p.getNeg());
			points.orig.addPoint(p.getPos(), p.getNeg());
		}
		
		//System.out.println(points);
		points.sort();
		points.orig.sort();
		//System.out.println(points);
		
		points.interpolate();
		//System.out.println(points);
		return points;
	}
}
