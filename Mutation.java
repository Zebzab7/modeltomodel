package modeltomodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.DcrModel.RELATION;

public class Mutation {
    
    static int actNum = 1;
    
    // Should be extended
    static String randomName = "nAct";
    static Random rand = new Random();
    
    public static void main(String[] args) {
        DcrModel dcr1 = new DcrModel();
        
        Set<Triple<String, String, DcrModel.RELATION>> dcrRelations1 = new HashSet<>();

        dcrRelations1.add(Triple.of("Start", "Act2", DcrModel.RELATION.CONDITION));
        dcrRelations1.add(Triple.of("Start", "Act2", DcrModel.RELATION.RESPONSE));
        dcrRelations1.add(Triple.of("Act2", "Start", DcrModel.RELATION.RESPONSE));
        dcrRelations1.add(Triple.of("Act2", "Act3", DcrModel.RELATION.RESPONSE));
        
        dcr1.addRelations(dcrRelations1);
        
        String newAct = getNewActivityName();
        
        insertActivityWithName(dcr1,newAct);
    }
    
    /**
     * Change pattern - Serial insert of activity
     * Mutation operations
     */
    public static void insertActivityWithName(DcrModel model, String newActivity) {
        ArrayList<Triple<String,String,RELATION>> relationList 
                = new ArrayList<Triple<String,String,RELATION>>(model.getRelations());
        Triple<String,String,RELATION> randomRelation 
            = relationList.get(rand.nextInt(relationList.size()));
        
        String act1 = randomRelation.getLeft();
        String act2 = randomRelation.getMiddle();
        
        Set<Triple<String,String,RELATION>> sourceEdges
            = model.getDcrRelationsWithSource(act1);
        
        ArrayList<Triple<String,String,RELATION>> targetEdgesList 
            = new ArrayList<Triple<String,String,RELATION>>();
        
        for (Triple<String,String,RELATION> relation : model.getDcrRelationsWithSource(act2)) {
            if (relation.getMiddle() == act1) targetEdgesList.add(relation);
        }
        
        Set<Triple<String,String,RELATION>> targetEdges 
            = new HashSet<Triple<String, String, RELATION>>(targetEdgesList);
        
        sourceEdges.addAll(targetEdges);
        
        model.removeRelations(sourceEdges);
        
        ArrayList<Triple<String,String,RELATION>> sourceEdgesList
            = new ArrayList<Triple<String,String,RELATION>>(sourceEdges);
        
        for (Triple<String,String,RELATION> relation : sourceEdgesList) {
            model.addRelation(Triple.of(relation.getLeft(), newActivity, relation.getRight()));  
            model.addRelation(Triple.of(newActivity, relation.getMiddle(), relation.getRight()));  
        }
    }
    
    /**
     * Change pattern - Remove activity 
     */
    public static void removeActivityWithName(DcrModel model, String activity) {
    }
    
    public static String getNewActivityName() {
        actNum++;
        return randomName + (actNum-1);
    }
    
    /*
     * Randomly inserts or deletes an activity from the model
     */
    public static void randomAcitityInsertDelete(DcrModel model) {
        if (rand.nextInt(2) == 1) {
            model.addActivity(randomName + actNum);
            actNum++;
            System.out.println("New activity with name " + randomName + actNum + " added.");
        } else {
            removeRandomActivity(model);
        }
    }
    
    // Removes a random activity from the model
    public static void removeRandomActivity(DcrModel model) {
        int count = model.getActivities().size();
        ArrayList<String> activityList = new ArrayList<String>(model.getActivities());
        try {
            String removedElement = activityList.get(rand.nextInt(count));
            model.removeActivity(removedElement);
            System.out.println("Activity with name " + removedElement + " removed.");
        } catch (Exception e) {
            System.out.println("Tried to remove activity when no more activities exist");
        }
    }
    
    /*
     * Randomly inserts or deletes an edge of a random type
     */
    public static void randomRelationInsertDelete(DcrModel model) {
        RELATION[] edges 
            = {RELATION.INCLUDE, RELATION.EXCLUDE, RELATION.CONDITION, RELATION.RESPONSE};
        if (rand.nextInt(2) == 1) {
            String act1 = getRandomActivityFromDCRModel(model);
            String act2 = getRandomActivityFromDCRModel(model);
            Triple<String,String,RELATION> tuple 
                = new MutableTriple<String,String,RELATION>(act1,act2,RELATION.CONDITION);
            model.addRelation(tuple);
        } else {
            removeRandomRelation(model);
        }
    }
    
    // Removes a random relation from the model
    public static void removeRandomRelation(DcrModel model) {
        int count = model.getRelations().size();
        ArrayList<Triple<String,String,RELATION>> relationList 
            = new ArrayList<Triple<String,String,RELATION>>(model.getRelations());
        try {
            Triple<String,String,RELATION> removedElement = relationList.get(rand.nextInt(count));
            model.removeRelation(removedElement.getLeft(), removedElement.getMiddle(), 
                                    removedElement.getRight());
        } catch (Exception e) {
            System.out.println("Tried to remove relation when none were left");
        }
    }
    
    public static String getRandomActivityFromDCRModel(DcrModel model) {
        ArrayList<String> activityList = new ArrayList<String>(model.getActivities());
        return activityList.get(rand.nextInt(activityList.size()));
    }
}








