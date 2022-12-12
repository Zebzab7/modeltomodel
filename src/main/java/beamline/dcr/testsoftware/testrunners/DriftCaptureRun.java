package beamline.dcr.testsoftware.testrunners;

import beamline.core.miner.exceptions.MinerException;
import beamline.core.web.miner.models.MinerParameterValue;
import beamline.core.web.miner.models.Stream;
import beamline.dcr.miners.DFGBasedMiner;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.testsoftware.ModelComparison;
import beamline.dcr.view.DcrModelXML;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DriftCaptureRun {
    public static void main(String[] args) throws IOException, MinerException, ParserConfigurationException, SAXException, TransformerException {
        int relationsThreshold = Integer.parseInt(args[0]);
        int observationsBeforeEvaluation = Integer.parseInt(args[1]);
        int traceSize = Integer.parseInt(args[2]);
        int maxTraces = Integer.parseInt(args[3]);
        boolean compareWithOnlineDisCoveR = Boolean.parseBoolean(args[4]);
        String streamMiningAlgo = "Sliding Window";

        String[] transitiveReductionList ={};


        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String pathToStreamlogs = currentPath + "/eventlogs/streamlogs/";
        String pathToConstraintsFile = currentPath+"/discovermodels/constraintfiles/";
        String outputDirectoryPath = currentPath + "/evaluations/driftjaccardtest";


        File folder = new File(pathToStreamlogs);
        File[] listOfEventStreams = folder.listFiles();

        for (int i = 0; i < listOfEventStreams.length; i++) {

            if (listOfEventStreams[i].isFile() &
                    listOfEventStreams[i].getName().substring(
                            listOfEventStreams[i].getName().length()-3,listOfEventStreams[i].getName().length()).equals("csv")) {
                Map<String, DcrModel> modelMap = new HashMap<>();

                DFGBasedMiner sc = new DFGBasedMiner();

                Collection<MinerParameterValue> coll = new ArrayList<>();
                MinerParameterValue confParam = new MinerParameterValue("DCR Patterns", new String[]{"Condition","Response"});
                coll.add(confParam);
                MinerParameterValue relationThresholdParam = new MinerParameterValue("Relations Threshold", relationsThreshold);
                coll.add(relationThresholdParam);
                MinerParameterValue dcrConstraintsParam = new MinerParameterValue("DCR Constraints", new String[]{"Condition","Response"});
                coll.add(dcrConstraintsParam);
                MinerParameterValue transParam = new MinerParameterValue("Transitive Reduction", transitiveReductionList);
                coll.add(transParam);
                MinerParameterValue fileParam2 = new MinerParameterValue("Stream Miner", streamMiningAlgo);
                coll.add(fileParam2);
                MinerParameterValue fileParam3 = new MinerParameterValue("Trace Window Size", traceSize);
                coll.add(fileParam3);
                MinerParameterValue fileParam4 = new MinerParameterValue("Max Traces", maxTraces);
                coll.add(fileParam4);
                sc.configure(coll);


                String streamNameExt = listOfEventStreams[i].getName();
                String streamName = streamNameExt.replace(".csv","");
                //For synthetic - without complexity
                String StreamNameStripped = streamName.replace("_complex","").replace("_simple","");
                String[] nameSplit = streamName.split("_");

                if (streamName.contains("500") && streamName.contains("pm")){
                    StringBuilder xmlString = new StringBuilder("observedEvents,memory(events used),max traces, max trace length,jaccardMined,jaccardDiscover,model,ModelConstraints,compModelConstraints,compModelActivity\n");
                    ModelComparison comparison = new ModelComparison(sc.getDcrModel());

                    BufferedReader csvReader = new BufferedReader(new FileReader(pathToStreamlogs + streamNameExt));

                    String row;
                    int currentObservedEvents = 0;
                    while ((row = csvReader.readLine()) != null) {
                        if (currentObservedEvents==0){
                            currentObservedEvents ++;
                            continue;
                        }
                        String[] data = row.split(",");
                        sc.consumeEvent(data[1],data[2]);
                        DcrModel adaptedModel;
                        if (!modelMap.containsKey(data[0])){
                            adaptedModel = new DcrModel();

                            String constraintPathFilePath = pathToConstraintsFile+ StreamNameStripped +"_" +
                                    data[0]+".xes.txt";

                            adaptedModel.loadModelFromTexturalConstraintFile(constraintPathFilePath);
                            modelMap.put(data[0],adaptedModel);
                        }

                        if (currentObservedEvents % observationsBeforeEvaluation == 0) {
                            adaptedModel = modelMap.get(data[0]);
                            comparison.loadOriginalModel(adaptedModel);
                            comparison.loadComparativeModel(sc.getDcrModel());
                            double jaccardMined = comparison.getJaccardSimilarity();
                            double jaccardDiscover = 0;
                            DcrModel onlineDisCoveR = new DcrModel();
                            if(compareWithOnlineDisCoveR){
                                String onlineDiscoverConstraintPath = pathToConstraintsFile+"online/" +
                                        currentObservedEvents+"_mt"+maxTraces +"_ws"+traceSize+"_"+streamName+".xes.txt";
                                onlineDisCoveR.loadModelFromTexturalConstraintFile(onlineDiscoverConstraintPath);
                                comparison.loadComparativeModel(onlineDisCoveR);
                                jaccardDiscover = comparison.getJaccardSimilarity();

                            }

                            xmlString.append(currentObservedEvents +",")
                                    .append(sc.getNumberEventsInWindow()+",")
                                    .append(maxTraces+",")
                                    .append(traceSize+",")
                                    .append(jaccardMined + ",")
                                    .append(jaccardDiscover + ",")
                                    .append(data[0] + ",")
                                    .append(sc.getDcrModel().getRelations().size() + ",")
                                    .append(onlineDisCoveR.getRelations().size() + ",")
                                    .append(adaptedModel.getRelations().size() + "\n");

                        }
                        currentObservedEvents++;
                    }

                    csvReader.close();

                    //SAVE

                    File outputDirectoryObject = new File(outputDirectoryPath);
                    if (!outputDirectoryObject.exists()){
                        outputDirectoryObject.mkdirs();
                    }
                    String outputFile = outputDirectoryPath +"/"+maxTraces+"_"+traceSize+"_"+streamName+"_"+ java.time.LocalDate.now() + ".csv";

                    File myObj = new File(outputFile);

                    myObj.createNewFile();


                    try {

                        FileWriter myWriter = new FileWriter(outputFile,true);
                        myWriter.write(xmlString.toString());
                        myWriter.close();

                    } catch (IOException e) {
                        System.out.println("An error occurred.");
                        e.printStackTrace();
                    }
                }

            }
        }
        System.exit(1);

    }
}
