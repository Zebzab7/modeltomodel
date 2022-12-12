package beamline.dcr.model.patterns;

import beamline.dcr.annotations.ExposedDcrPattern;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.UnionRelationSet;
import beamline.dcr.model.relations.dfg.ActivityDecoration;
import org.apache.commons.lang3.tuple.Triple;

@ExposedDcrPattern(
        name = "Sequence",
        dependencies = {}
)
public class Sequence implements RelationPattern {

    @Override
    public void populateConstraint(UnionRelationSet unionRelationSet) {
        String[] listOfActivities = unionRelationSet.getUniqueActivities();

        for (int i = 0; i<listOfActivities.length; i++) {
            for (int j = i + 1; j < listOfActivities.length; j++) {
                String activity1 = listOfActivities[i];
                ActivityDecoration decoration1 = unionRelationSet.getActivityDecoration(activity1);
                String activity2 = listOfActivities[j];
                ActivityDecoration decoration2 = unionRelationSet.getActivityDecoration(activity2);

                if (decoration1.getAverageFirstOccurrence() < decoration2.getAverageFirstOccurrence() &
                        decoration1.getAverageIndex() < decoration2.getAverageIndex()) {
                    unionRelationSet.addDcrRelation(Triple.of(activity1, activity2, DcrModel.RELATION.SEQUENCE));
                }
                if (decoration1.getAverageFirstOccurrence() > decoration2.getAverageFirstOccurrence() &
                        decoration1.getAverageIndex() > decoration2.getAverageIndex()) {
                    unionRelationSet.addDcrRelation(Triple.of(activity2, activity1, DcrModel.RELATION.SEQUENCE));
                }
            }
        }


    }

}
