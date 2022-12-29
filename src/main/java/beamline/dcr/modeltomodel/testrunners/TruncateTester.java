package beamline.dcr.modeltomodel.testrunners;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.christopherfrantz.dbscan.DistanceMetric;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DriftDetector;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison;
import distancemetrics.GraphEditDistance;
import distancemetrics.WeightedGraphEditDistance;

public class TruncateTester {
    static double eps = 0.15;
    static int minPoints = 10;
    static DistanceMetric<DcrModel> metric = new GraphEditDistance();
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DBSCANClusteringException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/";
        String modelPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/groundtruthmodels/process101.xml";
        
        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(modelPath);
        
        ArrayList<DcrModel> suddenModels = PatternChangeComparison.suddenDriftMutations(referenceModel);
        ArrayList<DcrModel> gradualModels = PatternChangeComparison.gradualDriftMutations(referenceModel);
        
//        System.out.println("Sudden no truncate: " + DriftDetector.DBSCAN(suddenModels, eps, minPoints, metric));
//        System.out.println("Sudden truncate: " + DriftDetector.DBSCANWithTruncation(suddenModels, eps, minPoints, metric));
        System.out.println("Gradual no truncate: " + DriftDetector.DBSCAN(gradualModels, eps, minPoints, metric));
        System.out.println("Gradual truncate: " + DriftDetector.DBSCANWithTruncation(gradualModels, eps, minPoints, metric));
    }
}
