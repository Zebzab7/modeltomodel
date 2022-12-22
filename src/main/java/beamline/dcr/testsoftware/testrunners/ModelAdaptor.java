package beamline.dcr.testsoftware.testrunners;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;
import beamline.dcr.testsoftware.ModelAdaption;
import beamline.dcr.view.DcrModelJson;
import beamline.dcr.view.DcrModelXML;

public class ModelAdaptor {
    

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        int driftStrength = 5;
        int eventLog = 101;

        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthPath = currentPath + "/groundtruthmodels/Process" + eventLog + ".xml";
        
        
        ArrayList<DcrModel> models = new ArrayList<DcrModel>();
        
        DcrModel model1 = new DcrModel();
        model1.loadModel(groundTruthPath);
        DcrModel model2 = model1.getClone();
        models.add(model1);
        models.add(model2);
        
        ModelAdaption modelAdaption = new ModelAdaption(model1.getClone());
        if (!modelAdaption.insertActivitySerial(driftStrength) ||
                !modelAdaption.insertActivityParallel(driftStrength) ||
                !modelAdaption.deleteActivity(driftStrength) ||
                !modelAdaption.replaceActivity(driftStrength) ||
                !modelAdaption.addConstraint(driftStrength) ||
                !modelAdaption.removeConstraint(driftStrength) ||
                !modelAdaption.swapActivities(driftStrength)) {
            System.out.println("Adaption unsuccessful");
        }
        models.add(modelAdaption.getModel());
        
        
        for (int i = 0; i < models.size(); i++) {
            new DcrModelXML(models.get(i)).toFile(currentPath + "/modelvariations/" + "suddenProcess101-" + i);
//            new DcrModelJson(models.get(i)).toFile(currentPath + "/modelvariations/" + "suddenProcess101-" + i);
        }
        
//        DcrModel m1 = new DcrModel();
//        m1.loadModel(currentPath + "/modelvariations/" + "suddenProcess1010" + ".xml");
//        DcrModel m2 = new DcrModel();
//        m2.loadModel(currentPath + "/modelvariations/" + "suddenProcess1011" + ".xml");
//        DcrModel m3 = new DcrModel();
//        m3.loadModel(currentPath + "/modelvariations/" + "suddenProcess1012" + ".xml");
        
//        System.out.println(DcrSimilarity.graphEditDistanceSimilarityWithWeights(m1, m2));
//        System.out.println(DcrSimilarity.graphEditDistanceSimilarityWithWeights(m1, m3));
//        System.out.println(DcrSimilarity.graphEditDistanceSimilarityWithWeights(m2, m3));
//        
        System.out.println("Models written...");
    }
}





