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
import distancemetrics.WeightedGraphEditDistance;
import helper.Pair;

public class StreamDriftDetection {
    static Random rand = new Random();
    
    public static void main(String[] args) throws Exception {
      //Test parameters
        int eventlogNumber = 111;
        int relationsThreshold = 0;
        double eps = 0.2;
        int minPoints = 8;
        String[] patternList = ("Condition Response").split(" ");
        String[] transitiveReductionList = (" ").split(" ");
        int maxTraces = 10;
        int traceSize = 10;
        double updatePercentage = 2.0;
        int logs = 1;
        DRIFT driftType = DRIFT.SUDDEN;
        String[] dcrConstraints = ("Condition Response").split(" ");
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
        
        StringBuilder outputString 
            = new StringBuilder("baseline-sim, discover-sim\n");
        
        DcrModel baseline = new DcrModel();
        baseline.loadModel(currentPath + "/groundtruthmodels/Process" + (eventlogNumber) +".xml");
        
        ArrayList<Pair<String, String>> traceExecutionOrder = getExecutionOrderFromDriftType(traceLogs, driftType);

        int observationsBeforeEvaluation = (int) (traceExecutionOrder.size()*(updatePercentage/100));
        int totalObservations = traceExecutionOrder.size();
        for (int i = 0; i < totalObservations; i++) {
            Pair<String, String> event = traceExecutionOrder.get(i);
            String traceID = event.getLeft();
            String activity = event.getRight();
            System.out.println("trace: " + traceID);
            System.out.println("activity: " + activity);
            sc.consumeEvent(traceID, activity);
            if (i % observationsBeforeEvaluation == 0) {
                DcrModel discoveredModel = sc.getDcrModel();
                if (discoveredModels.size() > 1) {
                    
                    double discoveredSimilarityScore = DcrSimilarity.jaccardSimilarity
                                (discoveredModel, discoveredModels.get(discoveredModels.size()-1));
                    
                    double baselineSimilarityScore = DcrSimilarity.graphEditDistanceSimilarity(discoveredModel, baseline);
                    outputString.append(baselineSimilarityScore + "," + discoveredSimilarityScore + "\n");
                }
                discoveredModels.add(discoveredModel);
            }
            System.out.println("Observed " + i + " out of " + totalObservations + " observations");
        }
        
        System.out.println(discoveredModels.size() + " models were added");
        System.out.println("Total observations: " + totalObservations);
        
        ArrayList<DcrModel> trimmedModels 
            = DriftDetector.removeAndReplaceBoundaryElements(discoveredModels, baseline, new GraphEditDistance());
        
        int drifts = DriftDetector.DBSCAN(trimmedModels, eps, minPoints, new GraphEditDistance()).getLeft();
        System.out.println("Detected " + drifts + " drifts...");
        outputString.append("Drifts detected: ," + drifts + "\n");

        FileWriter myWriter 
            = new FileWriter(currentPath + "/evaluations/StreamDriftTest/" + "StreamDriftTest-" 
                    + driftType + "-" + java.time.LocalDate.now() + ".csv"/*,true*/);
        myWriter.write(outputString.toString());
        myWriter.close();
        
        System.exit(0);
    }
    
    /**
     * Returns a randomized execution list that conforms to the type of drift
     * from a given list of trace logs generated from a number of process models
     */
    public static ArrayList<Pair<String, String>> getExecutionOrderFromDriftType(ArrayList<XLog> traceLogs, DRIFT driftType) {
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
                if (logs < 2) throw new IllegalArgumentException("Gradual drift not supported for less than 2 graphs");
                ArrayList<ArrayList<Pair<String, String>>> orders = new ArrayList<ArrayList<Pair<String, String>>>();
                
                for (int i = 0; i < logs; i++) {
                    orders.add(getRandomExecutionOrderFromLog(traceLogs.get(i), traceCurrentIndex));
                }
                
                int trimPercentage = 10;
                int numOfElementsTrimmed = orders.get(0).size()/trimPercentage;
                
                for (int i = 0; i < orders.size()-1; i++) {
                    ArrayList<Pair<String, String>> combination 
                        = getGradualCombinationOfEvents(orders.get(i), orders.get(i+1), numOfElementsTrimmed);
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
            = getSubArray(list1, (list1.size()-1)-numOfElementsTrimmed, list1.size()-1);
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
        for (int j = 0; j < sumElements; j++) {
            double probability = 0.0;
            if (j % (sumElements/20) == 0 && probability < 1.0) probability += 0.05;
            
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
        
        for (int i = startIndex; i < endIndex; i++) {
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




