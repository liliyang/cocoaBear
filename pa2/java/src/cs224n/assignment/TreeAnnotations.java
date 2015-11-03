package cs224n.assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cs224n.ling.Tree;
import cs224n.ling.Trees;
import cs224n.ling.Trees.MarkovizationAnnotationStripper;
import cs224n.util.Filter;

/**
 * Class which contains code for annotating and binarizing trees for
 * the parser's use, and debinarizing and unannotating them for
 * scoring.
 */
public class TreeAnnotations {

	public static Tree<String> annotateTree(Tree<String> unAnnotatedTree) {

		// Currently, the only annotation done is a lossless binarization

		// TODO: change the annotation from a lossless binarization to a
		// finite-order markov process (try at least 1st and 2nd order)

		// TODO : mark nodes with the label of their parent nodes, giving a second
		// order vertical markov process
		
		
//		verticalMarkov(unAnnotatedTree);

		verticalMarkovThird(unAnnotatedTree);
		
		Tree<String> annotatedTree = binarizeTree(unAnnotatedTree);
		
		horizontalMarkov(annotatedTree);
		return annotatedTree;

	}
	
	private static void horizontalMarkov(Tree<String> tree) {
//		String grandparent = tree.getLabel();
		for (Tree<String> child : tree.getChildren()) {
			
			String label = child.getLabel();
			
			if (label.contains("->")){
				String[] sp0 = label.split("->");
				if (sp0[1].contains("_")){
					String[] sp1 = sp0[1].split("_");
					child.setLabel(sp0[0]+"->_"+sp1[sp1.length-1]);
				}
			}
			horizontalMarkov(child);
		}
	}
	
	
//	private static void horizontalMarkovHelper(Tree<String> tree) {
//		if (!tree.isLeaf()) {
//			String label = tree.getLabel();
//			tree.setLabel(parent);
//			for (Tree<String> child : tree.getChildren()) {
//				verticalMarkovHelper(child, label);
//			}
//		}
//	}
//	
	private static void printTree(Tree<String> tree){
		for (Tree<String> child : tree.getChildren()) {
			System.out.println(child.getLabel());
			printTree(child);
		}
	}
	private static void verticalMarkovThird(Tree<String> tree) {
		String grandparent = tree.getLabel();
		for (Tree<String> child : tree.getChildren()) {
			String parent = child.getLabel();
			for (Tree<String> grandchild : child.getChildren()) {
			verticalMarkovThirdHelper(grandchild, parent, grandparent);
			}
		}
	}
	
	private static void verticalMarkovThirdHelper(Tree<String> tree, String parent, String grandparent) {
		if (!tree.isLeaf()) {
			String label = tree.getLabel();
			tree.setLabel(label + "^" + parent+ "^"+grandparent);
			for (Tree<String> child : tree.getChildren()) {
				verticalMarkovThirdHelper(child, label, parent);
			}
		}
	}
	
	
	private static void verticalMarkov(Tree<String> tree) {
		String label = tree.getLabel();
		for (Tree<String> child : tree.getChildren()) {
			verticalMarkovHelper(child, label);
		}
	}
	
	private static void verticalMarkovHelper(Tree<String> tree, String parent) {
		if (!tree.isLeaf()) {
			String label = tree.getLabel();
			tree.setLabel(label + "^" + parent);
			for (Tree<String> child : tree.getChildren()) {
				verticalMarkovHelper(child, label);
			}
		}
	}

	private static Tree<String> binarizeTree(Tree<String> tree) {
		String label = tree.getLabel();
		if (tree.isLeaf())
			return new Tree<String>(label);
		if (tree.getChildren().size() == 1) {
			return new Tree<String>
			(label, 
					Collections.singletonList(binarizeTree(tree.getChildren().get(0))));
		}
		// otherwise, it's a binary-or-more local tree, 
		// so decompose it into a sequence of binary and unary trees.
		String intermediateLabel = "@"+label+"->";
		Tree<String> intermediateTree =
				binarizeTreeHelper(tree, 0, intermediateLabel);
		return new Tree<String>(label, intermediateTree.getChildren());
	}

	private static Tree<String> binarizeTreeHelper(Tree<String> tree,
			int numChildrenGenerated, 
			String intermediateLabel) {
		Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
		List<Tree<String>> children = new ArrayList<Tree<String>>();
		children.add(binarizeTree(leftTree));
		if (numChildrenGenerated < tree.getChildren().size() - 1) {
			Tree<String> rightTree = 
					binarizeTreeHelper(tree, numChildrenGenerated + 1, 
							intermediateLabel + "_" + leftTree.getLabel());
			children.add(rightTree);
		}
		return new Tree<String>(intermediateLabel, children);
	} 

	public static Tree<String> unAnnotateTree(Tree<String> annotatedTree) {

		// Remove intermediate nodes (labels beginning with "@"
		// Remove all material on node labels which follow their base symbol
		// (cuts at the leftmost - or ^ character)
		// Examples: a node with label @NP->DT_JJ will be spliced out, 
		// and a node with label NP^S will be reduced to NP

		Tree<String> debinarizedTree =
				Trees.spliceNodes(annotatedTree, new Filter<String>() {
					public boolean accept(String s) {
						return s.startsWith("@");
					}
				});
		Tree<String> unAnnotatedTree = 
				(new Trees.FunctionNodeStripper()).transformTree(debinarizedTree);
    Tree<String> unMarkovizedTree =
        (new Trees.MarkovizationAnnotationStripper()).transformTree(unAnnotatedTree);
		return unMarkovizedTree;
	}
}
