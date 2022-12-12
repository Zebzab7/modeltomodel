package beamline.dcr.view;

import beamline.graphviz.DotEdge;
import beamline.graphviz.DotNode;

public class DcrRelationInclude extends DotEdge {

	public DcrRelationInclude(DotNode source, DotNode target) {
		super(source, target);
		
		setOption("color", "#30A627");
		setOption("arrowhead", "normal");
		setOption("arrowtail", "none");
		setOption("arrowsize", "0.5");
		setOption("headlabel", "<<font color='#30A627' point-size='12'>+</font>>");
	}

}
