package metrics;

import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.christopherfrantz.dbscan.DistanceMetric;

import beamline.dcr.model.relations.DcrModel;

public class DumbDistance implements DistanceMetric<DcrModel> {

    @Override
    public double calculateDistance(DcrModel arg0, DcrModel arg1) throws DBSCANClusteringException {
        return 0.0;
    }
    
    @Override
    public String toString() {
        return "DumbDist";
    }
}
