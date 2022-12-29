package beamline.dcr.modeltomodel.testrunners;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import beamline.core.web.miner.models.MinerParameterValue;
import beamline.dcr.miners.DFGBasedMiner;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.UnionRelationSet;
import beamline.dcr.modeltomodel.DcrSimilarity;
import beamline.dcr.modeltomodel.ModelRepository;
import beamline.dcr.testsoftware.ConformanceChecking;
import beamline.dcr.testsoftware.ModelComparison;
import beamline.dcr.testsoftware.TransitionSystem;
import beamline.dcr.view.DcrModelXML;

public class BasicStreamDriftDetection {
    public static void main(String[] args) throws Exception {
        //Test parameters
        int eventlogNumber = 111;
        int relationsThreshold = 0;
        double sigDiff = 0.9;
        String[] patternList = ("Condition Response").split(" ");
        String[] transitiveReductionList = (" ").split(" ");
        boolean saveAsXml = false;
        boolean saveEventLogs = false;
        int maxTraces = 5;
        int traceSize = 5;
        int observationsBeforeEvaluation = 5;
        int logs = 2;
        String[] dcrConstraints = ("Condition Response").split(" ");
        //
        ModelRepository repo = new ModelRepository();
        
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        
        DFGBasedMiner sc = new DFGBasedMiner();
        Collection<MinerParameterValue> coll = new ArrayList<>();
        
        MinerParameterValue confParam = new MinerParameterValue("DCR Patterns", patternList);
        coll.add(confParam);
        MinerParameterValue transParam = new MinerParameterValue("Transitive Reduction", transitiveReductionList);
        coll.add(transParam);
        MinerParameterValue relationThresholdParam = new MinerParameterValue("Relations Threshold", relationsThreshold);
        coll.add(relationThresholdParam);
        MinerParameterValue dcrConstraintsParam = new MinerParameterValue("DCR Constraints", dcrConstraints);
        coll.add(dcrConstraintsParam);
        
        MinerParameterValue fileParam2 = new MinerParameterValue("Stream Miner", "Sliding Window");
        coll.add(fileParam2);
        MinerParameterValue fileParam3 = new MinerParameterValue("Trace Window Size", traceSize);
        coll.add(fileParam3);
        MinerParameterValue fileParam4 = new MinerParameterValue("Max Traces", maxTraces);
        coll.add(fileParam4);
        
        sc.configure(coll);
        
        for (int i = 0; i < logs; i++) {
            String groundTruthModelPath = currentPath + "/groundtruthmodels/Process" + (eventlogNumber+i) +".xml";
            String streamPath = currentPath + "/eventlogs/eventlog_graph"+(eventlogNumber+i)+ ".xes";
            
            File xesFile = new File(streamPath);
            
            XesXmlParser xesParser = new XesXmlParser();
            List<XLog> parsedXesFile = xesParser.parse(xesFile);
            
            //Define test stream
            Map<String, List<String>> traceCollection = new LinkedHashMap<String, List<String>>();
            Map<String,Integer> traceExecutionTime= new LinkedHashMap<String, Integer>();
            Map<String,Integer> traceCurrentIndex= new LinkedHashMap<String, Integer>();
            int counter = 1;
            int totalObservations = 0;
            for (XLog traces : parsedXesFile){
                for (XTrace trace : traces){
                    String traceId = trace.getAttributes().get("concept:name").toString();
                    if (!traceCollection.containsKey(traceId)){
                        traceCollection.put(traceId,new ArrayList<>());
                        traceExecutionTime.put(traceId,(counter % 5)+1);
                        traceCurrentIndex.put(traceId,0);
                        counter ++;
                    }
                    for (XEvent event : trace ){
                        totalObservations++;
                        String activity = event.getAttributes().get("concept:name").toString();
                        //String activity = event.getAttributes().get("EventName").toString(); // Dreyer's fond
                        traceCollection.get(traceId).add(activity);
                    }
                }
            }
            
            DcrModel referenceModel = new DcrModel();
            referenceModel.loadModel(groundTruthModelPath);
            
            DcrModel groundTruthModel = new DcrModel();
            groundTruthModel.loadModel(groundTruthModelPath);
    
            // simulate stream
            int currentObservedEvents = 0;
            int currentIteration = 1;
            
            int comparisons = 0;
            int drifts = 0;
            
            while(currentObservedEvents < totalObservations) {
                for (Map.Entry<String, Integer> traceExecutionEntry : traceExecutionTime.entrySet()) {
                    String currentTraceId = traceExecutionEntry.getKey();
                    int currentTraceIndex = traceCurrentIndex.get(currentTraceId);
                    int numActivitiesInTrace = traceCollection.get(currentTraceId).size();
                    if (currentIteration % traceExecutionEntry.getValue() == 0 &&
                            currentTraceIndex<numActivitiesInTrace) {
                        String activityName = traceCollection.get(currentTraceId).get(currentTraceIndex);
                        sc.consumeEvent(currentTraceId, activityName);
                        traceCurrentIndex.replace(currentTraceId, currentTraceIndex + 1);
                        currentObservedEvents++;
                        if (currentObservedEvents % observationsBeforeEvaluation == 0) {
                            
                            DcrModel discoveredModel = sc.getDcrModel();
                            double simRef = DcrSimilarity.graphEditDistanceSimilarityWithWeights(referenceModel, discoveredModel);
                            double simTrue = DcrSimilarity.graphEditDistanceSimilarityWithWeights(groundTruthModel, discoveredModel);
                            
                            System.out.println("Similarity to reference: " + simRef);
                            System.out.println("Similarity to true model: " + simTrue);
                            
                            boolean changeDetected = false;
                            comparisons++;
                            if (simRef < sigDiff) {
                                drifts++;
                                changeDetected = true;
//                                System.out.println("A change is detected, updating model...");
                                referenceModel = discoveredModel;
                            } else {
                                changeDetected = false;
//                                System.out.println("Insignificant change...");
                            }
                            System.out.println();
                        }
                    }
                }
                currentIteration++;
    //                System.out.println(currentObservedEvents + " of " + totalObservations);
            }
            
            System.out.println(drifts + " drifts out of " + comparisons + " comparisons");
            
            // Reset all trace indexes to 0 
            for (XLog traces : parsedXesFile){
                for (XTrace trace : traces) {
                    String traceId = trace.getAttributes().get("concept:name").toString();
                    traceCurrentIndex.replace(traceId, 0);
                }
            }
        }
        /*
        String outputDirectoryPath =  currentPath + "/evaluations/"+ eventlogNumber +"/modelmodel";

        File outputDirectoryObject = new File(outputDirectoryPath);
        if (!outputDirectoryObject.exists()){
            outputDirectoryObject.mkdirs();
        }
        String filePath = outputDirectoryPath + "/results_" + "GEDW" + observationsBeforeEvaluation+ ".csv";
        File myObj = new File(filePath);

        myObj.createNewFile();
        try {
            FileWriter myWriter = new FileWriter(filePath);
            String columnTitles ="maxTraces,traceSize,observed,change_detected,sigDiff,GEDWRef,GEDWTrue\n";

            myWriter.write(columnTitles+csvResults);
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        */
        System.exit(0);
    }
}