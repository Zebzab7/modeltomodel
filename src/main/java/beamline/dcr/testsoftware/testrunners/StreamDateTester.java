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
import org.apache.commons.lang3.tuple.Triple;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class StreamDateTester {

    public static void main(String[] args) throws Exception {
        //Test parameters
        String eventlogNumber =args[0];
        int relationsThreshold = Integer.parseInt(args[1]);
        String[] patternList = args[2].split(" ");
        String[] transitiveReductionList = args[3].split(" ");
        boolean saveAsXml = Boolean.parseBoolean(args[4]);
        boolean compareToDisCoveR = Boolean.parseBoolean(args[5]); // false for reference model true for DisCoveR at windowsize
        boolean saveEventLogs= Boolean.parseBoolean(args[6]);
        String[] traceWindowSizesStringList = args[8].split(" ");
        String[] maxTracesStringList = args[7].split(" ");
        int observationsBeforeEvaluation = Integer.parseInt(args[9]);
        String[] dcrConstraints = args[10].split(" ");
        //
        
        int count = 0;

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
        List<Triple<String,String,String>> streamCollection = new ArrayList<>();
        for (XLog traces : parsedXesFile){
            for (XTrace trace : traces){
                String traceId = trace.getAttributes().get("concept:name").toString();
                for (XEvent event : trace ){

                    //String activity = event.getAttributes().get("concept:name").toString();
                    String activity = event.getAttributes().get("EventName").toString(); // Dreyer's fond
                    String timestamp = event.getAttributes().get("time:timestamp").toString();
                    streamCollection.add(Triple.of(traceId,activity,timestamp));
                }

            }
        }
        streamCollection.sort((d1,d2) -> d1.getRight().compareTo(d2.getRight()));

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
                int totalObservations = streamCollection.size();
                for(Triple<String,String,String> event : streamCollection){
                    String currentTraceId = event.getLeft();
                    String activityName = event.getMiddle();
                    sc.consumeEvent(currentTraceId, activityName);
                    currentObservedEvents++;
                    if (currentObservedEvents % observationsBeforeEvaluation == 0) {
                        if (saveEventLogs){
                            sc.saveCurrentWindowLog(currentPath + "/eventlogs/online/online_eventlog_graph"+eventlogNumber+
                                    "maxtraces"+maxTraces +"_tracesize"+traceSize + "_obs" + currentObservedEvents);
                        }

                        DcrModel dcrModel = sc.getDcrModel();
                        count++;
                        //comparison

                        ModelComparison modelComparison = new ModelComparison(dcrModel);
                        modelComparison.loadComparativeModel(compareModel);
                        double jaccard = modelComparison.getJaccardSimilarity();

                        String row = jaccard +"," + modelComparison.getPrecision() +
                                "," + modelComparison.getRecall();

                        csvResults.append(maxTraces + ",").append(traceSize).append(",").append(currentObservedEvents).append(",")
                                .append(event.getRight() +",").append(sc.getNumberEventsInWindow()+",")
                                .append(row).append("\n");
                        if (saveAsXml){
                            new DcrModelXML(dcrModel).toFile(fileName+"_obs"+currentObservedEvents);
                        }
                    }
                    if(currentObservedEvents%100==0) System.out.println(currentObservedEvents + " of " + totalObservations);

                }
            }
        }
        
        System.out.println("Count: " + count);
        String outputDirectoryPath =  currentPath + "/evaluations/"+ eventlogNumber +"/modelmodel";

        File outputDirectoryObject = new File(outputDirectoryPath);
        if (!outputDirectoryObject.exists()){
            outputDirectoryObject.mkdirs();
        }
        String filePath = outputDirectoryPath + "/date_results_" + observationsBeforeEvaluation+ ".csv";
        File myObj = new File(filePath);

        myObj.createNewFile();
        try {
            FileWriter myWriter = new FileWriter(filePath,true);
            String columnTitles ="maxTraces,traceSize,observed,date,memory,model_jaccard,model_precision,model_recall\n";

            myWriter.write(columnTitles+csvResults);
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        System.exit(0);
    }
}
