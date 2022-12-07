package modeltomodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;

import beamline.dcr.model.relations.DcrModel;

public class Mutation {
    
    static int actNum = 1;
    
    // Should be extended
    static String randomName = "nAct";
    static Random rand = new Random();
    
    public static void main(String[] args) {
        DcrModel dcr1 = new DcrModel();
        
        Set<Triple<String, String, DcrModel.RELATION>> dcrRelations1 = new HashSet<>();

        dcrRelations1.add(Triple.of("Start", "Act3", DcrModel.RELATION.CONDITION));
        dcrRelations1.add(Triple.of("Start", "Act2", DcrModel.RELATION.RESPONSE));
        dcrRelations1.add(Triple.of("Act4", "Act7", DcrModel.RELATION.RESPONSE));
        dcrRelations1.add(Triple.of("Act4", "Act7", DcrModel.RELATION.INCLUDE));
        dcrRelations1.add(Triple.of("Start", "Act4", DcrModel.RELATION.EXCLUDE));
        dcrRelations1.add(Triple.of("Act4", "Act5", DcrModel.RELATION.CONDITION));
        dcrRelations1.add(Triple.of("Act5", "Act6", DcrModel.RELATION.INCLUDE));
        
        dcr1.addRelations(dcrRelations1);

        System.out.println("Initial activities in model: \n" 
                            + dcr1.getActivities().toString());
        
        for (int i = 0; i < 10; i++) {
            randomInsertDeletion(dcr1);
        }
        
        System.out.println("Final activities in model: \n"
                            + dcr1.getActivities().toString());
    }

    /**
     * Mutation operations
     */
    
    /*
     * Randomly inserts or deletes an activity from the model
     */
    public static void randomInsertDeletion(DcrModel model) {
        Random rand = new Random();
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
    
    
}
