package beamline.dcr.model.relations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import beamline.dcr.model.relations.DcrModel.RELATION;

public class DcrModelExecution {
    DcrModel model = new DcrModel();
    
    private Set<Triple<String, String, RELATION>> includedRelations;
    
    ArrayList<String> executionTrace = new ArrayList<String>();
    
    // Marking = (Executed, Pending, Included)
    Triple<Set<String>, Set<String>, Set<String>> marking 
        = new MutableTriple<Set<String>, Set<String>, Set<String>>
            (new HashSet<String>(),new HashSet<String>(),new HashSet<String>());
    
    public DcrModelExecution() {
        marking.getRight().addAll(model.getActivities());
        includedRelations = new HashSet<Triple<String, String, RELATION>>();
    }
    
    public DcrModelExecution(DcrModel model) {
        this.model = model;
        marking.getRight().addAll(model.getActivities());
        includedRelations = new HashSet<Triple<String, String, RELATION>>(model.getRelations());
    }
    
    public void loadModel(DcrModel model) {
        this.model = model;
        includedRelations = new HashSet<Triple<String, String, RELATION>>(model.getRelations());
    }
    
    public void initPendingActivities(List<String> activities) {
        marking.getMiddle().addAll(activities);
    }
    public Triple<Set<String>, Set<String>, Set<String>> getMarking() {
        return marking;
    }
    public ArrayList<String> getTrace() {
        return executionTrace;
    }
    public Set<Triple<String, String, DcrModel.RELATION>> getIncludedRelationsWithSource(String source){
        return includedRelations.stream()
                .filter(entry -> entry.getLeft().equals(source))
                .collect(Collectors.toSet());
    }
    
    /**
     * Determines if given activity is executable with the current marking
     * @param activity
     * @return
     */
    public boolean isExecutable(String activity) {
        if (model.isParentActivity(activity)) return false;
        if (executionTrace.contains(activity) 
                || !marking.getRight().contains(activity) 
                    || !checkIfPreconditionsMet(activity)) return false;
        if (model.isSubActivity(activity)) {
            String parent = model.getParentActivity(activity);
            if (!checkIfPreconditionsMet(parent)) return false;
        }
        return true;
    }
    
    /**
     * Checks only the direct preconditions of a given activity are met,
     * thus excluding any preconditions from parent activity
     * @param activity
     */
    private boolean checkIfPreconditionsMet(String activity) {
        
        Set<Triple<String,String,RELATION>> relations 
            = model.getDcrRelationsWithActivity(activity);

        
        for (Triple<String,String,RELATION> relation : relations) {
            if (relation.getMiddle().equals(activity) 
                    && relation.getRight() == RELATION.CONDITION 
                        && marking.getRight().contains(relation.getLeft())) {
                if (!marking.getLeft().contains(relation.getLeft())) return false;
            }
        }
        return true;
    }
    
    /**
     * Executes the given activity in the model if possible
     * Returns true if successful, otherwise false
     * @param activity
     */
    public boolean executeActivity(String activity) {
        if (isExecutable(activity)) {
            marking.getLeft().add(activity);
            marking.getMiddle().remove(activity);
            executionTrace.add(activity);
            updateMarkingFromActivity(activity);
            return true;
        }
        return false;
    }
    
    private void updateMarkingFromActivity(String activity) {
        Set<Triple<String,String,RELATION>> relations 
            = model.getDcrRelationsWithSource(activity);
        
        for (Triple<String,String,RELATION> relation : relations) {
            updateMarkingFromRelation(relation);
        }
    }
    
    /**
     * Updates marking with respect to a given relation
     * @param relation
     */
    private void updateMarkingFromRelation(Triple<String,String,RELATION> relation) {
        
        String target = relation.getMiddle();
        
        ArrayList<String> children 
            = model.getSubActivitiesFromParent(relation.getMiddle());
        
        switch (relation.getRight()) {
            case RESPONSE:
                if (marking.getRight().contains(target)) {
                    marking.getMiddle().add(target);
                }
                break;
            case INCLUDE:
                includeActivity(target);
                for (String child : children) {
                    includeActivity(child);
                }
                break;
            case EXCLUDE:
                excludeActivity(target);
                for (String child : children) {
                    excludeActivity(child);
                }
                break;
            default:
                break;
        }
    }
    
    private void includeActivity(String activity) {
        if (!marking.getRight().contains(activity)) {
            marking.getRight().add(activity);
        }
    }
    
    private void excludeActivity(String activity) {
        if (marking.getRight().contains(activity)) {
            marking.getRight().remove(activity);
        }
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
     * Determines if a given activity is included in the graph
     * @param activity
     * @return
     */
    public boolean isIncluded(String activity) {
        if (marking.getRight().contains(activity)) return true;
        return false;
    }
}
