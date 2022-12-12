package beamline.dcr.view;

import beamline.graphviz.DotNode;

public class DcrActivity extends DotNode {

	protected DcrActivity(String label) {
		super(label, null);
		
		setOption("fontname", "arial");
		setOption("fontsize", "10");
		setOption("fillcolor", "#F8F6EC");
		setOption("shape", "rec");
		setOption("style", "filled");
		setOption("color", "#CBCBCB");
	}
	
	@Override
	public int hashCode() {
		return getLabel().hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return getLabel().equals(object);
	}
}
