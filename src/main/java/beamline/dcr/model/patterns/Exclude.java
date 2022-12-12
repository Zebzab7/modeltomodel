package beamline.dcr.model.patterns;

import beamline.dcr.annotations.ExposedDcrPattern;
import beamline.dcr.model.relations.UnionRelationSet;

@ExposedDcrPattern(
        name = "Exclude",
        dependencies = {"Sequence"}
)
public class Exclude  implements RelationPattern{
    @Override
    public void populateConstraint(UnionRelationSet unionRelationSet) {
        new ExcludeAndInclude().populateConstraint(unionRelationSet);
    }
}
