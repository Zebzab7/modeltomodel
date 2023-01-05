package distancemetrics;

import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.christopherfrantz.dbscan.DistanceMetric;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;

public class JaccardDistance1D implements DistanceMetric<DcrModel> {

    @Override
    public double calculateDistance(DcrModel arg0, DcrModel arg1) throws DBSCANClusteringException {
        return 1.0-DcrSimilarity.jaccardSimilarity(arg0, arg1);
    }
    
    @Override
    public String toString() {
        return "JaccardDist";
    }
}
