package beamline.dcr.testsoftware;

import org.apache.tomcat.util.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DcrApiCommunicator {
/* Not used atm*/
    private String modelId;
    private String instanceId;

    public DcrApiCommunicator(String modelId) {
        this.modelId = modelId;
    }

    public void instantiateModel() throws Exception {
        String url = "https://repository.dcrgraphs.net/api/graphs/" +modelId + "/sims";

        //instantiate and define instance id
        HttpURLConnection connection = sendRequest("POST","",url);
        this.instanceId = getInstanceId(connection);

        if (instanceId == null){
            throw new Exception("The process instance was not created");
        }

    }

    public List<String> getEnabledEvents(){
        String url="https://repository.dcrgraphs.net/api/graphs/"+ modelId + "/sims/"+
                instanceId + "/events?filter=only-enabled";

        HttpURLConnection connection = sendRequest("GET","",url);
        Document xmlBody = getXmlResponse(connection);
        NodeList events = xmlBody.getElementsByTagName("event");
        List<String> enabledEvents = new ArrayList<>();
        for(int i = 0;  i<events.getLength(); i++){

            enabledEvents.add(events.item(i).getAttributes().getNamedItem("id").getNodeValue());
        }

        return enabledEvents;
    }
    public boolean executeEvent(String eventId){

        String url="https://repository.dcrgraphs.net/api/graphs/"+ modelId +
                "/sims/"+ instanceId + "/events/" + eventId;

        HttpURLConnection connection = sendRequest("POST","",url);
        int requestStatus = getResponseStatus(connection);

        return requestStatus == 204;



    }

    public void deleteProcessInstance() throws Exception {
        String url="https://repository.dcrgraphs.net/api/graphs/"+ modelId +
                "/sims/"+ instanceId;
        HttpURLConnection connection = sendRequest("DELETE","",url);
        if(connection.getResponseCode()!= 204) throw new Exception("Instance not deleted");
    }

    private HttpURLConnection sendRequest(String requestType, String body, String urlString){
        try {
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set timeout as per needs
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);


            connection.setDoOutput(true);
            connection.setUseCaches(true);
            connection.setRequestMethod(requestType);

            // Set Headers
            connection.setRequestProperty("Accept", "application/xml");
            connection.setRequestProperty("Content-Type", "application/xml; utf-8");


            String auth = getAuthorizationFromFile();
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);
            connection.setRequestProperty("Authorization", authHeaderValue);

            // Write XML if POST
            if (requestType == "POST"){
                OutputStream outputStream = connection.getOutputStream();
                byte[] b = body.getBytes("UTF-8");
                outputStream.write(b);
                outputStream.flush();
                outputStream.close();
            }



            return connection;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Document getXmlResponse(HttpURLConnection connection){
        // Read XML
        try {
            InputStream inputStream = connection.getInputStream();


            byte[] res = new byte[2048];
            int i = 0;
            StringBuilder response = new StringBuilder();
            while ((i = inputStream.read(res)) != -1) {
                response.append(new String(res, 0, i));
            }

            //Remove quotationmark if present
            if (response.toString().startsWith("\"")){
                response=response.deleteCharAt(0);
                response=response.deleteCharAt(response.length()-1);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response.toString().replace("\\","")));
            Document doc = builder.parse(is);
            inputStream.close();
            connection.disconnect();
            return doc;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getInstanceId(HttpURLConnection connection){
        String headerFieldValue = connection.getHeaderField("X-DCR-simulation-ID");
        connection.disconnect();
        return headerFieldValue;

    }

    private int getResponseStatus(HttpURLConnection connection){
        int responseCode = 500;
        try {
            responseCode = connection.getResponseCode();
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseCode;
    }

    private String getAuthorizationFromFile(){
        try {

            File myObj = new File("src/main/java/beamline/dcr/testsoftware/ApiAuthorization.txt");
            Scanner myReader = new Scanner(myObj);
            String authenticationString ="";
            while (myReader.hasNextLine()) {
                authenticationString = myReader.nextLine();

            }
            myReader.close();
            return authenticationString;
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return "";
    }
}
