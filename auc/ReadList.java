package auc;


import java.io.*;
import java.util.*;

/**
 * Static methods for reading list type files
 * @author Mark Goadrich 2005
 * @author Kendrick Boyd 2010
 */
public class ReadList{
	public static final int TP = 0;
	public static final int FP = 1;
	public static final int FN = 2;
	public static final int TN = 3;

	public static ClassSort[] convertList(LinkedList<ClassSort> list){
		ClassSort[] sort = new ClassSort[list.size()];
		for(int ix = 0; ix < sort.length; ix++){
			sort[ix] = list.removeFirst();
		}
		Arrays.sort(sort);
		return sort;
	}


	/**
       @param probs
       @param cats
       @param display
	 */
	public static Confusion accuracyScoreAllSplits(ClassSort[] sort, int posCount, int negCount){
		Arrays.sort(sort);
		for(int ix = (sort.length-1); ix >= (sort.length-20); ix--){
			//System.out.println(sort[ix].getProb() + " " + sort[ix].getClassification());
		}

		Confusion points = new Confusion(posCount, negCount);
		int cat1Seen = 0;
		double prevThres = sort[sort.length-1].getProb();
		double currThres;
		int prevClass = sort[sort.length-1].getClassification();
		int currClass;
		double area = 0;
		double prevTPR = 0;
		double prevFPR = 0;
		int[] roc;
		double[] newProbs = new double[sort.length];
		int[] newCats = new int[sort.length];
		for(int ix = 0; ix < sort.length; ix++){
			newProbs[ix] = sort[ix].getProb();
			newCats[ix] = sort[ix].getClassification();
			//System.out.println(newProbs[ix] + " " + newCats[ix]);
		}
		LinkedList list = new LinkedList();
		//System.out.println(sort.length);
		for(int ix = sort.length-2; ix >=0 ; ix--){
			currClass = sort[ix].getClassification();
			currThres = sort[ix].getProb();

			if (prevClass ==1 && 0 == currClass){
				if (sort[ix+1].getProb() > currThres){

				}
				else if (sort[ix+1].getProb() > currThres){

				}
				else{
					System.out.println("Bad");
				}
				roc = fastAccuracy(newProbs, newCats, prevThres);

				points.addPoint(roc[TP], roc[FP]);
				/*
		  double recall = ((double)roc[TP])/((double)(roc[TP] + roc[FN]));
		  double prec = ((double)roc[TP])/((double)(roc[TP] + roc[FP]));

		  double falsePosRate = ((double)roc[FP])/((double)(roc[FP] + roc[TN]));
				 */
			}
			cat1Seen += currClass;
			prevThres = currThres;
			prevClass = currClass;

		}
		return points;
	}//end of method accuracyScoreAllSplits


	/**
       This method provides a fast way to recalculate the accuracy of
       a score set.  It is useful for picking a threshold for
       classifying something as positive. it is also useful for
       generating ROC or Precision/Recall curves.

       @param probs the probabilities of being cat1
       @param cats the classification of the example
       @param thres the threshold for calling example cat1

       @return
	 */
	public static int[] fastAccuracy(double[] probs, int[] cats, double thres){
		int[] roc = new int[4];
		for(int ix = 0; ix < roc.length; ix++){
			roc[ix] = 0;
		}
		for(int ix = 0; ix < probs.length; ix++){
			if (probs[ix] >= thres){
				if (cats[ix] == 1){
					roc[0]++; //true positive
				}
				else{
					roc[1]++; //false positive
				}
			}//end if
			else{
				if (cats[ix] == 1){
					roc[2]++; //false negative
				}
				else{
					roc[3]++; //true negative
				}
			}//end else
		}//end for ix
		return roc;
	}//end method fastAccuracy


	/**
	 * Read in file containing data points. Format is 1 example per line:
	 * probability class [weight]
	 * class is 0/1 or false/true
	 * weight is optional weight, default is 1.0
	 * @param fileName file to read
	 * @param fileType type of file, list
	 * @return Confusion object
	 */
	public static Confusion readFile(String fileName, String fileType) {

		ArrayList<ClassSort> list = new ArrayList<ClassSort>();

		BufferedReader fin = null;
		try {
			fin = new BufferedReader(new FileReader(new File(fileName)));


			while (fin.ready()) {

				String line = fin.readLine();
				if (AUCCalculator.DEBUG) { 
					// System.out.println(line);
				}

				// tokenize the line by tab, space or comma
				// scanner is nice since it deals with multiple spaces, tabs, commas and tabs, etc. but it is much slower
				Scanner sc = new Scanner(line);
				sc.useDelimiter("[\t ,]+");


				// attempt to read the points

				if (!sc.hasNextDouble()) {
					System.out.println("... skipping bad input line (no parsable double probability found");
					continue;
				}
				double prob = sc.nextDouble();
				if (!sc.hasNext()) {
					System.out.println("... skipping bad input line (no outcome token found");
					continue;
				}
				String outToken = sc.next();
				double weight = 1.0; // default weight
				if (sc.hasNextDouble()) {
					weight = sc.nextDouble();

					if (weight<0.0) {
						System.err.println("... skipping bad input line (weight cannot be negative)");
						continue;
					}
				}

				// support for multiple outcome tokens
				int outcome = -1;
				if (outToken.equals("0")) {
					outcome = 0;
				}
				else if (outToken.equals("1")) {
					outcome = 1;
				}
				else if (outToken.equalsIgnoreCase("false")) {
					outcome = 0;
				}
				else if (outToken.equalsIgnoreCase("true")) {
					outcome = 1;
				}
				else {
					System.err.println("... skipping bad input line (unknown outcome of '" + outToken + "'");
					continue;
				}

				if (outcome!=-1) {			
					list.add(new ClassSort(prob, outcome,weight));
				}



				if (AUCCalculator.DEBUG) { 
					//System.err.println(prob + "\t" + outcome);
				}
				if (AUCCalculator.DEBUG) { 
					//System.out.println("End of Line");
				}
			}
		} catch (FileNotFoundException fnfe) {
			// User didn't type in an existing fileName
			System.err.println("ERROR: File " + fileName + " not found - exiting...");
			System.exit(-1);
		} catch (NoSuchElementException nsee) {
			// Caused by incorrect fileType argument
			System.err.println("...incorrect fileType argument, either PR or ROC - exiting");
			System.exit(-1);
		} catch (IOException ioe) {
			// javac made me do it..
			System.err.println("ERROR: IO Exception in file " + fileName + " - exiting...");
			System.exit(-1);
		}
		//System.out.println(posCount + " " + (total-posCount));

		return Confusion.createConfusion(list);
	}
}
