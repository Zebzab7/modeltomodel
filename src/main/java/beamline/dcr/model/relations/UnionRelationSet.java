package beamline.dcr.model.relations;

import beamline.dcr.model.relations.dfg.ActivityDecoration;
import beamline.dcr.model.relations.dfg.ExtendedDFG;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class UnionRelationSet {

    private final Integer threshold;
    private ExtendedDFG extendedDFG;
    private Set<Triple<String,String, DcrModel.RELATION>> DcrRelations =  new HashSet<>();

    public UnionRelationSet(ExtendedDFG extendedDFG, Integer threshold){
        this.extendedDFG = extendedDFG;
        this.threshold = threshold;
    }
    public Set<Pair<String, String>> getDFGRelations(){
        return extendedDFG.getRelationsAboveThreshold(threshold);
    }

    public ActivityDecoration getActivityDecoration(String activity){
        return extendedDFG.getActivityDecoration(activity);
    }

    public Set<Triple<String, String, DcrModel.RELATION>> getDcrRelations() {
        return DcrRelations;
    }

    public void addDcrRelation(Triple<String,String, DcrModel.RELATION> relation){
        DcrRelations.add(relation);
    }

    public void removeDcrRelation(Triple<String,String, DcrModel.RELATION> relation){
        DcrRelations.remove(relation);
    }


    public Set<Triple<String, String, DcrModel.RELATION>> getDcrRelationWithConstraint(DcrModel.RELATION constraint){
        return DcrRelations.stream()
                .filter(entry -> entry.getRight() == constraint)
                .collect(Collectors.toSet());
    }

    public Set<Triple<String, String, DcrModel.RELATION>> getDcrRelationWithTarget(String target, DcrModel.RELATION relation){
        return DcrRelations.stream()
                .filter(entry -> entry.getMiddle().equals(target) && entry.getRight() == relation)
                .collect(Collectors.toSet());
    }
    public Set<Triple<String, String, DcrModel.RELATION>> getDcrRelationWithSource(String source, DcrModel.RELATION relation){
        return DcrRelations.stream()
                .filter(entry -> entry.getLeft().equals(source) && entry.getRight() == relation)
                .collect(Collectors.toSet());
    }

    public String[] getUniqueActivities(){
        return extendedDFG.getActivities().toArray(new String[0]);
    }
}
