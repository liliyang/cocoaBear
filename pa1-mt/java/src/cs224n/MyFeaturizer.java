package edu.stanford.nlp.mt.decoder.feat;

import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.mt.util.FeatureValue;
import edu.stanford.nlp.mt.util.Featurizable;
import edu.stanford.nlp.mt.util.Sequence;
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

		// target sentence length 
		features.add(new FeatureValue<String>(String.format("%s:%d","TGTD", f.targetPhrase.size()), 1.0)); 

		//  // source sentence length // +0.1
//		features.add(new FeatureValue<String>(String.format("%s:%d","SRTD", f.sourceSentence.size()), 1.0));

		// sentence length ratio 
//		features.add(new FeatureValue<String>(String.format("%s:%f","RatioSenLen", f.targetPhrase.size()*1.0/f.sourceSentence.size()), 1.0));

		// sentence length diff 
//		features.add(new FeatureValue<String>(String.format("%s:%d","RatioSenLen", f.targetPhrase.size() - f.sourceSentence.size()), 1.0));

		// avg len of words in target sentence
		double avgWLenSrc = avgWLen(f.sourcePhrase);
//		features.add(new FeatureValue<String>(String.format("%s:%f","AvgWordLenTgt", avgWLenSrc), 1.0));

		// avg len of words in target sentence
		double avgWLenTgt = avgWLen(f.targetPhrase);
//		features.add(new FeatureValue<String>( String.format("%s:%f","AvgWordLenTgt", avgWLenTgt), 1.0));

		// ratio of avg word len between target and source sentences
//		features.add(new FeatureValue<String>( String.format("%s:%f","RatioAvgWordLen", avgWLenTgt / avgWLenSrc), 1.0));

		// diff of avg word len between target and source sentences
//		features.add(new FeatureValue<String>( String.format("%s:%f","RatioAvgWordLen", avgWLenTgt - avgWLenSrc), 1.0));

		//num of unique words in source sentence
//		int uniqWSrc = uniqueWord(f.sourcePhrase);
//		features.add(new FeatureValue<String>( String.format("%s:%d","uniqWordCntSrc", uniqWSrc), 1.0));

		//num of unique words in target sentence
//		int uniqWTgt = uniqueWord(f.targetPhrase);
//		features.add(new FeatureValue<String>( String.format("%s:%d","uniqWordCntTgt", uniqWTgt), 1.0));

		//diff of unique words in target sentence
//		features.add(new FeatureValue<String>( String.format("%s:%d","uniqWordCntDiff", uniqWTgt-uniqWSrc), 1.0));

		//ratio of unique words in target sentence
//		features.add(new FeatureValue<String>( String.format("%s:%f","uniqWordCntDiff", uniqWTgt*1.0/uniqWSrc), 1.0));

		return features;
	}

	public int uniqueWord(Sequence<IString> sentence){
		int uniq = 0;
		int srcLen = sentence.size();
		HashSet<String> pool = new HashSet<String>();
		String word;
		for (int i =0; i< srcLen; i++){
			word = sentence.get(i).toString();
			if (!pool.contains(word)){
				pool.add(word);
				uniq++;
			}
		}
		return uniq;
	}
	public double avgWLen(Sequence<IString> sentence){
		double avgWLen = 0.0;

		int srcLen = sentence.size();
		for (int i =0; i< srcLen; i++){
			avgWLen += sentence.get(i).toString().length();
		}
		avgWLen /= srcLen;

		return avgWLen;
	}
	@Override
	public boolean isolationScoreOnly() {
		return false;
	}
}
