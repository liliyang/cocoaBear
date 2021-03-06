package cs224n.corefsystems;

import cs224n.coref.*;
import cs224n.util.Pair;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.RVFDataset;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Triple;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import edu.stanford.nlp.util.logging.StanfordRedwoodConfiguration;

import java.text.DecimalFormat;
import java.util.*;

import static edu.stanford.nlp.util.logging.Redwood.Util.*;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class ClassifierBased implements CoreferenceSystem {

	private static <E> Set<E> mkSet(E[] array){
		Set<E> rtn = new HashSet<E>();
		Collections.addAll(rtn, array);
		return rtn;
	}

	private static final Set<Object> ACTIVE_FEATURES = mkSet(new Object[]{

			/*
			 * TODO: Create a set of active features
			 */


			

			Feature.HeadMatch.class,
			Feature.ProxPath.class,
			Feature.CandiPath.class,
			Feature.SameNumber.class,
			
//			Feature.ExactMatch.class,
//			Feature.WordDistance.class,//barely help
//			Feature.SentenceDistance.class,//
//			Feature.ProxIsPron.class,
//			Feature.CandiIsPron.class,

//			Feature.ProxEntityType.class,
//			Feature.CandiEntityType.class,

			//skeleton for how to create a pair feature
//			Pair.make(Feature.WordDistance.class, Feature.SentenceDistance.class),
//			Pair.make(Feature.ProxIsPron.class, Feature.CandiIsPron.class),
//			Pair.make(Feature.ProxPath.class, Feature.CandiPath.class),
//			Pair.make(Feature.HeadMatch.class, Feature.CandiPath.class),
//			Pair.make(Feature.ProxEntityType.class, Feature.CandiEntityType.class),
	});


	private LinearClassifier<Boolean,Feature> classifier;

	public ClassifierBased(){
		StanfordRedwoodConfiguration.setup();
		RedwoodConfiguration.current().collapseApproximate().apply();
	}

	public FeatureExtractor<Pair<Mention,ClusteredMention>,Feature,Boolean> extractor = new FeatureExtractor<Pair<Mention, ClusteredMention>, Feature, Boolean>() {
		private <E> Feature feature(Class<E> clazz, Pair<Mention,ClusteredMention> input, Option<Double> count){
			
			//--Variables
			Mention onPrix = input.getFirst(); //the first mention (referred to as m_i in the handout)
			Mention candidate = input.getSecond().mention; //the second mention (referred to as m_j in the handout)
			Entity candidateCluster = input.getSecond().entity; //the cluster containing the second mention


			//--Features
			if(clazz.equals(Feature.ExactMatch.class)){
				//(exact string match)
				return new Feature.ExactMatch(onPrix.gloss().equals(candidate.gloss()));
			} else if(clazz.equals(Feature.WordDistance.class)) {
				/*
				 * TODO: Add features to return for specific classes. Implement calculating values of features here.
				 */
//				onPrix.sentence.
//				return new Feature.WordDistance(onPrix.beginIndexInclusive -candidate.endIndexExclusive);
				return new Feature.WordDistance(onPrix.headWordIndex -candidate.headWordIndex);
			} else if(clazz.equals(Feature.SentenceDistance.class)) {
				/*
				 * TODO: Add features to return for specific classes. Implement calculating values of features here.
				 */
				Document doc = onPrix.doc;
				int s_i = doc.indexOfSentence(onPrix.sentence);
				int s_j = doc.indexOfSentence(candidate.sentence);
				return new Feature.SentenceDistance(s_i - s_j);
			} else if(clazz.equals(Feature.HeadMatch.class)) {
				// exact head word match
				return new Feature.HeadMatch(onPrix.headWord().toLowerCase().equals(candidate.headWord().toLowerCase()));
			} else if(clazz.equals(Feature.ProxIsPron.class)) {
				// 
				Pronoun pro = Pronoun.valueOrNull(onPrix.gloss().toUpperCase().replaceAll(" ","_"));
				return new Feature.ProxIsPron(pro != null);
			}else if(clazz.equals(Feature.CandiIsPron.class)) {
				// 
				Pronoun pro = Pronoun.valueOrNull(candidate.gloss().toUpperCase().replaceAll(" ","_"));
				return new Feature.CandiIsPron(pro != null);
			} else if(clazz.equals(Feature.ProxPath.class)) {
				// 
				LinkedList<Pair<String, Integer>> path = onPrix.sentence.parse.pathToIndex(onPrix.headWordIndex);
				Set<String> p_set = new LinkedHashSet<String>();
				Document doc = onPrix.doc;
				int s_i = doc.indexOfSentence(onPrix.sentence);
				int s_j = doc.indexOfSentence(candidate.sentence);
				if (s_i == s_j) {
					for (Pair<String, Integer> pair: path){
						p_set.add(pair.getFirst());
					}
				}
				return new Feature.ProxPath(p_set);
			}else if(clazz.equals(Feature.CandiPath.class)) {
				// 
				LinkedList<Pair<String, Integer>> path = candidate.sentence.parse.pathToIndex(candidate.headWordIndex);
				Set<String> p_set = new LinkedHashSet<String>();
				Document doc = onPrix.doc;
				int s_i = doc.indexOfSentence(onPrix.sentence);
				int s_j = doc.indexOfSentence(candidate.sentence);
				if (s_i == s_j) {
					for (Pair<String, Integer> pair: path){
						p_set.add(pair.getFirst());
					}
				}
				return new Feature.CandiPath(p_set);
			} else if(clazz.equals(Feature.CandiEntityType.class)) {
				return new Feature.CandiEntityType(new LinkedHashSet(candidate.sentence.nerTags));
			} else if(clazz.equals(Feature.ProxEntityType.class)) {
				return new Feature.ProxEntityType(new LinkedHashSet(onPrix.sentence.nerTags));
			} else if(clazz.equals(Feature.SameNumber.class)) {
				Pair<Boolean,Boolean> pair = Util.haveNumberAndAreSameNumber(onPrix, candidate);
				return new Feature.SameNumber(pair.getFirst() && pair.getSecond());
			}
			
			else {
				throw new IllegalArgumentException("Unregistered feature: " + clazz);
			}
		}

		@SuppressWarnings({"unchecked"})
		@Override
		protected void fillFeatures(Pair<Mention, ClusteredMention> input, Counter<Feature> inFeatures, Boolean output, Counter<Feature> outFeatures) {
			//--Input Features
			for(Object o : ACTIVE_FEATURES){
				if(o instanceof Class){
					//(case: singleton feature)
					Option<Double> count = new Option<Double>(1.0);
					Feature feat = feature((Class) o, input, count);
					if(count.get() > 0.0){
						inFeatures.incrementCount(feat, count.get());
					}
				} else if(o instanceof Pair){
					//(case: pair of features)
					Pair<Class,Class> pair = (Pair<Class,Class>) o;
					Option<Double> countA = new Option<Double>(1.0);
					Option<Double> countB = new Option<Double>(1.0);
					Feature featA = feature(pair.getFirst(), input, countA);
					Feature featB = feature(pair.getSecond(), input, countB);
					if(countA.get() * countB.get() > 0.0){
						inFeatures.incrementCount(new Feature.PairFeature(featA, featB), countA.get() * countB.get());
					}
				}
			}

			//--Output Features
			if(output != null){
				outFeatures.incrementCount(new Feature.CoreferentIndicator(output), 1.0);
			}
		}

		@Override
		protected Feature concat(Feature a, Feature b) {
			return new Feature.PairFeature(a,b);
		}
	};

	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		startTrack("Training");
		//--Variables
		RVFDataset<Boolean, Feature> dataset = new RVFDataset<Boolean, Feature>();
		LinearClassifierFactory<Boolean, Feature> fact = new LinearClassifierFactory<Boolean,Feature>();
		//--Feature Extraction
		startTrack("Feature Extraction");
		for(Pair<Document,List<Entity>> datum : trainingData){
			//(document variables)
			Document doc = datum.getFirst();
			List<Entity> goldClusters = datum.getSecond();
			List<Mention> mentions = doc.getMentions();
			Map<Mention,Entity> goldEntities = Entity.mentionToEntityMap(goldClusters);
			startTrack("Document " + doc.id);
			//(for each mention...)
			for(int i=0; i<mentions.size(); i++){
				//(get the mention and its cluster)
				Mention onPrix = mentions.get(i);
				Entity source = goldEntities.get(onPrix);
				if(source == null){ throw new IllegalArgumentException("Mention has no gold entity: " + onPrix); }
				//(for each previous mention...)
				int oldSize = dataset.size();
				for(int j=i-1; j>=0; j--){
					//(get previous mention and its cluster)
					Mention cand = mentions.get(j);
					Entity target = goldEntities.get(cand);
					if(target == null){ throw new IllegalArgumentException("Mention has no gold entity: " + cand); }
					//(extract features)
					Counter<Feature> feats = extractor.extractFeatures(Pair.make(onPrix, cand.markCoreferent(target)));
					//(add datum)
					dataset.add(new RVFDatum<Boolean, Feature>(feats, target == source));
					//(stop if
					if(target == source){ break; }
				}
				//logf("Mention %s (%d datums)", onPrix.toString(), dataset.size() - oldSize);
			}
			endTrack("Document " + doc.id);
		}
		endTrack("Feature Extraction");
		//--Train Classifier
		startTrack("Minimizer");
		this.classifier = fact.trainClassifier(dataset);
		endTrack("Minimizer");
		//--Dump Weights
		startTrack("Features");
		//(get labels to print)
		Set<Boolean> labels = new HashSet<Boolean>();
		labels.add(true);
		//(print features)
		for(Triple<Feature,Boolean,Double> featureInfo : this.classifier.getTopFeatures(labels, 0.0, true, 100, true)){
			Feature feature = featureInfo.first();
			Boolean label = featureInfo.second();
			Double magnitude = featureInfo.third();
			//log(FORCE,new DecimalFormat("0.000").format(magnitude) + " [" + label + "] " + feature);
		}
		end_Track("Features");
		endTrack("Training");
	}

	public List<ClusteredMention> runCoreference(Document doc) {
		//--Overhead
		startTrack("Testing " + doc.id);
		//(variables)
		List<ClusteredMention> rtn = new ArrayList<ClusteredMention>(doc.getMentions().size());
		List<Mention> mentions = doc.getMentions();
		int singletons = 0;
		//--Run Classifier
		for(int i=0; i<mentions.size(); i++){
			//(variables)
			Mention onPrix = mentions.get(i);
			int coreferentWith = -1;
			//(get mention it is coreferent with)
			for(int j=i-1; j>=0; j--){

				ClusteredMention cand = rtn.get(j);
				
				boolean coreferent = classifier.classOf(new RVFDatum<Boolean, Feature>(
						       extractor.extractFeatures(Pair.make(onPrix, cand))));
				
				if(coreferent){
					coreferentWith = j;
					break;
				}
			}

			if(coreferentWith < 0){
				singletons += 1;
				rtn.add(onPrix.markSingleton());
			} else {
				//log("Mention " + onPrix + " coreferent with " + mentions.get(coreferentWith));
				rtn.add(onPrix.markCoreferent(rtn.get(coreferentWith)));
			}
		}
		//log("" + singletons + " singletons");
		//--Return
		endTrack("Testing " + doc.id);
		return rtn;
	}

	private class Option<T> {
		private T obj;
		public Option(T obj){ this.obj = obj; }
		public Option(){};
		public T get(){ return obj; }
		public void set(T obj){ this.obj = obj; }
		public boolean exists(){ return obj != null; }
	}
}
