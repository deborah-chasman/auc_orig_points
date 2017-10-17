# auc_orig_points

This is an adaptation of the original AUCCalculator by Jesse Davis and Mark Goadrich, which is available here: http://mark.goadrich.com/programs/AUC/

The original paper can be found here: 
The Relationship Between Precision-Recall and ROC Curves
Jesse Davis and Mark Goadrich
23rd International Conference on Machine Learning (ICML), Pittsburgh, PA, USA, 26th - 28th June, 2006

I received the predecessor to my executable from Kendrick Boyd, who updated some of the output.

Finally, my update simply prints out the P/R coordinates for the original data points. They will go into a file name ".opr".

## Recommended Usage
The original program had multiple usage options, but I have only tested my update with the following usage:
java -jar auc_orig_points -t list -o OUTPUTPREFIX FILE

List file format is tab-delimited:
prob outcome [weight]
 
Where prob is the score of the example (higher is better).
outcome is the true classification (0 negative, 1 positive),
weight is an optional weight for the example, defaults to 1.0.

## Example:
java -jar auc_orig_points.jar -t list -o test/test test/test.list 

