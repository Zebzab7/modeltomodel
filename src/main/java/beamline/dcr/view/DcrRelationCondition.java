package beamline.dcr.view;

import beamline.graphviz.DotEdge;
import beamline.graphviz.DotNode;

public class DcrRelationCondition extends DotEdge {

	public DcrRelationCondition(DotNode source, DotNode target) {
		super(source, target);
		
		setOption("color", "#FFA500");
		setOption("arrowhead", "dotnormal");
		setOption("arrowsize", "0.5");
		setOption("headlabel", "");
	}

}
