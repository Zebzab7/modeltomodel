package beamline.dcr.testsoftware;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;

import org.apache.commons.lang3.tuple.Triple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModelComparison {

    private DcrModel originalDcrModel;
    private DcrModel comparativeDcrModel;
    public ModelComparison() {

    }
    public ModelComparison(DcrModel dcrModel) {
        this.originalDcrModel = dcrModel;
    }
    public ModelComparison(String dcrModelpath) throws IOException, SAXException, ParserConfigurationException {
        this.originalDcrModel = new DcrModel();
        this.originalDcrModel.loadModel(dcrModelpath);
    }

    public void loadOriginalModel(DcrModel dcrModel) {
        this.originalDcrModel = dcrModel;
    }
    public double getPrecision(){
        double tp = getIntersectionSize();
        double fp = originalDcrModel.getRelations().size() - tp;

        return tp / (tp + fp);
    }
    
    private int getIntersectionSize(){

        Set<Triple<String,String, DcrModel.RELATION>> minedRelations = originalDcrModel.getRelations();

        int intersection = 0;
        for (Triple<String, String, DcrModel.RELATION> relation : minedRelations){
            if (comparativeDcrModel.containsRelation(relation)) intersection++;
        }

        return intersection;
    }
    private int getIntersectingActivitiesSize(){

        Set<String> minedActivities = originalDcrModel.getActivities();

        int intersection = 0;
        for (String relation : minedActivities){
            if (comparativeDcrModel.getActivities().contains(relation)) intersection++;
        }

        return intersection;
    }
    private int getConstraintIntersectionSize(DcrModel.RELATION constraint){

        Set<Triple<String,String, DcrModel.RELATION>> minedRelations = originalDcrModel.getDcrRelationWithConstraint(constraint);

        int intersection = 0;
        for (Triple<String, String, DcrModel.RELATION> relation : minedRelations){
            if (comparativeDcrModel.containsRelation(relation)) intersection++;
        }

        return intersection;
    }
    private double getConstraintUnionSize(DcrModel.RELATION constraint, Double intersection){
        return originalDcrModel.getDcrRelationWithConstraint(constraint).size() +
                comparativeDcrModel.getDcrRelationWithConstraint(constraint).size() - intersection;
    }
    public double getRecall(){
        double tp = getIntersectionSize();
        double fn = comparativeDcrModel.getRelations().size() - tp;

        return tp / (tp + fn);
    }

    public double getF1() {
        double recall = getRecall();
        double precision = getPrecision();

        return 2*precision*recall/(recall+precision);
    }
    public double getJaccardSimilarity(){
        Map<DcrModel.RELATION, Double> constraintWeight = new HashMap<>(){{
            put(DcrModel.RELATION.CONDITION, 0.15);
            put(DcrModel.RELATION.RESPONSE, 0.05);
            put(DcrModel.RELATION.PRECONDITION, 0.0);
            put(DcrModel.RELATION.MILESTONE, 0.0);
            put(DcrModel.RELATION.INCLUDE, 0.0);
            put(DcrModel.RELATION.EXCLUDE, 0.0);
            put(DcrModel.RELATION.NORESPONSE, 0.0);
            put(DcrModel.RELATION.SPAWN, 0.0);
        }};
        double activityWeight = 0.8;

        double intersection;
        double union;
        double jaccardTotal = 0;

        for (Map.Entry<DcrModel.RELATION,Double> entry : constraintWeight.entrySet()){
            intersection = getConstraintIntersectionSize(entry.getKey());
            union = getConstraintUnionSize(entry.getKey(),intersection);

            double w = entry.getValue();
            double sim = w * intersection/union;
            if(Double.isNaN(sim)) sim = w;

            jaccardTotal += sim;
        }
        double activityIntersection= getIntersectingActivitiesSize();
        double activityUnion = originalDcrModel.getActivities().size() + comparativeDcrModel.getActivities().size() - activityIntersection;

        jaccardTotal += activityWeight * activityIntersection/activityUnion;


        return jaccardTotal;
    }
    

    public String getJaccardString() {
        Map<DcrModel.RELATION, Double> constraintWeight = new HashMap<>() {{
            put(DcrModel.RELATION.CONDITION, 0.1);
            put(DcrModel.RELATION.RESPONSE, 0.1);
            put(DcrModel.RELATION.PRECONDITION, 0.1);
            put(DcrModel.RELATION.MILESTONE, 0.1);
            put(DcrModel.RELATION.INCLUDE, 0.1);
            put(DcrModel.RELATION.EXCLUDE, 0.1);
            put(DcrModel.RELATION.NORESPONSE, 0.1);
            put(DcrModel.RELATION.SPAWN, 0.1);
        }};

        double intersection;
        double union;
        StringBuilder jaccardString = new StringBuilder();
        for (Map.Entry<DcrModel.RELATION, Double> entry : constraintWeight.entrySet()) {
            intersection = getConstraintIntersectionSize(entry.getKey());
            union = getConstraintUnionSize(entry.getKey(), intersection);

            double sim = intersection / union;
            if (Double.isNaN(sim)) sim = 1;

            jaccardString.append( sim).append(",");

        }
        double activityIntersection = getIntersectingActivitiesSize();
        double activityUnion = originalDcrModel.getActivities().size() + comparativeDcrModel.getActivities().size() - activityIntersection;

        jaccardString.append(activityIntersection / activityUnion);

        return jaccardString.toString();
    }
    
    public String getLCSString() {
        double sim = DcrSimilarity.longestCommonSubtraceSimilarity(originalDcrModel, comparativeDcrModel);
        return ""+sim;
    }
    
    public String getGEDString() {
        double sim = DcrSimilarity.graphEditDistanceSimilarity(originalDcrModel, comparativeDcrModel);
        return ""+sim;
    }
    
    public String getGEDWString() {
        double sim = DcrSimilarity.graphEditDistanceSimilarityWithWeights(originalDcrModel, comparativeDcrModel);
        return ""+sim;
    }
    
    public String getCNEString() {
        double sim = DcrSimilarity.commonNodesAndEdgesSimilarity(originalDcrModel, comparativeDcrModel);
        return ""+sim;
    }

    public void loadComparativeModel(String xmlGraphPath) throws IOException, SAXException, ParserConfigurationException {
        this.comparativeDcrModel = new DcrModel();
        this.comparativeDcrModel.loadModel(xmlGraphPath);
    }

    public void loadComparativeModel(DcrModel adaptedModel) {
        this.comparativeDcrModel = adaptedModel;
    }
}
