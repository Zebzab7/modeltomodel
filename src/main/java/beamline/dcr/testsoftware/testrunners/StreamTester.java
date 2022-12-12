package beamline.dcr.testsoftware.testrunners;

import beamline.core.web.miner.models.MinerParameterValue;

import beamline.core.web.miner.models.Stream;
import beamline.dcr.miners.DFGBasedMiner;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.UnionRelationSet;
import beamline.dcr.model.relations.dfg.ExtendedDFG;
import beamline.dcr.testsoftware.ConformanceChecking;
import beamline.dcr.testsoftware.ModelComparison;
import beamline.dcr.testsoftware.TransitionSystem;
import beamline.dcr.view.DcrModelXML;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class StreamTester {
    public static void main(String[] args) throws Exception {
        //Test parameters
        String eventlogNumber =args[0];
        int relationsThreshold = Integer.parseInt(args[1]);
        String[] patternList = args[2].split(" ");
        String[] transitiveReductionList = args[3].split(" ");
        boolean saveAsXml = Boolean.parseBoolean(args[4]);
        boolean compareToDisCoveR = Boolean.parseBoolean(args[5]); // false for reference model true for DisCoveR at windowsize
        boolean saveEventLogs= Boolean.parseBoolean(args[6]);
        String[] traceWindowSizesStringList = args[7].split(" ");
        String[] maxTracesStringList = args[8].split(" ");
        int observationsBeforeEvaluation = Integer.parseInt(args[9]);
        String[] dcrConstraints = args[10].split(" ");
        //

        Set<Integer> traceWindowSizes = new HashSet<>();
        for (String size : traceWindowSizesStringList ){
            traceWindowSizes.add(Integer.parseInt(size));
        }
        Set<Integer> maxTracesList = new HashSet<>();
        for (String size : maxTracesStringList ){
            maxTracesList.add(Integer.parseInt(size));
        }

        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";

        StringBuilder csvResults = new StringBuilder();

        String groundTruthModelPath = currentPath + "/groundtruthmodels/Process" + eventlogNumber +".xml";
        String streamPath = currentPath + "/eventlogs/eventlog_graph"+eventlogNumber+ ".xes";

        File xesFile = new File(streamPath);
        XesXmlParser xesParser = new XesXmlParser();

        List<XLog> parsedXesFile = xesParser.parse(xesFile);

        //Define test stream
        Map<String, List<String>> traceCollection = new HashMap<String, List<String>>();
        Map<String,Integer> traceExecutionTime= new HashMap<String, Integer>();
        Map<String,Integer> traceCurrentIndex= new HashMap<String, Integer>();
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

        for(int maxTraces : maxTracesList){

            for(int traceSize : traceWindowSizes){


            String discoverModelPath = currentPath + "/discovermodels/DCR_graph" + eventlogNumber +
                    "_online_" +traceSize+".xml";
            String compareModel = compareToDisCoveR ? discoverModelPath: groundTruthModelPath ;


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

            String fileName = currentPath + "/minedmodels/online/online_mining_" + eventlogNumber+
                    "_map_" + maxTraces+ "_trace" + traceSize+
                    "_" + java.time.LocalDate.now();
            MinerParameterValue fileParam1 = new MinerParameterValue("filename", fileName);
            coll.add(fileParam1);
            MinerParameterValue fileParam2 = new MinerParameterValue("Stream Miner", "Sliding Window");
            coll.add(fileParam2);
            MinerParameterValue fileParam3 = new MinerParameterValue("Trace Window Size", traceSize);
            coll.add(fileParam3);
            MinerParameterValue fileParam4 = new MinerParameterValue("Max Traces", maxTraces);
            coll.add(fileParam4);

            sc.configure(coll);


            // simulate stream
            int currentObservedEvents = 0;
            int currentIteration = 1;
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
                            if (saveEventLogs){
                                sc.saveCurrentWindowLog(currentPath + "/eventlogs/online/online_eventlog_graph"+eventlogNumber+
                                        "maxtraces"+maxTraces +"_tracesize"+traceSize + "_obs" + currentObservedEvents);
                            }



                            ExtendedDFG extendedDFG = sc.getExtendedDFG();
                            DcrModel dcrModel = sc.getDcrModel();
                            //comparison
                            UnionRelationSet unionRelationSet = sc.getUnionRelationSet();
                            TransitionSystem transitionSystem = new TransitionSystem(unionRelationSet);
                            ConformanceChecking conformanceChecking = new ConformanceChecking(streamPath,transitionSystem);
                            conformanceChecking.checkConformance();
                            ModelComparison modelComparison = new ModelComparison(dcrModel);
                            modelComparison.loadComparativeModel(compareModel);
                            double jaccard = modelComparison.getJaccardSimilarity();

                            String row = modelComparison.getJaccardSimilarity() +"," + modelComparison.getPrecision() +
                                    "," + modelComparison.getRecall() + "," + conformanceChecking.getFitness() + "," +
                                    conformanceChecking.getPrecision() + "," + conformanceChecking.getIllegalTracesString();

                            csvResults.append(maxTraces + ",").append(traceSize).append(",").append(currentObservedEvents).append(",")
                                    .append(row).append("\n");
                            if (saveAsXml){
                                new DcrModelXML(dcrModel).toFile(fileName+"_obs"+currentObservedEvents);
                            }
                        }
                    }
                }
                currentIteration++;
                System.out.println(currentObservedEvents + " of " + totalObservations);
            }
        }
        }
        String outputDirectoryPath =  currentPath + "/evaluations/"+ eventlogNumber +"/modelmodel";

        File outputDirectoryObject = new File(outputDirectoryPath);
        if (!outputDirectoryObject.exists()){
            outputDirectoryObject.mkdirs();
        }
        String filePath = outputDirectoryPath + "/results_" + observationsBeforeEvaluation+ ".csv";
        File myObj = new File(filePath);

        myObj.createNewFile();
        try {
            FileWriter myWriter = new FileWriter(filePath,true);
            String columnTitles ="maxTraces,traceSize,observed,model_jaccard,model_precision,model_recall,log_fitness,log_precision,illegal_traces\n";

            myWriter.write(columnTitles+csvResults);
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        System.exit(0);
    }
}
