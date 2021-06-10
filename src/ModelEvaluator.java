import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class ModelEvaluator {
	
	private static double logE2 = Math.log(2.0);
	
	public static PredictionModel constructPredictionModel(String modelType, Dataset dataset, double[] subset, String parameters)
	{
		if(modelType.equals("ZeroR"))
		{
			ZeroR zeror = new ZeroR();
			zeror.construct(dataset, subset);
			return zeror;
		}
		if(modelType.equals("Rule"))
		{
			DecisionRule rule = new DecisionRule();
			if(!"".equals(parameters))
			{
				if(parameters.split(",")[3].equals("true"))
					subset = DataPreparator.oversampleClasses(subset, dataset.getClassValues(), dataset.getUniqueClassValues());
			}
			rule.construct(dataset, subset);
			return rule;
		}
		if(modelType.equals("Tree"))
		{
			DecisionTree tree = new DecisionTree();
			tree.pruneForPrediction = true;
			if(!"".equals(parameters))
			{
				tree.maxDepth = Integer.parseInt(parameters.split(",")[1]);
				if(parameters.split(",")[3].equals("true"))
					subset = DataPreparator.oversampleClasses(subset, dataset.getClassValues(), dataset.getUniqueClassValues());
				tree.minNodeSize = Integer.parseInt(parameters.split(",")[4]);
			}
			tree.construct(dataset, subset);
			return tree;
		}
		if(modelType.equals("Nested Tree"))
		{
			NestedDecisionTree tree = new NestedDecisionTree();
			tree.pruneForPrediction = true;
			tree.binaryConversion = false;
			if(!"".equals(parameters))
			{
				tree.maxDepth = Integer.parseInt(parameters.split(",")[1]);
				tree.maxDepthNested = Integer.parseInt(parameters.split(",")[2]);
				if(parameters.split(",")[3].equals("true"))
					subset = DataPreparator.oversampleClasses(subset, dataset.getClassValues(), dataset.getUniqueClassValues());
				tree.minNodeSize = Integer.parseInt(parameters.split(",")[4]);
			}
			tree.construct(dataset, subset);
			return tree;
		}
		if(modelType.equals("Binary Nested Tree"))
		{
			NestedDecisionTree tree = new NestedDecisionTree();
			tree.pruneForPrediction = true;
			tree.binaryConversion = true;
			if(!"".equals(parameters))
			{
				tree.maxDepth = Integer.parseInt(parameters.split(",")[1]);
				tree.maxDepthNested = Integer.parseInt(parameters.split(",")[2]);
				if(parameters.split(",")[3].equals("true"))
					subset = DataPreparator.oversampleClasses(subset, dataset.getClassValues(), dataset.getUniqueClassValues());
				tree.minNodeSize = Integer.parseInt(parameters.split(",")[4]);
			}
			tree.construct(dataset, subset);
			return tree;
		}
		
		System.out.println("Could not construct model " + modelType + ", this model type is not supported");
		return null;
	}
	
	public static double selectRegresionThreshold(PredictionModel model, Dataset dataset)
	{
		double[] classValues = dataset.getClassValues();
		double[] predictions = model.predictRawRegression(dataset, model.subsetUsed);		
		double trainingSum = Util.sum(model.subsetUsed);
		
		double[] uniquePredictions = Quicksort.sort(Util.unique(predictions));
		
		double threshold = 0.0;
		double bestAccuracy = 0.0;
		
		for(double p : uniquePredictions)
		{
			predictions = model.predictBinary(dataset, model.subsetUsed, p);
			double accuracy = Util.countMatchesWeighted(classValues, predictions, model.subsetUsed) / trainingSum;
			if(accuracy > bestAccuracy)
			{
				bestAccuracy = accuracy;
				threshold = p;
			}
		}
		predictions = model.predict(dataset, model.subsetUsed, threshold);
		return threshold;
	}
	
	public static double[][] evaluatePredictionModel(PredictionModel model, Dataset dataset, double[] subset)
	{		
		// Get real class values from the dataset
		
		double[] classValues = dataset.getClassValues();
		
		// Calculate the number of test instances
		
		double sum = 0.0;
		if(subset != null)
			sum = Util.sum(subset);
		else
			sum = 0.0 + dataset.getNumberOfInstances();
		
		// Get predictions of the model
		
		double[] predictions = model.predict(dataset, subset);
		
		// Adjust the threshold and predictions if regression is used
		double threshold = 0.0;
		
		if(model.regression)
		{
			threshold = selectRegresionThreshold(model, dataset);
			predictions = model.predictBinary(dataset, model.subsetUsed, threshold);
		}
		
		// Calculate accuracy
		
		if(model.regression)
		{
			predictions = model.predictBinary(dataset, subset, threshold);
		}
		
		double accuracy = Util.countMatchesWeighted(classValues, predictions, subset) / sum;
		
		
		// Calculate F-measures
		
		double[] classFMeasures = new double[dataset.getUniqueClassValues().length];
		double[] classFrequencies = new double[dataset.getUniqueClassValues().length];
		
		double[][] classifiedSubsets = new double[dataset.getUniqueClassValues().length][];
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			double classValue = dataset.getUniqueClassValues()[classValueIndex];
			classifiedSubsets[classValueIndex] = new double[dataset.getNumberOfInstances()];
			for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
			{
				if(predictions[instanceIndex] == classValue)
				{
					if(subset == null)
						classifiedSubsets[classValueIndex][instanceIndex] = 1.0;
					else
						classifiedSubsets[classValueIndex][instanceIndex] = subset[instanceIndex];
				}
			}
		}
		
		double[][] classCounts = new double[dataset.getUniqueClassValues().length][];
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			classCounts[classValueIndex] = Util.count(classValues, dataset.getUniqueClassValues(), classifiedSubsets[classValueIndex]);
		}
		
		double gini = 0.0;
		double totalCount;
		if(subset == null)
			totalCount = 0.0 + dataset.getNumberOfInstances();
		else
			totalCount = Util.sum(subset);
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			double classCount = Util.sum(classifiedSubsets[classValueIndex]);
			double classGini = calculateGini(classCounts[classValueIndex]);
			
			if(classCount != 0)
				gini += classGini * (classCount / totalCount);
		}
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			double classValue = dataset.getUniqueClassValues()[classValueIndex];
			double tp = 0.0;
			double tn = 0.0;
			double fp = 0.0;
			double fn = 0.0;
			
			for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
			{
				if(subset != null)
					if(subset[instanceIndex] == 0.0)
						continue;
				
				if(classValues[instanceIndex] == classValue)
				{
					if(subset != null)
						classFrequencies[classValueIndex] += subset[instanceIndex];
					else
						classFrequencies[classValueIndex] += 1.0;
					if(predictions[instanceIndex] == classValue)
						tp += 1.0;
					else
						fn += 1.0;
				}
				else
					if(predictions[instanceIndex] == classValue)
						fp += 1.0;
					else
						tn += 1.0;
			}
			
			double precision = tp / (tp + fp);
			double recall = tp / (tp + fn);
			
			double fMeasure = 2 * precision * recall / (precision + recall);
			if(precision + recall == 0)
				fMeasure = 0;
			
			// Avoid NaN values in experimental models
			if(tp == 0.0 && (fn == 0.0 || fp == 0.0))
				fMeasure = 0;
			
			classFMeasures[classValueIndex] = fMeasure;
		}
		
		double averageFMeasure = Util.sum(classFMeasures) / (0.0 + dataset.getUniqueClassValues().length);
		double weightedAverageFMeasure = 0.0;
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
			weightedAverageFMeasure += classFMeasures[classValueIndex] * classFrequencies[classValueIndex] / sum;		
		
		double size = 0.0 + model.size();
		
		double innerModelsSize = 0.0;
		
		if(model instanceof NestedDecisionTree)
			innerModelsSize = ((NestedDecisionTree)model).innerNodesAverageSize();
		
		if(Double.isNaN(innerModelsSize))
			innerModelsSize = 0;
		
		predictions = model.predict(dataset, subset);
		
		double[] uniquePredictions = Quicksort.sort(Util.unique(predictions));
		
		double[][] truePositiveRates = new double[dataset.getUniqueClassValues().length][];
		double[][] falsePositiveRates = new double[dataset.getUniqueClassValues().length][];
		
		for(int classIndex = 0; classIndex < dataset.getUniqueClassValues().length; classIndex++)
		{
			truePositiveRates[classIndex] = new double[uniquePredictions.length];
			falsePositiveRates[classIndex] = new double[uniquePredictions.length];
		}
		
		for(int i = 0; i < uniquePredictions.length; i++)
		{
			if(model.regression)
				predictions = model.predictBinary(dataset, subset, uniquePredictions[i]);
			else
				predictions = model.predict(dataset, subset);
			
			classFrequencies = new double[dataset.getUniqueClassValues().length];
			
			for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
			{
				double classValue = dataset.getUniqueClassValues()[classValueIndex];
				double tp = 0.0;
				double tn = 0.0;
				double fp = 0.0;
				double fn = 0.0;
				
				for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
				{
					if(subset != null)
						if(subset[instanceIndex] == 0.0)
							continue;
					
					if(classValues[instanceIndex] == classValue)
					{
						if(subset != null)
							classFrequencies[classValueIndex] += subset[instanceIndex];
						else
							classFrequencies[classValueIndex] += 1.0;
						if(predictions[instanceIndex] == classValue)
							tp += 1.0;
						else
							fn += 1.0;
					}
					else
						if(predictions[instanceIndex] == classValue)
							fp += 1.0;
						else
							tn += 1.0;
				}
				
				truePositiveRates[classValueIndex][i] = tp / (tp + fn);
				falsePositiveRates[classValueIndex][i] = fp / (fp + tn);				
			}
		}
		
		double averageAUROC = ModelEvaluator.calculateAUROC(model, dataset, subset, uniquePredictions, classValues);
		double[] aurocPerClass = new double[] {averageAUROC, averageAUROC};
		
		double[][] accuracyMeasures = new double[7][];
		
		accuracyMeasures[0] = new double[] {accuracy};
		accuracyMeasures[1] = classFMeasures;
		accuracyMeasures[2] = new double[] {averageFMeasure, weightedAverageFMeasure};
		accuracyMeasures[3] = new double[] {gini};
		accuracyMeasures[4] = aurocPerClass;
		accuracyMeasures[5] = new double[] {averageAUROC};
		accuracyMeasures[6] = new double[] {size, innerModelsSize};
		
		return accuracyMeasures;
	}
	
	public static double evaluatePredictionModelGini(PredictionModel model, Dataset dataset, double[] subset)
	{
		double[] classValues = dataset.getClassValues();
		
		double[] predictions = model.predict(dataset, subset);
		
		if(model.regression)
		{
			double threshold = selectRegresionThreshold(model, dataset);
			predictions = model.predictBinary(dataset, model.subsetUsed, threshold);
		}
		
		double[][] classifiedSubsets = new double[dataset.getUniqueClassValues().length][];
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			double classValue = dataset.getUniqueClassValues()[classValueIndex];
			classifiedSubsets[classValueIndex] = new double[dataset.getNumberOfInstances()];
			for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
			{
				if(predictions[instanceIndex] == classValue)
				{
					if(subset == null)
						classifiedSubsets[classValueIndex][instanceIndex] = 1.0;
					else
						classifiedSubsets[classValueIndex][instanceIndex] = subset[instanceIndex];
				}
			}
		}
		
		double[][] classCounts = new double[dataset.getUniqueClassValues().length][];
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			classCounts[classValueIndex] = Util.count(classValues, dataset.getUniqueClassValues(), classifiedSubsets[classValueIndex]);
		}
		
		double gini = 0.0;
		double totalCount;
		if(subset == null)
			totalCount = 0.0 + dataset.getNumberOfInstances();
		else
			totalCount = Util.sum(subset);
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			double classCount = Util.sum(classifiedSubsets[classValueIndex]);
			double classGini = calculateGini(classCounts[classValueIndex]);
			
			if(classCount != 0)
				gini += classGini * (classCount / totalCount);
		}
		
		return gini;
	}
	
	public static double[][][] generateStratifiedCrossValidationSplits(Dataset dataset, int numberOfSplits, long randomSeed)
	{		
		Random random = new Random(randomSeed);
		
		int numberOfInstances = dataset.getNumberOfInstances();
		
		double[][][] splits = new double[numberOfSplits][][];
		
		for(int i = 0; i < numberOfSplits; i++)
		{
			splits[i] = new double[2][];
			splits[i][0] = new double[numberOfInstances];
			splits[i][1] = new double[numberOfInstances];
		}
		
		if(numberOfSplits == 1)
		{
			splits[0][0] = Util.fill(1.0, dataset.getNumberOfInstances());
			splits[0][1] = Util.fill(1.0, dataset.getNumberOfInstances());
			return splits;
		}
		
		double[] classValuesInInstances = dataset.getClassValues();
		
		if(dataset.getUniqueClassValues() == null)
			dataset.initialiseUniqueClassValues();
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			ArrayList<Integer> instanceIndexes = new ArrayList<Integer>();
			for(int instanceIndex = 0; instanceIndex < numberOfInstances; instanceIndex++)
			{
				if(classValuesInInstances[instanceIndex] == dataset.getUniqueClassValues()[classValueIndex])
					instanceIndexes.add(new Integer(instanceIndex));
			}
			
			int splitIndex = 0;
			while(instanceIndexes.size() > 0)
			{
				int nextIndex = random.nextInt(instanceIndexes.size());
				splits[splitIndex][1][instanceIndexes.get(nextIndex)] = 1.0;
				splitIndex++;
				if(splitIndex == numberOfSplits)
					splitIndex = 0;
				instanceIndexes.remove(nextIndex);
			}
		}
		
		for(int splitIndex = 0; splitIndex < numberOfSplits; splitIndex++)
		{
			splits[splitIndex][0] = Util.invert(splits[splitIndex][1]);
		}
		
		return splits;
	}
	
	public static Object[] runCrossValidationExperiments(Dataset dataset, String modelType, String modelParameters, int numberOfSplits, int numberOfExperiments)
	{
		double[][] averageAccuracyMeasures = null;
		
		int[] bestModelSelectionMeasure = new int[3];
		// First two numbers indicate the index of the desired measure in the measures output
		// Third number is 1 if higher values is better, 0 if lower value is better
		
		if(modelParameters.indexOf("Accuracy") != -1)
			bestModelSelectionMeasure = new int[] {0,0,1};
		else if(modelParameters.indexOf("AverageFMeasure") != -1)
				bestModelSelectionMeasure = new int[] {2,0,1};
		else if(modelParameters.indexOf("WeightedFMeasure") != -1)
				bestModelSelectionMeasure = new int[] {2,1,1};
		else
			bestModelSelectionMeasure = new int[] {0,0,1}; // Default to Accuracy
		
		boolean optimisingForHigherValue = bestModelSelectionMeasure[2] == 1;
		double bestMeasure = (bestModelSelectionMeasure[2] == 1)?Double.MIN_VALUE:Double.MAX_VALUE;
		PredictionModel bestModel = null;
		
		for(int experimentIndex = 0; experimentIndex < numberOfExperiments; experimentIndex++)
		{
			double[][][] crossValidationSplits = ModelEvaluator.generateStratifiedCrossValidationSplits(dataset, numberOfSplits, 0l + experimentIndex);
			for(int crossValidationIndex = 0; crossValidationIndex < numberOfSplits; crossValidationIndex++)
			{
				double[] trainingSet = crossValidationSplits[crossValidationIndex][0];
				double[] testSet = crossValidationSplits[crossValidationIndex][1];
				PredictionModel model = ModelEvaluator.constructPredictionModel(modelType, dataset, trainingSet, modelParameters);
				if(model.constructionFailed)
				{
					System.out.println("Construction Failed");
					System.exit(0);
					model = ModelEvaluator.constructPredictionModel("ZeroR", dataset, trainingSet, modelParameters);
				}
				if(modelParameters.split(",")[5].equals("true"))
					model.convertToRegression();
				double[][] accuracyMeasures = ModelEvaluator.evaluatePredictionModel(model, dataset, testSet);
				ModelEvaluator.printSingleExperimentResults("CrossValidation" + crossValidationIndex, accuracyMeasures, model, dataset);
				averageAccuracyMeasures = Util.sum(averageAccuracyMeasures, accuracyMeasures);
				double accuracyMeasureForOptimisation = 
						accuracyMeasures[bestModelSelectionMeasure[0]][bestModelSelectionMeasure[1]];
				if(accuracyMeasureForOptimisation > bestMeasure && optimisingForHigherValue)
				{
					bestMeasure = accuracyMeasures[bestModelSelectionMeasure[0]][bestModelSelectionMeasure[1]];
					bestModel = model;
				}
				else if(accuracyMeasureForOptimisation < bestMeasure && !optimisingForHigherValue)
				{
					bestMeasure = accuracyMeasures[bestModelSelectionMeasure[0]][bestModelSelectionMeasure[1]];
					bestModel = model;
				}
			}
		}
		
		averageAccuracyMeasures = Util.divide(averageAccuracyMeasures, ((0.0 + numberOfSplits) * (0.0 + numberOfExperiments)));
		
		return new Object[] {averageAccuracyMeasures, bestModel};
	}
	
	public static Object[] runCrossValidationExperimentsParallelised(Dataset dataset, String modelType, String modelParameters, int numberOfSplits, int numberOfExperiments)
	{
		ArrayList<Integer> experimentIndexes = new ArrayList<Integer>();
		for(int experimentIndex = 0; experimentIndex < numberOfExperiments; experimentIndex++)
			experimentIndexes.add(new Integer(experimentIndex));
		double[][] averageAccuracyMeasures = null;
		
		double[][][] experimentResults = new double[numberOfExperiments][][];
		PredictionModel[] bestModels = new PredictionModel[numberOfExperiments];
		
		int[] bestModelSelectionMeasure = new int[3];
		// First two numbers indicate the index of the desired measure in the measures output
		// Third number is 1 if higher values is better, 0 if lower value is better
		
		if(modelParameters.indexOf("Accuracy") != -1)
			bestModelSelectionMeasure = new int[] {0,0,1};
		else if(modelParameters.indexOf("AverageFMeasure") != -1)
				bestModelSelectionMeasure = new int[] {2,0,1};
		else if(modelParameters.indexOf("WeightedFMeasure") != -1)
				bestModelSelectionMeasure = new int[] {2,1,1};
		else
			bestModelSelectionMeasure = new int[] {0,0,1}; // Default to Accuracy
		
		boolean optimisingForHigherValue = bestModelSelectionMeasure[2] == 1;
		double bestMeasure = (bestModelSelectionMeasure[2] == 1)?Double.MIN_VALUE:Double.MAX_VALUE;
		PredictionModel bestModel = null;
		
		experimentIndexes
			.parallelStream()
			.forEach(e -> 
				{
					Object[] results = runCrossValidationExperiment(dataset, modelType, modelParameters, numberOfSplits, e);
					experimentResults[e] = (double[][])results[0];
					bestModels[e] = (PredictionModel)results[1];
				});
		
		for(int experimentIndex = 0; experimentIndex < numberOfExperiments; experimentIndex++)
		{
			averageAccuracyMeasures = Util.sum(averageAccuracyMeasures, experimentResults[experimentIndex]);
		}
		
		averageAccuracyMeasures = Util.divide(averageAccuracyMeasures, (0.0 + numberOfSplits * numberOfExperiments));
		
		for(int experimentIndex = 0; experimentIndex < numberOfExperiments; experimentIndex++)
		{
			double accuracyMeasureForOptimisation = 
					experimentResults[experimentIndex][bestModelSelectionMeasure[0]][bestModelSelectionMeasure[1]];
			if(accuracyMeasureForOptimisation > bestMeasure && optimisingForHigherValue)
			{
				bestMeasure = experimentResults[experimentIndex][bestModelSelectionMeasure[0]][bestModelSelectionMeasure[1]];
				bestModel = bestModels[experimentIndex];
			}
			else if(accuracyMeasureForOptimisation < bestMeasure && !optimisingForHigherValue)
			{
				bestMeasure = experimentResults[experimentIndex][bestModelSelectionMeasure[0]][bestModelSelectionMeasure[1]];
				bestModel = bestModels[experimentIndex];
			}
		}
		
		return new Object[] {averageAccuracyMeasures, bestModel};
	}
	
	private static Object[] runCrossValidationExperiment(Dataset dataset, String modelType, String modelParameters, int numberOfSplits, int experimentIndex)
	{
		double[][] averageAccuracyMeasures = null;
		double[][][] crossValidationSplits = ModelEvaluator.generateStratifiedCrossValidationSplits(dataset, numberOfSplits, 0l + experimentIndex);
		
		int[] bestModelSelectionMeasure = new int[3];
		// First two numbers indicate the index of the desired measure in the measures output
		// Third number is 1 if higher values is better, 0 if lower value is better
		
		if(modelParameters.indexOf("Accuracy") != -1)
			bestModelSelectionMeasure = new int[] {0,0,1};
		else if(modelParameters.indexOf("AverageFMeasure") != -1)
				bestModelSelectionMeasure = new int[] {2,0,1};
		else if(modelParameters.indexOf("WeightedFMeasure") != -1)
				bestModelSelectionMeasure = new int[] {2,1,1};
		else
			bestModelSelectionMeasure = new int[] {0,0,1}; // Default to Accuracy
		
		boolean optimisingForHigherValue = bestModelSelectionMeasure[2] == 1;
		double bestMeasure = (bestModelSelectionMeasure[2] == 1)?Double.MIN_VALUE:Double.MAX_VALUE;
		PredictionModel bestModel = null;
		
		for(int crossValidationIndex = 0; crossValidationIndex < numberOfSplits; crossValidationIndex++)
		{
			double[] trainingSet = crossValidationSplits[crossValidationIndex][0];
			double[] testSet = crossValidationSplits[crossValidationIndex][1];
			PredictionModel model = ModelEvaluator.constructPredictionModel(modelType, dataset, trainingSet, modelParameters);
			if(modelParameters.split(",")[5].equals("true"))
				model.convertToRegression();
			double[][] accuracyMeasures = ModelEvaluator.evaluatePredictionModel(model, dataset, testSet);
			averageAccuracyMeasures = Util.sum(averageAccuracyMeasures, accuracyMeasures);
			double accuracyMeasureForOptimisation = 
					accuracyMeasures[bestModelSelectionMeasure[0]][bestModelSelectionMeasure[1]];
			if(accuracyMeasureForOptimisation > bestMeasure && optimisingForHigherValue)
			{
				bestMeasure = accuracyMeasures[bestModelSelectionMeasure[0]][bestModelSelectionMeasure[1]];
				bestModel = model;
			}
			else if(accuracyMeasureForOptimisation < bestMeasure && !optimisingForHigherValue)
			{
				bestMeasure = accuracyMeasures[bestModelSelectionMeasure[0]][bestModelSelectionMeasure[1]];
				bestModel = model;
			}
		}
		return new Object[] {averageAccuracyMeasures, bestModel};
	}
	
	public static String experimentResultsToString(String prefix, Object[] results, Dataset dataset)
	{
		double[][] accuracyResults = (double[][]) results[0];
		String out = "";
		out = out + "-------------------------------------------" + "\n";
		out = out + prefix + "\n";
		out = out + "Accuracy: " + accuracyResults[0][0] + "\n";
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			out = out + "F-Measure of class " + dataset.getUniqueClassValues()[classValueIndex] + " is " + accuracyResults[1][classValueIndex] + "\n";
		}
		out = out + "Average F-Measure is " + accuracyResults[2][0] + "\n";
		out = out + "Weighted average F-Measure is " + accuracyResults[2][1] + "\n";
		out = out + "Model Gini is " + accuracyResults[3][0] + "\n";
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			out = out + "AUROC of class " + dataset.getUniqueClassValues()[classValueIndex] + " is " + accuracyResults[4][classValueIndex] + "\n";
		}
		out = out + "Average AUROC is " + accuracyResults[5][0] + "\n";
		out = out + "Average model size is " + accuracyResults[6][0] + "\n";
		out = out + "Average inner model size is " + accuracyResults[6][1] + "\n";
		out = out + "BestModel:" + "\n";
		out = out + ((PredictionModel)results[1]).toString() + "\n";
		out = out + "-------------------------------------------" + "\n";
		
		return out;
	}
	
	public static void printCrossExperimentResults(String prefix, Object[] results, Dataset dataset)
	{
		System.out.println(experimentResultsToString(prefix, results, dataset));
	}
	
	public static void printSingleExperimentResults(String prefix, double[][] results, PredictionModel model, Dataset dataset)
	{
		System.out.println(experimentResultsToString(prefix, new Object[] {results, model}, dataset));
	}
	
	public static void saveCrossExperimentResults(String fileName, String prefix, Object[] results, Dataset dataset)
	{
		String path = "results/" + fileName + ".txt";
		FileWriter myWriter;
		try {
			myWriter = new FileWriter(path);
			myWriter.write(experimentResultsToString(prefix, results, dataset));
			myWriter.close();
			
		} catch (IOException e) {
			System.out.println("Failed to write to " + path);
			e.printStackTrace();
		}
	}
	
	public static double estimateGiniOfDataset(Dataset dataset, double[] subset)
	{
		double classCounts[] = new double[dataset.getUniqueClassValues().length];
		int classIndex = dataset.getClassAttributeIndex();
		double[] uniqueClassValues = dataset.getUniqueClassValues();
		
		for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
		{
			double classValue = dataset.getValue(instanceIndex, classIndex);
			int classValueIndex = Util.indexOf(classValue, uniqueClassValues);
			if(subset == null)
				classCounts[classValueIndex] += 1.0;
			else
				classCounts[classValueIndex] += subset[instanceIndex];
		}
		
		return ModelEvaluator.calculateGini(classCounts);
	}
	
	public static double calculateGini(double[] classCounts)
	{
		return calculateGini(Util.sum(classCounts), classCounts);
	}
	
	public static double calculateGini(double totalCount, double[] classCounts)
	{
		double gini = 0.0;
		
		for(double classCount : classCounts)
		{
			double p = classCount / totalCount;
			gini += p * (1.0 - p);
		}
		
		return gini;
	}
	
	public static double calculateEntropy(double[] classCounts)
	{
		return calculateEntropy(Util.sum(classCounts), classCounts);
	}
	
	public static double calculateEntropy(double totalCount, double[] classCounts)
	{
		double entropy = 0.0;
		
		for(double classCount : classCounts)
		{
			double p = classCount / totalCount;
			entropy += p * (Math.log(p) / logE2);
		}
		
		return entropy;
	}
	
	public static double calculateTotalEntropy(double[][] classCountsOfSplits)
	{
		double totalSum = Util.sum(classCountsOfSplits);
		for(double[] classCounts : classCountsOfSplits)
		{
			double sum = Util.sum(classCounts);
			totalSum += calculateEntropy(sum, classCounts) * (sum / totalSum);
		}
		return totalSum;
	}
	
	public static double calculateInformationGainRatio(double[] originalClassCounts, double[][] classCountsOfSplits)
	{
		double originalEntropy = calculateEntropy(originalClassCounts);
		
		double splitEntropy = calculateTotalEntropy(classCountsOfSplits);
		
		double informationGain = originalEntropy - splitEntropy;
		
		double informationGainRatio = informationGain / splitEntropy;
		
		return informationGainRatio;
	}
	
	public static double calculateInformationGain(double[] originalClassCounts, double[][] classCountsOfSplits)
	{
		double originalEntropy = calculateEntropy(originalClassCounts);
		
		double splitEntropy = calculateTotalEntropy(classCountsOfSplits);
		
		double informationGain = originalEntropy - splitEntropy;
		
		return informationGain;
	}
	
	public static double calculateAUROC(PredictionModel model, Dataset dataset, double[] subset, double[] predictions, double[] classValues)
	{
		// Initialise variables for AUROC calculation
		
		double[] aurocPerClass = new double[dataset.getUniqueClassValues().length];
		double[] thresholds = Quicksort.sort(Util.unique(predictions));
		
		double[][] truePositiveRates = new double[dataset.getUniqueClassValues().length][];
		double[][] falsePositiveRates = new double[dataset.getUniqueClassValues().length][];

		double[] classFrequencies = new double[dataset.getUniqueClassValues().length];
		
		for(int classIndex = 0; classIndex < dataset.getUniqueClassValues().length; classIndex++)
		{
			truePositiveRates[classIndex] = new double[thresholds.length];
			falsePositiveRates[classIndex] = new double[thresholds.length];
		}
		
		// Iterate over all thresholds to get points of the AUROC curve
		
		for(int i = 0; i < thresholds.length; i++)
		{
			// Predict the class values based on the current threshold
			if(model.regression)
				predictions = model.predictBinary(dataset, subset, thresholds[i]);
			else
				predictions = model.predict(dataset, subset);
			
			// Generate a point on the AUROC curves of all classes
			
			for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
			{
				// The class value
				double classValue = dataset.getUniqueClassValues()[classValueIndex];
				
				// Calculate the components of TPR and TNR
				double tp = 0.0;
				double tn = 0.0;
				double fp = 0.0;
				double fn = 0.0;
				
				for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
				{
					if(subset != null)
						if(subset[instanceIndex] == 0.0)
							continue;
					
					if(classValues[instanceIndex] == classValue)
					{
//						if(subset != null)
//							classFrequencies[classValueIndex] += subset[instanceIndex];
//						else
							classFrequencies[classValueIndex] += 1.0;
						if(predictions[instanceIndex] == classValue)
							tp += 1.0;
						else
							fn += 1.0;
					}
					else
						if(predictions[instanceIndex] == classValue)
							fp += 1.0;
						else
							tn += 1.0;
				}
				
				// Calculate TPR and FPR of the currecnt class at the current threshold
				truePositiveRates[classValueIndex][i] = tp / (tp + fn);
				falsePositiveRates[classValueIndex][i] = fp / (fp + tn);				
			}
		}
		
		// Calculate AUROC values for each class
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			aurocPerClass[classValueIndex] = 0;
			for(int i = 0; i < thresholds.length + 1; i++)
			{
				double currentTPR;
				double currentFPR;
				
				if(i == thresholds.length)
				{
					if(classValueIndex == 0.0)
					{
						currentTPR = 1.0;
						currentFPR = 1.0;
					}
					else
					{
						currentTPR = 0.0;
						currentFPR = 0.0;
					}
					
				}
				else
				{
					currentTPR = truePositiveRates[classValueIndex][i];
					currentFPR = falsePositiveRates[classValueIndex][i];
				}
				
				double previousTPR;
				double previousFPR;
				
				if(classValueIndex == 0.0)
				{
					previousTPR = 0.0;
					previousFPR = 0.0;
				}
				else
				{
					previousTPR = 1.0;
					previousFPR = 1.0;
				}
				
				if(i > 0)
				{
					previousTPR = truePositiveRates[classValueIndex][i-1];
					previousFPR = falsePositiveRates[classValueIndex][i-1];
				}
//				else
//				{
//					continue;
//				}
				
				aurocPerClass[classValueIndex] += Math.abs(currentFPR - previousFPR) * Math.min(currentTPR, previousTPR);
				aurocPerClass[classValueIndex] += 0.5 * Math.abs(currentFPR - previousFPR) * Math.abs(currentTPR - previousTPR);
			}
		}
		
		double averageAUROC = 0.0;
		
		for(int classValueIndex = 0; classValueIndex < dataset.getUniqueClassValues().length; classValueIndex++)
		{
			averageAUROC += aurocPerClass[classValueIndex];
		}
		
		averageAUROC /= 0.0 + dataset.getUniqueClassValues().length;
		
		if(averageAUROC > 1.0)
		{
			Dataset toSave = new Dataset();
			toSave.setAttributeNames(new String[] {"Class_" + dataset.getClassValues().length, "Predictions_" + predictions.length, "Subset_" + subset.length});
			toSave.setValues(Util.transpose(new double[][] {dataset.getClassValues(), model.predict(dataset, subset), subset}));
			toSave.writeToCSV("syntheticDatasets/" + System.currentTimeMillis() + "_AUROC_HIGH_" + averageAUROC + ".csv");
		}
		
		return averageAUROC;
	}
}
