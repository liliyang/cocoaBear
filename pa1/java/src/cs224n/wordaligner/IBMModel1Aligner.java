package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;

/**
 * Simple word alignment baseline model that maps source positions to target 
 * positions along the diagonal of the alignment grid.
 * 
 * IMPORTANT: Make sure that you read the comments in the
 * cs224n.wordaligner.WordAligner interface.
 * 
 * @author Dan Klein
 * @author Spence Green
 */
public class IBMModel1Aligner implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  
  // TODO: Use arrays or Counters for collecting sufficient statistics
  // from the training data.
//  private CounterMap<String,String> sourceTargetCounts;
  private CounterMap<String,String> countAlignment; //target, src
  private CounterMap<String,String> transProb; //tgt, src

  public Alignment align(SentencePair sentencePair) {
    // Placeholder code below. 
    // TODO Implement an inference algorithm for Eq.1 in the assignment
    // handout to predict alignments based on the counts you collected with train().
    Alignment alignment = new Alignment();
    List<String> sourceWords = sentencePair.getSourceWords();
    List<String> targetWords = sentencePair.getTargetWords();
    double maxScore, score;
    int sourceId, targetId = 0;
    // YOUR CODE HERE
    for (int i = 0; i < sourceWords.size(); i++) {
    	sourceId = i;
    	maxScore = 0.0;
  		for (int j= 0; j < targetWords.size(); j++) {
  			score = transProb.getCount(targetWords.get(j), sourceWords.get(i));
  			if (score > maxScore) {
  				maxScore = score;
  				targetId = j;
  			}
  		}
  		alignment.addPredictedAlignment(targetId, sourceId);
  	}
   
    return alignment;
  }

  public void train(List<SentencePair> trainingPairs) {
	  double sum;
	  
	// start with uniform t
	  countAlignment = new CounterMap<String, String>();
	  transProb = new CounterMap<String,String>();
	  List<String> sourceWords, targetWords;
	  for (SentencePair p : trainingPairs) {
		  sourceWords = p.getSourceWords();
		  targetWords = p.getTargetWords();
		  for (String sWord : sourceWords) {
			  for (String tWord : targetWords) {
				  transProb.setCount(tWord, sWord, 0.1);			  
			  }
		  }
	  }

	// iterate 
	  for (int i = 0; i < 100; i++){
		  for (SentencePair p : trainingPairs) {
			  sourceWords = p.getSourceWords();
			  targetWords = p.getTargetWords();
			  for (String sWord : sourceWords) {
				  
				  // calc the sum once
				  sum = 0.0;
				  for (String tWord : targetWords) {
					  sum += transProb.getCount(tWord, sWord);
				  }				  
				
				  for (String tWord : targetWords) {		  
					  countAlignment.incrementCount(tWord, sWord, transProb.getCount(tWord, sWord)/sum);
				  }
			  }
		  }
		  
		  //Renormalize counts
		  double sCountTotal;
		  Counter<String> sCounter;
		  for (String tWord: transProb.keySet()){
			  sCounter = transProb.getCounter(tWord);
			  sCountTotal = countAlignment.getCounter(tWord).totalCount();
			  for (String sWord: sCounter.keySet()){
				  transProb.setCount(tWord, sWord, countAlignment.getCount(tWord, sWord) / sCountTotal);
			  }
		  }
	  }
  }
}
