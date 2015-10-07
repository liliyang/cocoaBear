package edu.stanford.nlp.mt.decoder.feat;

import java.util.List;

import edu.stanford.nlp.mt.util.FeatureValue;
import edu.stanford.nlp.mt.util.Featurizable;
import edu.stanford.nlp.mt.util.IString;
import edu.stanford.nlp.mt.decoder.feat.RuleFeaturizer;
import edu.stanford.nlp.util.Generics;

/**
 * A rule featurizer.
 */
public class MyFeaturizer implements RuleFeaturizer<IString, String> {
  private String punctutations = ".,:;!?";
  
  @Override
  public void initialize() {
    // Do any setup here.
  }

  @Override
  public List<FeatureValue<String>> ruleFeaturize(
      Featurizable<IString, String> f) {

    // TODO: Return a list of features for the rule. Replace these lines
    // with your own feature.
    List<FeatureValue<String>> features = Generics.newLinkedList();
//    features.add(new FeatureValue<String>("MyFeature", 1.0));
    
    // sentence length
    // 15.088 newstest2011.myfeature  (TGTD)  -0.107
//    String.format("%s:%d","TGTD", f.targetPhrase.size(), 1.0); 
    
   
//  // sentence length
//     String.format("%s:%d","SRTD", f.sourceSentence.size(), 1.0);
    
//    // is the first char capitalized?
//    features.add(new FeatureValue<String>( String.format("%s:%d","UpperCase", Character.isUpperCase(f.targetPhrase.get(0).toString().charAt(0))), 1.0));
    
//    // Punctuation
//    String lastWord = f.targetPhrase.get(f.targetPhrase.size()-1).toString();
//    String punct = lastWord.charAt(lastWord.length()-1)+"";
//    if(punctutations.contains(punct))
//        features.add(new FeatureValue<String>( String.format("%s:%d","Punctuation", punct, 1.0)));
    
//    // Ratio between target sentence and source sentence
    double tgtSrcRatio = (double) f.targetPhrase.size() / (double) f.sourceSentence.size();
    features.add(new FeatureValue<String>( String.format("%s:%f","SentenceRatio", tgtSrcRatio), 1.0));
    
    
    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
