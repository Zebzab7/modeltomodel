package beamline.dcr.modeltomodel.testrunners;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;
import beamline.dcr.testsoftware.ModelAdaption;
import beamline.dcr.view.DcrModelXML;

public class ModelGenerator {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModelPath = currentPath + "/groundtruthmodels/Process" + 101 + ".xml";
        
        ArrayList<DcrModel> referenceModels = new ArrayList<DcrModel>();
        
        DcrModel model101 = new DcrModel();
        model101.loadModel(groundTruthModelPath);
        
        referenceModels.add(model101);
        
        (new DcrModelXML(model101)).toFile(currentPath + "/groundtruthmodels/" + "Process" + (121));
        
        ModelAdaption modelAdaption = new ModelAdaption(model101.getClone());
        
        modelAdaption.everyMutation(3);
        
        System.out.println(DcrSimilarity.jaccardSimilarity(model101, modelAdaption.getModel()));
        
        (new DcrModelXML(modelAdaption.getModel())).toFile(currentPath + "/groundtruthmodels/" + "Process" + (122));
        referenceModels.add(modelAdaption.getModel().getClone());
        
        modelAdaption.everyMutation(3);
        
        (new DcrModelXML(modelAdaption.getModel())).toFile(currentPath + "/groundtruthmodels/" + "Process" + (123));
        referenceModels.add(modelAdaption.getModel().getClone());
        
        modelAdaption.everyMutation(3);
        
        (new DcrModelXML(modelAdaption.getModel())).toFile(currentPath + "/groundtruthmodels/" + "Process" + (124));
        referenceModels.add(modelAdaption.getModel().getClone());
        
        System.out.println(DcrSimilarity.jaccardSimilarity(model101, modelAdaption.getModel()));
//        TraceGenerator.generateTraces(referenceModels);
    }
}
