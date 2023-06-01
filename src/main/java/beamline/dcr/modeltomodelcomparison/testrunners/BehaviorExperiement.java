
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

    static Random rand = new Random();
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModels = currentPath + "/publicrepodataset";
        
        String modelId = "7188";
        String modelPath = groundTruthModels + "/model" + modelId + "/original.xml";

        String modelComparisonString = randomMutationsString(modelPath);
        try {
            FileWriter myWriter 
            = new FileWriter(currentPath + "/evaluations/randomMutations/BehaviorExperiement/" + java.time.LocalDate.now()
               + "-" + modelId + "randomEdits" + ".csv"/*,true*/);
            myWriter.write(modelComparisonString.toString());
            myWriter.close();
            
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    
    private static String randomMutationsString(String modelPath) throws IOException, SAXException, ParserConfigurationException {

        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String adaptions = currentPath + "/evaluations/BehaviorExperiement/model7188";

        StringBuilder output = new StringBuilder("sep=,\nnumOfChanges,GED,LCS,BehavioralProfile\n");
        DcrModel originalModel = new DcrModel();
        originalModel.loadModel(modelPath);
        ModelAdaption modelAdaption = new ModelAdaption(originalModel.getClone());

        System.out.println(originalModel.getActivities().size());

        int totalIterations = 20;

        for (int i = 0; i < totalIterations; i++) {

            switch(rand.nextInt(7)) {
                case 0:
                    System.out.println("insertActivitySerial");
                    if (!modelAdaption.insertActivitySerial(1)) continue;
                    break;
                case 1:
                    System.out.println("insertActivityParallel");
                    if (!modelAdaption.insertActivityParallel(1)) continue;
                    break;
                case 2:
                    System.out.println("deleteActivity");
                    if (!modelAdaption.deleteActivity(1)) continue;
                    break;
                case 3:
                    System.out.println("replaceActivity");
                    if (!modelAdaption.replaceActivity(1)) continue;
                    break;
                case 4:
                    System.out.println("addConstraint");
                    if (!modelAdaption.addConstraint(1)) continue;
                    break;
                case 5:
                    System.out.println("removeConstraint");
                    if (!modelAdaption.removeConstraint(1)) continue;
                    break;
                case 6:
                    System.out.println("swapActivities");
                    if (!modelAdaption.swapActivities(1)) continue;
                    break;
            }

            DcrModel adaptedModel  = modelAdaption.getModel();

            ModelViewer.dcrGraphToImage(adaptedModel, adaptions + "/adaption" + (i+1) + ".png");

            double GEDScore = DcrSimilarity.graphEditDistanceSimilarity(originalModel, adaptedModel);
            double LCSScore = DcrSimilarity.longestCommonSubtraceSimilarity(originalModel, adaptedModel);
            double behavioralScore = DcrSimilarity.behavioralProfileSimilarity(originalModel, adaptedModel);
            output.append(i + "," + GEDScore + "," + LCSScore + "," + behavioralScore + "\n");
        }

        return output.toString();
    }
}
