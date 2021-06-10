import java.util.ArrayList;

public class Experimenter {
	
	public static void main(String[] args)
	{
		/**
		 * Experiment parameters:
		 * 
		 * datasetPath - path to the dataset .csv file
		 * datasetName - name of the dataset, used in the name of the results file
		 * idAttribute - id attribute name, as it appears in the dataset file, the id attribute gets ignored during model construction
		 * classAttribute - class attribute name, as it appears in the dataset file
		 * missingValue - missing value representation in the dataset
		 * numberOfExperiments - number of times the experiment will be repeated (with different random seeds)
		 * numberOfFolds - number of cross-validation folds
		 * modelType - type of the model being evaluated, supports "Tree", "Nested Tree" and "Binary Nested Tree" values
		 * oblique - unused attribute, this implementation relies on prepared oblique datasets
		 * initialClassOversampling - flag for class balancing to be introduced at pre-processing
		 * regression - flag to perform classification-by-regression instead of regular classification, necessary for AUROC estimations
		 * optimalAccuracyMeasure - the criteria for selecting the final output model, currently only supports "Accuracy"
		 * maxTreeDepth - maximum depth of the tree (outer tree)
		 * maxNestedTreeDepth- maximim depth of the inner tree
		 * minNodeSize - minimum size of the tree nodes
		 * longitudinalAttributes - list of longitudinal attribute names (as they appear in the dataset file)
		 * 
		 */
		
		String datasetPath = "path/to/dataset.csv";
		String datasetName = "myDataset";
		String idAttribute = "Id";
		String classAttribute = "Class";
		String missingValue = "?";
		int numberOfExperiments = 30;
		int numberOfFolds = 10;
		String modelType = "Binary Nested Tree";
		boolean oblique = false;
		boolean initialClassOversampling = true;
		boolean regression = true;
		String optimalAccuracyMeasure = "Accuracy";
		int maxTreeDepth = 10;
		int maxNestedTreeDepth = 5;
		int minNodeSize = 2;
		String[] longitudinalAttributes = new String[] {/* Longitudinal attributes here */};
		
		/**
		 * End of parameters list
		 */
		
		// Initialise parameters and experiment name
		String parameters = optimalAccuracyMeasure + "," + maxTreeDepth + "," + maxNestedTreeDepth + "," + initialClassOversampling + "," + minNodeSize + "," + regression;
		String experimentName = 
				datasetName + "_" + 
				modelType.replace(' ', '_') + "_" +
				(oblique?"Oblique_":"")+
				(regression?"Regression_":"")+
				(initialClassOversampling?"InitialOversampling_":"")+ 
				numberOfExperiments + "_" + 
				numberOfFolds;
		
		// Read the dataset and initialise necessary fields
		Dataset dataset = new Dataset();
		dataset.setMissingValueString(missingValue);
		dataset.readFromCSV(datasetPath);
		dataset.setIdAttribute(idAttribute);
		dataset.setClassAttribute(classAttribute);
		dataset.preSort();
		dataset.initialiseUniqueClassValues();
		dataset.generateLongitudinalAttributes(Util.getElsaAttributes());
		
		// Run the experiment. Try using the parallelised implementation. If parallel fails with OutOfMemoryError, default to non-parallel
		Object[] results;
		try {
			results = ModelEvaluator.runCrossValidationExperimentsParallelised(dataset, modelType, parameters, numberOfFolds, numberOfExperiments);
		}
		catch(OutOfMemoryError e)
		{
			System.out.println("OutOfMemoryError occured, defaulting to non-parallel implementation");
			results = ModelEvaluator.runCrossValidationExperiments(dataset, modelType, parameters, numberOfFolds, numberOfExperiments);
		}
		
		// Print the results to console and save them to the results folder
		ModelEvaluator.printCrossExperimentResults(experimentName + ", " + numberOfExperiments + " experiments " + numberOfFolds  + " folds, " + modelType + ", parameters: " + parameters, results, dataset);
		ModelEvaluator.saveCrossExperimentResults(experimentName, experimentName + ", " + numberOfExperiments + " experiments " + numberOfFolds  + " folds, " + modelType + ", parameters: " + parameters, results, dataset);
		
		// Done! 
		System.exit(0);
	}
}
