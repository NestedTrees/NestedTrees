
public class Util {

	public static void main(String[] args) {
		System.out.println(Util.averageOfDistribution(new double[] {125.0, 83.0}, new double[] {1.0, 0.0}));
		Util.printArray(Util.thresholds(new double[] {0.0, 1.0, 4.0}, 0.6));
	}
	
	public static void printWithExtraSpaces(String toPrint, int totalLength)
	{
		System.out.print(toPrint);
		
		for(int i = toPrint.length(); i < totalLength; i++)
			System.out.print(" ");
	}
	
	public static void printArray(double[][] values)
	{
		for(int i = 0; i < values.length; i++)
		{
			for(int j = 0; j < values[i].length; j++)
			{
				System.out.print(values[i][j] + " ");
			}
			System.out.println();
		}
	}
	
	public static void printArray(int[] values)
	{
		if(values == null)
		{
			System.out.println("null array");
			return;
		}
		for(int i = 0; i < values.length; i++)
		{
			System.out.print(values[i] + " ");
		}
		System.out.println();
	}
	
	public static void printArray(double[] values)
	{
		if(values == null)
		{
			System.out.println("null array");
			return;
		}
		for(int i = 0; i < values.length; i++)
		{
			System.out.print(values[i] + " ");
		}
		System.out.println();
	}
	
	public static void printArray(boolean[] values)
	{
		if(values == null)
		{
			System.out.println("null array");
			return;
		}
		for(int i = 0; i < values.length; i++)
		{
			System.out.print(values[i] + " ");
		}
		System.out.println();
	}
	
	public static void printArray(String[] values)
	{
		if(values == null)
		{
			System.out.println("null array");
			return;
		}
		for(int i = 0; i < values.length; i++)
		{
			System.out.print(values[i] + " ");
		}
		System.out.println();
	}
	
	public static double[] unique(double[] values)
	{
		if(values.length == 0)
			return new double[0];
		double[] sorted = Quicksort.sort(Util.copy(values));
		int countUnique = 1;
		double current = sorted[0];
		for(int i = 0; i < sorted.length; i++)
			if(current != sorted[i])
			{
				current = sorted[i];
				countUnique++;
			}
		double[] unique = new double[countUnique];
		unique[0] = sorted[0];
		current = sorted[0];
		int index = 1;
		for(int i = 0; i < sorted.length; i++)
			if(current != sorted[i])
			{
				current = sorted[i];
				unique[index] = sorted[i];
				index++;
			}
		return unique;
	}
	
	public static int maxIndex(double[] values)
	{
		int index = 0;
		double max = values[0];
		for(int i = 0; i < values.length; i++)
			if(values[i] > max)
			{
				max = values[i];
				index = i;
			}
		return index;
	}
	
	public static double[] fill(double value, int length)
	{
		double[] out = new double[length];
		for(int i = 0; i < length; i++)
			out[i] = value;
		return out;
	}
	
	public static int[] fill(int value, int length)
	{
		int[] out = new int[length];
		for(int i = 0; i < length; i++)
			out[i] = value;
		return out;
	}

	public static int indexOf(double value, double[] values)
	{
		for(int i = 0; i < values.length; i++)
			if(values[i] == value)
				return i;
		return -1;
	}
	
	public static double sum(double[] values)
	{
		double sum = 0.0;
		for(double value : values)
			sum += value;
		return sum;
	}
	
	public static double sum(double[][] values)
	{
		double sum = 0.0;
		for(double[] row : values)
			sum += sum(row);
		return sum;
	}
	
	public static boolean[] invert(boolean[] bs)
	{
		boolean[] bsi = new boolean[bs.length];
		for(int i = 0; i < bs.length; i++)
			bsi[i] = !bs[i];
		return bsi;
	}
	
	public static double[] invert(double[] bs)
	{
		double[] bsi = new double[bs.length];
		for(int i = 0; i < bs.length; i++)
			bsi[i] = 1.0 - bs[i];
		return bsi;
	}
	
	public static double[][] sum(double[][] a, double[][] b)
	{
		if(a == null) return b;
		if(b == null) return a;
		double[][] c = new double[a.length][];
		for(int i = 0; i < a.length; i++)
		{
			c[i] = new double[a[i].length];
			for(int j = 0; j < a[i].length; j++)
			{
				c[i][j] = a[i][j] + b[i][j];
			}
		}
		return c;
	}
	
	public static double[][] divide(double[][] a, double b)
	{
		double[][] c = new double[a.length][];
		for(int i = 0; i < a.length; i++)
		{
			c[i] = new double[a[i].length];
			for(int j = 0; j < a[i].length; j++)
			{
				c[i][j] = a[i][j] / b;
			}
		}
		return c;
	}
	
	public static void sleep(long millis)
	{
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static double[] subtract(double[] a, double[] b)
	{
		double[] c = new double[a.length];
		for(int i = 0; i < a.length; i++)
			c[i] = a[i] - b[i];
		return c;
	}
	
	public static String[] getElsaAttributes()
	{
		return new String[] {"sex", "indager_w8", "apoe", "bmiobe", "cfib", "chestin", "chol", "clotb", "dheas", "diaval", "eyesurg", "fglu", "hastro", "hasurg", "hdl", "hgb", "hipval", "hscrp", "htfev", "htfvc", "htpf", "htval", "ldl", "mapval", "mch", "mmcrre", "mmgsdavg", "mmgsnavg", "mmlore", "mmlsre", "mmrroc", "mmssre", "mmstre", "pulval", "rtin", "sysval", "trig", "vitd", "wbc", "whval", "wstval", "wtval"};
	}
	
	public static double[] count(double[] values, double[] valuesToCount, double[] weights)
	{
		double[] counts = new double[valuesToCount.length];
		for(int valueIndex = 0; valueIndex < valuesToCount.length; valueIndex++)
			for(int i = 0; i < values.length; i++)
				if(values[i] == valuesToCount[valueIndex])
				{
					if(weights == null)
						counts[valueIndex] += 1.0;
					else
						counts[valueIndex] += weights[i];
				}
		return counts;
	}
	
	public static double[][] transpose(double[][] values)
	{
		double[][] transposed = new double[values[0].length][];
		for(int i = 0; i < values[0].length; i++)
		{
			transposed[i] = new double[values.length];
			for(int j = 0; j < values.length; j++)
				transposed[i][j] = values[j][i];
		}
		return transposed;
	}
	
	public static double averageOfDistribution(double[] count, double[] values)
	{
		double out = 0.0;
		double total = 0.0;
		for(int i = 0; i < count.length; i++)
		{
			out += count[i] * values[i];
			total += count[i];
		}
		if(total == 0.0)
			return 0.0;
		return out/total;
	}
	
	/*
	 * If values are [0, 1, 4] and threshold is 0.6, it turns them into [0, 0.6, 2.8]
	 * This method assumes values are sorted in ascending order and the first value is 0.0
	 */
	public static double[] thresholds(double[] values, double threshold)
	{
		if(values[0] != 0.0)
			return null;
		double[] thresholds = new double[values.length];
		for(int i = 0; i < values.length; i++)
		{
			if(i == 0)
				thresholds[i] = 0.0;
			else
				thresholds[i] = values[i-1] + threshold * (values[i] - values[i-1]);
		}
		return thresholds;
	}
	
	/*
	 * If values are [0, 0.6, 2.8] and value is 0.5, it returns 0
	 * If values are [0, 0.6, 2.8] and value is 0.7, it returns 1
	 * If values are [0, 0.6, 2.8] and value is 2.7, it returns 1
	 * If values are [0, 0.6, 2.8] and value is 2.9, it returns 2
	 * This method assumes values are sorted in ascending order and the first value is 0.0, and the value is >= 0.0
	 */
	public static int indexOfTheHighestBelowOrEqual(double[] values, double value)
	{
		for(int i = values.length - 1; i >= 0; i--)
		{
			if(values[i] <= value)
				return i;
		}
		return 0;
	}
	
	public static int countMatches(double[] a, double[] b)
	{
		int matches = 0;
		for(int i = 0; i < a.length; i++)
			if(a[i] == b[i])
				matches++;
		return matches;
	}
	
	public static double countMatchesWeighted(double[] a, double[] b, double[] weights)
	{
		if(weights == null)
			return 0.0 + countMatches(a,b);
		double matches = 0;
		for(int i = 0; i < a.length; i++)
			if(a[i] == b[i])
				matches += weights[i];
		return matches;
	}
	
	public static double[] copy(double[] original)
	{
		double[] copy = new double[original.length];
		for(int i = 0; i < original.length; i++)
			copy[i] = original[i];
		return copy;
	}
	
	public static double[][] copy(double[][] original)
	{
		double[][] copy = new double[original.length][];
		for(int i = 0; i < original.length; i++)
		{
			copy[i] = new double[original[i].length];
			for(int j = 0; j < original[i].length; j++)
				copy[i][j] = original[i][j];
		}
		return copy;
	}
	
}
