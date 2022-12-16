package metrics;

import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.christopherfrantz.dbscan.DistanceMetric;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;

public class CommonNodesAndEdges implements DistanceMetric<DcrModel> {

    @Override
    public double calculateDistance(DcrModel arg0, DcrModel arg1) throws DBSCANClusteringException {
        return DcrSimilarity.commonNodesAndEdgesSimilarity(arg0, arg1);
    }
    
}
