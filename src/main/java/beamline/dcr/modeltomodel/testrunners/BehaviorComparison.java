package beamline.dcr.modeltomodel.testrunners;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import beamline.dcr.model.relations.DcrModel;

public class BehaviorComparison {
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        String rootPath = System.getProperty("user.dir");
        String filepath = rootPath + "/src/main/java/beamline/dcr/testsoftware/publicrepodataset";

        String modelResponse = null;
        ArrayList<String> modelIds = null;

        // Retrieve available models
        try {
            URL url = new URL("https://repository.dcrgraphs.net/api/graphs");
            String response = httpRequest(url);
            modelIds = extractIds(response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.err.println("Found " + modelIds.size() + " models");

        int countOverX = 0;
        int max = 0;
        int X = 15;

        double edgesSum = 0;
        int edgesCount = 0;

        double activitiesSum = 0;
        int activitiesCount = 0;
        
        double minorRevisionSum = 0;
        int minorRevisionCount = 0;      
        
        double majorRevisionSum = 0;
        int majorRevisionCount = 0;    

        double totalRevisionsSum = 0;
        int totalRevisionsCount = 0;

        // for (int i = 0; i < modelIds.size(); i++) {
        //     int id = Integer.parseInt(modelIds.get(i));
        //     URL modelsURL = new URL("https://repository.dcrgraphs.net/api/graphs/" + id);
        //     modelResponse = httpRequest(modelsURL);

        //     if (modelResponse != null && !modelResponse.equals("")) {
        //         DcrModel currentModel = new DcrModel();
        //         currentModel.loadModelFromString(modelResponse);
        //         activitiesSum += currentModel.getActivities().size();
        //         activitiesCount++;
        //         edgesSum += currentModel.getRelations().size();
        //         edgesCount++;

        //         if (currentModel.getActivities().size() > X) {
        //             countOverX++;
        //         }
                
        //         if (currentModel.getActivities().size() > max) {
        //             max = currentModel.getActivities().size();
        //         }

        //         String versions = httpRequest(new URL("https://repository.dcrgraphs.net/api/graphs/" + id + "/versions"));
    
        //         String word = "major";
        //         int occurrences = StringUtils.countMatches(versions, word);

        //         majorRevisionSum += occurrences;
        //         majorRevisionCount++;

        //         String word2 = "minor";
        //         int occurrences2 = StringUtils.countMatches(versions, word2);

        //         minorRevisionSum += occurrences2;
        //         minorRevisionCount++;

        //         totalRevisionsSum += occurrences + occurrences2;
        //         totalRevisionsCount++;
        //     }
        // }

        System.err.println("Max number of activities: " + max);
        System.err.println("Average number of activities: " + activitiesSum / activitiesCount);
        System.err.println("Average number of edges: " + edgesSum / edgesCount);
        System.err.println("Number of minor revisions average number of activities: " + minorRevisionSum / minorRevisionCount);
        System.err.println("Number of major revisions average number of activities: " + majorRevisionSum / majorRevisionCount);
        System.err.println("Number of total revisions average number of activities: " + totalRevisionsSum / totalRevisionsCount);
        System.err.println("Number of models with more than " + X + " activities: " + countOverX);

        // URL modelsURL = new URL("https://repository.dcrgraphs.net/api/graphs/" + id);

        DcrModel.writeXmlToFile(httpRequest(new URL("https://repository.dcrgraphs.net/api/graphs/" + modelIds.get(0))), filepath);
    }

    public static BufferedImage getModelImage(String image) {
        // Remove the prefix and get the image data
        String imageData = image.replace("data:image/png;base64,", "");

        try {
            // Decode Base64 string into a byte array
            byte[] decodedBytes = Base64.getDecoder().decode(imageData);

            // Read the byte array and create a BufferedImage object
            ByteArrayInputStream bis = new ByteArrayInputStream(decodedBytes);
            BufferedImage finalImage = ImageIO.read(bis);

            // Return the image
            return finalImage;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }        
    }

    public static String httpRequest(URL url) {
        try {
            HttpURLConnection con;

            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            // TODO - load username and password from file

            
            String authString = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes());

            con.setRequestProperty("Authorization", "Basic " + encodedAuth);

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
                reader.close();
                return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<String> extractIds(String input) {
        ArrayList<String> ids = new ArrayList<>();
        
        Pattern pattern = Pattern.compile("id=\"(\\d+)\"");
        Matcher matcher = pattern.matcher(input);
        
        while (matcher.find()) {
            String id = matcher.group(1);
            ids.add(id);
        }
        
        return ids;
    }

}
