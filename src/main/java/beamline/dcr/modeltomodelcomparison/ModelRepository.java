package beamline.dcr.modeltomodelcomparison;

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
    
    private static METRIC metric = METRIC.Jaccard;
    private static double sigDiff = 0.9;
    
    public static void setMetric(METRIC metric) {
        ModelRepository.metric = metric;
    }
    
    // Returns 0 if successful, otherwise -1
    public boolean addModelToRep(DcrModel model) {
        boolean unique = true;
        
        for (DcrModel repModel : repository) {
            if (isSimilar(repModel, model, sigDiff, metric)) unique = false;
        }
        
        if (unique) {
            System.out.println("Addition sucessful");
            repository.add(model);
            return true; 
        }
        System.out.println("Addition not succesful");
        return false;
    }
    
    public int size() {
        return repository.size();
    }
    
    public DcrModel getModelAt(int index) {
        return repository.get(index);
    }
    
    private static double similarity(DcrModel modelA, DcrModel modelB, METRIC metric) {
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
