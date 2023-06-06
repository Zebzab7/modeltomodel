package beamline.dcr.model.relations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ActivityRelations {

    private String activityA;
    private String activityB;
    private Set<String> relations = new HashSet<String>();

    public ActivityRelations(String activityA, String activityB) {
        this.activityA = activityA;
        this.activityB = activityB;
    }

    public String getActivityA() {
        return activityA;
    }

    public String getActivityB() {
        return activityB;
    }

    public Set<String> getRelations() {
        return relations;
    }

    public void addRelation(String relation) {
        relations.add(relation);
    }

    public void removeRelation(String relation) {
        relations.remove(relation);
    }

    public static Set<ActivityRelations> getRelationsBetweenPair(Set<ActivityRelations> relations, String activityA, String activityB) {
        Set<ActivityRelations> result = new HashSet<ActivityRelations>();
        for (ActivityRelations relation : relations) {
            if (relation.getActivityA().equals(activityA) && relation.getActivityB().equals(activityB)) {
                result.add(relation);
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + activityA.hashCode();
        result = 31 * result + activityB.hashCode();
        result = 31 * result + relations.hashCode();
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        // Two relations are equal if they have the same two activities and the same relation type
        if (obj instanceof ActivityRelations) {
            ActivityRelations other = (ActivityRelations) obj;
            return activityA.equals(other.activityA) && activityB.equals(other.activityB) && relations.equals(other.relations);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return activityA + " AND " + activityB + " WITH RELATIONS " + relations.toString();
    }
}
