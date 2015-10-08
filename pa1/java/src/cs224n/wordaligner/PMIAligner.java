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
public class PMIAligner implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  
  // TODO: Use arrays or Counters for collecting sufficient statistics
  // from the training data.
  private CounterMap<String,String> sourceTargetCounts;
  private Counter<String> sourceCounts;
  private Counter<String> targetCounts;

  public Alignment align(SentencePair sentencePair) {
    // Placeholder code below. 
    // TODO Implement an inference algorithm for Eq.1 in the assignment
    // handout to predict alignments based on the counts you collected with train().
    Alignment alignment = new Alignment();
    List<String> sourceWords = sentencePair.getSourceWords();
    List<String> targetWords = sentencePair.getTargetWords();
    double maxScore, score;
    int sourceId, targetId = 0;
    
    for (int i = 0; i < sourceWords.size(); i++) {
      sourceId = i;
      maxScore = 0.0;
      for (int j= 0; j < targetWords.size(); j++) {
        score = scoreAlignment(sourceWords.get(i), targetWords.get(j));
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
    // initialize global counters
    sourceTargetCounts = new CounterMap<String,String>();
    sourceCounts = new Counter<String>();
    targetCounts = new Counter<String>();

    List<String> sourceWords;
    List<String> targetWords;
  	
    for (SentencePair p : trainingPairs) {
      sourceWords = p.getSourceWords();
      targetWords = p.getTargetWords();
      for (String sWord : sourceWords) {
        sourceCounts.incrementCount(sWord, 1.0);
        for (String tWord : targetWords) {
          targetCounts.incrementCount(tWord, 1.0);
          sourceTargetCounts.incrementCount(sWord, tWord, 1.0);
        }
      }
    }
  }
       
  public double scoreAlignment(String sWord, String tWord) {
    double sCount = sourceCounts.getCount(sWord);
    double tCount = targetCounts.getCount(tWord);
    double count = sourceTargetCounts.getCount(sWord, tWord);
    return count/(sCount*tCount);
  }
}
