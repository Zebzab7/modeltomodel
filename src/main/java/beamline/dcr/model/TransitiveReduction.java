package beamline.dcr.model;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.UnionRelationSet;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;

public class TransitiveReduction {

    private List<String> activityList;
    private BitSet[] originalMatrix;
    private Set<Triple<String, String, DcrModel.RELATION>> allRelationsWithPattern;
    private DcrModel.RELATION reducedPattern;
    public TransitiveReduction() {
    }

    private void setUp(UnionRelationSet unionRelationSet, DcrModel.RELATION pattern){

        this.reducedPattern = pattern;
        this.allRelationsWithPattern = unionRelationSet.getDcrRelationWithConstraint(pattern);
        //Consider ordering Set in alphabetic order
        //Get set of unique activies
        Set<String> activitySet = new TreeSet<>();
        for (Triple<String, String, DcrModel.RELATION> patternRelation : allRelationsWithPattern) {
            activitySet.add(patternRelation.getLeft());
            activitySet.add(patternRelation.getMiddle());
        }
        this.activityList = new ArrayList<>(activitySet);
        //initialize originalMatrix
        this.originalMatrix = new BitSet[activityList.size()];
        for(int i = 0; i < originalMatrix.length; i++) {
            this.originalMatrix[i] = new BitSet(activityList.size());
        }

    }

    private void transformToPathMatrix(final BitSet[] matrix) {
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
    private void transitiveReduction(BitSet[] pathMatrix){
        // transitively reduce
        for (int j = 0; j < pathMatrix.length; j++) {
            for (BitSet matrix : pathMatrix) {
                if (matrix.get(j)) {
                    for (int k = 0; k < pathMatrix.length; k++) {
                        if (pathMatrix[j].get(k)) {
                            matrix.set(k, false);
                        }
                    }
                }
            }
        }
    }
    public void reduce(UnionRelationSet unionRelationSet, DcrModel.RELATION patternToReduce){
        setUp(unionRelationSet,patternToReduce);
        //initialize matrix with edges
        for (Triple<String, String, DcrModel.RELATION> relationPattern : allRelationsWithPattern) {

            String src = relationPattern.getLeft();
            String tar = relationPattern.getMiddle();
            int i1 = activityList.indexOf(src);
            int i2 = activityList.indexOf(tar);
            this.originalMatrix[i1].set(i2);
        }

        // transitive closure
        final BitSet[] pathMatrix = deepCopyBitSet(originalMatrix);
        transformToPathMatrix(pathMatrix);

        // create reduced matrix from path matrix
        final BitSet[] transitivelyReducedMatrix = deepCopyBitSet(pathMatrix);

        transitiveReduction(transitivelyReducedMatrix);
        for (int i = 0; i < originalMatrix.length; i++) {
            for (int j = 0; j < originalMatrix.length; j++) {
                if (!transitivelyReducedMatrix[i].get(j)) {
                    String src = activityList.get(i);
                    String tar = activityList.get(j);
                    unionRelationSet.removeDcrRelation(Triple.of(src,tar, reducedPattern));

                }
            }
        }
    }
    private void printBit(BitSet[] b,String name){
        System.out.println(name);

        StringBuilder s = new StringBuilder();
        for( int i = 0; i < activityList.size();  i++ )
        {
            for( int j = 0; j < activityList.size();  j++ ){
                s.append( b[i].get( j ) == true ? 1: 0 );
            }
            s.append("\n");

        }

        System.out.println( s );
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



}
