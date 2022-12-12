package beamline.dcr.model.patterns;


import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.UnionRelationSet;
import beamline.dcr.model.relations.dfg.ActivityDecoration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;


public class ExcludeAndInclude implements RelationPattern {
    private List<String> activityList;
    private Set<Pair<String,String>> excludeSet;
    private Set<Pair<String,String>> includeSet;
    private UnionRelationSet unionRelationSet;
    private BitSet[] dfgAdjacencyMatrix;

    @Override
    public void populateConstraint(UnionRelationSet unionRelationSet) {
        this.activityList = new ArrayList<>(Arrays.asList(unionRelationSet.getUniqueActivities()));
        this.excludeSet = new HashSet<>();
        this.includeSet = new HashSet<>();
        this.unionRelationSet = unionRelationSet;

        Set<Pair<String,String>> dfgRelations = unionRelationSet.getDFGRelations();
        this.dfgAdjacencyMatrix = computeAdjacencyMatrix(dfgRelations);

        //Self-exclusion / at most once
        selfExclusion();

        //precedence
        precedence();

        //notChainSuccession
        notChainSuccession();

        removeRedundantExcludes();

        for(Pair<String,String> exclude : excludeSet){
            unionRelationSet.addDcrRelation(Triple.of(exclude.getLeft(), exclude.getRight(), DcrModel.RELATION.EXCLUDE));

        }
        for(Pair<String,String> include : includeSet){
            unionRelationSet.addDcrRelation(Triple.of(include.getLeft(), include.getRight(), DcrModel.RELATION.INCLUDE));

        }
    }
    //For framework setting test
    public void populateConstraint(UnionRelationSet unionRelationSet,Set<Integer> parameterSetting) {
        this.activityList = new ArrayList<>(Arrays.asList(unionRelationSet.getUniqueActivities()));
        this.excludeSet = new HashSet<>();
        this.includeSet = new HashSet<>();
        this.unionRelationSet = unionRelationSet;

        Set<Pair<String,String>> dfgRelations = unionRelationSet.getDFGRelations();
        this.dfgAdjacencyMatrix = computeAdjacencyMatrix(dfgRelations);
        if (parameterSetting.contains(1)){
            //Self-exclusion / at most once
            selfExclusion();
        }

        if (parameterSetting.contains(2)){
            //precedence
            precedence();
        }

        if (parameterSetting.contains(3)){
            //notChainSuccession
            notChainSuccession();
        }

        if (parameterSetting.contains(4)){
            removeRedundantExcludes();
        }


        for(Pair<String,String> exclude : excludeSet){
            unionRelationSet.addDcrRelation(Triple.of(exclude.getLeft(), exclude.getRight(), DcrModel.RELATION.EXCLUDE));

        }
        for(Pair<String,String> include : includeSet){
            unionRelationSet.addDcrRelation(Triple.of(include.getLeft(), include.getRight(), DcrModel.RELATION.INCLUDE));

        }
    }

    private void selfExclusion(){
        for (String activity : unionRelationSet.getUniqueActivities()){
            if (unionRelationSet.getActivityDecoration(activity).appearMostOnce()){
                this.excludeSet.add(Pair.of(activity,activity));
            }
        }

    }

    private void precedence(){
        Set<Triple<String, String, DcrModel.RELATION>> dcrSequences =
                unionRelationSet.getDcrRelationWithConstraint(DcrModel.RELATION.SEQUENCE);

        for (Triple<String, String, DcrModel.RELATION> sequence : dcrSequences) {
            String source = sequence.getLeft();
            ActivityDecoration sourceDecoration = unionRelationSet.getActivityDecoration(source);
            String target = sequence.getMiddle();
            ActivityDecoration targetDecoration = unionRelationSet.getActivityDecoration(target);
            if (sourceDecoration.getNumObservations() == targetDecoration.getNumObservations()) {
                this.excludeSet.add(Pair.of(target, target));
                this.includeSet.add(Pair.of(source, target));
            }
            //precedence not successor
            if (!excludeSet.contains(Pair.of(source, source))) {
                this.excludeSet.add(Pair.of(target, source));
            }

        }
    }

    private void notChainSuccession(){
        for (int i = 0; i < dfgAdjacencyMatrix.length; i++){
            for (int j = 0; j < dfgAdjacencyMatrix.length; j++){
                if (!dfgAdjacencyMatrix[i].get(j) & i != j){

                    this.excludeSet.add(Pair.of(activityList.get(i),activityList.get(j)));
                    //get events in between i and j
                    Set<String> inBetween = getActivitiesBetween(i,j);

                    for(String activityBetween : inBetween){
                        this.includeSet.add(Pair.of(activityBetween,activityList.get(j)));
                    }
                }
            }
        }
    }

    private BitSet[] computeAdjacencyMatrix(Set<Pair<String,String>>  relationSet){
        BitSet[] matrix = new BitSet[activityList.size()];
        for(int i = 0; i < matrix.length; i++) {
            matrix[i] = new BitSet(activityList.size());
        }
        for(Pair<String,String> relation : relationSet){
            String src = relation.getLeft();
            String tar = relation.getRight();
            int i1 = activityList.indexOf(src);
            int i2 = activityList.indexOf(tar);
            matrix[i1].set(i2);
        }
        return matrix;
    }

    private void computeTransitiveClosure(final BitSet[] matrix) {
        // compute path matrix / transitive closure
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                if (i == j) {
                    continue;
                }
                if (matrix[j].get(i)) {
                    for (int k = 0; k < matrix.length; k++) {
                        if (!matrix[j].get(k)) {
                            matrix[j].set(k, matrix[i].get(k));
                        }
                    }
                }
            }
        }
    }

    private BitSet[] deepCopyBitSet(BitSet[] bitSet){
        BitSet[] newBitSet = new BitSet[bitSet.length];
        for(int i = 0; i < bitSet.length; i++) {
            newBitSet[i] = new BitSet(activityList.size());

            for (int j = 0; j<bitSet.length; j++){
                if (bitSet[i].get(j)) newBitSet[i].set(j);
            }
        }
        return newBitSet;
    }

    private void removeRedundantExcludes(){

        BitSet[] adjacencyMatrixExcludes = computeAdjacencyMatrix(excludeSet);
        Set<Triple<String, String, DcrModel.RELATION>> dcrSequences =
                unionRelationSet.getDcrRelationWithConstraint(DcrModel.RELATION.SEQUENCE);

        for (Triple<String, String, DcrModel.RELATION> sequence : dcrSequences) {
            int sourceIndex = activityList.indexOf(sequence.getLeft());
            int targetIndex = activityList.indexOf(sequence.getMiddle());

            Set<String> activitiesBetween = getActivitiesBetween(sourceIndex,targetIndex);

            for (int k = 0; k < activityList.size(); k++){
                // if exclude(a1,a3) and exlude(a2,a3)
                if (adjacencyMatrixExcludes[sourceIndex].get(k) & adjacencyMatrixExcludes[targetIndex].get(k)){
                    String sink = activityList.get(k);
                    for (String activityBetween : activitiesBetween){
                        //And there is no include(u,a3) where u is between a1,a2
                        if(!includeSet.contains(Pair.of(activityBetween,sink))){
                            // exclude(a2,a3) is redundant
                            excludeSet.remove(Pair.of(sequence.getMiddle(),sink));
                        }
                    }
                }
            }
        }
    }

    private Set<String> getActivitiesBetween(int sourceIndex, int sinkIndex){

        final BitSet[] transitiveClosure = deepCopyBitSet(dfgAdjacencyMatrix);
        computeTransitiveClosure(transitiveClosure);
        Set<String> inBetween = new HashSet<>();
        BitSet visited = new BitSet(activityList.size());

        dfs(sourceIndex,sinkIndex, inBetween,transitiveClosure,visited);
        return inBetween;
    }

    private void dfs(int sourceIndex, int sinkIndex, Set<String> inBetween,
                     BitSet[] transitiveClosure,
                     BitSet visited){

        for(int k=0; k < activityList.size(); k++){
            if(k != sourceIndex &
                    k != sinkIndex &
                    dfgAdjacencyMatrix[sourceIndex].get(k) &
                    transitiveClosure[k].get(sinkIndex) &
                    !visited.get(k)
            ){
                String source = activityList.get(k);
                visited.set(sourceIndex);
                inBetween.add(source);

                dfs(k,sinkIndex,inBetween,transitiveClosure,visited);

            }
        }

    }
}
