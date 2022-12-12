package beamline.dcr.view;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.DcrModel.RELATION;
import beamline.graphviz.Dot;
import beamline.graphviz.DotEdge;
import beamline.graphviz.DotNode;

public class DcrModelView extends Dot {

	private DcrModel model;
	Map<String, DotNode> activityToNode;
	
	public DcrModelView(DcrModel model) {
		this.model = model;
		this.activityToNode = new HashMap<String, DotNode>();
		
		realize();
	}
	
	private void realize() {
		for(Triple<String, String, RELATION> r : model.getRelations()) {
			addRelation(r.getLeft(), r.getMiddle(), r.getRight());
		}
	}
	
	public DotNode getNodeIfNeeded(String activity) {
		if (!activityToNode.containsKey(activity)) {
			DcrActivity node = new DcrActivity(activity);
			addNode(node);
			activityToNode.put(activity, node);
		}
		return activityToNode.get(activity);
	}
	
	public void addRelation(String source, String target, RELATION relation) {
		DotNode sourceNode = getNodeIfNeeded(source);
		DotNode targetNode = getNodeIfNeeded(target);
		
		DotEdge edge = null;
		switch (relation) {
			case CONDITION:
				edge = new DcrRelationCondition(sourceNode, targetNode);
				break;
			case RESPONSE:
				edge = new DcrRelationResponse(sourceNode, targetNode);
				break;
			case INCLUDE:
				edge = new DcrRelationInclude(sourceNode, targetNode);
				break;
			case EXCLUDE:
				edge = new DcrRelationExclude(sourceNode, targetNode);
				break;
			case SPAWN:
				edge = new DcrRelationSpawn(sourceNode, targetNode);
				break;
			case MILESTONE:
				edge = new DcrRelationMilestone(sourceNode, targetNode);
				break;
		}
		if (edge != null) {
			addEdge(edge);
		}
	}
}
