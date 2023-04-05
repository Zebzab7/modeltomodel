package beamline.dcr.modeltomodel.testrunners;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;

public class LevenshteinTest {
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String OGModelPath = currentPath + "/driftedmodels/ResearchPaper/ResearchPaper1.xml";
        String newModelPath = currentPath + "/driftedmodels/ResearchPaper/ResearchPaper10.xml";
        
        DcrModel OGModel = new DcrModel();
        DcrModel newModel = new DcrModel();
        
        OGModel.loadModel(OGModelPath);
        newModel.loadModel(newModelPath);
        
        System.out.println(DcrSimilarity.levenshteinDistanceSimilarity(OGModel, newModel));    
    }
}
