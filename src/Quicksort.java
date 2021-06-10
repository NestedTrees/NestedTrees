public class Quicksort
{
	public static int partition(double arr[], int low, int high)
    {
        double pivot = arr[high];
        int i = (low-1);
        for(int j=low; j<high; j++)
        {
            if(arr[j] < pivot)
            {
                i++;
                double temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        
        double temp = arr[i+1];
        arr[i+1] = arr[high];
        arr[high] = temp;
        return i+1;
    }

    public static void sort(double arr[], int low, int high)
    {
        if(low < high)
        {
            int pi = partition(arr, low, high);
            sort(arr, low, pi-1);
            sort(arr, pi+1, high);
        }
    }
    
    public static int partition2(double arr[][], int index, int low, int high)
    {
        double pivot = arr[high][index];
        int i = (low-1);
        for(int j=low; j<high; j++)
        {
            if(arr[j][index] < pivot)
            {
                i++;
                double[] temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        
        double temp[] = arr[i+1];
        arr[i+1] = arr[high];
        arr[high] = temp;
        return i+1;
    }
    
    public static int partition25(double arr[][], double[] subset, int index, int low, int high)
    {
        double pivot = arr[high][index];
        int i = (low-1);
        for(int j=low; j<high; j++)
        {
            if(arr[j][index] < pivot)
            {
                i++;
                double[] temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
                
                double tempb = subset[i];
                subset[i] = subset[j];
                subset[j] = tempb;
            }
        }
        
        double temp[] = arr[i+1];
        arr[i+1] = arr[high];
        arr[high] = temp;
        
        double tempb = subset[i+1];
        subset[i+1] = subset[high];
        subset[high] = tempb;
        
        return i+1;
    }
    
    public static int partition25(double arr[][], int[] indexes, int index, int low, int high)
    {
        double pivot = arr[high][index];
        int i = (low-1);
        for(int j=low; j<high; j++)
        {
            if(arr[j][index] < pivot)
            {
                i++;
                double[] temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
                
                int tempb = indexes[i];
                indexes[i] = indexes[j];
                indexes[j] = tempb;
            }
        }
        
        double temp[] = arr[i+1];
        arr[i+1] = arr[high];
        arr[high] = temp;
        
        int tempb = indexes[i+1];
        indexes[i+1] = indexes[high];
        indexes[high] = tempb;
        
        return i+1;
    }
    
    public static void sort2(double arr[][], int index, int low, int high)
    {
        if(low < high)
        {
            int pi = partition2(arr, index, low, high);
            sort2(arr, index, low, pi-1);
            sort2(arr, index, pi+1, high);
        }
    }
    
    public static void sort25(double arr[][], double[] subset, int index, int low, int high)
    {
        if(low < high)
        {
            int pi = partition25(arr, subset, index, low, high);
            sort25(arr, subset, index, low, pi-1);
            sort25(arr, subset, index, pi+1, high);
        }
    }
    
    public static void sort25(double arr[][], int[] indexes, int index, int low, int high)
    {
        if(low < high)
        {
            int pi = partition25(arr, indexes, index, low, high);
            sort25(arr, indexes, index, low, pi-1);
            sort25(arr, indexes, index, pi+1, high);
        }
    }
    
    public static double[] sort(double[] arr)
    {
    	int n = arr.length;
        Quicksort.sort(arr, 0, n-1);
        return arr;
    }
    
    public static double[][] sort2(double[][] arr, int index)
    {
    	int n = arr.length;
        Quicksort.sort2(arr, index, 0, n-1);
        return arr;
    }
    
    public static double[][] sort25(double[][] arr, double[] subset, int index)
    {
    	int n = arr.length;
    	try {
			Quicksort.sort25(arr, subset, index, 0, n-1);
			return arr;
    	}
    	catch(StackOverflowError e)
    	{
    		return sort25Bubble(arr, subset, index);
    	}
    }
    
    public static double[][] sort25(double[][] arr, int[] indexes, int index)
    {
    	int n = arr.length;
    	try {
			Quicksort.sort25(arr, indexes, index, 0, n-1);
			return arr;
    	}
    	catch(StackOverflowError e)
    	{
    		return sort25Bubble(arr, indexes, index);
    	}
    }
    
    public static double[][] sort25Bubble(double[][] arr, double[] subset, int index)
    {
    	for(int i = 0; i < arr.length; i++)
    	{
    		for(int j = 0; j < arr.length; j++)
    		{
    			if(arr[i][index] < arr[j][index])
    			{
    				double temp = subset[i];
    				subset[i] = subset[j];
    				subset[j] = temp;
    				
    				double[] temp2 = arr[i];
    				arr[i] = arr[j];
    				arr[j] = temp2;
    			}
    		}
    	}     
    	return arr;
    }
    
    public static double[][] sort25Bubble(double[][] arr, int[] indexes, int index)
    {
    	for(int i = 0; i < arr.length; i++)
    	{
    		for(int j = 0; j < arr.length; j++)
    		{
    			if(arr[i][index] < arr[j][index])
    			{
    				int temp = indexes[i];
    				indexes[i] = indexes[j];
    				indexes[j] = temp;
    				
    				double[] temp2 = arr[i];
    				arr[i] = arr[j];
    				arr[j] = temp2;
    			}
    		}
    	}     
    	return arr;
    }
}     
