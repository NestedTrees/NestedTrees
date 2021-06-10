import java.util.Arrays;

public class DataPreparator {
	
	public static double[] oversampleClasses(double[] subset, double[] classValues, double[] uniqueClassValues)
	{
		if(subset == null)
			subset = Util.fill(1.0, classValues.length);
		double[] classSums = new double[uniqueClassValues.length];
		for(int classValueIndex = 0; classValueIndex < classValues.length; classValueIndex++)
			classSums[Util.indexOf(classValues[classValueIndex], uniqueClassValues)] += subset[classValueIndex];
		
		double[] multipliers = new double[classSums.length];
		double maxClassCount = Arrays.stream(classSums).max().getAsDouble();
		
		for(int classIndex = 0; classIndex < uniqueClassValues.length; classIndex++)
		{
			multipliers[classIndex] = maxClassCount / classSums[classIndex];
		}
		
		double[] newSubset = new double[subset.length];
		
		for(int classValueIndex = 0; classValueIndex < classValues.length; classValueIndex++)
		{
			newSubset[classValueIndex] = subset[classValueIndex] * (multipliers[Util.indexOf(classValues[classValueIndex], uniqueClassValues)]);
		}
		
		return newSubset;
	}

}
