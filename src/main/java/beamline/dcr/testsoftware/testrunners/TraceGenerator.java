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
import beamline.dcr.view.DcrModelXML;

public class TraceGenerator {
    static Map<String, List<String>> observedActivitiesInTrace = new LinkedHashMap<>();
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        
        int eventLogNumber = 101;
        int traceLength = 20;
        int traces = 10;
        int modelVariations = 2;
        int driftStrength = 10;
        
        Random rand = new Random();
        
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";

        String groundTruthModelPath = currentPath + "/groundtruthmodels/Process" + eventLogNumber + ".xml";

        DcrModel initModel = new DcrModel();
        initModel.loadModel(groundTruthModelPath);
        
        ArrayList<DcrModel> models = new ArrayList<DcrModel>();
        
        models.add(initModel.getClone());
        
        ModelAdaption modelAdaption = new ModelAdaption(initModel);
        for (int i = 0; i < modelVariations-1; i++) {
            if (!modelAdaption.insertActivitySerial(driftStrength) ||
                    !modelAdaption.insertActivityParallel(driftStrength) ||
                    !modelAdaption.deleteActivity(driftStrength) ||
                    !modelAdaption.replaceActivity(driftStrength) ||
                    !modelAdaption.addConstraint(driftStrength) ||
                    !modelAdaption.removeConstraint(driftStrength) ||
                    !modelAdaption.swapActivities(driftStrength)) {
                System.out.println("Mutation operation was unsuccessful");
            } 
            models.add(modelAdaption.getModel().getClone());
        }
        
        for (int k = 0; k < modelVariations; k++) {
            DcrModel model = models.get(k);
            
            System.out.println("Size (Activity): " + model.getActivities().size());
            (new DcrModelXML(model)).toFile(currentPath + "/groundtruthmodels/" + "Process" + (eventLogNumber+k+10));
            
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
                observedActivitiesInTrace.put("Trace" + i, execution.getTrace());
//                System.out.println(execution.getTrace().toString());
            }
            saveLog(currentPath + "/eventlogs/eventlog_graph" + (eventLogNumber+k+10));
            System.out.println(observedActivitiesInTrace.size());
        }
    }
    
    public static void saveLog(String fileName) {
        
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
