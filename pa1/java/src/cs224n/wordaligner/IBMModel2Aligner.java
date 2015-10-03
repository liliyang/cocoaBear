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
public class IBMModel2Aligner implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  
  // TODO: Use arrays or Counters for collecting sufficient statistics
  // from the training data.
  private CounterMap<String,String> countAlignment; //target, src
  private CounterMap<String,String> transProb; //tgt, src
  private CounterMap<String,Integer> countDistortion; // "i_l_m", j
  private CounterMap<String,Integer> distortion; // "i_l_m", j

  public Alignment align(SentencePair sentencePair) {
    // Placeholder code below. 
    // TODO Implement an inference algorithm for Eq.1 in the assignment
    // handout to predict alignments based on the counts you collected with train().
  	 Alignment alignment = new Alignment();
     List<String> sourceWords = sentencePair.getSourceWords();
     List<String> targetWords = sentencePair.getTargetWords();
     double maxScore, score;
     String key;
     int sourceId, targetId = 0;
     // YOUR CODE HERE
     for (int i = 0; i < sourceWords.size(); i++) {
     	sourceId = i;
     	maxScore = 0.0;
   		for (int j= 0; j < targetWords.size(); j++) {
   			key = i + "-" + targetWords.size() + "-" + sourceWords.size();
   			score = transProb.getCount(targetWords.get(j), sourceWords.get(i))*distortion.getCount(key, j);
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
    
  	// initialize transProb with values from model 1
  	IBMModel1Aligner model1 = new IBMModel1Aligner();
  	model1.train(trainingPairs);
  	transProb = model1.getTransProb();
  	
  	countAlignment = new CounterMap<String,String>();
  	countDistortion = new CounterMap<String,Integer>();
	  distortion = new CounterMap<String,Integer>();
	  // uniformly initialize distortion
	  int lenSource, lenTarget;
	  String key;
	  for (SentencePair p : trainingPairs) {
		  lenSource = p.getSourceWords().size();
		  lenTarget = p.getTargetWords().size();
		  for (int i = 0; i < lenSource; i++) {
			  for (int j = 0; j < lenTarget; j++) {
			  	key = i + "-" + lenTarget + "-" + lenSource;
			  	distortion.setCount(key, j, 0.1);
			  }
		  }
	  }

	  // iterate 
	  List<String> sourceWords, targetWords;
	  double sum = 0.0;
	  double prob;
	  for (int k = 0; k < 100; k++){
	  	for (SentencePair p : trainingPairs) {
	  		sourceWords = p.getSourceWords();
	  		targetWords = p.getTargetWords();
	  		for (int i = 0; i < sourceWords.size(); i++) {

	  			// calc the sum once
	  			sum = 0.0;
	  			for (int j = 0; j < targetWords.size(); j++) {
	  				key = i + "-" + targetWords.size() + "-" + sourceWords.size();
	  				sum += transProb.getCount(targetWords.get(j), sourceWords.get(i))*distortion.getCount(key, j);
	  			}				  

	  			for (int j = 0; j < targetWords.size(); j++) {
	  				key = i + "-" + targetWords.size() + "-" + sourceWords.size();
	  				prob = transProb.getCount(targetWords.get(j), sourceWords.get(i))*distortion.getCount(key, j);
	  				countAlignment.incrementCount(targetWords.get(j), sourceWords.get(i), prob/sum);
	  				countDistortion.incrementCount(key, j, prob/sum);
	  			}
	  		}
	  	}
	  	
	  	//Renormalize counts
		  double sCountTotal;
		  Counter<String> sCounter;
		  double dCountTotal;
		  Counter<Integer> dCounter;
		  for (String tWord: transProb.keySet()){
			  sCounter = transProb.getCounter(tWord);
			  sCountTotal = countAlignment.getCounter(tWord).totalCount();
			  for (String sWord: sCounter.keySet()){
				  transProb.setCount(tWord, sWord, countAlignment.getCount(tWord, sWord) / sCountTotal);
			  }
		  }
		  for (String base: distortion.keySet()) {
		  	dCounter = distortion.getCounter(base);
		  	dCountTotal = countDistortion.getCounter(base).totalCount();
		  	for (Integer j: dCounter.keySet()){
		  		distortion.setCount(base, j, countDistortion.getCount(base, j) / dCountTotal);
		  	}
		  }
	  }
  }
}
