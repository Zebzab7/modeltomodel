package beamline.dcr.testsoftware.testrunners;

import beamline.core.web.miner.models.MinerParameterValue;
import beamline.dcr.miners.DFGBasedMiner;

import java.io.File;
import java.util.*;

import beamline.core.web.miner.models.MinerView;
import beamline.core.web.miner.models.Stream;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.testsoftware.ModelComparison;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;


import java.util.List;

public class BasicTester {
    public static void main(String[] args) throws Exception {
        
        //Test parameters
        String eventLogNumber = args[0];
        int relationsThreshold = Integer.parseInt(args[1]);
        String[] patternList = args[2].split(" ");
        String[] transitiveReductionList =  args[3].split(" ");
        boolean saveAsXml = Boolean.parseBoolean(args[4]);
        boolean compareToDisCoveR = Boolean.parseBoolean(args[5]); // false for reference model true for DisCoveR
        String[] dcrConstraints = args[6].split(" ");
        
        
        
        
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

        sc.setStream(new Stream("test", "localhost", ""));
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";

        if (saveAsXml){
            String fileName = currentPath + "/minedmodels/basictest_model_" +eventLogNumber + "_" +java.time.LocalDate.now();
            MinerParameterValue fileParam = new MinerParameterValue("filename", fileName);
            coll.add(fileParam);
        }

        sc.configure(coll);
        sc.start();

        String eventLog = "/eventlogs/eventlog_graph" +eventLogNumber+".xes";
        File xesFile = new File(currentPath + eventLog);
        XesXmlParser xesParser = new XesXmlParser();

        List<XLog> parsedXesFile = xesParser.parse(xesFile);
        int shortestTrace = 1110;
        int longestTrace = 0;
        int counter = 0;
        int numTraces = 0;
        int totalEvents = 0;

        for (XLog traces : parsedXesFile){
            for (XTrace trace : traces){
                String traceId = trace.getAttributes().get("concept:name").toString();
                counter=0;
                for (XEvent event : trace ){
                    counter++;
                    String activity = event.getAttributes().get("concept:name").toString();
                    //String activity = event.getAttributes().get("EventName").toString(); // For Dreyer's Fond
                    sc.consumeEvent(traceId,activity);
                }
                totalEvents += counter;
                if (counter <shortestTrace){
                    shortestTrace = counter;
                }
                if (counter > longestTrace){
                    longestTrace = counter;
                }
                numTraces ++;
            }
        }

        List<MinerView> views = sc.getViews(coll);


       //Comparison

        String groundTruthModelPath = currentPath + "/groundtruthmodels/Process" + eventLogNumber+ ".xml";
        String discoverModelPath = currentPath + "/discovermodels/DCR_graph" + eventLogNumber + ".xml";

        String compareModel = compareToDisCoveR ? discoverModelPath : groundTruthModelPath;

        DcrModel dcrModel = sc.getDcrModel();
        ModelComparison modelComparison = new ModelComparison(dcrModel);
        modelComparison.loadComparativeModel(compareModel);
        System.out.println("Jaccard Similarity: " + modelComparison.getJaccardSimilarity());
        System.out.println("F1-score: " + modelComparison.getF1());
        System.out.println("Model precision: " + modelComparison.getPrecision());
        System.out.println("Model recall: " + modelComparison.getRecall());
        
        sc.stop();
        System.exit(0);
    }

}
