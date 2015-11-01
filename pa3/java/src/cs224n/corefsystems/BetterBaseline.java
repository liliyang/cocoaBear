package cs224n.corefsystems;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import cs224n.coref.*;
import cs224n.util.*;

public class BetterBaseline implements CoreferenceSystem {
	
	// create a map to store the heads of co-reference mention pairs
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
        	coRefHeads.incrementCount(head1, head2, 1);
        }
      }
    }

	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		
		// initialize list of ClusterMention
    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
    // keep track of existing clusters
    Map<String,Entity> clusters = new HashMap<String,Entity>();
    
    String mentionHead;
    Counter<String> headCounter;
    double maxCount;
    String bestRef;
    // boolean to track if we've found a matching cluster
    boolean foundCluster;
    
    // iterate through mentions
    for(Mention m : doc.getMentions()){
    	foundCluster = false;
    	
      // get the head word
      mentionHead = m.headWord().toLowerCase();
      headCounter = coRefHeads.getCounter(mentionHead);
      
      // if we've seen the same head word before
      if (clusters.containsKey(mentionHead)) {
        mentions.add(m.markCoreferent(clusters.get(mentionHead)));
        foundCluster = true;
      // otherwise if the head word has a coreference in our training set
      } else if (!headCounter.isEmpty()) {
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
      		mentions.add(m.markCoreferent(clusters.get(bestRef)));
  				foundCluster = true;
      	}
      }
      // else create a new cluster 
      if (!foundCluster) {
      	ClusteredMention newCluster = m.markSingleton();
      	mentions.add(newCluster);
      	clusters.put(mentionHead,newCluster.entity);
      }
    }

    return mentions;
	}
}
