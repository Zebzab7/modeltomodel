package distancemetrics;

import org.christopherfrantz.dbscan.DistanceMetric;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodelcomparison.DcrSimilarity;

public class WeightedGraphEditDistance implements DistanceMetric<DcrModel> {
    public double calculateDistance(DcrModel arg0, DcrModel arg1) throws org.christopherfrantz.dbscan.DBSCANClusteringException {
        return 1.0-DcrSimilarity.graphEditDistanceSimilarityWithWeights(arg0, arg1);
    }
    
    @Override
    public String toString() {
        return "Weighted GED";
    }
}
