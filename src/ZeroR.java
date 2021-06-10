
public class ZeroR extends PredictionModel {

	@Override
	public void construct(Dataset dataset, double[] subset)
	{
		construct(dataset, subset, false);
	}

	public void construct(Dataset dataset, double[] subset, boolean regression)
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
						countClasses[classValueIndex] += 1.0;
					break;
				}
			}
		}
		
		if(regression)
			defaultPrediction = Util.averageOfDistribution(countClasses, uniqueClassValues);
		else
			defaultPrediction = uniqueClassValues[Util.maxIndex(countClasses)];
		if(countClasses[0] == 0.0 && countClasses[1] == 0.0)
			defaultPrediction = 0.0;
		numberOfLeaves = 1;
		leafNextNodeIndexes = new int[] {-1};
		leafPredictions = new double[] {defaultPrediction};
		parentNodeIndex = -1;
		
		datasetUsed = dataset;
		subsetUsed = subset;
	}

	@Override // Overridden for optimisation
	public double predict(Dataset dataset, int instanceIndex)
	{
		return defaultPrediction;
	}
	
	@Override
	public void convertToRegression()
	{
		int parentNodeIndex = this.parentNodeIndex;
		construct(datasetUsed, subsetUsed, true);
		this.parentNodeIndex = parentNodeIndex;
		this.regression = true;
	}

	@Override // Overridden for optimisation
	public double[] predict(Dataset dataset, double[] subset)
	{
		if(subset != null)
			return Util.fill(defaultPrediction, subset.length);
		else
			return Util.fill(defaultPrediction, dataset.getNumberOfInstances());
	}
	
	public double[] predictRegression(Dataset dataset, double[] subset, double threshold)
	{
		double[] uniqueClassValues = dataset.getUniqueClassValues();
		double[] thresholds = Util.thresholds(uniqueClassValues, threshold);
		int classPrediction = Util.indexOfTheHighestBelowOrEqual(thresholds, defaultPrediction);
		if(subset != null)
			return Util.fill(uniqueClassValues[classPrediction], subset.length);
		else
			return Util.fill(uniqueClassValues[classPrediction], dataset.getNumberOfInstances());
	}
	
	@Override
	public double[] predictRawRegression(Dataset dataset, double[] subset)
	{
		return Util.fill(defaultPrediction, subset.length);
	}
	
	@Override
	public double predictRawRegression(Dataset dataset, int instanceIndex)
	{
		return defaultPrediction;
	}

	@Override
	public int predictAsLeafIndex(Dataset dataset, int instanceIndex)
	{
		return 0;
	}

	@Override // Overridden for optimisation
	public int[] predictAsLeafIndexes(Dataset dataset, double[] subset)
	{		
		if(subset != null)
			return Util.fill(0, subset.length);
		else
			return Util.fill(0, dataset.getNumberOfInstances());
	}
	
	@Override // Overridden for optimisation
	public double[][] constructLeafSubsets(Dataset dataset, double[] subset)
	{
		return new double[][] {subset};
	}
	
	@Override
	public String toString()
	{
		return "ZeroR: " + defaultPrediction;
	}
	
	@Override
	public int size()
	{
		return 1;
	}

}
