package modeltomodel;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.DcrModel.RELATION;

public class GED {
    
    /*
     * Initial (ugly version) of test for GED
     */
    public static void main(String[] args) {
        
        DcrModel dcr1 = new DcrModel();
        DcrModel dcr2 = new DcrModel();
        
        Set<Triple<String, String, DcrModel.RELATION>> dcrRelations1 = new HashSet<>();

        dcrRelations1.add(Triple.of("Start", "Activity 3", DcrModel.RELATION.CONDITION));
        dcrRelations1.add(Triple.of("Start", "Activity 2", DcrModel.RELATION.RESPONSE));
        dcrRelations1.add(Triple.of("Activity 3", "Activity 2", DcrModel.RELATION.RESPONSE));
        dcrRelations1.add(Triple.of("Activity 2", "Activity 4", DcrModel.RELATION.INCLUDE));
        dcrRelations1.add(Triple.of("Activity 4", "Activity 4", DcrModel.RELATION.EXCLUDE));
        dcrRelations1.add(Triple.of("Activity 4", "Activity 7", DcrModel.RELATION.RESPONSE));
        dcrRelations1.add(Triple.of("Activity 4", "Activity 7", DcrModel.RELATION.INCLUDE));
        dcrRelations1.add(Triple.of("Start", "Activity 4", DcrModel.RELATION.EXCLUDE));
        dcrRelations1.add(Triple.of("Activity 4", "Activity 5", DcrModel.RELATION.CONDITION));
        dcrRelations1.add(Triple.of("Activity 4", "Activity 6", DcrModel.RELATION.INCLUDE));

        dcr1.addRelations(dcrRelations1);
        
        Set<Triple<String, String, DcrModel.RELATION>> dcrRelations2 = new HashSet<>();
        
        dcrRelations2.add(Triple.of("Start", "Activity 3", DcrModel.RELATION.CONDITION));
        dcrRelations2.add(Triple.of("Start", "Activity 2", DcrModel.RELATION.RESPONSE));
        dcrRelations2.add(Triple.of("Activity 3", "Activity 2", DcrModel.RELATION.RESPONSE));
        
        dcrRelations2.add(Triple.of("Activity 2", "Activity 4", DcrModel.RELATION.INCLUDE));
        dcrRelations2.add(Triple.of("Activity 4", "Activity 4", DcrModel.RELATION.EXCLUDE));
        dcrRelations2.add(Triple.of("Start", "Activity 4", DcrModel.RELATION.EXCLUDE));
        dcrRelations2.add(Triple.of("Activity 4", "Activity 5", DcrModel.RELATION.CONDITION));
        dcrRelations2.add(Triple.of("Activity 4", "Activity 6", DcrModel.RELATION.INCLUDE));

        dcr2.addRelations(dcrRelations2);
        
        System.out.println(graphEditDistanceSimilarity(dcr1, dcr2));
    }
    
    /*
     * Calculates the Graph edit distance similarity between two DCR models
     */
    public static double graphEditDistanceSimilarity(DcrModel modelA, DcrModel modelB) {
        Set<String> actA = modelA.getActivities();
        Set<String> actB = modelB.getActivities();
        Set<Triple<String, String, RELATION>> relationsA = modelA.getRelations();
        Set<Triple<String, String, RELATION>> relationsB = modelB.getRelations();
        
        Set<String> activitiesDiff = symmetricDifference(actA, actB);
        Set<Triple<String, String, RELATION>> relationsDiff 
            = symmetricDifference(relationsA, relationsB);
        
        double dist = activitiesDiff.size() + relationsDiff.size();
        
        double sim = dist/(actA.size() + actB.size() + relationsA.size() + relationsB.size());
        
        return 1-sim;
    }
    
    /*
     * Returns a set which representing the symmetric difference between two sets
     */
    public static <T> Set<T> symmetricDifference(Set<T> s1, Set<T> s2) {
        Set<T> symmetricDiff = new HashSet<T>(s1);
        symmetricDiff.addAll(s2);
        Set<T> tmp = new HashSet<T>(s1);
        tmp.retainAll(s2);
        symmetricDiff.removeAll(tmp);
        return symmetricDiff;
    }
    
}
