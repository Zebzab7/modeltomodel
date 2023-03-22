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
        String OGModelPath = currentPath + "/driftedmodels/ComputerRepairOriginal.xml";
//        String newModelPath = currentPath + "/driftedmodels/ComputerRepairOriginal.xml";
        String newModelPath = currentPath + "/driftedmodels/AlternateComputerRepair16.xml";
        
        DcrModel OGModel = new DcrModel();
        DcrModel newModel = new DcrModel();
        
        OGModel.loadModel(OGModelPath);
        newModel.loadModel(newModelPath);
        
        TraceGenerator traceGenerator = new TraceGenerator();
        ArrayList<ArrayList<String>> modelATraces = new ArrayList<ArrayList<String>>();
        ArrayList<ArrayList<String>> modelBTraces = new ArrayList<ArrayList<String>>();
        
        modelATraces.add(traceGenerator.generateRandomTraceFromModel(OGModel, 10));
        modelBTraces.add(traceGenerator.generateRandomTraceFromModel(newModel, 10));
        
        ArrayList<String> trace1 = modelATraces.get(0);
        ArrayList<String> trace2 = modelBTraces.get(0);
        
        System.out.println(trace1);
        System.out.println(trace2);
        
        System.out.println(DcrSimilarity.levenshteinDistance(trace1, trace2));
    }
}
