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
		// Nothing for now
	}

	// map to keep track of most recent mapping of Mention to ClusteredMention
	private Map<Mention,ClusteredMention> mentionToCM;
	@Override
	public List<ClusteredMention> runCoreference(Document doc) {

		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
		mentionToCM = new HashMap<Mention,ClusteredMention>();
		//layer 1: exact text match
		exactMatch(doc);

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
			String mentionString = m.gloss();
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

}