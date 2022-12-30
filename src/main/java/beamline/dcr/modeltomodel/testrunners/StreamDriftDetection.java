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
import java.util.Random;
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
import beamline.dcr.modeltomodel.DriftDetector;
import beamline.dcr.testsoftware.ConformanceChecking;
import beamline.dcr.testsoftware.ModelComparison;
import beamline.dcr.testsoftware.TransitionSystem;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison.DRIFT;
import beamline.dcr.view.DcrModelXML;
import distancemetrics.GraphEditDistance;
import distancemetrics.WeightedGraphEditDistance;
import helper.Pair;

public class StreamDriftDetection {
    static Random rand = new Random();
    
    public static void main(String[] args) throws Exception {
      //Test parameters
        int eventlogNumber = 111;
        int relationsThreshold = 0;
        double eps = 0.15;
        int minPoints = 3;
        String[] patternList = ("Condition Response Include Exclude").split(" ");
        String[] transitiveReductionList = (" ").split(" ");
        int maxTraces = 10;
        int traceSize = 10;
        double updatePercentage = 2.0;
        int logs = 2;
        DRIFT driftType = DRIFT.SUDDEN;
        String[] dcrConstraints = ("Condition Response Include Exclude").split(" ");
        //
        
        ArrayList<DcrModel> discoveredModels = new ArrayList<DcrModel>();
        
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
        XesXmlParser xesParser = new XesXmlParser();
        ArrayList<XLog> traceLogs = new ArrayList<XLog>();
        
        for (int i = 0; i < logs; i++) {
            String groundTruthModelPath = currentPath + "/groundtruthmodels/Process" + (eventlogNumber+i) +".xml";
            String streamPath = currentPath + "/eventlogs/eventlog_graph"+ (eventlogNumber+i) + ".xes";
            
            File xesFile = new File(streamPath);
            
            List<XLog> parsedXesFile = xesParser.parse(xesFile);
            traceLogs.addAll(parsedXesFile);
        }
        
        DcrModel baseline = new DcrModel();
        baseline.loadModel(currentPath + "/groundtruthmodels/Process" + (eventlogNumber) +".xml");
        
        ArrayList<Pair<String, String>> traceExecutionOrder = getExecutionOrder(traceLogs, driftType);
        
        int observationsBeforeEvaluation = (int) (traceExecutionOrder.size()*(updatePercentage/100));
        int totalObservations = traceExecutionOrder.size();
        for (int i = 0; i < totalObservations; i++) {
            Pair<String, String> event = traceExecutionOrder.get(i);
            String traceID = event.getLeft();
            String activity = event.getRight();
            sc.consumeEvent(traceID, activity);
            if (i % observationsBeforeEvaluation == 0) {
                DcrModel discoveredModel = sc.getDcrModel();
                if (discoveredModels.size() > 1) {
                    System.out.println(
                            DcrSimilarity.graphEditDistanceSimilarity(
                                    discoveredModel, discoveredModels.get(discoveredModels.size()-1)));
                }
                discoveredModels.add(discoveredModel);
            }
        }
        
        System.out.println("\n" + discoveredModels.size());
        System.out.println("Total: " + totalObservations);
        
        ArrayList<DcrModel> trimmedModels 
            = DriftDetector.removeAndReplaceBoundaryElements(discoveredModels, baseline, new WeightedGraphEditDistance());
        
        int drifts = DriftDetector.DBSCAN(discoveredModels, eps, minPoints, new WeightedGraphEditDistance());
        System.out.println("Detected " + drifts + " drifts...");
        System.exit(0);
    }
    
    public static ArrayList<Pair<String, String>> getExecutionOrder(ArrayList<XLog> traceLogs, DRIFT driftType) {
        ArrayList<Pair<String, String>> order = new ArrayList<Pair<String, String>>();
        
        int logs = traceLogs.size();
        int tracesPerLog = traceLogs.get(0).size();
        int traceSize = traceLogs.get(0).get(0).size();
        
        int numOfEvents = logs * tracesPerLog * traceSize;
        
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        
        Map<String,Integer> traceCurrentIndex= new HashMap<String, Integer>();
        
        for (int i = 0; i < logs; i++){
            
            XLog traceLog = traceLogs.get(i);
            for (int j = 0; j < traceLog.size(); j++){
                
                XTrace trace = traceLog.get(j);
                String traceId = trace.getAttributes().get("concept:name").toString();
                traceCurrentIndex.put(traceId,0);
            }
        }
        
        switch (driftType) {
            case GRADUAL:
                break;
            case INCREMENTAL:
                break;
            default:
                int subDivision = numOfEvents/logs;
                for (int i = 0; i < logs; i++) {
                    
                    int count = 0;
                    XLog currentLog = traceLogs.get(i);
                    int logSize = currentLog.size();
                    
                    while (count < subDivision) {
                        
                        int num = rand.nextInt(logSize);
                        int currentIndex = traceSize;
                        XTrace trace = currentLog.get(num);
                        String traceId = trace.getAttributes().get("concept:name").toString();
                        
                        while (currentIndex == traceSize) {
                            trace = currentLog.get(num);
                            traceId = trace.getAttributes().get("concept:name").toString();
                            currentIndex = traceCurrentIndex.get(traceId);
                            num = (num+1)%logSize;
                        }
                        
                        String activity = trace.get(currentIndex)
                                    .getAttributes().get("concept:name").toString();
                        order.add(new Pair<String,String>(traceId, activity));
                        traceCurrentIndex.replace(traceId, (currentIndex+1));
                        count++;
                    }
                }
                break;
        }
        return order;
    }
}



