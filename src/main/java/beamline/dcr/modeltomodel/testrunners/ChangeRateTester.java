package beamline.dcr.modeltomodel.testrunners;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DriftDetector;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison;
import distancemetrics.GraphEditDistance;

public class ChangeRateTester {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DBSCANClusteringException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/";
        String modelPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/groundtruthmodels/process101.xml";
        
        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(modelPath);
        
        ArrayList<DcrModel> models = PatternChangeComparison.gradualDriftMutations(referenceModel);
        
        int drifts = DriftDetector.DBSCANChangeRate(models, referenceModel, 0.05, 5, new GraphEditDistance());
        System.out.println(drifts);
    }
}
