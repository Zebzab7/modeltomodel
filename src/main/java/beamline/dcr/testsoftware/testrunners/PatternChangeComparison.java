package beamline.dcr.testsoftware.testrunners;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.testsoftware.ModelAdaption;
import beamline.dcr.testsoftware.ModelComparison;
import beamline.dcr.view.DcrModelXML;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class PatternChangeComparison {

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "\\src\\main\\java\\beamline\\dcr\\testsoftware";
        String groundTruthModels = currentPath + "\\groundtruthmodels";


        StringBuilder modelComparisonString = new StringBuilder("model,addActSerial,addActParallel,deleteAct,replaceAct,addConst,removeConst,swapAct," +
                "jac_con,jac_resp,jac_precond,jac_mile,jac_incl,jac_excl," +
                "jac_noresp,jac_spawn,jac_act\n");


        try (Stream<Path> paths = Files.walk(Paths.get(groundTruthModels))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(path -> {if(path.toString().endsWith(".xml")) {
                        try {
                            String filenameFull = path.getFileName().toString();
                            String filenameTrimmed = filenameFull.substring(0, filenameFull.lastIndexOf('.'));
                            String simString = createAdaptionString(path.toString(),filenameTrimmed);
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
            //FileWriter myWriter = new FileWriter(currentPath+"/evaluations/patternChangeComparison/Jaccards"+ java.time.LocalDate.now() + ".csv",true);
            FileWriter myWriter = new FileWriter(currentPath + java.time.LocalDate.now() + ".csv",true);
            myWriter.write(modelComparisonString.toString());
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


    }
    public static String createAdaptionString(String modelPath, String filename) throws ParserConfigurationException, SAXException, IOException {
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
                                    String jaccardSim = modelComparison.getJaccardString();

                                    xmlString.append(filename +",").append(addActivitySerialInt + ",").append(addActivityParallelInt+ ",")
                                            .append(deleteActivityInt+ ",").append(replaceActivityInt+ ",").append(addConstraintInt+ ",")
                                            .append(removeConstraintInt+ ",").append(swapActivitiesInt +",");
                                    xmlString.append( jaccardSim + "\n");
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
