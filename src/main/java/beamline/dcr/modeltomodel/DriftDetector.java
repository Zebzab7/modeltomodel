package beamline.dcr.modeltomodel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.christopherfrantz.dbscan.DBSCANClusterer;
import org.christopherfrantz.dbscan.DBSCANClusteringException;
import org.christopherfrantz.dbscan.DistanceMetric;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.testrunners.LinearRegression;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison.DRIFT;
import distancemetrics.CommonNodesAndEdges;
import distancemetrics.GraphEditDistance;
import distancemetrics.JaccardDistance;
import distancemetrics.WeightedGraphEditDistance;

public class DriftDetector {

    static double eps = 0.15;
    static int minPoints = 5;
    
    public static void setEps(double eps) {
        DriftDetector.eps = eps;
    }
    
    public static void setMinPoints(int minPoints) {
        DriftDetector.minPoints = minPoints;
    }
    
    static ArrayList<DistanceMetric<DcrModel>> metrics = new ArrayList<DistanceMetric<DcrModel>>
        (Arrays.asList(new GraphEditDistance(), new CommonNodesAndEdges(), new JaccardDistance(), new WeightedGraphEditDistance()));

    public static void addDistanceMetric(DistanceMetric<DcrModel> newMetric) {
        metrics.add(newMetric);
    }
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DBSCANClusteringException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/";
        String modelPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/groundtruthmodels/process101.xml";

        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(modelPath);
        
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
                = new StringBuilder("Metric,MSE,Precision,Recall,F-measure\n");
            
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
//                    predictedVals[i] = DBSCAN(modelSeries.get(i), eps, minPoints, metric);
                    predictedVals[i] = DBSCANChangeRate(modelSeries.get(i), referenceModel, eps, minPoints, metric);
                }
                
                double MSE = getMeanSquareError(predictedVals, expectedVals);
                // FScore
                // R-Squared score
                
                outputString.append(metric.toString() + ",").append(MSE + ",").append(",").append(",").append("\n");
            }
            System.out.println(driftType.toString() + " drift type evaluation finished");
            myWriter.write(outputString.toString());
            myWriter.close();
        }
    }
  
    /**
     * Returns the number of concept drifts that have been detected from the models
     * @throws DBSCANClusteringException 
     */
    public static int DBSCANWithTruncation(ArrayList<DcrModel> models, double eps, 
                int minPoints, DistanceMetric<DcrModel> metric) throws DBSCANClusteringException {
        
        System.out.println(models.size());
        
        double sigDiff = 0.95;
        int index = 0;
        while (index < models.size()) {
            DcrModel model = models.get(index);
            double nextSim = 1;
            double prevSim = 1;
            if (!(index == models.size()-1)) {
                nextSim 
                = DcrSimilarity.graphEditDistanceSimilarity(models.get(index), models.get(index+1));
            }
            if (!(index==0)) {
                prevSim 
                = DcrSimilarity.graphEditDistanceSimilarity(models.get(index-1), models.get(index));
            }
            
            if (nextSim < sigDiff || prevSim < sigDiff) {
                models.remove(model);
            } else {
                System.out.println("next: " + nextSim);
                System.out.println("prev: " + prevSim);
            }
            index++;
        }
        System.out.println(models.size());
        
        DBSCANClusterer<DcrModel> scanner = new DBSCANClusterer<DcrModel>(models, minPoints, eps, metric);
        ArrayList<ArrayList<DcrModel>> list = new ArrayList<ArrayList<DcrModel>>();
        try {
            list = scanner.performClustering();
        } catch (DBSCANClusteringException e) {
            System.out.println("There was some kind of error with clustering data...");
        }
        
        return list.size()-1;
    }
    
    /**
     * Returns the number of concept drifts that have been detected from the models
     * @throws DBSCANClusteringException 
     */
    public static int DBSCANChangeRate(ArrayList<DcrModel> models, DcrModel referenceModel, double eps, 
                int minPoints, DistanceMetric<DcrModel> metric) throws DBSCANClusteringException {
        
        double sensitivity = 0.005;
        
        System.out.println("Size before: " + models.size());
        
        int strictness = 10;
        int index = strictness/2;
        
        ArrayList<DcrModel> trimmedModels = new ArrayList<DcrModel>(models);
        
        while (index < trimmedModels.size()-(strictness/2)) {
            
            double[] yVals = new double[strictness];
            double[] xVals = new double[strictness];
            
            for (int i = 0; i < strictness; ++i) {
                xVals[i] = index-(strictness/2)+i;
                yVals[i] = DcrSimilarity.graphEditDistanceSimilarity(referenceModel, trimmedModels.get(index-(strictness/2)+i));
            }
            
            LinearRegression lg = new LinearRegression(xVals, yVals);
            
            System.out.println("Iteration: " + index + " Slope: " + String.format("%.5g%n", Math.abs(lg.getSlope())));
            if (Math.abs(lg.getSlope()) > sensitivity) {
                System.out.println("Stripping point");
                trimmedModels.remove(index);
            }
            
            index++;
        }
        System.out.println("Size after: " + trimmedModels.size());
        
        DBSCANClusterer<DcrModel> scanner = new DBSCANClusterer<DcrModel>(models, minPoints, eps, metric);
        ArrayList<ArrayList<DcrModel>> list = new ArrayList<ArrayList<DcrModel>>();
        try {
            list = scanner.performClustering();
        } catch (DBSCANClusteringException e) {
            System.out.println("There was some kind of error with clustering data...");
        }
        
        return list.size()-1;
    }
    
    /**
     * Returns the number of concept drifts that have been detected from the models
     * @throws DBSCANClusteringException 
     */
    public static int DBSCAN(ArrayList<DcrModel> models, double eps, 
                int minPoints, DistanceMetric<DcrModel> metric) throws DBSCANClusteringException {
        
        DBSCANClusterer<DcrModel> scanner = new DBSCANClusterer<DcrModel>(models, minPoints, eps, metric);
        ArrayList<ArrayList<DcrModel>> list = new ArrayList<ArrayList<DcrModel>>();
        try {
            list = scanner.performClustering();
        } catch (DBSCANClusteringException e) {
            System.out.println("There was some kind of error with clustering data...");
        }
        return list.size()-1;
    }
    
    public static int iterativeDBSCAN(ArrayList<DcrModel> models, double eps, 
            int minPoints, DistanceMetric<DcrModel> metric) throws DBSCANClusteringException {
    
        DBSCANClusterer<DcrModel> scanner = new DBSCANClusterer<DcrModel>(models, minPoints, eps, metric);
    
        ArrayList<ArrayList<DcrModel>> list = new ArrayList<ArrayList<DcrModel>>();
        try {
            list = scanner.performClustering();
        } catch (DBSCANClusteringException e) {
            System.out.println("There was some kind of error with clustering data...");
        }
       
        return list.size()-1;
    }
    
    /**
     * Returns the number of concept drifts that have been detected from the models
     */
    public static int simpleDetection(ArrayList<DcrModel> models, double sigDiff) {
        DcrModel currentModel = models.get(0);
        
        int drifts = 0;
        
        int n = models.size();
        for (int i = 1; i < n; i++) {
            DcrModel modelB = models.get(i);
            double sim = DcrSimilarity.graphEditDistanceSimilarity(currentModel, modelB);
            if (sim < sigDiff) {
                drifts++;
                currentModel = modelB;
            }
        }
        
        return drifts;
    }
    
    /**
     * Considers two models and determines if they are significantly different from each other
     */
    public static boolean detectSignificantDifference(DcrModel modelA, DcrModel modelB, double sigDiff, String sim) {
        double similarity = -1.0;
        switch(sim) {
            case "GED": 
                similarity = DcrSimilarity.graphEditDistanceSimilarity(modelA, modelB);
                break;
            case "CNE": 
                similarity = DcrSimilarity.commonNodesAndEdgesSimilarity(modelA, modelB);
                break;
        }
        
        if (similarity < sigDiff) return true;
        return false;
    }
//    
//    public static double getFScore(int[] predictedVals, int[] expectedVals) {
//        
//    }
    
    /**
     * @param predictedVals
     * @param expectedVals
     */
    public static double getRootMeanSquareError(int[] predictedVals, int[] expectedVals) {
        return Math.sqrt(getMeanSquareError(predictedVals, expectedVals));
    }
    
    /**
     * Calculates the mean square error from predicted values and expected values
     * @param predictedVals
     * @param expectedVals
     */
    public static double getMeanSquareError(int[] predictedVals, int[] expectedVals) {
        int n = predictedVals.length;
        double MSE = 0.0;
        for (int i = 0; i < n; i++) {
            MSE += Math.pow((predictedVals[i]-expectedVals[i]), 2);
        }
        return (1.0/n)*MSE; 
    }
}
