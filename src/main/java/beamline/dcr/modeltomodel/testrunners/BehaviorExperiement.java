
package beamline.dcr.modeltomodel.testrunners;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;
import beamline.dcr.testsoftware.ModelAdaption;

public class BehaviorExperiement {

    static Random rand = new Random();
    
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

        StringBuilder output = new StringBuilder("sep=,\nnumOfChanges,GED,LCS,BehavioralProfile\n");

        DcrModel originalModel = new DcrModel();
        originalModel.loadModel(modelPath);

        // int totalIterations = 4*4*4*4*4;
        int iteration = 0;
        int totalIterations = 20*20;

        ModelAdaption modelAdaption = new ModelAdaption(originalModel);
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {

                if(iteration % (totalIterations/100) == 0) {
                    System.out.println("Iteration: " + iteration + " out of " + totalIterations);
                }

                modelAdaption = new ModelAdaption(modelPath);

                if (rand.nextDouble() < 0.5) {
                    relationType = DcrModel.RELATION.EXCLUDE;
                } else {
                    relationType = DcrModel.RELATION.INCLUDE;
                }

                if (!modelAdaption.addConstraintOfType(i, relationType)) {
                    System.out.println("Failed to add constraint of type " + relationType);
                    continue;
                }

                DcrModel adaptedModel  = modelAdaption.getModel();
                double GEDScore = DcrSimilarity.graphEditDistanceSimilarity(originalModel, adaptedModel);
                double LCSScore = DcrSimilarity.longestCommonSubtraceSimilarity(originalModel, adaptedModel);
                double behavioralScore = DcrSimilarity.behavioralProfileSimilarity(originalModel, adaptedModel);
                output.append(i + "," + GEDScore + "," + LCSScore + "," + behavioralScore + "\n");
                iteration++;
            }
        }

        return output.toString();
    }
}
