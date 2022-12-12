package beamline.dcr.testsoftware.testrunners;


import beamline.core.web.miner.models.MinerParameterValue;
import beamline.core.web.miner.models.Stream;
import beamline.dcr.miners.DFGBasedMiner;
import beamline.dcr.model.relations.dfg.ExtendedDFG;
import beamline.dcr.testsoftware.ConfigurationEvaluation;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import java.io.File;
import java.util.*;

public class FrameworkSettingTester {

	public static void main(String[] args) throws Exception {
		//Test parameters
		String eventlogNumber = args[0];
		int relationsThreshold = Integer.parseInt(args[1]);
		boolean compareToDisCoveR = Boolean.parseBoolean(args[2]); // false for reference model true for DisCoveR
		boolean createPlots = Boolean.parseBoolean(args[3]);


		//
		String rootPath = System.getProperty("user.dir");
		String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";

		DFGBasedMiner sc = new DFGBasedMiner();

		Collection<MinerParameterValue> coll = new ArrayList<>();

		String[] patternList = {};
		String[] transitiveReductionList = {};
		MinerParameterValue confParam = new MinerParameterValue("DCR Patterns", patternList);
		coll.add(confParam);
		MinerParameterValue transParam = new MinerParameterValue("Transitive Reduction", transitiveReductionList);
		coll.add(transParam);
		MinerParameterValue relationThresholdParam = new MinerParameterValue("Relations Threshold", relationsThreshold);
		coll.add(relationThresholdParam);

		String fileName = currentPath + "/minedmodels/mined_model_"+eventlogNumber+ "_" + java.time.LocalDate.now();

		sc.configure(coll);
		sc.setStream(new Stream("test", "localhost", ""));
		sc.start();


		String eventLog = "/eventlogs/eventlog_graph"+eventlogNumber+ ".xes";
		File xesFile = new File(currentPath + eventLog);
		XesXmlParser xesParser = new XesXmlParser();

		List<XLog> parsedXesFile = xesParser.parse(xesFile);

		for (XLog traces : parsedXesFile){
			for (XTrace trace : traces){
				String traceId = trace.getAttributes().get("concept:name").toString();
				for (XEvent event : trace ){
					String activity = event.getAttributes().get("concept:name").toString();
					//String activity = event.getAttributes().get("EventName").toString(); //Dreyer's Fond
					sc.consumeEvent(traceId,activity);
				}

			}
		}

		ExtendedDFG extendedDFG = sc.getExtendedDFG();

		String groundTruthModelPath = currentPath + "/groundtruthmodels/Process" + eventlogNumber +".xml";
		String discoverModelPath = currentPath + "/discovermodels/DCR_graph" + eventlogNumber +".xml";
		String compareModel = compareToDisCoveR ? discoverModelPath : groundTruthModelPath;
		ConfigurationEvaluation configurationEvaluation = new ConfigurationEvaluation(eventlogNumber,compareModel,extendedDFG,createPlots);
		configurationEvaluation.startEval();

		sc.stop();
		System.out.println("Evaluation for log " + eventlogNumber + " succesfully terminated");
		//System.exit(0);


	}


}
