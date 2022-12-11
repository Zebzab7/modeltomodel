package modeltomodel;

import beamline.dcr.model.relations.DcrModel;

public class DriftSimulator {
    /*
     * 
     */
    
    public static void simpleActivityMutation(DcrModel model) {
        for (int i = 0; i < 10; i++) {
            Mutation.randomAcitityInsertDelete(model);
        }
    }
    
    public static void simpleRelationMutation(DcrModel model) {
        for (int i = 0; i < 2; i++) {
            Mutation.randomRelationInsertDelete(model);
        }
    }
}
