package beamline.dcr.testsoftware.testrunners;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.testsoftware.ModelAdaption;
import beamline.dcr.testsoftware.ModelComparison;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison.DRIFT;

public class PatternChangeRandom {
    public static void main(String[] args) throws IOException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModels = currentPath + "/groundtruthmodels";
        
       StringBuilder modelComparisonString = new StringBuilder("model,addActSerial,addActParallel,deleteAct,replaceAct,addConst,removeConst,swapAct," +
            "GED, CNE, Jaccard\n");
    
        try (Stream<Path> paths = Files.walk(Paths.get(groundTruthModels))) {
            paths
            .filter(Files::isRegularFile)
            .forEach(path -> {if(path.toString().endsWith("101.xml")) {
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
                }
                
                
            }});
        }
        
        try {
            FileWriter myWriter 
            = new FileWriter(currentPath  + "RANDOM-" + java.time.LocalDate.now() + ".csv"/*,true*/);
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
        ModelComparison modelComparison = new ModelComparison(modelPath);
        
        for (int addActivitySerialInt = 0; addActivitySerialInt <= 3; addActivitySerialInt++){
            for (int addActivityParallelInt = 0; addActivityParallelInt <= 3; addActivityParallelInt++){
                for (int deleteActivityInt = 0; deleteActivityInt <= 3; deleteActivityInt++){
                    for (int replaceActivityInt = 0; replaceActivityInt <= 3; replaceActivityInt++){
                        for (int addConstraintInt = 0; addConstraintInt <= 3; addConstraintInt++){
                            for (int removeConstraintInt = 0; removeConstraintInt <= 3; removeConstraintInt++){
                                for (int swapActivitiesInt = 0; swapActivitiesInt <= 3; swapActivitiesInt++){

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
                                    DcrModel adaptedModel  = modelAdaption.getModel();
                                    modelComparison.loadComparativeModel(adaptedModel);
                                    String GEDString = modelComparison.getGEDString();
                                    String CNEString = modelComparison.getCNEString();
                                    double jaccard = modelComparison.getJaccardSimilarity();
                                    String jaccardSim = ""+jaccard;

                                    xmlString.append(filename +",").append(addActivitySerialInt + ",").append(addActivityParallelInt+ ",")
                                            .append(deleteActivityInt+ ",").append(replaceActivityInt+ ",").append(addConstraintInt+ ",")
                                            .append(removeConstraintInt+ ",").append(swapActivitiesInt +",");
                                    xmlString.append(GEDString + ",").append(CNEString + ",").append(jaccardSim + "\n");
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
