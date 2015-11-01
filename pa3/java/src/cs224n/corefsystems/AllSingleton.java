package cs224n.corefsystems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.util.Pair;

public class AllSingleton implements CoreferenceSystem {

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		// Nothing to do here 

	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
		// iterate through all mentions
		for(Mention m : doc.getMentions()){
			// We mark each mention as its own entity
			ClusteredMention newCluster = m.markSingleton();
      mentions.add(newCluster);
		}
		return mentions;
	}

}
