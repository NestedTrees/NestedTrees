import java.util.ArrayList;

public class NestedDecisionTree extends PredictionModel {

	// Tree nodes
	private ArrayList<PredictionModel> nodes;
	
	// Leaf index conversion attributes
	private int[] treeToModelLeafIndexes;
	private int[] modelToTreeLeafIndexes;
	
	// Construction parameters
	public boolean pruneForPrediction;
	public int maxDepth = -1;
	public int maxDepthNested = -1;
	public boolean binaryConversion = false;
	public double minNodeSize = 0;
	
	@Override
	public void construct(Dataset dataset, double[] subset)
	{
		nodes = new ArrayList<PredictionModel>();
		constructNode(dataset, subset, -1, maxDepth);
		if(pruneForPrediction)
			pruneForPrediction();
		initialiseModelLeaves();
		leafNextNodeIndexes = Util.fill(-1, numberOfLeaves);
		parentNodeIndex = -1;
		initialiseLeafPredictions(dataset, subset);
		datasetUsed = dataset;
		subsetUsed = subset;
	}
	
	public void constructNode(Dataset dataset, double[] subset, int parentNodeIndex, int maxDepth)
	{
		if(!dataset.longitudinalAttributesInitialised())
			dataset.generateLongitudinalAttributes(Util.getElsaAttributes());
		boolean[][] longitudinalAttributeSubsets = dataset.getLongitudinalAttributes();
		
		double bestGini = ModelEvaluator.estimateGiniOfDataset(dataset, subset);
		
		PredictionModel bestModel;
		
		ZeroR zeror = new ZeroR();
		zeror.construct(dataset, subset);
		
		bestModel = zeror;
		
		if(maxDepth > 1 && Util.sum(subset) >= minNodeSize)
			for(boolean[] longitudinalAttributeSubset : longitudinalAttributeSubsets)
			{
				DecisionTree tree = new DecisionTree();
				tree.setAttributeFilter(longitudinalAttributeSubset);
				tree.maxDepth = maxDepthNested;
				tree.minNodeSize = minNodeSize;
				tree.construct(dataset, subset);
				if(binaryConversion)
				{
					tree.convertToBinaryTree();
				}
				double giniOfTree = ModelEvaluator.evaluatePredictionModelGini(tree, dataset, subset);
				if(giniOfTree < bestGini)
				{
					bestGini = giniOfTree;
					bestModel = tree;
				}
			}
		
		bestModel.setParentNodeIndex(parentNodeIndex);
		nodes.add(bestModel);
		
		// If the node has a single leaf - it is a leaf node, no further construction needed
		if(bestModel.getNumberOfLeaves() == 1)
			return;
		
		// If node has multiple leaves - break the current set into leaf subsets and construct a node for each
		double[][] leafSubsets = bestModel.constructLeafSubsets(dataset, subset);
		int currentNodeIndex = nodes.size();
		for(int leafIndex = 0; leafIndex < leafSubsets.length; leafIndex++)
		{
			int nextNodeIndex = nodes.size();
			constructNode(dataset, leafSubsets[leafIndex], currentNodeIndex, maxDepth - 1);
			// Attach current node to each child node when it is constructed
			bestModel.setLeafNextNodeIndex(leafIndex, nextNodeIndex);
		}
	}
	
	public void convertToRegression()
	{
		this.regression = true;
		for(PredictionModel model : nodes)
			model.convertToRegression();
	}
	
	@Override
	public double[] predictRawRegression(Dataset dataset, double[] subset)
	{
		if(!regression)
			convertToRegression();
		int[] leafIndexes = predictAsLeafIndexes(dataset, subset);
		double[] predictions = new double[subset.length];
		for(int instanceIndex = 0; instanceIndex < subset.length; instanceIndex++)
		{
			if(subset != null)
				if(subset[instanceIndex] == 0.0)
					continue;
			predictions[instanceIndex] = nodes.get(leafIndexes[instanceIndex]).predict(dataset, instanceIndex);
		}
		return predictions;
	}
	
	@Override
	public double predictRawRegression(Dataset dataset, int instanceIndex)
	{
		int leafIndex = predictAsLeafIndex(dataset, instanceIndex);
		return nodes.get(leafIndex).predictRawRegression(dataset, instanceIndex);
	}

	public void initialiseModelLeaves()
	{
		// Count the leaves
		numberOfLeaves = 0;
		for(int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++)
			if(nodes.get(nodeIndex).getNumberOfLeaves() == 1)
				numberOfLeaves++;
		
		// Initialise the index conversion between leaf indexes of the tree
		// and leaf indexes of the model
		treeToModelLeafIndexes = new int[nodes.size()];
		modelToTreeLeafIndexes = new int[numberOfLeaves];
		
		int leafIndex = 0;
		for(int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++)
			if(nodes.get(nodeIndex).getNumberOfLeaves() == 1)
			{
				treeToModelLeafIndexes[nodeIndex] = leafIndex;
				modelToTreeLeafIndexes[leafIndex] = nodeIndex;
				leafIndex++;
			}
			else
			{
				treeToModelLeafIndexes[nodeIndex] = -1;
			}
	}
	
	@Override
	public int predictAsLeafIndex(Dataset dataset, int instanceIndex)
	{
		// Start at root node
		int currentIndex = 0;
		
		// While leaf node not reached
		while(nodes.get(currentIndex).getNumberOfLeaves() != 1)
		{
			// Go to the next node
			currentIndex = nodes.get(currentIndex).predictAsNextNodeIndex(dataset, instanceIndex);
		}
		
		return treeToModelLeafIndexes[currentIndex];
	}

	@Override
	public String toString()
	{
		String output = "";
		
		output = output + "Nested Tree: ";
		
		for(int leafIndex = 0; leafIndex < numberOfLeaves; leafIndex++)
		{
			output = output + this.modelToTreeLeafIndexes[leafIndex] + " -> " + this.leafNextNodeIndexes[leafIndex];
			if(leafIndex < numberOfLeaves - 1)
				output = output + "; ";
		}
		output = output + "\n";
		for(int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++)
		{
			if(nodes.get(nodeIndex) == null)
				output = output + nodeIndex + ": NULL" + "\n";
			else
				if(nodes.get(nodeIndex).getClass() != new ZeroR().getClass())
					output = output + nodeIndex + ": " + nodes.get(nodeIndex).toString();
				else
					output = output + nodeIndex + ": " + nodes.get(nodeIndex).toString() + "\n";
		}
		
		return output;
	}
	
	public void pruneForPrediction()
	{
		if(this.regression)
			return;
		// Keep trying to prune the tree as long as previous pruning was successful
		boolean pruneSuccessful = true;
		while(pruneSuccessful)
		{
			pruneSuccessful = false;
			
			// Prune nodes that can be pruned
			for(int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++)
			{
				// Skip already pruned nodes
				if(nodes.get(nodeIndex) == null)
					continue;
				
				// Skip leaf nodes
				if(nodes.get(nodeIndex).getNumberOfLeaves() == 1)
					continue;
				
				// Check if node only has leaves as child nodes and all of them make the same prediction
				boolean leavesOnly = true;
				boolean predictionsSame = true;
				double defaultPrediction = nodes.get(nodes.get(nodeIndex).getLeafNextNodeIndex(0)).getDefaultPrediction();
				for(int leafIndex = 0; leafIndex < nodes.get(nodeIndex).numberOfLeaves; leafIndex++)
				{
					if(nodes.get(nodes.get(nodeIndex).getLeafNextNodeIndex(leafIndex)).numberOfLeaves != 1)
					{
						leavesOnly = false;
						break;
					}
					if(defaultPrediction != nodes.get(nodes.get(nodeIndex).getLeafNextNodeIndex(leafIndex)).getDefaultPrediction())
					{
						predictionsSame = false;
						break;
					}
				}
				if(!leavesOnly)
					continue;
				if(!predictionsSame)
					continue;
				
				// This part of the loop is only reached if the node being considered has all identical child nodes (leaves with the same predictions)
				
				// Destroy leaf nodes
				for(int leafIndex = 1; leafIndex < nodes.get(nodeIndex).numberOfLeaves; leafIndex++)
					nodes.set(nodes.get(nodeIndex).getLeafNextNodeIndex(leafIndex), null);
				
				// Create a new leaf node and replace the current one with it
				ZeroR newLeafNode = new ZeroR();
				newLeafNode.construct(nodes.get(nodeIndex).getDatasetUsed(), nodes.get(nodeIndex).getSubsetUsed());
				nodes.set(nodeIndex, newLeafNode);
				
				pruneSuccessful = true;
			}
		}
		cleanUpTree();
	}
	
	public void cleanUpTree()
	{
		ArrayList<PredictionModel> newNodes = new ArrayList<PredictionModel>();
		cleanUpTree(newNodes, 0);
		this.nodes = newNodes;
	}
	
	public void cleanUpTree(ArrayList<PredictionModel> nodes2, int index)
	{
		PredictionModel node = nodes.get(index);
		nodes2.add(node);
		
		if(node.numberOfLeaves == 1)
			return;
		
		for(int leafIndex = 0; leafIndex < node.numberOfLeaves; leafIndex++)
		{
			int nextIndex = node.getLeafNextNodeIndex(leafIndex);
			node.setLeafNextNodeIndex(leafIndex, nodes2.size());
			cleanUpTree(nodes2, nextIndex);
		}
	}
	
	@Override
	public int size()
	{
		return nodes.size();
	}
	
	public double innerNodesAverageSize()
	{
		int sum = 0;
		int count = 0;
		for(int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++)
		{
			PredictionModel node = nodes.get(nodeIndex);
			if(node instanceof DecisionTree)
			{
				sum += ((DecisionTree)node).size();
				count++;
			}
		}
		double averageSize = (0.0 + sum) / (0.0 + count);
		if(count == 0)
			averageSize = 0.0;
		return averageSize;
	}
}
