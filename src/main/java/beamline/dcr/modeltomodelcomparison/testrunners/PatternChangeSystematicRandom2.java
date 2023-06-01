package beamline.dcr.modeltomodelcomparison.testrunners;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodelcomparison.DcrSimilarity;
import beamline.dcr.testsoftware.ModelAdaption;

public class PatternChangeSystematicRandom2 {
    public static void main(String[] args) throws IOException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModels = currentPath + "/publicrepodataset";
        String filename = "/model7188/original";
        
        StringBuilder modelComparisonString = new StringBuilder("sep=,\nmodel,addActSerial,addActParallel,deleteAct,replaceAct,addConst,removeConst,swapActivities," +
            "GED,LCS,BehavioralProfile\n");

        // groundTruthModels = currentPath + "/driftedmodels/ResearchPaperExample/";
        String modelPath = groundTruthModels + filename + ".xml";

        try {
            String simString = randomMutationsString(modelPath, filename);
            modelComparisonString.append(simString);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter 
            = new FileWriter(currentPath + "/evaluations/randomMutations/SystematicRandom/" + java.time.LocalDate.now()
               + "-" + "modelsCompared" + ".csv"/*,true*/);
            myWriter.write(modelComparisonString.toString());
            myWriter.close();
            
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    
    private static String randomMutationsString(String modelPath, String filename) throws IOException, SAXException, ParserConfigurationException {
        
        StringBuilder xmlString = new StringBuilder();
        
        ModelAdaption modelAdaption;
        int iteration = 0;
        int totalIterations = (int) Math.pow(4, 7);
        
        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(modelPath);
        
        System.out.println("size: " + referenceModel.getActivities().size());
        
        for (int addActivitySerialInt = 0; addActivitySerialInt <= 3; addActivitySerialInt++){
            for (int addActivityParallelInt = 0; addActivityParallelInt <= 3; addActivityParallelInt++){
                for (int deleteActivityInt = 0; deleteActivityInt <= 3; deleteActivityInt++){
                    for (int replaceActivityInt = 0; replaceActivityInt <= 3; replaceActivityInt++){
                        for (int addConstraintInt = 0; addConstraintInt <= 3; addConstraintInt++){
                            for (int removeConstraintInt = 0; removeConstraintInt <= 3; removeConstraintInt++){
                                for (int swapActivitiesInt = 0; swapActivitiesInt <= 3; swapActivitiesInt++){
                                    

                                    if(iteration % 100 == 0)
                                    System.out.println("Iteration: " + iteration + " out of " + totalIterations);
                                    iteration++;

                                    modelAdaption = new ModelAdaption(modelPath);
                                    if (!modelAdaption.insertActivitySerial(addActivitySerialInt) ||
                                    !modelAdaption.insertActivityParallel(addActivityParallelInt) ||
                                    ! modelAdaption.deleteActivity(deleteActivityInt) ||
                                    ! modelAdaption.replaceActivity(replaceActivityInt) ||
                                    ! modelAdaption.addConstraint(addConstraintInt) ||
                                    ! modelAdaption.removeConstraint(removeConstraintInt) ||
                                    ! modelAdaption.swapActivities(swapActivitiesInt)){
                                        continue;
                                    }
                                    
                                    xmlString.append(filename +",").append(addActivitySerialInt + ",").append(addActivityParallelInt+ ",")
                                    .append(deleteActivityInt+ ",").append(replaceActivityInt+ ",").append(addConstraintInt+ ",")
                                    .append(removeConstraintInt+ ",").append(swapActivitiesInt +",");
                                    
                                    DcrModel adaptedModel  = modelAdaption.getModel();
                                    double GEDScore = DcrSimilarity.graphEditDistanceSimilarity(referenceModel, adaptedModel);
                                    double behavioralScore = DcrSimilarity.behavioralProfileSimilarity(referenceModel, adaptedModel);
                                    double LCSScore = DcrSimilarity.longestCommonSubtraceSimilarity(referenceModel, adaptedModel);
                                    
                                    xmlString.append(GEDScore + "," + LCSScore + "," + behavioralScore + "\n");
                                }
                            }
                        }
                    }
                }
            }
        }
        return xmlString.toString();
    }
}
