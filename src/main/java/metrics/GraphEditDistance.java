package metrics;

import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.christopherfrantz.dbscan.DistanceMetric;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;

public class GraphEditDistance implements DistanceMetric<DcrModel> {

    @Override
    public double calculateDistance(DcrModel arg0, DcrModel arg1) throws DBSCANClusteringException {
//        System.out.println("Similarity: " + DcrSimilarity.graphEditDistanceSimilarity(arg0, arg1));
        return 1.0-DcrSimilarity.graphEditDistanceSimilarity(arg0, arg1);
    }
    
    @Override
    public String toString() {
        return "GED";
    }
}
