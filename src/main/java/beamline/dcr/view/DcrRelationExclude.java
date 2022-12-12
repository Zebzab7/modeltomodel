package beamline.dcr.view;

import beamline.graphviz.DotEdge;
import beamline.graphviz.DotNode;

public class DcrRelationExclude extends DotEdge {

	public DcrRelationExclude(DotNode source, DotNode target) {
		super(source, target);
		
		setOption("color", "#FC0C1B");
		setOption("arrowhead", "normal");
		setOption("arrowtail", "none");
		setOption("arrowsize", "0.5");
		setOption("headlabel", "<<font color='#FC0C1B' point-size='8'>%</font>>");
	}

}
