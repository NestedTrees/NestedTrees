import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Dataset {
	
	// Data variables
	private double[][] values;
	private String[] attributeNames;
	
	// Data format variables
	private String idAttributeName;
	private int idAttributeIndex;
	private String classAttributeName;
	private int classAttributeIndex;
	private String missingValueString;
	private double missingValue = Double.MAX_VALUE;
	
	// Optimisation variables
	private boolean preSorted;
	private double[][][] preSortedValues;
	private int[][] originalToSortedIndexes;
	private int[][] sortedToOriginalIndexes;
	private double[] uniqueClassValues;
	
	// Longitudinal variables
	private boolean[][] longitudinalAttributes;
	private boolean longitudinalAttributesInitialised;
	
	public Dataset()
	{
		
	}
	
	public void setValues(double[][] values)
	{
		this.values = values;
	}
	
	public void setAttributeNames(String[] attributeNames)
	{
		this.attributeNames = attributeNames;
	}
	
	public double getValue(int instanceIndex, int attributeIndex)
	{
		return values[instanceIndex][attributeIndex];
	}
	
	public String getAttributeName(int attributeIndex)
	{
		return attributeNames[attributeIndex];
	}
	
	public void setIdAttribute(String newIdAttributeName)
	{
		boolean found = false;
		for(int attributeIndex = 0; attributeIndex < attributeNames.length; attributeIndex++)
			if(attributeNames[attributeIndex].equals(newIdAttributeName))
			{
				idAttributeIndex = attributeIndex;
				idAttributeName = newIdAttributeName;
				found = true;
				break;
			}
		if(!found)
			System.out.println("WARNING: Id attribute " + newIdAttributeName + " was not present in the dataset");
	}
	
	public int getIdAttributeIndex()
	{
		return idAttributeIndex;
	}
	
	public String getIdAttributeName()
	{
		return idAttributeName;
	}
	
	public void setClassAttribute(String newClassAttributeName)
	{
		boolean found = false;
		for(int attributeIndex = 0; attributeIndex < attributeNames.length; attributeIndex++)
			if(attributeNames[attributeIndex].equals(newClassAttributeName))
			{
				classAttributeIndex = attributeIndex;
				classAttributeName = newClassAttributeName;
				found = true;
				break;
			}
		if(!found)
			System.out.println("WARNING: Class attribute " + newClassAttributeName + " was not present in the dataset");
	}
	
	public int getClassAttributeIndex()
	{
		return classAttributeIndex;
	}
	
	public String getClassAttributeName()
	{
		return classAttributeName;
	}
	
	public int getNumberOfAttributes()
	{
		return attributeNames.length;
	}
	
	public int getNumberOfInstances()
	{
		return values.length;
	}
	
	public void readFromCSV(String path)
	{
		try
		{
			// Initialise buffered reader
			BufferedReader reader = new BufferedReader(new FileReader(path));
			
			// Read the attribute names
			String line = reader.readLine();
			String[] atrributeNamesSplit = line.split(",");
			attributeNames = new String[atrributeNamesSplit.length];
			for(int attributeIndex = 0; attributeIndex < attributeNames.length; attributeIndex++)
				attributeNames[attributeIndex] = atrributeNamesSplit[attributeIndex].trim();
			
			// Read raw values
			ArrayList<String> valuesStrings = new ArrayList<String>();
			
			line = reader.readLine();
			while(line != null)
			{
				valuesStrings.add(line);
				line = reader.readLine();
			}
			
			// Store the values as doubles
			values = new double[valuesStrings.size()][];
			for(int instanceIndex = 0; instanceIndex < values.length; instanceIndex++)
			{
				String instanceString = valuesStrings.get(instanceIndex);
				String[] instanceStringSplit = instanceString.split(",");
				values[instanceIndex] = new double[attributeNames.length]; 
				for(int attributeIndex = 0; attributeIndex < attributeNames.length; attributeIndex++)
				{
					instanceStringSplit[attributeIndex] = instanceStringSplit[attributeIndex].trim();
					if(instanceStringSplit[attributeIndex].equals(missingValueString))
						values[instanceIndex][attributeIndex] = missingValue;
					else
						values[instanceIndex][attributeIndex] = Double.parseDouble(instanceStringSplit[attributeIndex]);
				}
			}
			
			reader.close();
		}
		catch(Exception e)
		{
			System.out.println("Error reading dataset from CSV file " + path);
			e.printStackTrace();
		}
	}
	
	public void writeToCSV(String path)
	{
		FileWriter myWriter;
		try {
			myWriter = new FileWriter(path);
			for(int attributeIndex = 0; attributeIndex < this.attributeNames.length; attributeIndex++)
			{
				if(attributeIndex > 0)
					myWriter.write(", ");
				myWriter.write(attributeNames[attributeIndex]);
				
			}
			for(int instanceIndex = 0; instanceIndex < this.values.length; instanceIndex++)
			{
				myWriter.write("\n");
				for(int attributeIndex = 0; attributeIndex < this.attributeNames.length; attributeIndex++)
				{
					if(attributeIndex > 0)
						myWriter.write(", ");
					myWriter.write("" + values[instanceIndex][attributeIndex]);
					
				}
			}
			myWriter.close();
			
		} catch (IOException e) {
			System.out.println("Failed to write to " + path);
			e.printStackTrace();
		}
	}
	
	public void setMissingValueString(String newMissingValueString)
	{
		missingValueString = newMissingValueString;
	}
	
	public void setMissingValue(double newMissingValue)
	{
		missingValue = newMissingValue;
	}
	
	public double getMissingValue()
	{
		return missingValue;
	}
	
	public void preSort()
	{
		// Initialise index conversion sets
		preSortedValues = new double[attributeNames.length][][];
		originalToSortedIndexes = new int[attributeNames.length][];
		sortedToOriginalIndexes = new int[attributeNames.length][];
		
		for(int attributeIndex = 0; attributeIndex < attributeNames.length; attributeIndex++)
		{
			sortedToOriginalIndexes[attributeIndex] = new int[values.length];
			for(int instanceIndex = 0; instanceIndex < values.length; instanceIndex++)
				sortedToOriginalIndexes[attributeIndex][instanceIndex] = instanceIndex;
			this.preSortedValues[attributeIndex] = Quicksort.sort25(values.clone(), sortedToOriginalIndexes[attributeIndex], attributeIndex);
			originalToSortedIndexes[attributeIndex] = new int[values.length];
			for(int instanceIndex = 0; instanceIndex < values.length; instanceIndex++)
				originalToSortedIndexes[attributeIndex][sortedToOriginalIndexes[attributeIndex][instanceIndex]] = instanceIndex;
		}
		
		this.preSorted = true;
	}
	
	public boolean preSorted()
	{
		return preSorted;
	}
	
	public void printDataset()
	{
		printDataset(null);
	}
	
	public void printDataset(double[] subset)
	{
		printDataset(values, subset);
	}
	
	public void printSortedDataset(int attributeIndex)
	{
		printSortedDataset(attributeIndex, null);
	}
	
	public void printSortedDataset(int attributeIndex, double[] subset)
	{
		if(!preSorted)
			preSort();
		
		double[][] valuesSortedByAttribute = new double[values.length][];
		for(int instanceIndex = 0; instanceIndex < values.length; instanceIndex++)
		{
			valuesSortedByAttribute[originalToSortedIndexes[attributeIndex][instanceIndex]] = values[instanceIndex];
		}
		printDataset(valuesSortedByAttribute, subset);
	}
	
	private void printDataset(double[][] values, double[] subset)
	{
		int[] lengths = new int[attributeNames.length];
		
		for(int attributeIndex = 0; attributeIndex < attributeNames.length; attributeIndex++)
		{
			lengths[attributeIndex] = attributeNames[attributeIndex].length();
			
			for(int instanceIndex = 0; instanceIndex < values.length; instanceIndex++)
			{
				if(subset != null)
					if(subset[instanceIndex] <= 0.0)
						continue;
				int attributeValueLength = ("" + values[instanceIndex][attributeIndex]).length();
				if(values[instanceIndex][attributeIndex] == missingValue)
					attributeValueLength = 1;
				if(attributeValueLength > lengths[attributeIndex])
					lengths[attributeIndex] = attributeValueLength;
			}
		}
		
		for(int attributeIndex = 0; attributeIndex < attributeNames.length; attributeIndex++)
		{
			Util.printWithExtraSpaces(attributeNames[attributeIndex], lengths[attributeIndex]);
			System.out.print(" ");
		}
		System.out.println();
		
		for(int instanceIndex = 0; instanceIndex < values.length; instanceIndex++)
		{
			if(subset != null)
				if(subset[instanceIndex] <= 0.0)
					continue;
			
			for(int attributeIndex = 0; attributeIndex < values[instanceIndex].length; attributeIndex++)
			{
				if(values[instanceIndex][attributeIndex] == missingValue)
					Util.printWithExtraSpaces("?", lengths[attributeIndex]);
				else
					Util.printWithExtraSpaces("" + values[instanceIndex][attributeIndex], lengths[attributeIndex]);
				System.out.print(" ");
			}
			if(subset != null)
				if(subset[instanceIndex] < 1.0)
					System.out.print(" (instance weight :" + subset[instanceIndex] + ")");
			System.out.println();
		}
	}	
	
	public int originalToSortedIndex(int originalInstanceIndex, int attributeIndex)
	{
		return this.originalToSortedIndexes[attributeIndex][originalInstanceIndex];
	}
	
	public int sortedToOriginalIndex(int sortedInstanceIndex, int attributeIndex)
	{
		return this.sortedToOriginalIndexes[attributeIndex][sortedInstanceIndex];
	}
	
	public double[] convertSubsetOriginalToSorted(double[] subset, int attributeIndex)
	{
		if(subset == null)
			return null;
		double[] sortedSubset = new double[subset.length];
		for(int instanceIndex = 0; instanceIndex < subset.length; instanceIndex++)
		{
			sortedSubset[originalToSortedIndex(instanceIndex, attributeIndex)] = subset[instanceIndex];
		}
		return sortedSubset;
	}
	
	public double[] convertSubsetSortedToOriginal(double[] sortedSubset, int attributeIndex)
	{
		if(sortedSubset == null)
			return null;
		double[] subset = new double[sortedSubset.length];
		for(int instanceIndex = 0; instanceIndex < sortedSubset.length; instanceIndex++)
		{
			subset[sortedToOriginalIndex(instanceIndex, attributeIndex)] = sortedSubset[instanceIndex];
		}
		return subset;
	}
	
	public double[] getClassValues()
	{
		return getAttributeValues(classAttributeIndex);
	}
	
	public double[] getAttributeValues(int attributeIndex)
	{
		double[] attributeValues = new double[values.length];
		for(int instanceIndex = 0; instanceIndex < values.length; instanceIndex++)
		{
			attributeValues[instanceIndex] = values[instanceIndex][attributeIndex];
		}
		return attributeValues;
	}
	
	public double[] getClassValuesFromSorted(int attributeSortedIndex)
	{
		return getAttributeValuesFromSorted(classAttributeIndex, attributeSortedIndex);
	}
	
	public double[] getAttributeValuesFromSorted(int attributeIndex, int attributeSortedIndex)
	{
		double[] attributeValues = new double[values.length];
		for(int instanceIndex = 0; instanceIndex < values.length; instanceIndex++)
		{
			attributeValues[originalToSortedIndexes[attributeSortedIndex][instanceIndex]] = values[instanceIndex][attributeIndex];
		}
		return attributeValues;
	}
	
	public void initialiseUniqueClassValues()
	{
		uniqueClassValues = Quicksort.sort(Util.unique(getClassValues()));
	}
	
	public double[] getUniqueClassValues()
	{
		if(uniqueClassValues == null)
			initialiseUniqueClassValues();
		return uniqueClassValues;
	}
	
	public boolean longitudinalAttributesInitialised()
	{
		return longitudinalAttributesInitialised;
	}
	
	public void generateLongitudinalAttributes(String[] longAttributeNames)
	{
		if(this.longitudinalAttributesInitialised)
			return;
		longitudinalAttributes = new boolean[longAttributeNames.length][];
		
		for(int longitudinalAttributeIndex = 0; longitudinalAttributeIndex < longAttributeNames.length; longitudinalAttributeIndex++)
		{
			longitudinalAttributes[longitudinalAttributeIndex] = new boolean[this.getNumberOfAttributes()];
			for(int attributeIndex = 0; attributeIndex < this.getNumberOfAttributes(); attributeIndex++)
			{
				if(attributeNames[attributeIndex].indexOf(longAttributeNames[longitudinalAttributeIndex]) != -1)
					longitudinalAttributes[longitudinalAttributeIndex][attributeIndex] = true;
			}
		}
		longitudinalAttributesInitialised = true;
	}
	
	public boolean[][] getLongitudinalAttributes()
	{
		return this.longitudinalAttributes;
	}
}
