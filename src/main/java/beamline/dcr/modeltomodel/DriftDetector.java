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
import beamline.dcr.testsoftware.ModelAdaption;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison;
import beamline.dcr.testsoftware.testrunners.PatternChangeComparison.DRIFT;
import distancemetrics.CommonNodesAndEdges;
import distancemetrics.GraphEditDistance;
import distancemetrics.JaccardDistance;
import distancemetrics.WeightedGraphEditDistance;
import helper.LinearRegression;
import helper.Pair;

public class DriftDetector {

    public static ArrayList<DcrModel> transformData(ArrayList<DcrModel> models, DcrModel referenceModel,
            DistanceMetric<DcrModel> metric, boolean replace, double sensitivityInput) throws DBSCANClusteringException {
        int strictness = 20;
        int index = strictness/2;
        double sensitivity = sensitivityInput;
        
        boolean[] elementRemoved = new boolean[models.size()];
        double[] similarityScore = new double[models.size()];
        
        for (int i = 0; i < models.size(); i++) {
            similarityScore[i] = DcrSimilarity.jaccardSimilarity(referenceModel, models.get(i));
        }
        //TODO Remove outliers before proceeding to improve function
        
        int n = models.size();
        
        while (index < n-(strictness/2)) {
            double[] yVals = new double[strictness];
            double[] xVals = new double[strictness];
            
            for (int i = 0; i < strictness; ++i) {
                xVals[i] = index-(strictness/2)+i;
                yVals[i] = DcrSimilarity.jaccardSimilarity(referenceModel, models.get(index-(strictness/2)+i));
            }
            
            LinearRegression lg = new LinearRegression(xVals, yVals);
            
            if (Math.abs(lg.getSlope()) > sensitivity) {
                elementRemoved[index] = true;
            }
            index++;
        }
        ArrayList<DcrModel> newList = new ArrayList<DcrModel>();
        if (replace) {
            newList = replace(models, elementRemoved);
        } else {
            for (int i = strictness/2; i < (elementRemoved.length-strictness/2); i++) {
                if (elementRemoved[i] == false) {
                    newList.add(models.get(i));
                }
            }
        }
        return newList;
    }
    
    public static ArrayList<DcrModel> replace(ArrayList<DcrModel> models, boolean[] elementRemoved) {
        ArrayList<DcrModel> newList = new ArrayList<DcrModel>(models);
        int n = models.size();
        for (int i = 0; i < n; i++) {
            if (elementRemoved[i] == true) {
                if (i != n && elementRemoved[i+1] == false) {
                    int tempIndex = i;
                    while (tempIndex >= 0 && elementRemoved[tempIndex] == true) {
                        newList.set(tempIndex, models.get(i+1).getClone());
                        tempIndex--;
                    }
                } else if (i != 0 && elementRemoved[i-1] == false) {
                    int tempIndex = i;
                    while (tempIndex <= n && elementRemoved[tempIndex] == true) {
                        newList.set(tempIndex, models.get(i-1).getClone());
                        tempIndex++;
                    }
                }
            }
        } 
        return newList;
    }
    
    /**
     * Returns the number of concept drifts that have been detected from the models
     * @throws DBSCANClusteringException 
     */
    public static Pair<Integer,ArrayList<Integer>> DBSCAN(ArrayList<DcrModel> models, double eps, 
                int minPoints, DistanceMetric<DcrModel> metric) throws DBSCANClusteringException {
        DBSCANClusterer<DcrModel> scanner = new DBSCANClusterer<DcrModel>(models, minPoints, eps, metric);
        ArrayList<ArrayList<DcrModel>> list = new ArrayList<ArrayList<DcrModel>>();
        try {
            list = scanner.performClustering();
        } catch (DBSCANClusteringException e) {
            System.out.println("There was some kind of error with clustering data...");
        }
        
        ArrayList<Integer> driftIndices = new ArrayList<Integer>();
        
        for (int i = 0; i < list.size(); i++) {
            driftIndices.add(list.get(i).size()-1);
        }
        
        Pair<Integer, ArrayList<Integer>> returnValues = new Pair<Integer, ArrayList<Integer>>(list.size()-1, driftIndices);
        return returnValues;
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
    
    
    public static double getAccuracy(int[] predictedVals, int[] expectedVals) {
        int num = 0;
        for (int i = 0; i < expectedVals.length; i++) {
            if (predictedVals[i] == expectedVals[i]) num++;
        }
        return (double) num/expectedVals.length;
    }

    
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
