package modeltomodel;

import java.util.ArrayList;

import beamline.dcr.model.relations.DcrModel;

public class DriftDetector {
    
    public enum SIMILARITY_MEASURE {
        GED,
        CNE
    }
    
    double sigDiff = 0.7;
    
    /**
     * Returns the number of concept drifts that have been detected from the models
     */
    public int detectConceptDriftFromRepository(ArrayList<DcrModel> models) {
        
        return 1;
    }
    
    /*
     * Considers two models and determines if they are significantly different from each other
     */
    public int detectConceptDrift(DcrModel modelA, DcrModel modelB, SIMILARITY_MEASURE sim) {
        double similarity = -1.0;
        switch(sim) {
            case GED: 
                similarity = DcrSimilarity.graphEditDistanceSimilarity(modelA, modelB);
                break;
            case CNE: 
                similarity = DcrSimilarity.commonNodesAndEdgesSimilarity(modelA, modelB);
        }
        if (similarity < sigDiff) return 1;
        return 0;
    }
    
    public void setSigDiff(double num) {
        sigDiff = num;
    }
}
