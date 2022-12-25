package beamline.dcr.testsoftware.testrunners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.tuple.Triple;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.DcrModelExecution;
import beamline.dcr.testsoftware.ModelAdaption;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison.DRIFT;
import beamline.dcr.view.DcrModelXML;

public class TraceGenerator {
    static Random rand = new Random();
    private static int eventLogNumber = 101;
    private static int traceLength = 20;
    private static int traces = 10;
    private static int modelVariations = 3;
    private static int driftStrength = 10;
    private static DRIFT driftType = DRIFT.SUDDEN;
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModelPath = currentPath + "/groundtruthmodels/Process" + eventLogNumber + ".xml";
        
        ArrayList<DcrModel> referenceModels = new ArrayList<DcrModel>();

        DcrModel initModel = new DcrModel();
        initModel.loadModel(groundTruthModelPath);
        
        referenceModels.add(initModel.getClone());
        (new DcrModelXML(initModel)).toFile(currentPath + "/groundtruthmodels/" + "Process" + (eventLogNumber+10));
        
        ModelAdaption modelAdaption = new ModelAdaption(initModel);
        
        for (int i = 1; i < modelVariations; i++) {
            (new DcrModelXML(initModel)).toFile(currentPath + "/groundtruthmodels/" + "Process" + (eventLogNumber+10+i));
            if (!modelAdaption.everyMutation(driftStrength)) {
                System.out.println("Mutation operation was unsuccessful");
            } 
            referenceModels.add(modelAdaption.getModel().getClone());
        }
        
        if (generateTraces(referenceModels)) {
            System.out.println("Traces have been generated");
        } 
    }
    
    public static boolean generateTraces(ArrayList<DcrModel> models) {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        
        LinkedHashMap<String, List<String>> generatedTraces = new LinkedHashMap<String, List<String>>();
        
        for (int k = 0; k < modelVariations; k++) {
            DcrModel model = models.get(k);
            
            for (int i = 0; i < traces; i++) {
                DcrModelExecution execution = new DcrModelExecution(model.getClone());
                Triple<ArrayList<String>, ArrayList<String>, ArrayList<String>> marking 
                    = execution.getMarking();
                
                for (int j = 0; j < traceLength; j++) {
                    ArrayList<String> executionOrder = new ArrayList<String>(model.getActivities());
                    Collections.shuffle(executionOrder);
                    
                    for (int l = 0; l < executionOrder.size(); l++) {
                        if (execution.executeActivity(executionOrder.get(l))) {
                            break;
                        }
                    }
                }
                generatedTraces.put("Trace" + i, execution.getTrace());
//                System.out.println(execution.getTrace().toString());
            }
            saveLog(currentPath + "/eventlogs/eventlog_graph" + (eventLogNumber+k+10), generatedTraces);
        }
        return true;
    }
    
    public static void saveLog(String fileName, Map<String, List<String>> observedActivitiesInTrace) {
        
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        Document document = documentBuilder.newDocument();

        Element root = document.createElement("log");
        document.appendChild(root);

        observedActivitiesInTrace.forEach((key, value) -> {
            Element trace = document.createElement("trace");
            Element traceString = document.createElement("string");
            traceString.setAttribute("key","concept:name");
            traceString.setAttribute("value",key);
            trace.appendChild(traceString);
            for (String activityName : value){
                Element event = document.createElement("event");
                Element eventString = document.createElement("string");
                eventString.setAttribute("key","concept:name");
                eventString.setAttribute("value",activityName);
                event.appendChild(eventString);

                trace.appendChild(event);

            }
            root.appendChild(trace);
        });


        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(fileName + ".xes"));
            transformer.transform(domSource, streamResult);
        } catch (TransformerException e) {
            System.out.println(e);
            e.printStackTrace();
        }

    }
    
}
