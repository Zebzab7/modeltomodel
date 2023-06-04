package beamline.dcr.modeltomodelcomparison.testrunners;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.christopherfrantz.dbscan.DistanceMetric;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodelcomparison.DriftDetector;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison.DRIFT;
import distancemetrics.CommonNodesAndEdges;
import distancemetrics.GraphEditDistance;
import distancemetrics.JaccardDistance;
import distancemetrics.WeightedGraphEditDistance;

public class DriftDetectionTester {
    
    static ArrayList<DistanceMetric<DcrModel>> metrics = new ArrayList<DistanceMetric<DcrModel>>
        (Arrays.asList(new GraphEditDistance(), new CommonNodesAndEdges(), 
                new JaccardDistance(), new WeightedGraphEditDistance()));
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DBSCANClusteringException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/";
        String modelPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/groundtruthmodels/";

        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(modelPath + "process25.xml");

        double sensitivity = 0.005;
        
        boolean trim = true;
        boolean replace = false;
        
        double eps = 0.15;
        int minPoints = 5;
         
        // Initialize expected values and simulate data sets of process models
        int iterations = 100;
        int[] predictedVals = new int[iterations];
        int[] expectedVals = new int[iterations];
        for (int i = 0; i < iterations; i++) {
            expectedVals[i] = 1;
        }
        for (DRIFT driftType : DRIFT.values()) {
            
            System.out.println("DRIFT: " + driftType.toString());
            
            StringBuilder outputString
                = new StringBuilder("Metric, MSE, RMSE, Accuracy\n");
            
            FileWriter myWriter 
                = new FileWriter(currentPath + "/evaluations/DriftDetection/Test-Truncate-" + "Trim" + trim +"-" + "Replace" + replace + "-" 
                        + driftType.toString() + java.time.LocalDate.now() + ".csv"/*,true*/);
            
            ArrayList<ArrayList<DcrModel>> modelSeries = new ArrayList<ArrayList<DcrModel>>();
            switch (driftType) {
                case SUDDEN:
                    for (int i = 0; i < iterations; i++) {
                        modelSeries.add(PatternChangeComparison.suddenDriftMutations(referenceModel));
                    }
                    break;
                case GRADUAL:
                    for (int i = 0; i < iterations; i++) {
                        modelSeries.add(PatternChangeComparison.gradualDriftMutations(referenceModel));
                    }
                    break;
                case SEASONAL:
                    for (int i = 0; i < iterations; i++) {
                        modelSeries.add(PatternChangeComparison.seasonalDriftMutations(referenceModel));
                    }
                    break;
                case INCREMENTAL:
                    for (int i = 0; i < iterations; i++) {
                        modelSeries.add(PatternChangeComparison.incrementalDriftMutations(referenceModel));
                    }
                    break;
            }
            
            for (DistanceMetric<DcrModel> metric : metrics) {
                for (int i = 0; i < iterations; i++) {
                    ArrayList<DcrModel> models = new ArrayList<DcrModel>();
                    if (trim) {
                        models = DriftDetector.transformData(modelSeries.get(i), referenceModel, 
                                metric, replace, sensitivity);
                    } else {
                        models = new ArrayList<DcrModel>(modelSeries.get(i));
                    }
                    predictedVals[i] = DriftDetector.DBSCAN(models, eps, minPoints, metric).getLeft();
                }
                
                double MSE = DriftDetector.getMeanSquareError(predictedVals, expectedVals);
                double RMSE = DriftDetector.getRootMeanSquareError(predictedVals, expectedVals);
                double accuracy = 100.0*DriftDetector.getAccuracy(predictedVals, expectedVals);
                
                outputString.append(metric.toString() + "," + MSE + "," + RMSE + "," + accuracy + "\n");
            }
            System.out.println(driftType.toString() + " drift type evaluation finished");
            myWriter.write(outputString.toString());
            myWriter.close();
        }
    }
}
 