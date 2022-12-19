package beamline.dcr.testsoftware.testrunners;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;
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
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Stream;

public class PatternChangeComparison {
    
    static int iterations = 100;
    
    static boolean applyNoise = true;
    
    static Random rand = new Random();
    
    public enum DRIFT {
        SUDDEN,
        GRADUAL,
        SEASONAL,
        INCREMENTAL
    }
    
    public static void setApplyNoise(boolean applyNoise) {
        PatternChangeComparison.applyNoise = applyNoise;
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String groundTruthModels = currentPath + "/groundtruthmodels";
        String noiseString = "Noise";
        
        if (!applyNoise) noiseString = "NoNoise";

        for (DRIFT driftType : DRIFT.values()) {
            StringBuilder modelComparisonString 
            = new StringBuilder("model,GED,CNE,Jaccard\n");
            
            try (Stream<Path> paths = Files.walk(Paths.get(groundTruthModels))) {
                paths
                .filter(Files::isRegularFile)
                .forEach(path -> {if(path.toString().endsWith("101.xml")) {
                    try {
                        String filenameFull = path.getFileName().toString();
                        String filenameTrimmed = filenameFull.substring(0, filenameFull.lastIndexOf('.'));
                        String simString = createSimulationString(path.toString(),filenameTrimmed,driftType);
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
                = new FileWriter(currentPath + "/evaluations/driftChange/" + driftType.toString() 
                    + "-" + noiseString + "-" + java.time.LocalDate.now() + ".csv"/*,true*/);
                myWriter.write(modelComparisonString.toString());
                myWriter.close();
                
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }
    }
    
    /*
     * Creates an output string for a csv file.
     */
    public static String createSimulationString(String modelPath, String filename, DRIFT driftType) throws ParserConfigurationException, SAXException, IOException {
        
        ArrayList<DcrModel> models = new ArrayList<DcrModel>();
        StringBuilder xmlString = new StringBuilder();
        
        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(modelPath);

        /*
         * These functions should really return a list of models (the mutations) given a specific model
         */
        switch (driftType) {
            case SUDDEN:
                models = suddenDriftMutations(referenceModel);
                break;
            case GRADUAL:
                models = gradualDriftMutations(referenceModel);
                break;
            case SEASONAL:
                models = seasonalDriftMutations(referenceModel);
                break;
            case INCREMENTAL:
                models = incrementalDriftMutations(referenceModel);
                break;
        }
        
        ModelComparison modelComparison = new ModelComparison(referenceModel);
        for (int i = 0; i < models.size(); i++) {
            modelComparison.loadComparativeModel(models.get(i));
            
            String GEDString = modelComparison.getGEDString();
            String CNEString = modelComparison.getCNEString();
            String jaccardSim = modelComparison.getJaccardSimilarity() + "";
            
            xmlString.append(filename +",");
            xmlString.append(GEDString + ",").append(CNEString + ",").append(jaccardSim + "\n");
        }
        
        return xmlString.toString();
    }
    
    /*
     * Sudden drift should incorporate only one single and very significant drift
     */
    public static ArrayList<DcrModel> suddenDriftMutations(DcrModel referenceModel) {
        
        ArrayList<DcrModel> models = new ArrayList<DcrModel>();
        
        int driftStrength = 5;
        int driftIteration = rand.nextInt(iterations);
        
        ModelAdaption modelAdaption = new ModelAdaption(referenceModel.getClone());
        
        for (int i = 0; i < iterations; i++) {
            if (i == driftIteration) {
                if (!modelAdaption.insertActivitySerial(driftStrength) ||
                        !modelAdaption.insertActivityParallel(driftStrength) ||
                        !modelAdaption.deleteActivity(driftStrength) ||
                        !modelAdaption.replaceActivity(driftStrength) ||
                        !modelAdaption.addConstraint(driftStrength) ||
                        !modelAdaption.removeConstraint(driftStrength) ||
                        !modelAdaption.swapActivities(driftStrength)) {
                    System.out.println("Mutation operation was unsuccessful");
                } 
                models.add(modelAdaption.getModel());
                modelAdaption = new ModelAdaption(modelAdaption.getModel().getClone());
            } else {
                
                DcrModel nextModel = modelAdaption.getModel().getClone();
                if (applyNoise) {
                    nextModel = modelAdaption.getModel().getClone();
                    if (!modelAdaption.insertActivitySerial(1)) {
                        System.out.println("Noise mutation was unsuccessful");
                    }
                } 
                models.add(modelAdaption.getModel());
                modelAdaption = new ModelAdaption(nextModel);
            }
        }
        
        return models;
    }

    public static ArrayList<DcrModel> gradualDriftMutations(DcrModel referenceModel) {
        
        ArrayList<DcrModel> models = new ArrayList<DcrModel>();
        
        int driftStrength = 1;
        
        int divisor = 4;
        int temp = iterations/divisor;
        
        int gradualStart = temp;
        int gradualEnd = iterations-temp;
        
        ModelAdaption modelAdaption = new ModelAdaption(referenceModel.getClone());
        for (int i = 0; i < iterations; i++) {
            if (i < gradualStart || i > gradualEnd) {
                DcrModel nextModel = modelAdaption.getModel().getClone();
                if (applyNoise) {
                    nextModel = modelAdaption.getModel().getClone();
                    if (!modelAdaption.insertActivitySerial(1)) {
                        System.out.println("Noise mutation was unsuccessful");
                    }
                } 
                models.add(modelAdaption.getModel());
                modelAdaption = new ModelAdaption(nextModel);
            } else {
                if (!modelAdaption.insertActivitySerial(driftStrength)) {
                    System.out.println("Mutation operation was unsuccessful");
                } 
                models.add(modelAdaption.getModel());
                modelAdaption = new ModelAdaption(modelAdaption.getModel().getClone());
            }
        }
        return models;
    }
    
    public static ArrayList<DcrModel> seasonalDriftMutations(DcrModel referenceModel) {
        
        ArrayList<DcrModel> models = new ArrayList<DcrModel>();
        
        int driftStrength = 10;
        
        int seasonalStart = iterations/3;
        int seasonalEnd = iterations - seasonalStart;
        
        ModelAdaption modelAdaption = new ModelAdaption(referenceModel.getClone());
        
        for (int i = 0; i < iterations; i++) {
            if (seasonalStart == i) {
                if (!modelAdaption.insertActivitySerial(driftStrength) ||
                        !modelAdaption.insertActivityParallel(driftStrength) ||
                        !modelAdaption.deleteActivity(driftStrength) ||
                        !modelAdaption.replaceActivity(driftStrength) ||
                        !modelAdaption.addConstraint(driftStrength) ||
                        !modelAdaption.removeConstraint(driftStrength) ||
                        !modelAdaption.swapActivities(driftStrength)) {
                    System.out.println("Mutation operation was unsuccessful");
                } 
                models.add(modelAdaption.getModel());
                modelAdaption = new ModelAdaption(modelAdaption.getModel().getClone());
            } else if (seasonalEnd == i) {
                modelAdaption = new ModelAdaption(referenceModel.getClone());
            } else {
                DcrModel nextModel = modelAdaption.getModel().getClone();
                if (applyNoise) {
                    nextModel = modelAdaption.getModel().getClone();
                    if (!modelAdaption.insertActivitySerial(1)) {
                        System.out.println("Noise mutation was unsuccessful");
                    }
                } 
                models.add(modelAdaption.getModel());
                modelAdaption = new ModelAdaption(nextModel);
            }
        }
        
        return models;
    }
    
    public static ArrayList<DcrModel> incrementalDriftMutations(DcrModel referenceModel) {

        ArrayList<DcrModel> models = new ArrayList<DcrModel>();
        
        int driftStrength = 1;
        
        int increments = 4;
        int incrementSize = iterations/increments;
        
        ModelAdaption modelAdaption = new ModelAdaption(referenceModel.getClone());
        
        for (int i = 0; i < iterations; i++) {
            if (i != 0 && i != iterations && (i%incrementSize) == 0) {
                if (!modelAdaption.insertActivitySerial(driftStrength) ||
                        !modelAdaption.insertActivityParallel(driftStrength) ||
                        !modelAdaption.deleteActivity(driftStrength) ||
                        !modelAdaption.replaceActivity(driftStrength) ||
                        !modelAdaption.addConstraint(driftStrength) ||
                        !modelAdaption.removeConstraint(driftStrength) ||
                        !modelAdaption.swapActivities(driftStrength)) {
                    System.out.println("Mutation operation was unsuccessful");
                } 
                models.add(modelAdaption.getModel());
                modelAdaption = new ModelAdaption(modelAdaption.getModel().getClone());
            } else {
                DcrModel nextModel = modelAdaption.getModel().getClone();
                if (applyNoise) {
                    nextModel = modelAdaption.getModel().getClone();
                    if (!modelAdaption.insertActivitySerial(1)) {
                        System.out.println("Noise mutation was unsuccessful");
                    }
                } 
                models.add(modelAdaption.getModel());
                modelAdaption = new ModelAdaption(nextModel);
            }
        }
        return models;
    }
    
    public static void setIterations(int num) {
        iterations = num;
    }
    
}
