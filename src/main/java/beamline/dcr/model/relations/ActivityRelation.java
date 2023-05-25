package beamline.dcr.model.relations;

import java.util.HashSet;
import java.util.Set;

public class ActivityRelation {

    private String activityA;
    private String activityB;
    private String relationType;

    public ActivityRelation(String activityA, String activityB, String relationType) {
        this.activityA = activityA;
        this.activityB = activityB;
        this.relationType = relationType;
    }

    public String getActivityA() {
        return activityA;
    }

    public String getActivityB() {
        return activityB;
    }

    public String getRelationType() {
        return relationType;
    }

    public static Set<ActivityRelation> getRelationsBetweenPair(Set<ActivityRelation> relations, String activityA, String activityB) {
        Set<ActivityRelation> result = new HashSet<ActivityRelation>();
        for (ActivityRelation relation : relations) {
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
        result = 31 * result + relationType.hashCode();
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        // Two relations are equal if they have the same two activities and the same relation type
        if (obj instanceof ActivityRelation) {
            ActivityRelation other = (ActivityRelation) obj;
            return activityA.equals(other.activityA) && activityB.equals(other.activityB) && relationType.equals(other.relationType);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return activityA + " " + activityB + " " + relationType;
    }
}
