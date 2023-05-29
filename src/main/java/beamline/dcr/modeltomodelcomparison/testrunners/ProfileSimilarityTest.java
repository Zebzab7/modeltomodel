package beamline.dcr.modeltomodelcomparison.testrunners;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.ActivityRelations;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodelcomparison.DcrSimilarity;

public class ProfileSimilarityTest {

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String modelPath = currentPath + "/driftedmodels/ResearchPaperExample/ResearchPaper1.xml";
        String modelPath2 = currentPath + "/driftedmodels/ResearchPaperExample/ResearchPaper2.xml";

        // String modelPath = currentPath + "/RegexModels/DCRRegexTest.xml";
        // String modelPath2 = currentPath + "/RegexModels/DCRRegexTest2.xml";
        
        DcrModel model1 = new DcrModel();
        model1.loadModel(modelPath);

        DcrModel model2 = new DcrModel();
        model2.loadModel(modelPath2);

        System.out.println();
        System.out.println(DcrSimilarity.behavioralProfileSimilarity(model1, model2));
    }
}
