/*
 * PredictionModel abstract class for creating prediction models
 * 
 * Methods can be redefined for custom and/or non-leaf-based models
 */
public abstract class PredictionModel {
	
	// Construction parameters
	protected boolean[] attributeFilter;
	
	// Construction attributes
	protected Dataset datasetUsed;
	protected double[] subsetUsed;
	protected boolean regression;
	protected boolean regressionThreshold;
	
	// Prediction attributes
	protected double defaultPrediction;
	protected int numberOfLeaves;
	protected double[] leafPredictions;
	
	// Model connectivity attributes
	protected int parentNodeIndex;
	protected int[] leafNextNodeIndexes;
	
	// Flags
	public boolean constructed;
	public boolean constructionFailed;
	
	// Printing
	public int printLevel = 0; 
	
	// Model construction
	public void construct(Dataset dataset)
	{
		construct(dataset, null);
	}
	public abstract void construct(Dataset dataset, double[] subset);
	
	// Instance class prediction
	public double predict(Dataset dataset, int instanceIndex)
	{
		return leafPredictions[predictAsLeafIndex(dataset, instanceIndex)];
	}
	
	public double predict(Dataset dataset, int instanceIndex, double threshold)
	{
		double prediction = predict(dataset, instanceIndex);
		double[] uniqueClassValues = dataset.getUniqueClassValues();
		double[] thresholds = Util.thresholds(uniqueClassValues, threshold);
		int classPrediction = Util.indexOfTheHighestBelowOrEqual(thresholds, prediction);
		return uniqueClassValues[classPrediction];
	}
	
	public double predictBinary(Dataset dataset, int instanceIndex, double threshold)
	{
		double prediction = predict(dataset, instanceIndex);
		if(prediction <= threshold)
			return 0.0;
		else
			return 1.0;
	}
	
	public double[] predict(Dataset dataset, double[] subset)
	{
		double[] predictions = new double[dataset.getNumberOfInstances()];
		for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
		{
			if(subset != null)
				if(subset[instanceIndex] == 0.0)
					continue;
			predictions[instanceIndex] = predict(dataset, instanceIndex);
		}
		return predictions;
	}
	
	public abstract double[] predictRawRegression(Dataset dataset, double[] subset);
	public abstract double predictRawRegression(Dataset dataset, int instanceIndex);
	
	public double[] predict(Dataset dataset, double[] subset, double threshold)
	{
		double[] predictions = new double[dataset.getNumberOfInstances()];
		for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
		{
			if(subset != null)
				if(subset[instanceIndex] == 0.0)
					continue;
			predictions[instanceIndex] = predict(dataset, instanceIndex, threshold);
		}
		return predictions;
	}
	
	public double[] predictBinary(Dataset dataset, double[] subset, double threshold)
	{
		double[] predictions = new double[dataset.getNumberOfInstances()];
		for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
		{
			if(subset != null)
				if(subset[instanceIndex] == 0.0)
					continue;
			predictions[instanceIndex] = predictBinary(dataset, instanceIndex, threshold);
		}
		return predictions;
	}
	
	// Instance leaf prediction
	public abstract int predictAsLeafIndex(Dataset dataset, int instanceIndex);
	public int[] predictAsLeafIndexes(Dataset dataset, double[] subset)
	{
		int[] leafIndexes = new int[dataset.getNumberOfInstances()];
		for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
		{
			if(subset != null)
				if(subset[instanceIndex] == 0.0)
					continue;
			leafIndexes[instanceIndex] = predictAsLeafIndex(dataset, instanceIndex);
		}
		return leafIndexes;		
	}
	
	public int predictAsNextNodeIndex(Dataset dataset, int instanceIndex)
	{
		return leafNextNodeIndexes[predictAsLeafIndex(dataset, instanceIndex)];
	}
	
	// Leaf subset prediction
	public double[][] constructLeafSubsets(Dataset dataset, double[] subset)
	{
		double[][] leafSubsets = new double[numberOfLeaves][];
		
		for(int leafIndex = 0; leafIndex < numberOfLeaves; leafIndex++)
			leafSubsets[leafIndex] = new double[dataset.getNumberOfInstances()];
		
		for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
		{
			if(subset != null)
			{
				if(subset[instanceIndex] == 0.0)
					continue;
				leafSubsets[predictAsLeafIndex(dataset, instanceIndex)][instanceIndex] = subset[instanceIndex];
			}
			else
			{
				leafSubsets[predictAsLeafIndex(dataset, instanceIndex)][instanceIndex] = 1.0;
			}	
		}
		return leafSubsets;
	}
	
	// Utility
	public abstract String toString();
	
	public void print()
	{
		System.out.println(toString());
	}
	
	public boolean[] getAttributeFilter()
	{
		return attributeFilter;
	}
	
	public void setAttributeFilter(boolean[] attributeFilter)
	{
		this.attributeFilter = attributeFilter;
	}
	
	public double getDefaultPrediction()
	{
		return defaultPrediction;
	}
	
	public void setDefaultPrediction(double defaultPrediction)
	{
		this.defaultPrediction = defaultPrediction;
	}
	
	public int getNumberOfLeaves()
	{
		return numberOfLeaves;
	}
	
	public void setNumberOfLeaves(int numberOfLeaves)
	{
		this.numberOfLeaves = numberOfLeaves;
	}
	
	public int getParentNodeIndex()
	{
		return parentNodeIndex;
	}
	
	public void setParentNodeIndex(int parentNodeIndex)
	{
		this.parentNodeIndex = parentNodeIndex;
	}
	
	public double getLeafPrediction(int leafIndex)
	{
		return leafPredictions[leafIndex];
	}
	
	public int getLeafNextNodeIndex(int leafIndex)
	{
		return leafNextNodeIndexes[leafIndex];
	}
	
	public void setLeafNodeIndex(int leafIndex, int nextNodeIndex)
	{
		leafNextNodeIndexes[leafIndex] = nextNodeIndex;
	}
	
	public void setLeafNextNodeIndex(int leafIndex, int newNextNodeIndex)
	{
		leafNextNodeIndexes[leafIndex] = newNextNodeIndex;
	}
	
	public Dataset getDatasetUsed()
	{
		return datasetUsed;
	}
	
	public double[] getSubsetUsed()
	{
		return subsetUsed;
	}
	
	public abstract void convertToRegression();
	
	/*
	 * Once the predictAsLeafIndex method is defined and the model is constructed, this method can be used
	 * to initialise leaf predictions as well as the default prediction
	 */
	public void initialiseLeafPredictions(Dataset dataset, double[] subset)
	{
		leafPredictions = new double[numberOfLeaves];
		
		// Predict all valid instances as leaves to calculate leaf predictions and default prediction
		int[] leafIndexPredictions = predictAsLeafIndexes(dataset, subset);
		
		double[] countLeafInstances = new double[numberOfLeaves];
		
		double[][] leafClassFrequencies = new double[numberOfLeaves][];
		
		for(int leafIndex = 0; leafIndex < numberOfLeaves; leafIndex++)
			leafClassFrequencies[leafIndex] = new double[dataset.getUniqueClassValues().length];
		
		// Calculate how often each class appears in each leaf
		for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
		{
			if(subset != null)
			{
				if(subset[instanceIndex] == 0.0)
					continue;
				else
				{
					countLeafInstances[leafIndexPredictions[instanceIndex]] += subset[instanceIndex];
					leafClassFrequencies[leafIndexPredictions[instanceIndex]][Util.indexOf(dataset.getValue(instanceIndex, dataset.getClassAttributeIndex()), dataset.getUniqueClassValues())] += subset[instanceIndex];
				}
			}
			else
			{
				countLeafInstances[leafIndexPredictions[instanceIndex]] += 1.0;
				leafClassFrequencies[leafIndexPredictions[instanceIndex]][Util.indexOf(dataset.getValue(instanceIndex, dataset.getClassAttributeIndex()), dataset.getUniqueClassValues())] += 1.0;
			}
		}
		
		// Assign leaf predictions based on most common classes in each leaf
		double[] leafClassFrequenciesSums = new double[numberOfLeaves];
		for(int leafIndex = 0; leafIndex < numberOfLeaves; leafIndex++)
		{
			if(regression)
				leafPredictions[leafIndex] = Util.averageOfDistribution(leafClassFrequencies[leafIndex], dataset.getUniqueClassValues());
			else
				leafPredictions[leafIndex] = dataset.getUniqueClassValues()[Util.maxIndex(leafClassFrequencies[leafIndex])];
			leafClassFrequenciesSums[leafIndex] = Util.sum(leafClassFrequencies[leafIndex]);
		}
		
		// Assign default prediction as average class value (regression) or the leaf prediction of the most popular leaf (classification)
		if(regression)
		{
			defaultPrediction = PredictionModel.generateDefaultRegressionPrediction(dataset, subset);
		}
		else
			defaultPrediction = leafPredictions[Util.maxIndex(leafClassFrequenciesSums)];
	}
	
	public abstract int size();
	
	public static double generateDefaultRegressionPrediction(Dataset dataset, double[] subset)
	{
		double[] classValues = dataset.getClassValues();
		double[] uniqueClassValues = dataset.getUniqueClassValues();
		double[] countClasses = new double[uniqueClassValues.length];
		
		for(int instanceIndex = 0; instanceIndex < classValues.length; instanceIndex++)
		{
			if(subset != null)
				if(subset[instanceIndex] == 0.0) continue;
			for(int classValueIndex = 0; classValueIndex < uniqueClassValues.length; classValueIndex++)
			{
				if(uniqueClassValues[classValueIndex] == classValues[instanceIndex])
				{
					if(subset != null)
						countClasses[classValueIndex] += subset[instanceIndex];
					else
						countClasses[classValueIndex] += 1.0;;
					break;
				}
			}
		}
		double defaultPrediction = Util.averageOfDistribution(countClasses, uniqueClassValues);
		return defaultPrediction;
	}
}
