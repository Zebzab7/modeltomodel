package beamline.dcr.modeltomodel.testrunners;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import jdk.nashorn.api.tree.ForInLoopTree;

public class Test {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModelPath = currentPath + "/driftedmodels/ResearchPaper/ResearchPaper10.xml";
        
        int numOfTraces = 1;
        int traceLength = 20;
        
        DcrModel CRModel = new DcrModel();
        CRModel.loadModel(groundTruthModelPath);
        
        HashMap<String, String> labelMappings = CRModel.getLabelMappings();
        
        TraceGenerator traceGenerator = new TraceGenerator();
        ArrayList<ArrayList<String>> traces = new ArrayList<ArrayList<String>>();
        ArrayList<ArrayList<String>> allTracesLabels = new ArrayList<ArrayList<String>>();
        
        for (int i = 0; i < numOfTraces; i++) {
            ArrayList<String> trace = traceGenerator.generateRandomTraceFromModel(CRModel, traceLength);
            ArrayList<String> traceLabels = new ArrayList<String>();
            for (int j = 0; j < trace.size(); j++) {
                traceLabels.add(labelMappings.get(trace.get(j)));
            }
            allTracesLabels.add(traceLabels);
            traces.add(trace); 
        }
        
        for (int i = 0; i < traces.size(); i++) {
            for (int j = 0; j < allTracesLabels.get(i).size(); j++) {
                System.out.println(allTracesLabels.get(i).get(j));
            }
        }
    }
}






