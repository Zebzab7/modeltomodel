package beamline.dcr.model.patterns;

import beamline.dcr.annotations.ExposedDcrPattern;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.UnionRelationSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Set;

@ExposedDcrPattern(
        name = "Condition",
        dependencies = {}
        )
public class Condition implements RelationPattern {

    @Override
    public void populateConstraint(UnionRelationSet unionRelationSet) {
        Set<Pair<String,String>> dfgRelations = unionRelationSet.getDFGRelations();

        for (Pair<String,String> relation : dfgRelations){

            String source = relation.getLeft();
            double avgFOSource =
                    unionRelationSet.getActivityDecoration(source).getAverageFirstOccurrence();
            double numTraceAppearancesSource = unionRelationSet.getActivityDecoration(source).getTraceAppearances();
            String target = relation.getRight();
            double avgFOTarget =
                    unionRelationSet.getActivityDecoration(target).getAverageFirstOccurrence();
            double numTraceAppearancesTarget = unionRelationSet.getActivityDecoration(target).getTraceAppearances();

            if(avgFOSource<avgFOTarget && numTraceAppearancesSource >= numTraceAppearancesTarget){
                unionRelationSet.addDcrRelation(Triple.of(source, target, DcrModel.RELATION.CONDITION));
            }

        }
    }

    //For framework setting test
    public void populateConstraint(UnionRelationSet unionRelationSet,Set<Integer> parameterCombination) {
        Set<Pair<String,String>> dfgRelations = unionRelationSet.getDFGRelations();

        for (Pair<String,String> relation : dfgRelations){
            String source = relation.getLeft();
            double avgFOSource =
                    unionRelationSet.getActivityDecoration(source).getAverageFirstOccurrence();
            double numTraceAppearancesSource = unionRelationSet.getActivityDecoration(source).getTraceAppearances();
            String target = relation.getRight();
            double avgFOTarget =
                    unionRelationSet.getActivityDecoration(target).getAverageFirstOccurrence();
            double numTraceAppearancesTarget = unionRelationSet.getActivityDecoration(target).getTraceAppearances();

            boolean isCondition = true;
            if(parameterCombination.contains(1)){
                isCondition = avgFOSource<avgFOTarget;
                if(parameterCombination.contains(2) && isCondition ){
                    isCondition = numTraceAppearancesSource >= numTraceAppearancesTarget;
                }
            }

            if(isCondition){
                unionRelationSet.addDcrRelation(Triple.of(source, target, DcrModel.RELATION.CONDITION));
            }

        }
    }
}
