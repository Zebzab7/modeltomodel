package beamline.dcr.testsoftware;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.TransitiveReduction;
import beamline.dcr.model.relations.UnionRelationSet;
import beamline.dcr.model.relations.dfg.ExtendedDFG;
import beamline.dcr.model.patterns.Condition;
import beamline.dcr.model.patterns.ExcludeAndInclude;
import beamline.dcr.model.patterns.Response;
import beamline.dcr.model.patterns.Sequence;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ConfigurationEvaluation {


    private String eventlogNumber;
    private ExtendedDFG extendedDFG;
    private String compareModelPath;
    private boolean createPlots;
    public ConfigurationEvaluation(String eventlogNumber,
                                   String compareModelPath, ExtendedDFG extendedDFG, boolean createPlots
                                   ) {
        this.eventlogNumber = eventlogNumber;
        this.extendedDFG = extendedDFG;
        this.compareModelPath = compareModelPath;
        this.createPlots = createPlots;
    }

    public void startEval() throws Exception {
        String eventLog = "/eventlogs/eventlog_graph"+eventlogNumber+ ".xes";

        
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";

        //Brute force check
        Set<Set<String>> relationPatternSet = new LinkedHashSet<>();
        relationPatternSet.add(new LinkedHashSet<>(Arrays.asList("Condition")));
        relationPatternSet.add(new LinkedHashSet<>(Arrays.asList("Condition","Response")));
        relationPatternSet.add(new LinkedHashSet<>(Arrays.asList("Condition","ExcludeAndInclude")));
        relationPatternSet.add(new LinkedHashSet<>(Arrays.asList("Response","ExcludeAndInclude")));
        relationPatternSet.add(new LinkedHashSet<>(Arrays.asList("Condition","Response","ExcludeAndInclude")));

        Set<Set<Integer>> conditionParameters = new LinkedHashSet<>();
        conditionParameters.add(new LinkedHashSet<>(Arrays.asList(1)));
        conditionParameters.add(new LinkedHashSet<>(Arrays.asList(1,2)));

        Set<Set<Integer>> excludeIncludeParameters = new LinkedHashSet<>();
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(2)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(3)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,2)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,3)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(2,3)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(2,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(3,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,2,3)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,2,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,3,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(2,3,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,2,3,4)));

        Set<Set<Integer>> transitiveReductionParameters = new LinkedHashSet<>();
        transitiveReductionParameters.add(new LinkedHashSet<>());
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(1)));
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(2)));
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(3)));
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(4)));
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(1,2)));
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(1,3)));
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(1,4)));
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(2,3)));
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(2,4)));
        transitiveReductionParameters.add(new LinkedHashSet<>(Arrays.asList(3,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,2,3)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,2,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,3,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(2,3,4)));
        excludeIncludeParameters.add(new LinkedHashSet<>(Arrays.asList(1,2,3,4)));


        //Check Parameters
        StringBuilder csvText = new StringBuilder();
        Set<String> checkedCombinations = new HashSet<>();

        UnionRelationSet unionRelationSet;
        DcrModel dcrModel;
        StringBuilder patternCombination;
        boolean containsCondition;
        boolean containsResponse;
        boolean containsExcludeAndInclude;
		for(Set<String> relationPatterns : relationPatternSet){
			for (Set<Integer> transitiveReductionParameter : transitiveReductionParameters){
				for (Set<Integer> conditionParameter : conditionParameters){
					for(Set<Integer> excludeIncludeParameter : excludeIncludeParameters) {
						patternCombination = new StringBuilder();
						unionRelationSet = new UnionRelationSet(extendedDFG, 0);
						containsCondition = false;
						containsResponse = false;
						containsExcludeAndInclude = false;
						for (String pattern : relationPatterns) {
							patternCombination.append(pattern);
							switch (pattern) {
								case "Condition":
									new Condition().populateConstraint(unionRelationSet, conditionParameter);
									for (int parameterSetting : conditionParameter) {
										patternCombination.append(parameterSetting).append("-");
									}
									containsCondition=true;
									break;
								case "Response":
									new Response().populateConstraint(unionRelationSet);
									containsResponse=true;
									break;
								case "ExcludeAndInclude":
									for (int parameterSetting : excludeIncludeParameter) {
										patternCombination.append(parameterSetting).append("-");
									}
                                    if (excludeIncludeParameter.contains(2) || excludeIncludeParameter.contains(4)){
                                        new Sequence().populateConstraint(unionRelationSet);
                                    }
									new ExcludeAndInclude().populateConstraint(unionRelationSet, excludeIncludeParameter);
									containsExcludeAndInclude = true;
									break;
							}
							patternCombination.append(";");

						}
						if (transitiveReductionParameter.size() > 0) {
							StringBuilder transitiveString =new StringBuilder("TransitiveReduction");

							TransitiveReduction transitiveReduction = new TransitiveReduction();
							if (transitiveReductionParameter.contains(1) && containsCondition) {
								transitiveReduction.reduce(unionRelationSet, DcrModel.RELATION.CONDITION);
								transitiveString.append(1);
							}
							if (transitiveReductionParameter.contains(2) && containsResponse) {
								transitiveReduction.reduce(unionRelationSet, DcrModel.RELATION.RESPONSE);
								transitiveString.append("-").append(2);
							}
							if (transitiveReductionParameter.contains(3) && containsExcludeAndInclude) {
								transitiveReduction.reduce(unionRelationSet, DcrModel.RELATION.EXCLUDE);
								transitiveString.append("-").append(3);
							}
							if (transitiveReductionParameter.contains(4) && containsExcludeAndInclude) {
								transitiveReduction.reduce(unionRelationSet, DcrModel.RELATION.INCLUDE);
								transitiveString.append("-").append(4);
							}

							if(transitiveString.length()!= 19){
								patternCombination.append(transitiveString);
							}
						}

						if (!checkedCombinations.contains(patternCombination.toString())) {
							checkedCombinations.add(patternCombination.toString());
							dcrModel = new DcrModel();
							dcrModel.addRelations(unionRelationSet.getDcrRelations());
							dcrModel.addActivities(extendedDFG.getActivities());

                            Pair<List<Double>,String> imperativeEvaluation = getImperativeEvaluation(dcrModel, unionRelationSet,
                                    compareModelPath, currentPath + eventLog);
							List<Double> performanceList = imperativeEvaluation.getLeft();
							String identifiedIllegalTraces = imperativeEvaluation.getRight();
							StringBuilder performanceString = new StringBuilder();
                            for (double performanceValue : performanceList) {
                                //Add to csv file
                                performanceString.append(performanceValue).append(",");

                            }
							String finaleString = patternCombination + "," + performanceString + identifiedIllegalTraces + "\n";
							csvText.append(finaleString);
						}
					}
				}
			}
		}

		saveResults(currentPath,eventlogNumber,csvText.toString());

    }

    private void printBestPerformingCombination(List<Pair<String,Double>> performanceList){
        List<String> metrics = new ArrayList<>(List.of("Jaccard", "Model Precision", "Model Recall",
                "Log Fitness", "Log Precision","Illegal traces"));
        for (int i = 0; i < performanceList.size(); i++){
            System.out.println(metrics.get(i) + " - Combination:" +performanceList.get(i).getLeft() +
                    " - value:" + performanceList.get(i).getRight());
        }
    }
    private Pair<List<Double>,String> getImperativeEvaluation
            (DcrModel dcrModel, UnionRelationSet unionRelationSet,
             String CompareFilePath, String eventLogFilePath) throws Exception {


        TransitionSystem transitionSystem = new TransitionSystem(unionRelationSet);

        ModelComparison modelComparison = new ModelComparison(dcrModel);
        modelComparison.loadComparativeModel(CompareFilePath);

        ConformanceChecking conformanceChecking = new ConformanceChecking(eventLogFilePath, transitionSystem);
        conformanceChecking.checkConformance();
        List<Double> outputList = new ArrayList<>(List.of(modelComparison.getJaccardSimilarity(), modelComparison.getPrecision(),
                modelComparison.getRecall(), conformanceChecking.getFitness(),
                conformanceChecking.getPrecision(),(double)dcrModel.getRelations().size(),modelComparison.getF1()));

        String identifiedIllegalTraces = conformanceChecking.getIllegalTracesString();

        return Pair.of(outputList,identifiedIllegalTraces);
    }
    public void saveResults(String currentPath,String eventLogNumber,String csvText) throws IOException {
        String outputDirectoryPath = currentPath + "/evaluations/"+ eventLogNumber;
        File outputDirectoryObject = new File(outputDirectoryPath);
        if (!outputDirectoryObject.exists()){
            outputDirectoryObject.mkdirs();
        }
        String outputFile = outputDirectoryPath +"/performance_test_"+ java.time.LocalDate.now() + ".csv";

        File myObj = new File(outputFile);

        myObj.createNewFile();
        String title = "constraints,model_jaccard,model_precision,model_recall,log_fitness,log_precision,numConstraints,f1,illegal_traces\n";
        writeMetricsToFile(outputFile,title);

        writeMetricsToFile(outputFile,csvText.toString());
        if (createPlots){
            PlotResults plotResults = new PlotResults(outputFile);
            String plotTitle = "Performance";
            String[] metricsToPlot = {"model_jaccard","log_fitness"};
            plotResults.createLinePlot(outputDirectoryPath,plotTitle,metricsToPlot);

        }


    }
    private void writeMetricsToFile(String filePath,String textString){
        try {
            FileWriter myWriter = new FileWriter(filePath,true);
            myWriter.write(textString);
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
