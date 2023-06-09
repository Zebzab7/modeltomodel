
package beamline.dcr.modeltomodelcomparison.testrunners;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodelcomparison.DcrSimilarity;
import beamline.dcr.testsoftware.ModelAdaption;

public class BehaviorExperiement {

    public static Random rand = new Random(989);
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModels = currentPath + "/publicrepodataset";
        
        String modelId = "7188";
        String modelPath = groundTruthModels + "/model" + modelId + "/original.xml";
        
        DcrModel originalModel = new DcrModel();
        originalModel.loadModel(modelPath);
        
        String adaptions = currentPath + "/evaluations/randomMutations/BehaviorExperiment/model" + modelId;
        
        String modelComparisonString = randomMutationsString(modelPath, modelId);
        try {
            FileWriter myWriter 
            = new FileWriter(currentPath + "/evaluations/randomMutations/BehaviorExperiment/model" + modelId + "/" + java.time.LocalDate.now()
               + "-" + modelId + "randomEdits" + ".csv"/*,true*/);
            myWriter.write(modelComparisonString.toString());
            myWriter.close();
            
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    
    private static String randomMutationsString(String modelPath, String modelId) throws IOException, SAXException, ParserConfigurationException {

        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String adaptions = currentPath + "/evaluations/randomMutations/BehaviorExperiment/model" + modelId;

        StringBuilder output = new StringBuilder("sep=,\nnumOfChanges,GED,LCS,BP,edit\n");
        DcrModel originalModel = new DcrModel();
        originalModel.loadModel(modelPath);
        ModelAdaption modelAdaption = new ModelAdaption(originalModel.getClone());

        int totalIterations = 20;

        double GEDScore = DcrSimilarity.graphEditDistanceSimilarity(originalModel, modelAdaption.getModel());
        double LCSScore = DcrSimilarity.longestCommonSubtraceSimilarity(originalModel, modelAdaption.getModel());
        double behavioralScore = DcrSimilarity.behavioralProfileSimilarity(originalModel, modelAdaption.getModel());
        output.append(0 + "," + GEDScore + "," + LCSScore + "," + behavioralScore + "\n");

        for (int i = 0; i < totalIterations; i++) {
            String edit = "";

            System.out.println("#############################\n#######################\n"+ 
            "Iteration " + (i+1) + " of " + totalIterations);

            DcrModel previousModel = modelAdaption.getModel().getClone();
            switch(rand.nextInt(6)) {
            // switch(0) {
                case 0:
                    edit = "insertActivitySerial";
                    if (!modelAdaption.insertActivitySerial(1)) continue;
                    break;
                case 1:
                    edit = "insertActivityParallel";
                    if (!modelAdaption.insertActivityParallel(1)) continue;
                    break;
                case 2:
                    edit = "deleteActivity";
                    if (!modelAdaption.deleteActivity(1)) continue;
                    break;
                case 3:
                    edit = "replaceActivity";
                    if (!modelAdaption.replaceActivity(1)) continue;
                    break;
                case 4:
                    edit = "addConstraint";                    
                    if (!modelAdaption.addConstraint(1)) continue;
                    break;
                case 5:
                    edit = "removeConstraint";
                    if (!modelAdaption.removeConstraint(1)) continue;
                    break;
                case 6:
                    edit = "swapActivities";
                    if (!modelAdaption.swapActivities(1)) continue;
                    break;
            }

            DcrModel adaptedModel  = modelAdaption.getModel();
            ModelViewer.dcrGraphToImage(adaptedModel, adaptions + "/adaption" + (i+1) + ".png");

            GEDScore = DcrSimilarity.graphEditDistanceSimilarity(originalModel, adaptedModel);
            LCSScore = DcrSimilarity.longestCommonSubtraceSimilarity(originalModel, adaptedModel);
            behavioralScore = DcrSimilarity.behavioralProfileSimilarity(originalModel, adaptedModel);
            output.append((i+1) + "," + GEDScore + "," + LCSScore + "," + behavioralScore + ","+ edit + "\n");
        }

        return output.toString();
    }
}
