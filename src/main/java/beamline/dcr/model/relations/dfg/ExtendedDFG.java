package beamline.dcr.model.relations.dfg;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class ExtendedDFG {

	private Map<String, ActivityDecoration> activities;
	private Map<Pair<String, String>, RelationDecoration> relations;
	
	public ExtendedDFG() {
		this.activities = new HashMap<String, ActivityDecoration>();
		this.relations = new HashMap<Pair<String, String>, RelationDecoration>();
	}
	
	public ActivityDecoration addActivityIfNeeded(String activity) {
		if (activities.containsKey(activity)) {
			return activities.get(activity);
		}
		ActivityDecoration decoration = new ActivityDecoration();
		activities.put(activity, decoration);
		return decoration;
	}
	
	public RelationDecoration addRelationIfNeeded(String source, String sink) {
		Pair<String, String> relation = new ImmutablePair<String, String>(source, sink);
		if (relations.containsKey(relation)) {
			return relations.get(relation);
		}
		RelationDecoration decoration = new RelationDecoration();
		relations.put(relation, decoration);
		return decoration;
	}
	public RelationDecoration getRelation(Pair<String, String> relationPair){
		return relations.get(relationPair);
	}
	public void decrementRelationFrequency(Pair<String, String> relationPair){
		RelationDecoration relationDecoration = relations.get(relationPair);
		int frequency = relationDecoration.getFrequency();
		if(frequency<=1){
			relations.remove(relationPair);
		}else{
			relationDecoration.decrementFrequency();
		}
	}
	public Set<String> getActivities() {
		return activities.keySet();
	}

	public Set<Pair<String, String>> getRelations() {
		return relations.keySet();
	}

	public Set<Pair<String, String>> getRelationsAboveThreshold(int threshold){
		return relations.entrySet().stream()
				.filter(entry -> entry.getValue().getFrequency() > threshold)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

	}

	public ActivityDecoration getActivityDecoration(String activity) {
		return this.activities.get(activity);
	}

	public RelationDecoration getRelationDecoration(Pair<String, String> relation) {
		return this.relations.get(relation);
	}
	
	public Set<String> getIncomingActivities(String candidate) {
		Set<String> result = new HashSet<String>();
		for (Pair<String, String> relation : getRelations()) {
			if (relation.getRight().equals(candidate)) {
				result.add(relation.getLeft());
			}
		}
		return result;
	}

	public Set<String> getOutgoingActivities(String candidate) {
		Set<String> result = new HashSet<String>();
		for (Pair<String, String> relation : getRelations()) {
			if (relation.getLeft().equals(candidate)) {
				result.add(relation.getRight());
			}
		}
		return result;
	}

	public boolean isStartActivity(String candidate) {
		return getIncomingActivities(candidate).size() == 0;
	}

	public boolean isEndActivity(String candidate) {
		return getOutgoingActivities(candidate).size() == 0;
	}

	public boolean isIsolatedNode(String candidate) {
		return getOutgoingActivities(candidate).equals(getIncomingActivities(candidate));
	}


	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		
		b.append("ACTIVITIES");
		b.append(System.lineSeparator());
		
		for (String activity : activities.keySet()) {
			b.append(activity + " - " + activities.get(activity));
			b.append(System.lineSeparator());
		}
		
		b.append("RELATIONS");
		b.append(System.lineSeparator());
		for (Pair<String, String> relation : relations.keySet()) {
			b.append(relation + " - " + relations.get(relation));
			b.append(System.lineSeparator());
		}
		
		return b.toString();
	}
}
