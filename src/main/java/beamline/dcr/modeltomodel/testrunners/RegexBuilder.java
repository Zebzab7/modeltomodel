package beamline.dcr.modeltomodel.testrunners;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.modeltomodel.DcrSimilarity;

public class RegexBuilder {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        String rootPath = System.getProperty("user.dir");
        String currentPath = rootPath + "/src/main/java/beamline/dcr/testsoftware";
        String modelPath = currentPath + "/RegexModels/DCRRegexTest.xml";

        DcrModel model = new DcrModel();
        
        model.loadModel(modelPath);
        int maxDepth = 30;

        System.out.println(model.getLabels());

        // String regex = buildRegex(model, maxDepth);

        // System.out.println("Result: " + regex);
    }

    // public static String buildRegex(DcrModel model, int maxDepth, String startNode) {
    //     StringBuilder output = new StringBuilder();
        
    //     Set<String> expanded = new HashSet<>();
    //     Queue<String> frontier = new LinkedList<>();

    //     expanded.add(startNode);
    //     frontier.add(startNode);
    //     while (!frontier.isEmpty()) {
    //         String currentNode = frontier.poll();

    //         output.append(currentNode);

    //         LinkedList<String> neighbors = model.getNeighbors(currentNode);

    //         for (String neighbor : neighbors) {
    //             if (!expanded.contains(neighbor)) {
    //                 expanded.add(neighbor);
    //                 frontier.add(neighbor);
    //             }
    //         }
    //     }

    //     return output.toString();
    // }
}
