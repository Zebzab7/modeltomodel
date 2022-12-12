package beamline.dcr.view;

import beamline.graphviz.DotEdge;
import beamline.graphviz.DotNode;

public class DcrRelationMilestone extends DotEdge {

	public DcrRelationMilestone(DotNode source, DotNode target) {
		super(source, target);
		
		setOption("color", "#BC1AF2");
		setOption("arrowhead", "odiamondnormal");
		setOption("arrowtail", "none");
		setOption("arrowsize", "0.5");
		setOption("headlabel", "");
	}

}
