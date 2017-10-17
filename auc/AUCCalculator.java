package auc;


import java.util.*;
import java.io.*;


/**
 * Calculates Area Under Recall-Precision Curve for a file
 * of Recall Precision Points.  Also outputs a new file
 * with correct interpolation which can be read into Excel, gnuplot, etc.
 *
 * @author Mark Goadrich 2004 - 2005
 * @author Kendrick Boyd 2010
 * @author Debbie Chasman added update to print out original points to .opr file.
 * Usage: java AUCCalculator <fileName> <fileType> <posCount> <negCount> <*minRecall>
 *
 * requires PNPoint.java
 * requires Confusion.java
 */
public class AUCCalculator {
    /**
     * List of files to process, if files.size()>1 then do vertical 
     * averaging
     */
    private static ArrayList<String> files;
    
    /**
     * Type of file, list, pr, or roc
     */
    private static String fileType;

    /**
     * Number of positives in the dataset, required for pr or roc file types
     */
    private static double posCount = -1;

    /**
     * Number of negatives in the dataset, required for pr or roc file types
     */
    private static double negCount = -1;

    /**
     * Min recall for calculating area under PR curve
     */
    private static double minRecall = 0.0;
    
    /**
     * Prefix for output files (.pr, .roc, and .spr) or null for no output files
     */
    private static String outputPrefix = null;
    
    /**
     * Turn debugging on or off
     */
    public static boolean DEBUG = true;
       
    
    /**
     * reads in arguments of fileName, posCount and negCount and minPos
     * @param args command line arguments
     */
    public static void main(String[] args) {
	if (!readArgs(args)) {
	    showUsage();
	    return;
	}
	
	if (files.size()==0) {
	    System.out.println("Must specify at least one file to read");
	    showUsage();
	    return;	    
	}
	
	if (files.size()==1) {
	    // single file
	    String fileName = files.get(0);
	    Confusion points;
	    if (fileType.equalsIgnoreCase("list")){
		points = ReadList.readFile(fileName, fileType);
	    }
	    else {
		if (posCount<=0.0 || negCount<=0.0) {
		    System.out.println("When using roc or pr filetypes must specify positive POSCOUNT and NEGCOUNT");
		    showUsage();
		    return;
		}		    
		points = readFile(fileName, fileType, posCount, negCount);
	    }
	    if (outputPrefix!=null) {
	    points.writeOriginalPRFile(outputPrefix + ".opr");
		points.writePRFile(outputPrefix + ".pr");
		points.writeStandardPRFile(outputPrefix + ".spr");
		points.writeROCFile(outputPrefix + ".roc");
	    }
	    double aucPR = points.calculateAUCPR(minRecall);
	    double aucROC = points.calculateAUCROC();
	    System.out.println("Area Under the Curve for Precision - Recall is " + aucPR);
	    System.out.println("Area Under the Curve for ROC is " + aucROC);
	}
	else {
	    // multiple files
	    
	    // TODO - add support somehow for roc and pr file types?
	    
	    if (!fileType.equalsIgnoreCase("list")) {
		System.out.println("Vertical averaging of multiple files only supported with list filetypes");
		showUsage();
		return;
	    }
	    
	    ArrayList<Curve> prCurves = new ArrayList<Curve>(),
		rocCurves = new ArrayList<Curve>();
	    
	    for (String fileName : files) {
		System.out.println("Processing '" + fileName + "'");
		Confusion points = ReadList.readFile(fileName,fileType);
		double aucPR = points.calculateAUCPR(minRecall);
		double aucROC = points.calculateAUCROC();
		System.out.println("Area Under the Curve for Precision - Recall is " + aucPR);
		System.out.println("Area Under the Curve for ROC is " + aucROC);
		
		prCurves.add(points.createPRCurve());
		rocCurves.add(points.createROCCurve());
	    }

	   
	    Curve prCurve = Curve.createVerticalAverage(prCurves);
	    Curve rocCurve = Curve.createVerticalAverage(rocCurves);
	    System.out.println("\nVertically averaged totals:");
	    if (outputPrefix!=null) {
		prCurve.write(outputPrefix + ".pr");
		rocCurve.write(outputPrefix + ".roc");
	    }
	    double aucPR = prCurve.getArea(minRecall);
	    double aucROC = rocCurve.getArea();
	    System.out.println("Area Under the Curve for Precision - Recall is " + aucPR);
	    System.out.println("Area Under the Curve for ROC is " + aucROC);
	}
	    
    }
    

    /**
     * Parses the arguments and populates local static variables
     * @param args command line arguments
     * @return true if successfully parsed args, false otherwise
     */
    public static boolean readArgs(String[] args) {
	files = new ArrayList<String>();
	
	String cur = "";
	try {
	    int index = 0;
	    while (index<args.length) {
		cur = args[index];
		if (cur.equals("-t")) {
		    // file type
		    index++;
		    // validate file types
		    String temp = args[index].toLowerCase();
		    if (temp.equals("list") ||
			temp.equals("roc") ||
			temp.equals("pr")) {
			fileType = temp;
		    }
		    else {
			System.out.println("Option " + cur + " requires file type of list, pr, or roc");
			return false;
		    }
		}
		else if (cur.equals("-p")) {
		    // pos count
		    index++;
		    posCount = Double.parseDouble(args[index]);
		}
		else if (cur.equals("-n")) {
		    // neg count
		    index++;
		    negCount = Double.parseDouble(args[index]);		
		}
		else if (cur.equals("-r")) {
		    // min recall
		    index++;
		    minRecall = Double.parseDouble(args[index]);
		}
		else if (cur.equals("-o")) {
		    index++;
		    outputPrefix = args[index];
		}
		else {
		    // a file, we assume
		    files.add(args[index]);
		}
		index++;
	    }
	}
	catch (NumberFormatException nfe) {
	    System.out.println("Option " + cur + " requires a double argument");
	    return false;
	}
	catch (ArrayIndexOutOfBoundsException aioobe) {
	    System.out.println("Option " + cur + " requires argument");
	    return false;
	}
	return true;
    }

    /**
     * Display command usage to stdout
     */
    public static void showUsage() {
	System.out.println("Usage:");
	System.out.println("java auc [-t FILETYPE] [-p POSCOUNT] [-n NEGCOUNT] [-r MINRECALL] [-o OUTPUTPREFIX] FILES");
	System.out.println("FILETYPE - list, pr, roc");

	System.out.println("\nFILETYPE Details:");
	System.out.println(" roc:");
	System.out.println("  fpr tpr");
	System.out.println(" pr:");
	System.out.println("  recall precision");
	System.out.println(" list:");
	System.out.println("  prob outcome [weight]");
	System.out.println("  where prob is probability of positive, outcome is the true classification, and weight is an optional weight for the example, defaults to 1.0");
	System.out.println("  outcome can be 0 or false for negative outcomes");
	System.out.println("   and 1 or true for positive outcomes");
    }

    /**
     * Read in file containing data points
     * @param fileName file to read
     * @param fileType type of file, PR or ROC
     * @param totPos total number of positive examples
     * @param totNeg total number of negative examples
     * @return Confusion object
     */
    public static Confusion readFile(String fileName, String fileType,
			             double totPos, double totNeg) {
	if (DEBUG) {
	    System.out.println("--- Reading in " + fileType +  " File: " + fileName + " ---");
	}

	Confusion points = new Confusion(totPos, totNeg);

	BufferedReader fin = null;
	try {
	    fin = new BufferedReader(new FileReader(new File(fileName)));

            if (!(fileType.equals("pr") || fileType.equals("roc"))) {
           	throw new NoSuchElementException();
            }

	    while (fin.ready()) {
		
		String line = fin.readLine();
		if (DEBUG) { 
		    System.out.println(line);
		}

		// tokenize the line by tab, space or comma
		StringTokenizer strtok = new StringTokenizer(line, "\t ,");
		
		// attempt to read the points and then calculate the TP and FP numbers
		try {
		    double dp1 = Double.parseDouble(strtok.nextToken());
		    double dp2 = Double.parseDouble(strtok.nextToken());
		    if (DEBUG) { 
			System.out.println(dp1 + "\t" + dp2);
		    }

		    if (fileType.equals("pr")) {
		    	points.addPRPoint(dp1, dp2);
		    } else {
			points.addROCPoint(dp1, dp2);
                    }
		    if (DEBUG) { 
			System.out.println("End of Line");
		    }

		} catch (NumberFormatException nfe) {
		    // Something was not a double as expceted. Ignore.
		    System.err.println("...skipping bad input line (bad numbers)");
		} catch (NoSuchElementException nsee) {
		    // Caused by there not being two integers in this line.  Ignore.
		    System.err.println("...skipping bad input line (missing data)");
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

	points.sort();
	points.interpolate();
	return points;
    }


}
