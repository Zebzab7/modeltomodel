package beamline.dcr.modeltomodel.testrunners;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.christopherfrantz.dbscan.DistanceMetric;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DriftDetector;
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
        String modelPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/groundtruthmodels/process101.xml";

        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(modelPath);
        
        double eps = DriftDetector.getEps();
        int minPoints = DriftDetector.getMinPoints();
        
        // Initialize expected values and simulate data sets of process models
        int iterations = 1;
        int[] predictedVals = new int[iterations];
        int[] expectedVals = new int[iterations];
        for (int i = 0; i < iterations; i++) {
            expectedVals[i] = 1;
        }
        for (DRIFT driftType : DRIFT.values()) {
            
            System.out.println("DRIFT: " + driftType.toString());
            
            StringBuilder outputString
                = new StringBuilder("Metric,MSE,MSE-T,F-measure\n");
            
            FileWriter myWriter 
                = new FileWriter(currentPath + "/evaluations/DriftDetection/Test-Truncate" 
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
                    predictedVals[i] = DriftDetector.DBSCAN(modelSeries.get(i), eps, minPoints, metric);
                }
                
                double MSE = DriftDetector.getMeanSquareError(predictedVals, expectedVals);
                // FScore
                
                outputString.append(metric.toString() + ",").append(MSE + ",");
                for (int i = 0; i < iterations; i++) {
                    ArrayList<DcrModel> trimmedModels 
                        = DriftDetector.removeAndReplaceBoundaryElements(modelSeries.get(i), referenceModel, metric);
                    predictedVals[i] = DriftDetector.DBSCAN(trimmedModels, eps, minPoints, metric);
                }
                MSE = DriftDetector.getMeanSquareError(predictedVals, expectedVals);
                outputString.append(MSE + "\n");
            }
            System.out.println(driftType.toString() + " drift type evaluation finished");
            myWriter.write(outputString.toString());
            myWriter.close();
        }
    }
}
 