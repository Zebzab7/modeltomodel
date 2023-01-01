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
import beamline.dcr.modeltomodel.ModelRepository;
import beamline.dcr.testsoftware.ConformanceChecking;
import beamline.dcr.testsoftware.ModelComparison;
import beamline.dcr.testsoftware.TransitionSystem;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison.DRIFT;
import beamline.dcr.view.DcrModelXML;
import distancemetrics.GraphEditDistance;
import distancemetrics.JaccardDistance;
import distancemetrics.WeightedGraphEditDistance;
import helper.Pair;

public class StreamDriftDetection {
    static Random rand = new Random();
    
    public static void main(String[] args) throws Exception {
        int eventlogNumber = 111;
        double eps = 0.2;
        int minPoints = 8;
        int observationsBeforeEvaluation = 2;
        int logs = 2;
        String patterns = "Condition Response";
        DRIFT driftType = DRIFT.SUDDEN;
        
        boolean trimModels = true;
        
        int iterations = 10;
        
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        
        StringBuilder outputString 
            = new StringBuilder(", discover-sim\n");
        
        int[] expectedVals = new int[iterations];
        int[] predictedVals = new int[iterations];
        
        for (int i = 0; i < iterations; i++) {
            int drifts = simulateDrift(eventlogNumber, eps, minPoints, 
                    patterns, observationsBeforeEvaluation, logs, driftType, trimModels);
            expectedVals[i] = logs-1;
            predictedVals[i] = drifts;
        }

        double MSE = DriftDetector.getMeanSquareError(predictedVals, expectedVals);
        
        for (int i = 0; i < predictedVals.length; i++) {
            System.out.println(predictedVals[i]);
        }
            
        System.out.println("MSE: " + MSE);
        
//        FileWriter myWriter 
//        = new FileWriter(currentPath + "/evaluations/StreamDriftTest/" + "StreamDriftTest-" 
//                + driftType + "-" + java.time.LocalDate.now() + ".csv"/*,true*/);
//        myWriter.write(outputString.toString());
//        myWriter.close();
    }
    
    public static int simulateDrift(int eventlogNumber, double eps, int minPoints, 
                String patterns, int observationsBeforeEvaluation, int logs, DRIFT driftType, boolean trimModels) throws Exception {
        int relationsThreshold = 0;
        // Test parameters

        String[] patternList = patterns.split(" ");
        String[] transitiveReductionList = (" ").split(" ");
        String[] dcrConstraints = patterns.split(" ");
        int maxTraces = 10;
        int traceSize = 10;
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
            String streamPath = currentPath + "/eventlogs/eventlog_graph"+ (eventlogNumber+i) + ".xes";
            
            File xesFile = new File(streamPath);
            
            List<XLog> parsedXesFile = xesParser.parse(xesFile);
            traceLogs.addAll(parsedXesFile);
        }
        
        StringBuilder outputString 
            = new StringBuilder("baseline-sim, discover-sim\n");
        
        DcrModel baseline = new DcrModel();
        baseline.loadModel(currentPath + "/groundtruthmodels/Process" + (eventlogNumber) +".xml");
        
        ArrayList<Pair<String, String>> traceExecutionOrder = getExecutionOrderFromDriftType(traceLogs, driftType);

        int drifts = 0;
        
        int totalObservations = traceExecutionOrder.size();
        for (int i = 0; i < totalObservations; i++) {
            Pair<String, String> event = traceExecutionOrder.get(i);
            String traceID = event.getLeft();
            String activity = event.getRight();
            sc.consumeEvent(traceID, activity);

            // Discover model
            if (i % observationsBeforeEvaluation == 0) {
                DcrModel discoveredModel = sc.getDcrModel();
                discoveredModels.add(discoveredModel);

                // Determine if we should look for drifts
                if (discoveredModels.size() == 100) {
                    
                    // Trim models
                    ArrayList<DcrModel> trimmedModels 
                        = DriftDetector.trimModels(discoveredModels, baseline.getClone(), new JaccardDistance(), true);
                    int discoveredDrifts = DriftDetector.DBSCAN(trimmedModels, eps, minPoints, new JaccardDistance()).getLeft();
                    
                    // If we detect a drift its time to update model
                    if (discoveredDrifts > 0) {
                        baseline = discoveredModel;
                        drifts += discoveredDrifts;
                        System.out.println("Drift detected, updating model");
                    }
                    
                    // Remove all elements
                    int n = discoveredModels.size();
                    for (int j = 0; j < n; j++) {
                        discoveredModels.remove(0);
                    }
                    
                }
            }
//            System.out.println("Observed " + i + " out of " + totalObservations + " observations");
        }
        
        System.out.println("Total observations: " + totalObservations);
        
//        FileWriter myWriter 
//            = new FileWriter(currentPath + "/evaluations/StreamDriftTest/" + "StreamDriftTest-" 
//                    + driftType + "-" + java.time.LocalDate.now() + ".csv"/*,true*/);
//        myWriter.write(outputString.toString());
//        myWriter.close();
        
        return drifts;
//        System.exit(0);
    }
    
    /**
     * Returns a randomized execution list that conforms to the type of drift
     * from a given list of trace logs generated from a number of process models
     */
    public static ArrayList<Pair<String, String>> getExecutionOrderFromDriftType(ArrayList<XLog> traceLogs, DRIFT driftType) {
        ArrayList<Pair<String, String>> order = new ArrayList<Pair<String, String>>();
        
        int logs = traceLogs.size();
        
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
                if (logs < 2) throw new IllegalArgumentException("Gradual drift not supported for less than 2 graphs");
                ArrayList<ArrayList<Pair<String, String>>> orders = new ArrayList<ArrayList<Pair<String, String>>>();
                
                for (int i = 0; i < logs; i++) {
                    orders.add(getRandomExecutionOrderFromLog(traceLogs.get(i), traceCurrentIndex));
                }
                
                double trimPercentage = 15;
                int numOfElementsTrimmed = (int) (orders.get(0).size()*(trimPercentage/100.0));
                
                for (int i = 0; i < orders.size()-1; i++) {
                    ArrayList<Pair<String, String>> combination 
                        = getGradualCombinationOfEvents(orders.get(i), orders.get(i+1), numOfElementsTrimmed);
                    
                    for (Pair<String,String> pair : combination) {
                        System.out.println(pair.toString());
                    }
                    
                    order.addAll(combination);
                }
                break;
            default:
                /*
                 * Observe randomly and uniformly events from log1, then log2, then log3 etc.
                 */                
                for (int i = 0; i < logs; i++) {
                    XLog currentLog = traceLogs.get(i);
                    order.addAll(getRandomExecutionOrderFromLog(currentLog, traceCurrentIndex));
                }
                break;
        }
        return order;
    }
    
    public static <V> ArrayList<V> getGradualCombinationOfEvents(ArrayList<V> list1, ArrayList<V> list2, int numOfElementsTrimmed) {
        ArrayList<V> executionOrdersTrimmedA 
            = getSubArray(list1, list1.size()-numOfElementsTrimmed, list1.size()-1);
        ArrayList<V> executionOrdersTrimmedB 
            = getSubArray(list2, 0, numOfElementsTrimmed-1);
        
        list1.removeAll(executionOrdersTrimmedA);
        list2.removeAll(executionOrdersTrimmedB);
        
        ArrayList<V> boundaryExecutionOrder = new ArrayList<V>();
        
        int sumElements = executionOrdersTrimmedA.size() + executionOrdersTrimmedB.size();
        
        int indexA = 0;
        int indexB = 0;
        int sizeA = list1.size();
        int sizeB = list2.size();
        double probability = 0.0;
        for (int j = 0; j < sumElements; j++) {
            if (j % (sumElements/20) == 0) probability += 0.05;
            
            if (indexA == sizeA) {
                boundaryExecutionOrder.add(list2.get(indexB));
                indexB++;
            }
            if (indexB == sizeB) {
                boundaryExecutionOrder.add(list1.get(indexA));
                indexA++;
            }
            
            if (rand.nextDouble() > probability) {
                boundaryExecutionOrder.add(list1.get(indexA));
                indexA++;
            } else  {
                boundaryExecutionOrder.add(list2.get(indexB));
                indexB++;
            }
        }
        list1.addAll(boundaryExecutionOrder);
        list1.addAll(list2);
        return list1;
    }
    
    /**
     * Returns the subarray between indices as an arraylist
     */
    public static <V> ArrayList<V> getSubArray(ArrayList<V> fullList, int startIndex, int endIndex) {
        ArrayList<V> trimmedList = new ArrayList<V>();
        
        for (int i = startIndex; i <= endIndex; i++) {
            trimmedList.add(fullList.get(i));
        }
        return trimmedList;
    }
    
    /**
     * returns a list of events randomly shuffled as a mapping from the traceID to the event name
     */
    public static ArrayList<Pair<String, String>> getRandomExecutionOrderFromLog(XLog traceLog, Map<String,Integer> traceCurrentIndex) {
        ArrayList<Pair<String, String>> order = new ArrayList<Pair<String, String>>();
        
        int logSize = traceLog.size();
        int num = rand.nextInt(logSize);
        int totalEvents = 0;
        int traceSize = traceLog.get(0).size();
        
        for (int i = 0; i < traceLog.size(); i++) {
            for (XEvent event : traceLog.get(i)) totalEvents++;
        }
        
        for (int i = 0; i < totalEvents;) {
            
            XTrace trace = traceLog.get(num);
            String traceId = trace.getAttributes().get("concept:name").toString();
            int currentIndex = traceCurrentIndex.get(traceId);
            
            if (currentIndex == traceSize) {
                num = (num+1)%logSize;
                continue;
            } else {
                num = rand.nextInt(logSize);
                String activity = trace.get(currentIndex)
                        .getAttributes().get("concept:name").toString();
                order.add(new Pair<String,String>(traceId, activity));
                traceCurrentIndex.replace(traceId, (currentIndex+1));
                i++;
            }
        }
        return order;
    }
}




