package beamline.dcr.modeltomodel;

import java.util.ArrayList;

import beamline.dcr.model.relations.DcrModel;

public class ModelRepository {
    
    private ArrayList<DcrModel> repository = new ArrayList<DcrModel>();
    
    public enum METRIC {
        GED,
        GEDW,
        Jaccard,
        CNE
    }
    
    private static METRIC metric = METRIC.GED;
    private static double sigDiff = 0.9;
    
    // Returns 0 if successful, otherwise -1
    public int addModelToRep(DcrModel model) {
        boolean unique = true;
        
        for (DcrModel repModel : repository) {
            if (similarity(repModel, model, metric) < sigDiff) unique = false;
        }
        
        if (unique) {
            repository.add(model);
            return -1; 
        }
        return 0;
    }

    public static double similarity(DcrModel modelA, DcrModel modelB, METRIC metric) {
        switch (metric) {
            case GED:
                return DcrSimilarity.graphEditDistanceSimilarity(modelA, modelB);
            case GEDW:
                return DcrSimilarity.graphEditDistanceSimilarityWithWeights(modelA, modelB);
            case Jaccard:
                return DcrSimilarity.jaccardSimilarity(modelA, modelB);
            case CNE:
                return DcrSimilarity.commonNodesAndEdgesSimilarity(modelA, modelB);
        }
        return -1.0;
    }
    
    public boolean isSimilar(DcrModel modelA, DcrModel modelB, double sigDiff, METRIC metric) {
        if (similarity(modelA, modelB, metric) < sigDiff) {
            return false;
        }
        return true;
    }
}
