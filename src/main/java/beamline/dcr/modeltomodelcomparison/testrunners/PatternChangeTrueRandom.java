package beamline.dcr.modeltomodelcomparison.testrunners;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodelcomparison.DcrSimilarity;
import beamline.dcr.testsoftware.ModelAdaption;
import beamline.dcr.testsoftware.ModelComparison;

public class PatternChangeTrueRandom {
    
    public static void main(String[] args) throws IOException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModels = currentPath + "/groundtruthmodels";
        
        StringBuilder modelComparisonString = new StringBuilder("sep=,\nmodel,mutations,");
        modelComparisonString.append("GED, CNE, Jaccard, W-GED, LCS\n");
    
        try (Stream<Path> paths = Files.walk(Paths.get(groundTruthModels))) {
            paths
            .filter(Files::isRegularFile)
            .forEach(path -> {if(path.toString().endsWith("101.xml") || path.toString().endsWith("25.xml") 
                    || path.toString().endsWith("3.xml")) {
                try {
                    String filenameFull = path.getFileName().toString();
                    String filenameTrimmed = filenameFull.substring(0, filenameFull.lastIndexOf('.'));
                    String simString = randomMutationsString(path.toString(),filenameTrimmed);
                    //System.out.println(simString);
                    modelComparisonString.append(simString);
                    System.out.println(filenameTrimmed + " has been compared");
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (DBSCANClusteringException e) {
                    e.printStackTrace();
                }
                
            }});
        }
        
        try {
            FileWriter myWriter 
            = new FileWriter(currentPath + "/evaluations/randomMutations/TrueRandom/" + "RANDOM-" + java.time.LocalDate.now()
               + System.currentTimeMillis() + ".csv"/*,true*/);
            myWriter.write(modelComparisonString.toString());
            myWriter.close();
            
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    
    private static String randomMutationsString(String modelPath, String filename) throws IOException, SAXException, ParserConfigurationException, DBSCANClusteringException {
        
        StringBuilder xmlString = new StringBuilder();
        
        ModelAdaption modelAdaption;
        
        int mutations = 0;
        int totalIterations = 1000;
        
        DcrModel originalModel = new DcrModel();
        originalModel.loadModel(modelPath);
        
        System.out.println(originalModel.getActivities().size());
        for (int i = 0; i <= totalIterations; i++) {
            
            if(i % (totalIterations/30) == 0) {
                mutations++;
                System.out.println("Number of errors: " + mutations);
            }
            
            modelAdaption = new ModelAdaption(originalModel.getClone());
            modelAdaption.randomMutation(mutations);
            DcrModel adaptedModel = modelAdaption.getModel();
            
            
            xmlString.append(filename +",").append(mutations + ",");
            xmlString.append(DcrSimilarity.graphEditDistanceSimilarity(originalModel, adaptedModel) + ",");
            xmlString.append(DcrSimilarity.commonNodesAndEdgesSimilarity(originalModel, adaptedModel) + ",");
            xmlString.append(DcrSimilarity.jaccardSimilarity(originalModel, adaptedModel) + ",");
            xmlString.append(DcrSimilarity.graphEditDistanceSimilarityWithWeights(originalModel, adaptedModel) + ",");
            xmlString.append(DcrSimilarity.longestCommonSubtraceSimilarity(originalModel, adaptedModel) + "\n");
        }
        return xmlString.toString();
    }
}
 