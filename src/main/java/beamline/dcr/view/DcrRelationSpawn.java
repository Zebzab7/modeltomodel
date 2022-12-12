package beamline.dcr.view;

import beamline.graphviz.DotEdge;
import beamline.graphviz.DotNode;

public class DcrRelationSpawn extends DotEdge {

	public DcrRelationSpawn(DotNode source, DotNode target) {
		super(source, target);
		
		setOption("color", "#334960");
		setOption("arrowhead", "normal");
		setOption("arrowtail", "none");
		setOption("arrowsize", "0.5");
		setOption("headlabel", "<<font color='#334960' point-size='10'>*</font>>");
	}

}
