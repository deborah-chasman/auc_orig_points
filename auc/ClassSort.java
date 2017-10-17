package auc;
/**
 * Class to store and sort a probability value, classification, and example
 * weight.
 * 
 * @author Jesse Davis 2003
 * @author Kendrick Boyd 2010
 */
public class ClassSort implements Comparable{

	private double val;
	private int classification;
	private double weight;

	public ClassSort(double val, int classification){
		this.val = val;
		this.classification = classification;
		this.weight = 1.0; // default weight
	}

	public ClassSort(double val, int classification, double w) {
		this.val = val;
		this.classification = classification;
		this.weight = w;
	}

	public int getClassification(){
		return classification;
	}

	public double getProb(){
		return val;
	}

	public double getWeight() {
		return weight;
	}

	/**
	 * Sort by probability ascending and then by classification descending.
	 * So positive examples (with classification==1) will be listed first.
	 * @param o object to compare
	 * @return -1 if this<o, 0 if this==o, 1 if this>o
	 */
	public int compareTo(Object o){
		double tmpVal = ((ClassSort)o).getProb();
		if (val < tmpVal){
			return -1;
		}
		else if (val > tmpVal){
			return 1;
		}
		else{
			int tmpClass = ((ClassSort)o).getClassification();
			if (tmpClass == classification){
				return 0;
			}
			else if (classification > tmpClass){
				return -1;
			}
			else{
				return 1;
			}
		}
	}
}
