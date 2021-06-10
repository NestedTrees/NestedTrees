
public class DecisionRule extends PredictionModel {
	
	// Rule attributes
	private int attributeIndex;
	private double threshold;
	private boolean defaultDirection;
	
	// Construction attributes
	public double minNodeSize = 2.0;

	@Override
	public void construct(Dataset dataset, double[] subset)
	{
		// Record best rule found so far
		double bestGini = 1.0;
		int bestAttributeIndex = -1;
		double bestThreshold = 0.0;
		boolean bestDefaultDirection = false;
		
		double[] uniqueClassValues = dataset.getUniqueClassValues();
		
		// Search for best attribute to use for split
		for(int attributeIndex = 0; attributeIndex < dataset.getNumberOfAttributes(); attributeIndex++)
		{
			// Skip invalid attributes
			if(dataset.getClassAttributeIndex() == attributeIndex)
				continue;
			if(dataset.getIdAttributeIndex() == attributeIndex)
				continue;
			if(this.attributeFilter != null)
				if(!this.attributeFilter[attributeIndex])
					continue;
			
			// Initialise the attribute values, the class values and the subset used in search
			if(!dataset.preSorted())
				dataset.preSort();
			double[] sortedAttributeValues = dataset.getAttributeValuesFromSorted(attributeIndex, attributeIndex);
			double[] sortedClassValues = dataset.getClassValuesFromSorted(attributeIndex);
			double[] sortedSubset = dataset.convertSubsetOriginalToSorted(subset, attributeIndex);
			
			// Find all valid points where split can be made
			boolean[] thresholdsToConsider = new boolean[dataset.getNumberOfInstances()];
			
			boolean topFoundFlag = false;
			int highestValidSplitIndex = -1;
			double lastUsedValue = 0.0;
			
			// Also count total number of class values present in the subset
			// Only those instances where the attribute has a non-missing value are used
			double[] totalClassCounts = new double[uniqueClassValues.length];
			
			// Search through the potential split points and record the valid ones
			for(int instanceIndex = dataset.getNumberOfInstances()-1; instanceIndex >= 0; instanceIndex--)
			{
				// Ignore instances not in subset
				if(sortedSubset != null)
					if(sortedSubset[instanceIndex] == 0.0)
						continue;
				
				// Ignore missing values
				if(sortedAttributeValues[instanceIndex] == dataset.getMissingValue())
					continue;
				
				// Add the instance to class counts
				if(sortedSubset != null)
				{
					if(sortedSubset[instanceIndex] != 0.0)
						totalClassCounts[Util.indexOf(sortedClassValues[instanceIndex], uniqueClassValues)] += sortedSubset[instanceIndex];
				}
				else
				{
					totalClassCounts[Util.indexOf(sortedClassValues[instanceIndex], uniqueClassValues)] += 1.0;
				}
				
				// Start recording values only after the first instance in subset is found
				// This is to avoid using top value for split
				if(!topFoundFlag)
				{
					topFoundFlag = true;
					lastUsedValue = sortedAttributeValues[instanceIndex];
				}
				else
				{
					// Record a valid split point each time a new attribute value is reached
					if(lastUsedValue != sortedAttributeValues[instanceIndex])
					{
						thresholdsToConsider[instanceIndex] = true;
						lastUsedValue = sortedAttributeValues[instanceIndex];
						if(highestValidSplitIndex == -1)
							highestValidSplitIndex = instanceIndex;
					}
				}
			}
			
			// Pre-calculate total number of instances used in this split consideration
			double sum = Util.sum(totalClassCounts);
			double sumUnder = 0.0;
			double sumOver = sum;
			
			double[] classCountsUnder = new double[totalClassCounts.length];
			double[] classCountsOver = totalClassCounts.clone();
			
			// Iterate through the valid splits and update the best split if a better one is found
			for(int instanceIndex = 0; instanceIndex <= highestValidSplitIndex; instanceIndex++)
			{
				// Iteratively update the under and over class counts where appropriate
				int indexOfClassValue = Util.indexOf(sortedClassValues[instanceIndex], uniqueClassValues);
				if(sortedSubset != null)
				{
					if(sortedSubset[instanceIndex] > 0.0)
					{
						classCountsOver[indexOfClassValue] -= sortedSubset[instanceIndex];
						classCountsUnder[indexOfClassValue] += sortedSubset[instanceIndex];
						sumOver -= sortedSubset[instanceIndex];
						sumUnder += sortedSubset[instanceIndex];
					}
				}
				else
				{
					classCountsOver[indexOfClassValue] -= 1.0;
					classCountsUnder[indexOfClassValue] += 1.0;
					sumOver -= 1.0;
					sumUnder += 1.0;
				}
				
				// If a valid split is found
				if(thresholdsToConsider[instanceIndex] && sumUnder >= minNodeSize && sumOver >= minNodeSize)
				{
					double giniUnder = ModelEvaluator.calculateGini(sumUnder, classCountsUnder);
					double giniOver = ModelEvaluator.calculateGini(sumOver, classCountsOver);					
					
					double gini = giniUnder * (sumUnder / sum) + giniOver * (sumOver / sum);
					
					// If gini is higher than previous best
					if(gini < bestGini)
					{
						// Update the previous best split
						bestGini = gini;
						bestAttributeIndex = attributeIndex;
						bestThreshold = sortedAttributeValues[instanceIndex];
						bestDefaultDirection = sumUnder < sumOver;
					}
				}
			}
		}
		
		// Initialise the DecisionRule attributes
		attributeIndex = bestAttributeIndex;
		threshold = bestThreshold;
		defaultDirection = bestDefaultDirection;
		
		if(attributeIndex == -1)
		{
			constructionFailed = true;
			return;
		}
		
		// Initialise the PredictionModel attributes
		numberOfLeaves = 2;
		leafNextNodeIndexes = Util.fill(-1, numberOfLeaves);
		parentNodeIndex = -1;
		
		this.initialiseLeafPredictions(dataset, subset);
		
		datasetUsed = dataset;
		subsetUsed = subset;
		this.constructed = true;
	}
	
	@Override
	public void convertToRegression()
	{
		this.regression = true;
	}
	
	@Override
	public double[] predictRawRegression(Dataset dataset, double[] subset)
	{
		return predict(dataset, subset);
	}
	
	@Override
	public double predictRawRegression(Dataset dataset, int instanceIndex)
	{
		return predict(dataset, instanceIndex);
	}

	@Override
	public int predictAsLeafIndex(Dataset dataset, int instanceIndex)
	{
		if(attributeIndex == -1)
			System.out.println("Error: attributeIndex is -1");
		double attributeValue = dataset.getValue(instanceIndex, attributeIndex);
		
		if(attributeValue == dataset.getMissingValue())
			if(defaultDirection)
				return 1;
			else
				return 0;
		
		if(attributeValue <= threshold)
			return 0;
		else
			return 1;
	}
	
	@Override
	public double[][] constructLeafSubsets(Dataset dataset, double[] subset)
	{
		double[][] leafSubsets = new double[numberOfLeaves][];
		
		for(int leafIndex = 0; leafIndex < numberOfLeaves; leafIndex++)
			leafSubsets[leafIndex] = new double[dataset.getNumberOfInstances()];
		
		for(int instanceIndex = 0; instanceIndex < dataset.getNumberOfInstances(); instanceIndex++)
		{
			if(subset != null)
				if(subset[instanceIndex] == 0.0)
					continue;
			
			if(dataset.getValue(instanceIndex, attributeIndex) == dataset.getMissingValue())
			{
				if(subset != null)
				{
					double splitAmmount = subset[instanceIndex] / (0.0 + numberOfLeaves);
					for(int leafIndex = 0; leafIndex < numberOfLeaves; leafIndex++)
						leafSubsets[leafIndex][instanceIndex] = splitAmmount;
				}
				else
				{
					double splitAmmount = 1.0 / (0.0 + numberOfLeaves);
					for(int leafIndex = 0; leafIndex < numberOfLeaves; leafIndex++)
						leafSubsets[leafIndex][instanceIndex] = splitAmmount;
				}
			}
			else
			{
				if(subset != null)
				{
					leafSubsets[predictAsLeafIndex(dataset, instanceIndex)][instanceIndex] = subset[instanceIndex];
				}
				else
				{
					leafSubsets[predictAsLeafIndex(dataset, instanceIndex)][instanceIndex] = 1.0;
				}
			}
		}
		return leafSubsets;
	}
	
	@Override
	public String toString()
	{
		return "RULE: " + datasetUsed.getAttributeName(attributeIndex) + " <= " + threshold + " TRUE -> " + leafNextNodeIndexes[0] + " FALSE -> " + leafNextNodeIndexes[1];// + " PARENT -> " + parentNodeIndex;
	}

	@Override
	public int size() {
		return 1;
	}
}
