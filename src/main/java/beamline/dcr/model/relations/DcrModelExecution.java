package beamline.dcr.model.relations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import beamline.dcr.model.relations.DcrModel.RELATION;

public class DcrModelExecution {
    DcrModel model = new DcrModel();
    
    ArrayList<String> executionTrace = new ArrayList<String>();
    
    // Marking = (Executed, Pending, Included)
    Triple<ArrayList<String>, ArrayList<String>, ArrayList<String>> marking 
        = new MutableTriple<ArrayList<String>, ArrayList<String>, ArrayList<String>>
            (new ArrayList<String>(),new ArrayList<String>(),new ArrayList<String>());
    
    public DcrModelExecution() {
        marking.getRight().addAll(model.getActivities());
    }
    
    public DcrModelExecution(DcrModel model) {
        this.model = model;
        marking.getRight().addAll(model.getActivities());
    }
    
    public void loadModel(DcrModel model) {
        this.model = model;
    }
    
    public void initPendingActivities(List<String> activities) {
        marking.getMiddle().addAll(activities);
    }
    
    public Triple<ArrayList<String>, ArrayList<String>, ArrayList<String>> getMarking() {
        return marking;
    }
    
    /**
     * Returns execution trace
     */
    public ArrayList<String> getTrace() {
        return executionTrace;
    }
    
    /**
     * Determines if given activity is executable with the current marking
     * @param activity
     * @return
     */
    public boolean isExecutable(String activity) {
        boolean executable = true;
        
        Set<Triple<String,String,RELATION>> relations 
            = model.getDcrRelationsWithActivity(activity);
        
        for (Triple<String,String,RELATION> relation : relations) {
            if (relation.getMiddle().equals(activity) 
                    && relation.getRight() == RELATION.CONDITION) {
                if (!marking.getLeft().contains(relation.getLeft())) executable = false;
            }
        }
        return executable;
    }
    
    /**
     * Executes the given activity in the model if possible
     * Returns true if successful, otherwise false
     * @param activity
     */
    public boolean executeActivity(String activity) {
        if (isExecutable(activity)) {
            Set<Triple<String,String,RELATION>> relations 
                = model.getDcrRelationsWithSource(activity);
            
            for (Triple<String,String,RELATION> relation : relations) {
                updateMarkingWithRelation(relation);
            }
            
            marking.getLeft().add(activity);
            marking.getMiddle().remove(activity);
            executionTrace.add(activity);
            return true;
        }
        return false;
    }
    
    /**
     * Determines if a given activity is pending in the current state of execution
     * @param activity
     * @return
     */
    public boolean isPending(String activity) {
        if (marking.getMiddle().contains(activity)) return true;
        return false;
    }
    
    /**
     * Updates marking with respect to a given relation
     * @param relation
     */
    private void updateMarkingWithRelation(Triple<String,String,RELATION> relation) {
        switch (relation.getRight()) {
            case RESPONSE:
                marking.getMiddle().add(relation.getMiddle());
                break;
            case INCLUDE:
                marking.getRight().add(relation.getMiddle());
                break;
            case EXCLUDE:
                marking.getRight().remove(relation.getMiddle());
                break;
            default:
                break;
        }
    }
    
    /**
     * Determines if a given activity is included in the graph
     * @param activity
     * @return
     */
    public boolean isIncluded(String activity) {
        if (marking.getRight().contains(activity)) return true;
        return false;
    }
}
