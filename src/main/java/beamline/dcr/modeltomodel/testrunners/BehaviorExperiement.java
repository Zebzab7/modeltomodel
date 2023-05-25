
package beamline.dcr.modeltomodel.testrunners;

import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;
import beamline.dcr.testsoftware.ModelAdaption;

public class BehaviorExperiement {
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModels = currentPath + "/publicrepodataset";
        
        String fileName = "model89";
        String modelPath = groundTruthModels + "/" + fileName + "/original.xml";

        DcrModel.RELATION relationType = DcrModel.RELATION.EXCLUDE;

        String modelComparisonString = randomMutationsString(modelPath, relationType);
        
        try {
            FileWriter myWriter 
            = new FileWriter(currentPath + "/evaluations/randomMutations/SystematicRandom/" + java.time.LocalDate.now()
               + "-" + fileName + "-" + relationType + ".csv"/*,true*/);
            myWriter.write(modelComparisonString.toString());
            myWriter.close();
            
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    
    private static String randomMutationsString(String modelPath, DcrModel.RELATION relationType) throws IOException, SAXException, ParserConfigurationException {

        StringBuilder output = new StringBuilder("sep=,\naddActSerial,addActParallel,replaceAct,addConst,swapActivities,totalChanges,GED,LCS\n");

        DcrModel originalModel = new DcrModel();
        originalModel.loadModel(modelPath);

        int iteration = 0;
        int totalIterations = 4*4*4*4*4;
        
        ModelAdaption modelAdaption = new ModelAdaption(originalModel);
        for (int addActivitySerialInt = 0; addActivitySerialInt <= 3; addActivitySerialInt++){
            for (int addActivityParallelInt = 0; addActivityParallelInt <= 3; addActivityParallelInt++){
                for (int replaceActivityInt = 0; replaceActivityInt <= 3; replaceActivityInt++){
                    for (int addConstraintInt = 0; addConstraintInt <= 3; addConstraintInt++){
                        for (int swapActivitiesInt = 0; swapActivitiesInt <= 3; swapActivitiesInt++){
                            int totalChanges = addActivitySerialInt + addActivityParallelInt + replaceActivityInt 
                                + addConstraintInt + swapActivitiesInt;
                            
                            if(iteration % (totalIterations/10) == 0) {
                                System.out.println("Iteration: " + iteration + " out of " + totalIterations);
                            }
                            iteration++;

                            modelAdaption = new ModelAdaption(modelPath);
                            if (!modelAdaption.insertActivitySerial(addActivitySerialInt) ||
                            ! modelAdaption.insertActivityParallel(addActivityParallelInt) ||
                            ! modelAdaption.replaceActivity(replaceActivityInt) ||
                            ! modelAdaption.addConstraintOfType(addConstraintInt, relationType) ||
                            ! modelAdaption.swapActivities(swapActivitiesInt)){
                                continue;
                            }
                            
                            output.append(addActivitySerialInt + ",").append(addActivityParallelInt+ ",").append(replaceActivityInt+ ",")
                                .append(addConstraintInt+ ",").append(swapActivitiesInt +",").append(totalChanges + ",");
                            
                            DcrModel adaptedModel  = modelAdaption.getModel();
                            double GEDScore = DcrSimilarity.graphEditDistanceSimilarity(originalModel, adaptedModel);
                            double LCSScore = DcrSimilarity.longestCommonSubtraceSimilarity(originalModel, adaptedModel);
                            output.append(GEDScore + "," + LCSScore + "\n");
                        }
                    }
                }
            }
        }
        return output.toString();
    }
}
