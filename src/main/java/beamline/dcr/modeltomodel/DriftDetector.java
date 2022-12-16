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
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison.DRIFT;
import metrics.CommonNodesAndEdges;
import metrics.GraphEditDistance;
import metrics.JaccardDistance;

public class DriftDetector {
    
    public enum SIMILARITY_MEASURE {
        GED,
        CNE
    }
    
    static double eps = 0.15;
    static int minPoints = 5;
    
    public static void setEps(double eps) {
        DriftDetector.eps = eps;
    }
    
    public static void setMinPoints(int minPoints) {
        DriftDetector.minPoints = minPoints;
    }
    
    static ArrayList<DistanceMetric<DcrModel>> metrics = new ArrayList<DistanceMetric<DcrModel>>
    (Arrays.asList(new GraphEditDistance(), new CommonNodesAndEdges(),new JaccardDistance()));
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DBSCANClusteringException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String modelPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/groundtruthmodels/process101.xml";
        
//        StringBuilder outputString
//            = new StringBuilder("Metric,Sudden,Gradual,Seasonal,Incremental,eps,minPoints\n");
        
        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(modelPath);
        
        ArrayList<ArrayList<DcrModel>> suddenSeries = new ArrayList<ArrayList<DcrModel>>();
        ArrayList<ArrayList<DcrModel>> gradualSeries = new ArrayList<ArrayList<DcrModel>>();
        ArrayList<ArrayList<DcrModel>> seasonalSeries = new ArrayList<ArrayList<DcrModel>>();
        ArrayList<ArrayList<DcrModel>> incrementalSeries = new ArrayList<ArrayList<DcrModel>>();
        
        // Initialize expected values and simulate data sets of process models
        int iterations = 5;
        int[][] expectedVals = new int[metrics.size()][iterations];
        for(int i = 0; i < iterations; i++) {
            suddenSeries.add(PatternChangeComparison.suddenDriftMutations(referenceModel));
            gradualSeries.add(PatternChangeComparison.gradualDriftMutations(referenceModel));
            seasonalSeries.add(PatternChangeComparison.seasonalDriftMutations(referenceModel));
            incrementalSeries.add(PatternChangeComparison.incrementalDriftMutations(referenceModel));
            for (int j = 0; j < metrics.size(); j++) {
                expectedVals[j][i] = 1;
            }
        }
        
        double[][] MSE = new double[4][metrics.size()];
        
        MSE[0] = getMSEFromSeries(suddenSeries, expectedVals, iterations);
        MSE[1] = getMSEFromSeries(gradualSeries, expectedVals, iterations);
        MSE[2] = getMSEFromSeries(seasonalSeries, expectedVals, iterations);
        MSE[3] = getMSEFromSeries(incrementalSeries, expectedVals, iterations);
        
        for (int i = 0; i < 4; i++) {
            System.out.println("Drift type no: " + i);
            for (int j = 0; j < metrics.size(); j++) {
                System.out.println(metrics.get(j).toString() + ": " + MSE[i][j]);
            }
            System.out.println();
        }
        
//        FileWriter myWriter 
//            = new FileWriter(currentPath + "Test-" + java.time.LocalDate.now() + ".csv"/*,true*/);
//        myWriter.write(outputString.toString());
//        myWriter.close();
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
    
    public static double[] getMSEFromSeries(ArrayList<ArrayList<DcrModel>> series, int[][] expectedVals, int iterations) throws DBSCANClusteringException {
        
        int[][] predictedVals = new int[metrics.size()][iterations];
        double[] metricsMSE = new double[metrics.size()];
        for (int i = 0; i < metrics.size(); i++) {
            for (int j = 0; j < iterations; j++) {
                predictedVals[i][j] = DBSCAN(series.get(j), eps, minPoints, metrics.get(i));
            }
            metricsMSE[i] = meanSquareError(predictedVals[i], expectedVals[i]);
        }
        
        return metricsMSE;
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
    
    /*
     * Considers two models and determines if they are significantly different from each other
     */
    public int detectSignificantDifference(DcrModel modelA, DcrModel modelB, SIMILARITY_MEASURE sim, double sigDiff) {
        double similarity = -1.0;
        switch(sim) {
            case GED: 
                similarity = DcrSimilarity.graphEditDistanceSimilarity(modelA, modelB);
                break;
            case CNE: 
                similarity = DcrSimilarity.commonNodesAndEdgesSimilarity(modelA, modelB);
        }
        if (similarity < sigDiff) return 1;
        return 0;
    }
    /*
     * Calculates the mean square error from predicted values and expected values
     */
    public static double meanSquareError(int[] predictedVals, int[] expectedVals) {
        int n = predictedVals.length;
        double MSE = 0.0;
        for (int i = 0; i < n; i++) {
            MSE += Math.pow((predictedVals[i]-expectedVals[i]), 2);
        }
        return (1.0/n)*MSE; 
    }
}
