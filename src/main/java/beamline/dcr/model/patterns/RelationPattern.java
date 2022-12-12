package beamline.dcr.model.patterns;

import beamline.dcr.model.relations.UnionRelationSet;

public interface RelationPattern {
    public void populateConstraint(UnionRelationSet unionRelationSet);

}
