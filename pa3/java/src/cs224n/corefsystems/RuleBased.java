package cs224n.corefsystems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.coref.Name;
import cs224n.coref.Pronoun;
import cs224n.coref.Pronoun.Speaker;
import cs224n.coref.Sentence;
import cs224n.coref.Util;
import cs224n.ling.Tree;
import cs224n.util.Counter;
import cs224n.util.CounterMap;
import cs224n.util.Pair;

public class RuleBased implements CoreferenceSystem {
	CounterMap<String,String> coRefHeads;


	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		coRefHeads = new CounterMap<String,String>();
		
		for(Pair<Document, List<Entity>> pair : trainingData){
      List<Entity> clusters = pair.getSecond();
      
      String head1, head2;
      // iterate through co-reference pairs
      for(Entity e : clusters){
        for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
        	// get the head word for each mention and add to counter map
        	head1 = mentionPair.getFirst().headWord().toLowerCase();
        	head2 = mentionPair.getSecond().headWord().toLowerCase();
        	//if (!isPronoun(head1) && !isPronoun(head2)) {
        		coRefHeads.incrementCount(head1, head2, 1);
        	//}
        }
      }
    }	
	}

	// map to keep track of most recent mapping of Mention to ClusteredMention
	private Map<Mention,ClusteredMention> mentionToCM;
	@Override
	public List<ClusteredMention> runCoreference(Document doc) {

		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
		mentionToCM = new HashMap<Mention,ClusteredMention>();
		//layer 1: exact text match
		exactMatch(doc);
		//closeMatch(doc);
		
		//layer 2: Find appositives and predicate nominative
		preciseConstruct(doc);
		
		//layer 3: exact head match
		headMatch(doc);
		
		//layer 4: word inclusion
		//wordInclusion(doc);
		
		//layer 5: similar heads/pronoun assignment
		similarHead(doc);
		

		for (ClusteredMention cm: mentionToCM.values()) {
			mentions.add(cm);
		}
		return mentions;
	}

	public void exactMatch(Document doc) {
		//(variables)
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		//(for each mention...)
		for(Mention m : doc.getMentions()){
			//(...get its text)
			String mentionString = m.gloss().toLowerCase();
			//(...if we've seen this text before...)
			if(clusters.containsKey(mentionString)){
				//(...add it to the cluster)
				mentionToCM.put(m, m.markCoreferent(clusters.get(mentionString)));
			} else {
				//(...else create a new singleton cluster)
				ClusteredMention newCluster = m.markSingleton();
				mentionToCM.put(m, newCluster);
				clusters.put(mentionString, newCluster.entity);
			}
		}
	}
	
	
	public void preciseConstruct(Document doc) {
		Map<Sentence, List<Mention>> sentenceToMentions = new HashMap<Sentence, List<Mention>>();
		for(Mention m : doc.getMentions()){ 
			if (sentenceToMentions.containsKey(m.sentence)){
				sentenceToMentions.get(m.sentence).add(m);
			}else{
				sentenceToMentions.put(m.sentence, new ArrayList<Mention>(Arrays.asList(m)));
			}
		}
		for (Sentence sentence: sentenceToMentions.keySet()){
			List<Mention> list = sentenceToMentions.get(sentence);		
			Collections.sort(list, new Comparator<Mention>(){
				@Override
				public int compare(Mention o1, Mention o2) {
					return Integer.compare(o1.headWordIndex, o2.headWordIndex);
				}				
			});

			Mention candidate, m;
			for (int i=list.size()-1; i>0; i--){
				candidate = list.get(i-1);
				m = list.get(i);
				
				Entity curE = mentionToCM.get(m).entity;
				Entity matchingE = mentionToCM.get(candidate).entity;
					
					
				if (
						preciseHelper( m, candidate)
						&& (hasGender(candidate))
						&& !sameClusters(curE, matchingE)
						){	mergeClusters(mentionToCM.get(m).entity, mentionToCM.get(candidate).entity);}		
			}
		}
	}
	
	public boolean hasGender(Mention a){
		//(names)
		Name nameA = Name.get(a.gloss());
		//(pronouns)
		Pronoun proA = Pronoun.valueOrNull(a.gloss().toUpperCase().replaceAll(" ", "_"));
		//(error conditions)
		if(nameA == null && proA == null){ return false; }
		return true;
	}
	
	public boolean preciseHelper(Mention m, Mention candidate){
		List<String> tgt = new ArrayList<String>();

		tgt.add(",");//Appositive

		// predicate nominative
		tgt.add("is");
		tgt.add("are");
		tgt.add("am");
		tgt.add("was");
		tgt.add("were");

		int m_start = m.beginIndexInclusive;
		int candidate_end = candidate.endIndexExclusive;
		return (candidate_end+2 == m_start) && tgt.contains(m.sentence.words.get(m_start-1));
	}
	
	public void headMatch(Document doc){
		// keep track of existing clusters
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		Entity matchingE, curE;
		String headWord;

		for(Mention m : doc.getMentions()){ 
			headWord = m.headWord().toLowerCase();
			curE = mentionToCM.get(m).entity;
			
			if (clusters.containsKey(headWord)) {
				matchingE = clusters.get(headWord);
				if (!sameClusters(curE, matchingE)) {
					mergeClusters(curE, matchingE);
				}
			}
			clusters.put(headWord,mentionToCM.get(m).entity);
		}    
	}
	
	public void similarHead(Document doc){
		// keep track of existing clusters
		Map<String,Mention> clusters = new HashMap<String,Mention>();
		Entity matchingE, curE;
		String headWord;
		Counter<String> headCounter;
		double maxCount;
		String bestRef;
		double countLimit = 6;

		for(Mention m : doc.getMentions()){
			headWord = m.headWord().toLowerCase();
			headCounter = coRefHeads.getCounter(headWord);
			curE = mentionToCM.get(m).entity;
			
		// find the best matching reference
			if (!headCounter.isEmpty()) {
      	// find the best matching reference
      	maxCount = countLimit;
      	bestRef = "";
      	for (String key: headCounter.keySet()) { 
      		if (clusters.containsKey(key) 
//      				&& isSameGenderAndNumber(m, clusters.get(key))
      				&& headCounter.getCount(key) > maxCount 
      				&& computeDist(m, clusters.get(key), doc) < 4) {
      			maxCount = headCounter.getCount(key);
      			bestRef = key;
      		}
      	}
      	if (maxCount > countLimit) {
      		matchingE = mentionToCM.get(clusters.get(bestRef)).entity;
  				if (!sameClusters(curE, matchingE)) {
  					//System.out.println(headWord);
  					//System.out.println(bestRef);
  					mergeClusters(curE, matchingE);
  				}
      	} 
      }
			clusters.put(headWord,m);
		}    
	}
	
	private boolean isSameNumberAndGender(Mention a, Mention b) {
		Pair<Boolean, Boolean> isSameGender = Util.haveGenderAndAreSameGender(a,b);
		Pair<Boolean, Boolean> isSameNumber = Util.haveNumberAndAreSameNumber(a,b);
		return isSameGender.getFirst() && isSameGender.getSecond() && isSameNumber.getFirst() && isSameNumber.getSecond();
	}
	
	private int computeDist(Mention a, Mention b, Document doc) {
		int indexA = doc.indexOfMention(a);
		int indexB = doc.indexOfMention(b);
		return Math.abs(indexA - indexB);
	}
	
	public void wordInclusion(Document doc){
		// keep track of existing clusters
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		Entity matchingE, curE;
		String mentionString;

		for(Mention m : doc.getMentions()){ 
			mentionString = m.gloss().toLowerCase();
			curE = mentionToCM.get(m).entity;
			for (String key: clusters.keySet()) {
				// check if one phrase contains the other phrase, skipping pronouns
				if(containsPhrase(key, mentionString)){
					matchingE = clusters.get(key);
					if (!sameClusters(curE, matchingE)) {
						mergeClusters(curE, matchingE);
					}
					break;
				} 
			}
			clusters.put(mentionString,mentionToCM.get(m).entity);
		}    
	}
	
	private boolean containsPhrase(String s1, String s2) {
		String[] words1 = s1.split(" ");
		String[] words2 = s2.split(" ");
		
		if (words1.length == words2.length) {
			return false;
		}
		String[] larger, smaller;
		if (words1.length > words2.length) {
			larger = words1;
			smaller = words2;
		} else {
			larger = words2;
			smaller = words1;
		}
		if (smaller.length < 2) return false;
		//if (Pronoun.isSomePronoun(smaller[0]) || smaller[0].equals("this") || smaller[0].equals("that")) return false;
		
		int counter = 0;
		
		for (String word: larger) {
			if (counter < smaller.length && word.equals(smaller[counter])) {
				counter++;
				if (counter == smaller.length) break;
			}
		}
		if (counter == smaller.length && (larger.length - counter) < 2) {
			//System.out.println(s1);
			//System.out.println(s2);
			return true;
		}
		
		return false;
	}
	
	
	public void closeMatch(Document doc) {
		//(variables)
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		//(for each mention...)
		for(Mention m : doc.getMentions()){
			//(...get its text)
			String mentionString = m.gloss().toLowerCase();
			// if we're within a certain edit distance of any of the keys
			if (clusters.containsKey(mentionString)) {
				mentionToCM.put(m, m.markCoreferent(clusters.get(mentionString)));
			} else {
				for (String key: clusters.keySet()) {
					//System.out.println(key);
					if(key.length() > 2 && mentionString.length() > 2 && isClose(key, mentionString, 0)){
						//(...add it to the cluster)
						mentionToCM.put(m, m.markCoreferent(clusters.get(key)));
						clusters.put(mentionString, clusters.get(key));
						break;
					} 
				}
			}
			if (!mentionToCM.containsKey(m)) {
				//(...else create a new singleton cluster)
				ClusteredMention newCluster = m.markSingleton();
				mentionToCM.put(m, newCluster);
				clusters.put(mentionString, newCluster.entity);
			}
		}
	}
	
	// check that two strings are within 1 edit distance of each other
	private boolean isClose(String s1, String s2, int dist) {
		if (dist >= 3) {
			return false;
		} 
		if (s1.length() == 0) {
			if (dist + s2.length() < 2) {
				return true;
			} else {
				return false;
			}
		}
		if (s2.length() == 0) {
			if (dist + s1.length() < 2) {
				return true;
			} else {
				return false;
			}
		}

		int diff = 1;
		if (s1.substring(0, 1).equals(s2.substring(0, 1))) {
			diff = 0;
		}
		
		return isClose(s1.substring(1), s2, dist+1) || isClose(s1, s2.substring(1), dist+1) || isClose(s1.substring(1), s2.substring(1), dist+diff);
	}
	
	private ClusteredMention moveCluster(ClusteredMention cm, Entity cluster) {
		Mention m = cm.mention;
		Entity e = cm.entity;
		e.remove(m);
		m.removeCoreference();
		return m.markCoreferent(cluster);
	}

	private boolean sameClusters(Entity e1, Entity e2) {
		if (e1.size() != e2.size()) {
			return false;
		}
		for (Mention m: e1.mentions) {
			if (!e2.mentions.contains(m)) {
				return false;
			}
		}
		return true;
	}

	private void mergeClusters(Entity e1, Entity e2) {
		List<Mention> move = new ArrayList<Mention>();
		for (Mention m: e1.mentions) {
			move.add(m);
		}
		for (Mention m: move) {
			mentionToCM.put(m, moveCluster(mentionToCM.get(m), e2));
		}
	}
	
	private boolean isPronoun(String s) {
		if (Pronoun.isSomePronoun(s)) return true;
		return false;
	}

}