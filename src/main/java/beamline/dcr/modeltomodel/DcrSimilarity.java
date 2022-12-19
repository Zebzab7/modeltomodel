package beamline.dcr.modeltomodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.DcrModel.RELATION;
import beamline.dcr.testsoftware.ModelComparison;

public class DcrSimilarity {
    
    public static Map<DcrModel.RELATION, Double> constraintWeight = new HashMap<>(){{
        put(DcrModel.RELATION.CONDITION, 0.15);
        put(DcrModel.RELATION.RESPONSE, 0.05);
        put(DcrModel.RELATION.PRECONDITION, 0.0);
        put(DcrModel.RELATION.MILESTONE, 0.0);
        put(DcrModel.RELATION.INCLUDE, 0.0);
        put(DcrModel.RELATION.EXCLUDE, 0.0);
        put(DcrModel.RELATION.NORESPONSE, 0.0);
        put(DcrModel.RELATION.SPAWN, 0.0);
        put(DcrModel.RELATION.SEQUENCE, 0.0);
    }};
    
    public static double jaccardSimilarity(DcrModel modelA, DcrModel modelB) {
        ModelComparison modelComparison = new ModelComparison(modelA);
        modelComparison.loadComparativeModel(modelB);
        return modelComparison.getJaccardSimilarity();
    }
    
    /*
     * Returns similarity based on common nodes and edges of graphs
     */
    public static double commonNodesAndEdgesSimilarity(DcrModel modelA, DcrModel modelB) {
        Set<String> actA = modelA.getActivities();
        Set<String> actB = modelB.getActivities();
        Set<Triple<String, String, RELATION>> relationsA = modelA.getRelations();
        Set<Triple<String, String, RELATION>> relationsB = modelB.getRelations();
        
        
        double num = 2*(intersection(actA,actB).size() + intersection(relationsA,relationsB).size());
        double denom = (actA.size() + actB.size() + relationsA.size() + relationsB.size());
        
        return num/denom;
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
     * 
     */
    public static double graphEditDistanceSimilarityWithWeights(DcrModel modelA, DcrModel modelB) {
        Set<String> actA = modelA.getActivities();
        Set<String> actB = modelB.getActivities();
        Set<Triple<String, String, RELATION>> relationsA = modelA.getRelations();
        Set<Triple<String, String, RELATION>> relationsB = modelB.getRelations();
        
        Set<String> activitiesDiff = symmetricDifference(actA, actB);
        Set<Triple<String, String, RELATION>> relationsDiff 
            = symmetricDifference(relationsA, relationsB);
        
        double relationsDiffDist = 0.0;
        for (Triple<String,String,RELATION> relation : relationsDiff) {
            relationsDiffDist *= constraintWeight.get(relation.getRight());
        }
        
        Set<Triple<String,String,RELATION>> union = union(relationsA,relationsB);
        double relationsDist = 0.0;
        for (Triple<String,String,RELATION> relation : union) {
            relationsDist *= constraintWeight.get(relation.getRight());
        }
        
        double dist = activitiesDiff.size() + relationsDiffDist;
        double sim = dist/(actA.size() + actB.size() + relationsDist);
        
        return 1-sim;
    }
    
    /**
     * Helper functions
     */
    
    /*
     * Returns a set repsententing the union between two sets
     */
    public static <T> Set<T> union(Set<T> set1, Set<T> set2) {
        Set<T> union = new HashSet<T>(set1);
        union.retainAll(set2);
        return union;
    }
    
    /*
     * Returns a set representing the intersection between two sets
     */
    public static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        Set<T> intersect = new HashSet<T>(set1);
        intersect.retainAll(set2);
        return intersect;
    }
    
    /*
     * Returns a set which representing the symmetric difference between two sets.
     * Does not modify the input sets
     */
    public static <T> Set<T> symmetricDifference(Set<T> set1, Set<T> set2) {
        Set<T> symmetricDiff = new HashSet<T>(set1);
        symmetricDiff.addAll(set2);
        Set<T> tmp = new HashSet<T>(set1);
        tmp.retainAll(set2);
        symmetricDiff.removeAll(tmp);
        return symmetricDiff;
    }
    
}
