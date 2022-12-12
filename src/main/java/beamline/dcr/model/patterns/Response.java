package beamline.dcr.model.patterns;

import beamline.dcr.annotations.ExposedDcrPattern;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.UnionRelationSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Set;

@ExposedDcrPattern(
        name = "Response",
        dependencies = {}
)
public class Response implements RelationPattern {

    @Override
    public void populateConstraint(UnionRelationSet unionRelationSet) {

        Set<Pair<String, String>> dfgRelations = unionRelationSet.getDFGRelations();
        for (Pair<String, String> relation : dfgRelations) {
            String source = relation.getLeft();
            double avgIndexSource =
                    unionRelationSet.getActivityDecoration(source).getAverageIndex();

            String target = relation.getRight();
            double avgIndexTarget =
                    unionRelationSet.getActivityDecoration(target).getAverageIndex();

            if (avgIndexSource < avgIndexTarget) {
                unionRelationSet.addDcrRelation(Triple.of(source, target, DcrModel.RELATION.RESPONSE));
            }

        }
    }
}
