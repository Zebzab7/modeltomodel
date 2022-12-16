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
import metrics.GraphEditDistance;
import metrics.JaccardDistance;

public class DriftDetector {
    
    public enum SIMILARITY_MEASURE {
        GED,
        CNE
    }
    
    static double sigDiff = 0.7;
    
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DBSCANClusteringException {
        String rootPath = System.getProperty("user.dir");
        String modelPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/groundtruthmodels/process101.xml";
        
        DcrModel referenceModel = new DcrModel();
        referenceModel.loadModel(modelPath);
        
        ArrayList<DcrModel> models = PatternChangeComparison.seasonalDriftMutations(referenceModel);
        
        System.out.println("Number of drifts detected GED: " 
                + detectConceptDriftFromModelSeries(models, 0.1, 10, new GraphEditDistance()));
        System.out.println("Number of drifts detected Jaccard: " 
                + detectConceptDriftFromModelSeries(models, 0.1, 10, new JaccardDistance()));
    }
    
    /**
     * Returns the number of concept drifts that have been detected from the models
     * @throws DBSCANClusteringException 
     */
    public static int detectConceptDriftFromModelSeries(ArrayList<DcrModel> models, double eps, 
                int minPoints, DistanceMetric<DcrModel> metric) throws DBSCANClusteringException {
        
        DBSCANClusterer<DcrModel> scanner = new DBSCANClusterer<DcrModel>(models, minPoints, eps, metric);
        ArrayList<ArrayList<DcrModel>> list = new ArrayList<ArrayList<DcrModel>>();
        try {
            list = scanner.performClustering();
        } catch (DBSCANClusteringException e) {
            System.out.println("There was some kind of error with clustering data...");
        }
          
        int i = 1;
        for (ArrayList<DcrModel> cluster : list) {
            System.out.println("Cluster " + i + " size: " + cluster.size());
            i++;
        }
        return list.size()-1;
    }
    
    /**
     * Returns the number of concept drifts that have been detected from the models
     */
    public static int simpleDetection(ArrayList<DcrModel> models) {
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
    
    private ArrayList<DcrModel> getNeighbours(ArrayList<DcrModel> models, DcrModel model, double eps) {
        ArrayList<DcrModel> neighbours = new ArrayList<DcrModel>();
        for(int i=0; i< models.size(); i++) {
            DcrModel candidate = models.get(i);
            if (DcrSimilarity.graphEditDistanceSimilarity(model, candidate) <= eps) {
                neighbours.add(candidate);
            }
        }
        return neighbours;
    }
    
    /*
     * Considers two models and determines if they are significantly different from each other
     */
    public int detectConceptDriftFromModels(DcrModel modelA, DcrModel modelB, SIMILARITY_MEASURE sim) {
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
    
    public void setSigDiff(double num) {
        sigDiff = num;
    }
}
