package beamline.dcr.modeltomodelcomparison.testrunners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.tuple.Triple;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.DcrModel.RELATION;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.attribute.Rank.RankType;
import guru.nidi.graphviz.engine.*;
import guru.nidi.graphviz.model.*;
import static guru.nidi.graphviz.model.Factory.*;

/**
 * NOTE - Only supports up to 2 levels of nesting so far. 
 * So you can have activities with nested activities, 
 * but the nested activities cannot have further nested activities 
 */
public class ModelViewer {
    private static int count = 0;

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        int modelId = 7188;
        int version = 0;

        String rootPath = System.getProperty("user.dir");
        // String modelRepo = rootPath + "/src/main/java/beamline/dcr/testsoftware/publicrepodataset/model" + modelId;
        String modelRepo = rootPath + "/src/main/java/beamline/dcr/testsoftware/driftedmodels/ResearchPaperExample/";
        String filePath = null;
        String fileName = null;
        
        // if (version == 0) {
        //     filePath = modelRepo + "/original.xml";
        //     fileName = "original";
        // } else {
        //     filePath = modelRepo + "/version" + version + ".xml";
        //     fileName = "version" + version;
        // }

        filePath = modelRepo + "ResearchPaper9.xml";
        fileName = "ResearchPaper9";

        DcrModel model = new DcrModel();
        model.loadModel(filePath);

        System.out.println(model.getRelations().toString());
        
        String writePath = modelRepo + "/View/" + fileName + ".png";
        dcrGraphToImage(model, writePath);
        System.out.println("Graph rendered to " + writePath);
    }

    public static void dcrGraphToImage(DcrModel model, String outputFilePath) throws IOException {

        MutableGraph graph = mutGraph("DCRGraph" + count).setDirected(true);
        count++;

        HashMap<String, String> labelMap = model.getLabelMappings();
        ArrayList<String> parentActivities = model.getParentActivities();
        for (int i = 0; i < parentActivities.size(); i++) {
            String parentActivity = parentActivities.get(i);
            ArrayList<String> childActivities = model.getSubActivitiesFromParent(parentActivity);
            MutableGraph cluster = mutGraph(labelMap.get(parentActivity)).setDirected(true).setCluster(true);
            MutableGraph subGraph = mutGraph("").setDirected(true);
            // cluster.graphAttrs().add(Label.of(labelMap.get(parentActivity)));
            subGraph.add(mutNode(labelMap.get(parentActivity)).add(Color.TRANSPARENT));
            for (int j = 0; j < childActivities.size(); j++) {
                String childActivity = childActivities.get(j);
                addNode(subGraph, labelMap.get(childActivity));
            }
            cluster.add(subGraph);
            graph.add(cluster);
        }
        Set<String> activitySet = model.getActivities();
        String[] activities = activitySet.toArray(new String[activitySet.size()]);
        int n = activities.length;
        for (int i = 0; i < n; i++) {
            
            // First add the activity
            String activity = activities[i];
            
            // Then add all of its relations to other activities
            Set<Triple<String, String, RELATION>> relationSet = model.getDcrRelationsWithSource(activity);
            Triple<String, String, RELATION>[] relations = relationSet.toArray(new Triple[relationSet.size()]);
            for (int j = 0; j < relations.length; j++) {
                addEdge(graph, labelMap.get(activity), labelMap.get(relations[j].getMiddle()), relations[j].getRight());
            }
        }
        File outputImage = new File(outputFilePath);

        Graphviz.fromGraph(graph).render(Format.PNG).toFile(outputImage);
    }

    private static void addNode(MutableGraph graph, String label) {
        MutableNode currentNode = mutNode(label).add(Shape.RECTANGLE);
        graph.add(currentNode);
    }

    private static void addEdge(MutableGraph graph, String fromNode, String toNode, RELATION relation) {
        switch(relation) {
            case CONDITION:
                graph.add(mutNode(fromNode).add(Shape.RECTANGLE).addLink(to(mutNode(toNode).add(Shape.RECTANGLE)).add(Color.ORANGE)));
                break;
            case RESPONSE:
                graph.add(mutNode(fromNode).add(Shape.RECTANGLE).addLink(to(mutNode(toNode).add(Shape.RECTANGLE)).add(Color.BLUE)));
                break;
            case INCLUDE:
                graph.add(mutNode(fromNode).add(Shape.RECTANGLE).addLink(to(mutNode(toNode).add(Shape.RECTANGLE)).add(Color.GREEN)));
                break;
            case EXCLUDE: 
                graph.add(mutNode(fromNode).add(Shape.RECTANGLE).addLink(to(mutNode(toNode).add(Shape.RECTANGLE)).add(Color.RED)));
                break;
            case MILESTONE:
                graph.add(mutNode(fromNode).add(Shape.RECTANGLE).addLink(to(mutNode(toNode).add(Shape.RECTANGLE)).add(Color.PURPLE)));
                break;
        }
    }
}