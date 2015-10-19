package cs224n.assignment;

import cs224n.assignment.Grammar.BinaryRule;
import cs224n.assignment.Grammar.UnaryRule;
import cs224n.ling.Tree;
import cs224n.util.*;
import java.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;

    public void train(List<Tree<String>> trainTrees) {
    		// Annotate the trees in the training set
      	for (int i = 0; i < trainTrees.size(); i++) {
      			trainTrees.set(i, TreeAnnotations.annotateTree(trainTrees.get(i)));
      	}
        lexicon = new Lexicon(trainTrees);
        
        grammar = new Grammar(trainTrees);
    }

    public Tree<String> getBestParse(List<String> sentence) {
	      int numWords = sentence.size();
    	  // initialize a table to hold the probabilities
        // also initialize a table to hold back pointers
        List<List<Counter<String>>> table = new ArrayList<List<Counter<String>>>(numWords+1);
        List<List<Map<String, Triplet<Integer, String, String>>>> back = new ArrayList<List<Map<String, Triplet<Integer, String, String>>>>(numWords+1);
        for (int i = 0; i <= numWords; i++) {
	          table.add(new ArrayList<Counter<String>>(numWords+1));
            back.add(new ArrayList<Map<String, Triplet<Integer, String, String>>>(numWords+1));
            for (int j = 0; j <= numWords; j++) {
		            table.get(i).add(new Counter<String>());
        	      back.get(i).add(new HashMap<String, Triplet<Integer, String, String>>());
            }
        }
        
        String word;
        Set<String> tags = lexicon.getAllTags();
        double score;
        // add POS tags for all words in sentence
        for (int i = 0; i < numWords; i++) {
            word = sentence.get(i);
            for (String tag : tags) {
		            score = lexicon.scoreTagging(word, tag);
        	      table.get(i).get(i+1).setCount(tag, score);
            }
            // add unary rules
            addUnary(table, back, i, i+1);
        }
        
        Counter<String> cellL, cellR, cellN;
        double prob;
        String parent;
        // now fill in the rest of table
        for (int span = 2; span <= numWords; span++) {
	          for (int begin = 0; begin <= (numWords - span); begin++) {
		            int end = begin + span;
        	      cellN = table.get(begin).get(end);
        	      for (int split = (begin+1); split <= (end-1); split++) {
		                cellL = table.get(begin).get(split);
        	          cellR = table.get(split).get(end);
        	          for (String tag : cellL.keySet()) {
			                  for (BinaryRule rule : grammar.getBinaryRulesByLeftChild(tag)) {
			                      parent = rule.getParent();
        		                prob = cellL.getCount(tag)*rule.getScore()*cellR.getCount(rule.getRightChild());
        		                if (prob > cellN.getCount(parent)) {
				                        cellN.setCount(parent, prob);
        		                    back.get(begin).get(end).put(parent, new Triplet<Integer, String, String>(split, tag, rule.getRightChild()));
        		                }
        		            }
        	          }
        	      }
        	      // add any applicable Unary rules
        	      addUnary(table, back, begin, end);
            }
        }
        
        // build the best parse tree
        Tree<String> best = buildParseTree(table, back);
        best.setWords(sentence);
        return TreeAnnotations.unAnnotateTree(best);
    }

    private void addUnary(List<List<Counter<String>>> table, List<List<Map<String, Triplet<Integer, String, String>>>> back, int begin, int end) {
        boolean updated = true;
    	  Counter<String> cell = table.get(begin).get(end);
     	  List<UnaryRule> rules;
    	  double score, prob;
    	  String parent;
    	  List<String> tags;
    	  while (updated) {
	          updated = false;
	          tags = new ArrayList<String>(cell.keySet());
    	      for (String tag: tags) {
		            rules = grammar.getUnaryRulesByChild(tag);
    		        score = cell.getCount(tag);
    		        for (UnaryRule rule : rules) {
		                prob = rule.getScore()*score;
    		            parent = rule.getParent();
		                if (prob > cell.getCount(parent)) {
			                  cell.setCount(parent, prob);
    			              back.get(begin).get(end).put(parent, new Triplet<Integer, String, String>(-1, tag, ""));
    			              updated = true;
    		            }
    		        }
    	      }    				
    	  }
    }
    
    private Tree<String> buildParseTree(List<List<Counter<String>>> table, List<List<Map<String, Triplet<Integer, String, String>>>> back) {
	      int numWords = table.size()-1;
    	  Counter<String> finalCell = table.get(0).get(numWords);
    	  double maxScore = 0;
    	  String bestTag = null;
    	  for (String tag : finalCell.keySet()) {
	          if (finalCell.getCount(tag) > maxScore) {
		            maxScore = finalCell.getCount(tag);
    		        bestTag = tag;
    	      }
    	  }
    	  Tree<String> parseTree = buildParseTreeHelper(back, 0, numWords, bestTag);
    		
    	  return addRoot(parseTree);
    }
    
    private Tree<String> buildParseTreeHelper(List<List<Map<String, Triplet<Integer, String, String>>>> back, int begin, int end, String word) {
	      Triplet<Integer, String, String> backPointer = back.get(begin).get(end).get(word);
    	  if (backPointer == null) {
	          return new Tree<String>(word, Collections.singletonList(new Tree<String>("")));
    	  }
	      if (backPointer.getFirst() == -1) {
	          Tree<String> newChild = buildParseTreeHelper(back, begin, end, backPointer.getSecond());
	          return new Tree<String>(word, Collections.singletonList(newChild));
	      } else {
	          int split = backPointer.getFirst();
	          List<Tree<String>> children = new ArrayList<Tree<String>>();
	          Tree<String> leftChild = buildParseTreeHelper(back, begin, split, backPointer.getSecond());
	          children.add(leftChild);
	          Tree<String> rightChild = buildParseTreeHelper(back, split, end, backPointer.getThird());
	          children.add(rightChild);
	          return new Tree<String>(word, children);
	      }
    }
    
    private Tree<String> addRoot(Tree<String> tree) {
	      return new Tree<String>("ROOT", Collections.singletonList(tree));
    }
 }
