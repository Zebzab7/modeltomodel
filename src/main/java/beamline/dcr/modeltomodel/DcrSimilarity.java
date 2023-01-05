package beamline.dcr.modeltomodel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.tuple.Triple;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.DcrModel.RELATION;
import beamline.dcr.modeltomodel.testrunners.TraceGenerator;
import beamline.dcr.testsoftware.ModelAdaption;
import beamline.dcr.testsoftware.ModelComparison;

public class DcrSimilarity {
    
    static int traceLength = 10;
    static double rMax = 1000;
    static double rMin = 0;
    
    public static Map<DcrModel.RELATION, Double> constraintWeight = new HashMap<>(){{
        put(DcrModel.RELATION.CONDITION, 0.15);
        put(DcrModel.RELATION.RESPONSE, 0.15);
        put(DcrModel.RELATION.PRECONDITION, 0.0);
        put(DcrModel.RELATION.MILESTONE, 0.0);
        put(DcrModel.RELATION.INCLUDE, 0.15);
        put(DcrModel.RELATION.EXCLUDE, 0.15);
        put(DcrModel.RELATION.NORESPONSE, 0.0);
        put(DcrModel.RELATION.SPAWN, 0.0);
        put(DcrModel.RELATION.SEQUENCE, 0.0);
    }};
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/";
        String modelPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/groundtruthmodels/process101.xml";
        String modelPath2 = rootPath + "/src/main/java/beamline/dcr/testsoftware/groundtruthmodels/process25.xml";
        
        DcrModel model1 = new DcrModel();
        model1.loadModel(modelPath);
        
        ModelAdaption modelAdaption = new ModelAdaption(model1.getClone());
        
        modelAdaption.everyMutation(5);
        
        DcrModel model2 = modelAdaption.getModel();

        System.out.println(model1.getActivities().size());
        System.out.println(model2.getActivities().size());

        System.out.println(longestCommonSubtraceSimilarity(model1, model2));
    }
    
    /*
     * Jaccard similarity from Burattin, el.
     */
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
    
    public static double graphEditDistanceSimilarityWithWeights(DcrModel modelA, DcrModel modelB) {
        Set<String> actA = modelA.getActivities();
        Set<String> actB = modelB.getActivities();
        Set<Triple<String, String, RELATION>> relationsA = modelA.getRelations();
        Set<Triple<String, String, RELATION>> relationsB = modelB.getRelations();
        
        Set<String> activitiesDiff = symmetricDifference(actA, actB);
        Set<Triple<String, String, RELATION>> relationsDiff 
            = symmetricDifference(relationsA, relationsB);

        double relationsDiffDist = 0.0;
        
        for (RELATION relationType : RELATION.values()) {
            int sum = 0;
            for (Triple<String,String,RELATION> relation : relationsDiff) {
                if (relation.getRight().equals(relationType)) {
                    sum++;
                }
            }
            relationsDiffDist += (constraintWeight.get(relationType)*sum); 
        }

        Set<Triple<String,String,RELATION>> allRelations = union(modelA.getRelations(),modelB.getRelations());
        
        double allRelationsDist = 0.0;
        for (RELATION relationType : RELATION.values()) {
            int sum = 0;
            for (Triple<String,String,RELATION> relation : allRelations) {
                if (relation.getRight().equals(relationType)) {
                    sum++;
                }
            }
            allRelationsDist += (constraintWeight.get(relationType)*sum); 
        }
        
        double dist = (0.60*activitiesDiff.size()) + relationsDiffDist;
        double sim = (dist)/(actA.size() + actB.size() + allRelationsDist);
        
        return 1-sim;
    }
    
    public void setTraceLength(int traceLength) {
        this.traceLength = traceLength;
    }
    
    public static double longestCommonSubtraceSimilarity(DcrModel modelA, DcrModel modelB) {
        int numOfTraces = 20;
        
        ArrayList<ArrayList<String>> modelATraces = new ArrayList<ArrayList<String>>();
        ArrayList<ArrayList<String>> modelBTraces = new ArrayList<ArrayList<String>>();
        
        for (int i = 0; i < numOfTraces; i++) {
            modelATraces.add(TraceGenerator.generateRandomTraceFromModel(modelA, traceLength));
            modelBTraces.add(TraceGenerator.generateRandomTraceFromModel(modelB, traceLength));
        }
        
        double complianceDegreeSum = 0;
        double maturityDegreeSum = 0;
        
        for (ArrayList<String> traceB : modelBTraces) {
            double max = 0;
            for (ArrayList<String> traceA : modelATraces) {
                int lcs = longestCommonSubSequence(traceA, traceB);
                double complianceDegree = (double) lcs/traceB.size();
                if (complianceDegree > max) max = complianceDegree;
            }
            complianceDegreeSum += max;
        }
        
        for (ArrayList<String> traceA : modelATraces) {
            double max = 0;
            for (ArrayList<String> traceB : modelBTraces) {
                int lcs = longestCommonSubSequence(traceA, traceB);
                double maturityDegree = (double) lcs/traceA.size();
                if (maturityDegree > max) max = maturityDegree;
            }
            maturityDegreeSum += max;
        }
        
        double cd = complianceDegreeSum/modelBTraces.size();
        double md = maturityDegreeSum/modelATraces.size();
        
        double avg = (cd+md)/2;
        
        return avg;
    }
    
//    public static double diceCoefficient(DcrModel modelA, DcrModel modelB) {
//        return 1.0;
//    }
        
    /**
     * Helper functions
     */
    public static int longestCommonSubSequence(ArrayList<String> sequence1, ArrayList<String> sequence2) {
        if (sequence1.size() == 0 || sequence2.size() == 0) {
            return 0;
        }
        return longestCommonSubSequenceRecursive(sequence1, sequence2, sequence1.size()-1, sequence2.size()-1);
    }
    
    private static int longestCommonSubSequenceRecursive(ArrayList<String> sequence1, ArrayList<String> sequence2, int m, int n) {
        int L[][] = new int[m+1][n+1]; 
          
        /* Following steps build L[m+1][n+1] in bottom up fashion. Note 
            that L[i][j] contains length of LCS of X[0..i-1] and Y[0..j-1] */
        for (int i=0; i<=m; i++) { 
            
            for (int j=0; j<=n; j++) { 
                
                if (i == 0 || j == 0) 
                    L[i][j] = 0; 
                 
                else if (sequence1.get(i-1).equals(sequence2.get(j-1))) 
                    L[i][j] = L[i-1][j-1] + 1; 
                
                else
                    L[i][j] = Math.max(L[i-1][j], L[i][j-1]); 
            } 
        } 
        return L[m][n]; 
    } 
    
    
    
    /**
     * Returns a set representing the intersection between two sets
     */
    public static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        Set<T> intersect = new HashSet<T>(set1);
        intersect.retainAll(set2);
        return intersect;
    }
    
    /**
     * Returns a set representing the union between two sets (shallow copy)
     */
    public static <T> Set<T> union(Set<T> set1, Set<T> set2) {
        Set<T> union = new HashSet<T>(set1);
        union.retainAll(set2);
        return union;
    }
    
    /**
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
