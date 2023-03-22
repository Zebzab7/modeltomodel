package beamline.dcr.modeltomodel.testrunners;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;
import beamline.dcr.testsoftware.ModelAdaption;
import beamline.dcr.testsoftware.ModelComparison;



public class PrintPatternSimilarity {
    public static void main(String[] args) throws IOException {
       String rootPath = System.getProperty("user.dir");
       String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
       String groundTruthModels = currentPath + "/driftedmodels";
        
       StringBuilder modelComparisonString = new StringBuilder("Step,GED,LCS\n");
       String inputString = "GEDvsLCS";
       
       Path groundTruthPath = Paths.get(groundTruthModels); 
       ArrayList<Path> paths = getArrayListFromStream(Files.walk(groundTruthPath)
               .filter(Files::isRegularFile).filter(path -> path.toString().contains("Alternate")));
       
       Collections.sort(paths, new Comparator<Path>() {
           @Override
           public int compare(Path p1, Path p2) {
               int num1 = getNumFromString(p1.getFileName().toString());
               int num2 = getNumFromString(p2.getFileName().toString());
               return num1 - num2;
           }
       });
       
       for (int i = 0; i < paths.size(); i++) {
           try {
               String filenameFull = paths.get(i).getFileName().toString();
               String filenameTrimmed = filenameFull.substring(0, filenameFull.lastIndexOf('.'));
               String simString = writeSimilarity(paths.get(i).toString(),filenameTrimmed,
                       groundTruthModels + "/ComputerRepairOriginal.xml");
//               System.out.println(simString);
               modelComparisonString.append(simString);
               System.out.println(filenameTrimmed + " has been compared");
           } catch (ParserConfigurationException e) {
               e.printStackTrace();
           } catch (SAXException e) {
               e.printStackTrace();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
       
        try {
            FileWriter myWriter 
            = new FileWriter(currentPath + "/evaluations/randomMutations/BehaviorDrivenDrifts/" + java.time.LocalDate.now()
               + "-" + inputString + ".csv"/*,true*/);
            myWriter.write(modelComparisonString.toString());
            myWriter.close();
            
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
        
    private static String writeSimilarity(String modelPath, String filename, String referenceModelPath) 
            throws IOException, SAXException, ParserConfigurationException {
        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(referenceModelPath);   
        
        StringBuilder xmlString = new StringBuilder();
        DcrModel newModel = new DcrModel();
        newModel.loadModel(modelPath);
        
        double GEDScore = DcrSimilarity.graphEditDistanceSimilarity(newModel, referenceModel);
        double LCSScore = DcrSimilarity.longestCommonSubtraceSimilarity(newModel, referenceModel);
        xmlString.append(getNumFromString(filename) + "," + GEDScore + "," + LCSScore + "\n");
        
        return xmlString.toString();
    }
    
    public static int getNumFromString(String in) {
        String s = in.split("\\.")[0];
        int num = Integer.parseInt (s.replaceFirst("^.*\\D",""));
        return num;
    }
    
    public static <T> ArrayList<T>
    getArrayListFromStream(Stream<T> stream)
    {
        // Convert the Stream to ArrayList
        ArrayList<T>
            arrayList = stream
                            .collect(Collectors
                            .toCollection(ArrayList::new));
        // Return the ArrayList
        return arrayList;
    }
}
