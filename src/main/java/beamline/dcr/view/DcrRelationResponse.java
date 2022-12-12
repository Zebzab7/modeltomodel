package beamline.dcr.view;

import beamline.graphviz.DotEdge;
import beamline.graphviz.DotNode;

public class DcrRelationResponse extends DotEdge {

	public DcrRelationResponse(DotNode source, DotNode target) {
		super(source, target);
		
		setOption("color", "#2993FC");
		setOption("arrowhead", "normal");
		setOption("arrowtail", "dot");
		setOption("dir", "both");
		setOption("arrowsize", "0.5");
		setOption("headlabel", "");
	}

}
