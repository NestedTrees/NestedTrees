import java.util.ArrayList;

public class DecisionTree extends PredictionModel {
	
	// Tree nodes
	private ArrayList<PredictionModel> nodes;
	
	// Leaf index conversion attributes
	private int[] treeToModelLeafIndexes;
	private int[] modelToTreeLeafIndexes;
	
	// Construction parameters
	public boolean pruneForPrediction;
	public int maxDepth = -1;
	public double minNodeSize = 2.0;
	
	// Binary Conversion attributes
	public boolean binary;
	public int binarySelectedNodeIndex;
	
	public void convertToBinaryTree()
	{		
		int bestNodeIndex = 0;
		double bestGini = 1.0;
		
		for(int nodeIndex = 0; nodeIndex < this.nodes.size(); nodeIndex++)
		{
			double[][] subsets = generateNodeBinarySubsets(nodeIndex);
			double[] nodeSubset = subsets[0];
			double[] nodeComplimentarySubset = subsets[1];
			double nodeSubsetInstanceCount = Util.sum(nodeSubset);
			double nodeComplimentarySubsetInstanceCount = Util.sum(nodeComplimentarySubset);
			double totalInstanceCount = nodeSubsetInstanceCount + nodeComplimentarySubsetInstanceCount;
			
			double nodeSubsetGini = ModelEvaluator.estimateGiniOfDataset(datasetUsed, nodeSubset);
			double nodeComplimentarySubsetGini = ModelEvaluator.estimateGiniOfDataset(datasetUsed, nodeComplimentarySubset);
			
			double combinedGini = 
					(nodeSubsetGini * (nodeSubsetInstanceCount / totalInstanceCount)) + 
					(nodeComplimentarySubsetGini * (nodeComplimentarySubsetInstanceCount / totalInstanceCount));
			
			if(combinedGini < bestGini)
			{
				bestGini = combinedGini;
				bestNodeIndex = nodeIndex;
			}	
		}
		
		this.numberOfLeaves = 2;
		this.binarySelectedNodeIndex = bestNodeIndex;
		
		// Change treeToModelLeafIndexes
		modelToTreeLeafIndexes = new int[2];
		
		for(int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++)
		{
			if(nodeIndex == bestNodeIndex)
				treeToModelLeafIndexes[nodeIndex] = 0;
			else
				treeToModelLeafIndexes[nodeIndex] = 1;
		}		
		
		// Generate predictions for both subsets and store them
		leafNextNodeIndexes = new int[] {-1, -1};
		binaryCleanup();
		this.binary = true;
	}
	
	public double[][] generateNodeBinarySubsets(int nodeIndex)
	{
		double[] nodeSubset = nodes.get(nodeIndex).getSubsetUsed();
		if(nodeSubset == null)
			nodeSubset = Util.fill(1.0, datasetUsed.getNumberOfInstances());
		double[] modelSubset = this.subsetUsed;
		if(modelSubset == null)
			modelSubset = Util.fill(1.0, datasetUsed.getNumberOfInstances());
		double[] nodeComplimentarySubset = Util.subtract(modelSubset, nodeSubset);
		return new double[][] {nodeSubset, nodeComplimentarySubset};
	}
	
	@Override
	public void construct(Dataset dataset, double[] subset)
	{
		datasetUsed = dataset;
		subsetUsed = subset;
		nodes = new ArrayList<PredictionModel>();
		constructNode(dataset, subset, -1, maxDepth);
		if(pruneForPrediction)
			pruneForPrediction();
		initialiseModelLeaves();
		leafNextNodeIndexes = Util.fill(-1, numberOfLeaves);
		parentNodeIndex = -1;
		initialiseLeafPredictions(dataset, subset);
	}
	
	public void constructNode(Dataset dataset, double[] subset, int parentNodeIndex, int maxDepth)
	{
		// Create a new tree node
		PredictionModel newNode;
		
		// Decide what type of node should be used
		if(maxDepth == 1)
			newNode = new ZeroR();
		else
		{
			newNode = new DecisionRule();
			((DecisionRule)newNode).minNodeSize = minNodeSize;
		}
		if(subset != null)
			if(Util.sum(subset) < minNodeSize)
				newNode = new ZeroR();
		
		newNode.attributeFilter = this.attributeFilter;
		
		// Construct the node, attach it to parent node, add it to the tree
		newNode.construct(dataset, subset);
		
		if(newNode.constructionFailed)
		{
			newNode = new ZeroR();
			newNode.construct(dataset, subset);
		}
		
		newNode.setParentNodeIndex(parentNodeIndex);
		nodes.add(newNode);
		
		// If the node has a single leaf - it is a leaf node, no further construction needed
		if(newNode.getNumberOfLeaves() == 1)
			return;
		
		// If node has multiple leaves - break the current set into leaf subsets and construct a node for each
		double[][] leafSubsets = newNode.constructLeafSubsets(dataset, subset);
		int currentNodeIndex = nodes.size()-1;
		for(int leafIndex = 0; leafIndex < leafSubsets.length; leafIndex++)
		{
			int nextNodeIndex = nodes.size();
			constructNode(dataset, leafSubsets[leafIndex], currentNodeIndex, maxDepth - 1);
			// Attach current node to each child node when it is constructed
			newNode.setLeafNextNodeIndex(leafIndex, nextNodeIndex);
		}
	}
	
	@Override
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
			predictions[instanceIndex] = nodes.get(leafIndexes[instanceIndex]).predictRawRegression(dataset, instanceIndex);
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
			
			if(this.binary)
			{
				if(this.binarySelectedNodeIndex == currentIndex)
					return 0;
				
				if(currentIndex == -1)
					return 1;
			}
		}
		
		if(this.binary)
			return 1;
		
		return treeToModelLeafIndexes[currentIndex];
	}
	
	@Override
	public String toString()
	{
		String output = "";
		
		if(binary)
			output = output + "Binary ";
		output = output + "Tree: ";
		
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
		cleanUpTree(newNodes, 0, -1);
		this.nodes = newNodes;
	}
	
	public void cleanUpTree(ArrayList<PredictionModel> nodes2, int currentNodeIndex, int parentNodeIndex)
	{
		PredictionModel node = nodes.get(currentNodeIndex);
		node.setParentNodeIndex(parentNodeIndex);
		nodes2.add(node);
		
		if(node.numberOfLeaves == 1)
			return;
		
		int currentNodeNewIndex = nodes2.size()-1;
		
		for(int leafIndex = 0; leafIndex < node.numberOfLeaves; leafIndex++)
		{
			int nextNodeIndex = node.getLeafNextNodeIndex(leafIndex);
			node.setLeafNextNodeIndex(leafIndex, nodes2.size());
			cleanUpTree(nodes2, nextNodeIndex, currentNodeNewIndex);
		}
	}
	
	public void binaryCleanup()
	{
		boolean[] nodesToKeep = new boolean[nodes.size()];
		int nodeIndex = binarySelectedNodeIndex;
		while(nodeIndex > 0)
		{
			nodesToKeep[nodeIndex] = true;
			nodeIndex = nodes.get(nodeIndex).parentNodeIndex;
		}
		nodesToKeep[0] = true;
		ArrayList<PredictionModel> newNodes = new ArrayList<PredictionModel>();
		for(int originalNodeIndex = 0; originalNodeIndex < nodes.size(); originalNodeIndex++)
		{
			if(nodesToKeep[originalNodeIndex])
			{
				newNodes.add(nodes.get(originalNodeIndex));
				newNodes.get(newNodes.size()-1).parentNodeIndex = newNodes.size()-2;
				if(originalNodeIndex > 0)
				{
					PredictionModel parentNode = newNodes.get(newNodes.size()-2);
					for(int leafIndex = 0; leafIndex < parentNode.getNumberOfLeaves(); leafIndex++)
					{
						if(parentNode.getLeafNextNodeIndex(leafIndex) == originalNodeIndex)
							parentNode.setLeafNextNodeIndex(leafIndex, newNodes.size()-1);
						else
							parentNode.setLeafNextNodeIndex(leafIndex, -1);
					}
				}
			}
		}
		nodes = newNodes;
		
		// Add a leaf representing the selected node
		PredictionModel selectedNode = nodes.get(nodes.size()-1);
		PredictionModel replacementLeaf = new ZeroR();
		replacementLeaf.construct(selectedNode.getDatasetUsed(), selectedNode.getSubsetUsed());
		replacementLeaf.setParentNodeIndex(nodes.size()-2);
		nodes.set(nodes.size()-1, replacementLeaf);
		this.binarySelectedNodeIndex = nodes.size()-1;
		
		// Add a leaf representing the outcome for the rest of the subset
		PredictionModel theOtherLeaf = new ZeroR();
		
		double[] subsetUsed = this.subsetUsed;
		if(subsetUsed == null)
			subsetUsed = Util.fill(1.0, datasetUsed.getNumberOfInstances());
		double[] replacementSubset = this.subsetUsed;
		if(replacementSubset == null)
			replacementSubset = Util.fill(1.0, datasetUsed.getNumberOfInstances());
		
		theOtherLeaf.construct(datasetUsed, Util.subtract(subsetUsed, replacementSubset));
		nodes.add(theOtherLeaf);
		for(nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++)
		{
			PredictionModel node = nodes.get(nodeIndex);
			if(node.getNumberOfLeaves() == 0)
				continue;
			
			for(int leafIndex = 0; leafIndex < node.getNumberOfLeaves(); leafIndex++)
			{
				if(node.getLeafNextNodeIndex(leafIndex) == -1)
					node.setLeafNextNodeIndex(leafIndex, nodes.size()-1);
			}
		}
		
		initialiseModelLeaves();
		leafNextNodeIndexes = Util.fill(-1, numberOfLeaves);
		parentNodeIndex = -1;
		initialiseLeafPredictions(this.datasetUsed, this.subsetUsed);
	}
	
	@Override
	public int size()
	{
		return nodes.size();
	}
}
