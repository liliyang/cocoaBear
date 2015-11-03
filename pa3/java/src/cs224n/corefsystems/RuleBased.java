package cs224n.corefsystems;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Gender;
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
		// TODO Auto-generated method stub
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
					coRefHeads.incrementCount(head1, head2, 1);
				}
			}
		}

	}

	private Map<Mention,ClusteredMention> mentionToCM;
	@Override
	public List<ClusteredMention> runCoreference(Document doc) {

		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
		mentionToCM = new HashMap<Mention,ClusteredMention>();
		//layer 1:
		//		BaselineCoreferenceSystem exactMatch = new BaselineCoreferenceSystem();
		exactMatch(doc);

		//layer 2: Precise Construct
		preciseConstruct(doc);
		//		acronymMatch(doc);
		//layer 3: head Match
		headMatch(doc, 3);


		//layer 5: head Match
		headMatch(doc, 5);

		// pass 6: relaxedHeadMatch
		//				relaxedHeadMatch(doc); //not helping
		
		// pass 7:
//		pronoun(doc);

		for (ClusteredMention cm: mentionToCM.values()) {
			mentions.add(cm);
		}
		return mentions;
	}
	  
	public boolean sameTag(Mention a, Mention b){
		Pair<Boolean,Boolean> gen, num;		
		
	    num = Util.haveNumberAndAreSameNumber(a, b);
	    if (num.getFirst() && !num.getSecond())//T F
	    	return false;
	    
	    gen = Util.haveGenderAndAreSameGender(a, b);
	    if (gen.getFirst() && !gen.getSecond())//T F
	    	return false;

	    if (Pronoun.isSomePronoun(a.headWord()) 
	    		&& Pronoun.isSomePronoun(b.headWord()
	    	      //&& not quoted	    				
	    				)){

	    	Pronoun a_pron = Pronoun.getPronoun(a.gloss());
	    	Pronoun b_pron = Pronoun.getPronoun(b.gloss());
	    	if(a_pron!=null && b_pron!= null){
	    		
	    		 Speaker a_speaker = a_pron.speaker;
	    		 Speaker b_speaker = b_pron.speaker;
	    		 if (a_speaker != null && b_speaker != null){
//	    			 System.out.println("both have speakers");
	    			 if (!a_speaker.equals(b_speaker))
	    				 
	                     return false;
	    		 }
	    	}
	    }
	    
//	    if (!a.sentence.nerTags.get(a.headWordIndex).equals("PERSON")
//	    		|| !b.sentence.nerTags.get(b.headWordIndex).equals("PERSON")
//	    		)
//	    	return false;
	    
	    if (!a.sentence.nerTags.equals(b.sentence.nerTags))
	    	return false;

		return true;
	}
	public boolean diff(Integer a, Integer b){
		if (a == 0 || b==0)
			return false;
		return !a.equals(b);
	}
	public void pronoun(Document doc){
		// keep track of existing clusters
		Entity  curE, matchingE;
	
		for(int i=0; i<doc.getMentions().size(); i++){
			Mention m = doc.getMentions().get(i);
			
			for (int j = 0; j<i; j++){
				Mention candidate = doc.getMentions().get(j);
				curE = mentionToCM.get(m).entity;
				matchingE = mentionToCM.get(candidate).entity;
				if (!sameClusters(curE, matchingE) && m.sentence.equals(candidate.sentence)) {
					if (sameTag(m,candidate)){					
						mergeClusters(curE, matchingE);
					}
				}
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
			int m_start, candidate_end;
			for (int i=list.size()-1; i>0; i--){
				candidate = list.get(i-1);
				m = list.get(i);
				
				Entity curE = mentionToCM.get(m).entity;
				Entity matchingE = mentionToCM.get(candidate).entity;
					
					
				if (
						preciseHelper( m, candidate)
						&& (/*m.sentence.nerTags.contains("NER")&& */ hasGender(candidate))// role appositive: not helping
//						&& (isRelativePron(m) && (candidate.headWordIndex < m.headWordIndex) && (m.endIndexExclusive <= candidate.endIndexExclusive)) // relative pronnot helping
//						&& acronym(m.gloss()).equals(acronym(candidate.gloss())) //worsen
						&& !sameClusters(curE, matchingE)
						){	mergeClusters(mentionToCM.get(m).entity, mentionToCM.get(candidate).entity);}		
			}
		}
	}
	public boolean isNer(Mention m){
		for (Tree<String> tree: m.parse.getPostOrderTraversal()){
			if (tree.getLabel().contains("NER")){
				System.out.println("find NER");
				return true;
			}
		}
		return  false;//m.parse.getLabel().contains("NER"); //a
		
	}
	public String acronym(String ext){
		String trueAcr = "";
		for (Character c: ext.toCharArray()){
			if (Character.isUpperCase(c))
				trueAcr += c;
		}
		return trueAcr;
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

	public boolean isRelativePron(Mention m){
		Set<String> relativePron = new HashSet<String>();
		relativePron.add("which");
		relativePron.add("where");
		relativePron.add("that");
		relativePron.add("what");
		relativePron.add("who");
		relativePron.add("whom");
		boolean result =  relativePron.contains(m.headWord());
				if (result) 
					System.out.println("found relative pron: "+m.gloss());
		return result;
	}

	public void headMatch(Document doc, int layer){
		// keep track of existing clusters
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		Entity matchingE, curE;
		String headWord;
		boolean cluster_Head_Match, word_Inclusion;
		for(Mention m : doc.getMentions()){ 
			headWord = m.headWord();
			curE = mentionToCM.get(m).entity;
			cluster_Head_Match = clusters.containsKey(headWord);

			if (cluster_Head_Match) {
				matchingE = clusters.get(headWord);

				boolean det = !sameClusters(curE, matchingE);
				if (layer == 3){
					det = det && wordInclusion(curE, matchingE);
				}
				if (det) {
		
					mergeClusters(curE, matchingE);
					//	    			}
				}
			} else {
				clusters.put(headWord,curE);
			}
		}    
	}

	public void relaxedHeadMatch(Document doc){
		// keep track of existing clusters
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		Entity matchingE, curE;
		String headWord;
		boolean cluster_Head_Match, word_Inclusion;
		String mentionHead;
		Counter<String> headCounter;
		double maxCount;
		String bestRef;

		for(Mention m : doc.getMentions()){ 
			headWord = m.headWord();
			curE = mentionToCM.get(m).entity;
			cluster_Head_Match = clusters.containsKey(headWord);

			// get the head word
			mentionHead = m.headWord().toLowerCase();
			headCounter = coRefHeads.getCounter(mentionHead);

			if (!headCounter.isEmpty() ) {
				// find the best matching reference
				maxCount = 0;
				bestRef = "";
				for (String key: headCounter.keySet()) {
					if (clusters.containsKey(key) && headCounter.getCount(key) > maxCount ) {
						maxCount = headCounter.getCount(key);
						bestRef = key;
					}
				}
				if (maxCount > 0) {
					//	          		mentions.add(m.markCoreferent(clusters.get(bestRef)));
					mergeClusters(curE, clusters.get(bestRef));
				}
			}
			else {
				clusters.put(headWord,curE);
			}
		}

	}

	public boolean iWithinI(Entity curE, Entity matchingE){
		for (Mention m: curE){
			for (Mention candidate: matchingE.mentions){
				if (candidate.beginIndexInclusive <= m.beginIndexInclusive
						&& m.endIndexExclusive <= candidate.endIndexExclusive)
					return true;
			}
		}
		return false;
	}

	private boolean wordInclusion(Entity small, Entity big){
		Set<String> smallSet = flattenEntity(small);
		Set<String> bigSet = flattenEntity(big);
		return bigSet.containsAll(smallSet);
	}
	private Set<String> flattenEntity(Entity e){
		Set<String> set = new HashSet<String>();
		for (Mention m: e.mentions){
			
			set.addAll(m.text());
		}
		return set;
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

	public void exactMatch(Document doc) {
		//(variables)
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		//(for each mention...)
		for(Mention m : doc.getMentions()){
			//(...get its text)
			String mentionString = m.gloss();
			//(...if we've seen this text before...)
			if(clusters.containsKey(mentionString)){
				//(...add it to the cluster)
				mentionToCM.put(m, m.markCoreferent(clusters.get(mentionString)));
			} else {
				//(...else create a new singleton cluster)
				ClusteredMention newCluster = m.markSingleton();
				mentionToCM.put(m, newCluster);
				clusters.put(mentionString,newCluster.entity);
			}
		}
	}

}