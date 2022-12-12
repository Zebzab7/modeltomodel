package beamline.dcr.testsoftware.testrunners;

import beamline.core.miner.exceptions.MinerException;
import beamline.core.web.miner.models.MinerParameterValue;
import beamline.core.web.miner.models.Stream;
import beamline.dcr.miners.DFGBasedMiner;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.testsoftware.ModelComparison;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CreateOnlineLogs {
    public static void main(String[] args) throws IOException, MinerException, ParserConfigurationException, SAXException, TransformerException {
        int relationsThreshold = Integer.parseInt(args[0]);
        int observationsBeforeEvaluation = Integer.parseInt(args[1]);
        int traceSize = Integer.parseInt(args[2]);
        int maxTraces = Integer.parseInt(args[3]);
        String streamMiningAlgo = "Sliding Window";

        String[] transitiveReductionList ={};


        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String pathToStreamlogs = currentPath + "/eventlogs/streamlogs/";



        File folder = new File(pathToStreamlogs);
        File[] listOfEventStreams = folder.listFiles();

        for (int i = 0; i < listOfEventStreams.length; i++) {

            if (listOfEventStreams[i].isFile()) {

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
                if (streamName.contains("500") && streamName.contains("pm")){
                    BufferedReader csvReader = new BufferedReader(new FileReader(pathToStreamlogs  + streamNameExt));

                    String row;
                    int currentObservedEvents = 0;
                    while ((row = csvReader.readLine()) != null) {

                        if (currentObservedEvents==0){
                            currentObservedEvents ++;
                            continue;
                        }
                        String[] data = row.split(",");
                        sc.consumeEvent(data[1],data[2]);


                        if (currentObservedEvents % observationsBeforeEvaluation == 0) {
                            //Save logs for online comparison
                            System.out.println(currentObservedEvents);
                            sc.saveCurrentWindowLog("/Users/lassestarklit/Downloads/DisCoveR20210809/eventlogs/online/"+currentObservedEvents+"_mt"+maxTraces+"_ws"+traceSize+"_"+streamName);

                        }

                        currentObservedEvents++;
                    }

                    csvReader.close();
                    System.out.println(streamName + " processed");
                }


            }
        }
        System.exit(1);

    }
}
