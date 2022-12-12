package beamline.dcr.testsoftware;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.UnionRelationSet;
import beamline.dcr.model.relations.dfg.ExtendedDFG;
import org.apache.commons.lang3.tuple.Triple;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TransitionSystem {
    //TODO write to dcrmodel instead of unionRelationSet
    private List<String> eventList;
    private BitSet[] marking; //executed,included,pending

    UnionRelationSet unionRelationSet;

    public TransitionSystem(String xmlGraphPath) throws IOException, SAXException, ParserConfigurationException {
        this.eventList = new ArrayList<>();
        this.unionRelationSet = new UnionRelationSet(new ExtendedDFG(),0);
        loadGraph(xmlGraphPath);
        createInitialMarking();

    }
    public TransitionSystem(UnionRelationSet unionRelationSet) {

        this.unionRelationSet = unionRelationSet;
        this.eventList = Arrays.asList(unionRelationSet.getUniqueActivities());
        createInitialMarking();



    }

    private void createInitialMarking(){
        this.marking = new BitSet[eventList.size()];
        for(int i = 0; i < marking.length; i++) {
            this.marking[i] = new BitSet(3);
            //set included to true initially
            this.marking[i].set(1);
        }
    }

    public List<String> getEnabledEvents() throws Exception {

        List<String> includedEvents = getIncludedEvents();
        List<String> enabledEvents = new ArrayList<>();

        //Check each event
        for(int i = 0; i < includedEvents.size(); i++){

            String event = includedEvents.get(i);
            //Set all includedto true - unconstrainted
            int eventListIndex = eventList.indexOf(event);
            enabledEvents.add(event);

            //get constraints where activity is target in condition
            Set<Triple<String,String, DcrModel.RELATION>> conditionWithEventTarget = unionRelationSet.getDcrRelationWithTarget(event, DcrModel.RELATION.CONDITION);

            for (Triple<String,String, DcrModel.RELATION> relation : conditionWithEventTarget){
                String source = relation.getLeft();

                if(!isExecuted(source) && isIncluded(source)) {
                    enabledEvents.remove(event);
                }
            }
        }
        return enabledEvents;
    }
    public List<String> getIncludedEvents(){
        List<String> includedEvents = new ArrayList<>();
        for(int i = 0; i < eventList.size(); i++){
            if(marking[i].get(1)){
                includedEvents.add(eventList.get(i));
            }
        }
        return includedEvents;
    }
    public boolean executeEvent(String event) throws Exception {
        String enabledString = "";
        for(int i = 0; i < getEnabledEvents().size(); i++){
            enabledString = enabledString + ", " + getEnabledEvents().get(i);
        }

        if (getEnabledEvents().contains(event)){
            int eventIndex = eventList.indexOf(event);
            //Set to executed
            marking[eventIndex].set(0);
            //Set pending to false
            marking[eventIndex].set(2,false);

            //Change marking
            //Responses with event as source - target -> pending
            for(Triple<String,String, DcrModel.RELATION> relation : unionRelationSet.getDcrRelationWithSource(event, DcrModel.RELATION.RESPONSE)){
                String target = relation.getMiddle();
                int targetIndexEventList = eventList.indexOf(target);
                marking[targetIndexEventList].set(2);
            }

            //excluded with event as source - target -> excluded
            for(Triple<String,String, DcrModel.RELATION> relation : unionRelationSet.getDcrRelationWithSource(event, DcrModel.RELATION.EXCLUDE)){
                String target = relation.getMiddle();
                int targetIndexEventList = eventList.indexOf(target);
                marking[targetIndexEventList].set(1,false);
            }
            //included with event as source - target -> included
            for(Triple<String,String, DcrModel.RELATION> relation : unionRelationSet.getDcrRelationWithSource(event, DcrModel.RELATION.INCLUDE)){
                String target = relation.getMiddle();
                int targetIndexEventList = eventList.indexOf(target);
                marking[targetIndexEventList].set(1);
            }
            return true;
        }else{
            return false;
        }
    }
    public boolean isExecuted(String event) throws Exception {
        int eventIndex = eventList.indexOf(event);
        try{
            marking[eventIndex].get(0);
        }catch (Exception e){
            System.out.println(event);
            System.out.println(eventList);
        }
        return marking[eventIndex].get(0);
    }
    public boolean isIncluded(String event){
        int eventIndex = eventList.indexOf(event);
        return marking[eventIndex].get(1);
    }
    public boolean anyPendingEvents(){

        for(int i = 0; i < eventList.size(); i++){
            if (marking[i].get(2)){
                return true;
            }
        }

        return false;
    }
    public void resetMarking(){
        createInitialMarking();
    }
    public int getExecutedOfEnabled() throws Exception {
        List<String> enabledEvents = getEnabledEvents();
        int numExecuted = 0;

        //Check each event
        for(int i = 0; i < enabledEvents.size(); i++) {

            String event = enabledEvents.get(i);
            //is event also executed
            int eventListIndex = eventList.indexOf(event);
            if (marking[eventListIndex].get(0)) numExecuted++;
        }
        return numExecuted;
    }
    public BitSet[] getMarking(){
        return marking;
    }
    private void loadGraph(String xmlGraphPath) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.parse(new File(xmlGraphPath));

        //Set activity list
        NodeList eventList = doc.getElementsByTagName("events").item(0).getChildNodes();

        for (int i = 0; i < eventList.getLength(); i++) {
            Node event = eventList.item(i);
            Element eventElement = null;
            if (event.getNodeName().equals("event")){
                 eventElement = (Element) event;
                String id = eventElement.getAttribute("id");
                this.eventList.add(id);
            }

        }

        //Set constraints in unionRelationSet
        NodeList constraints = doc.getElementsByTagName("constraints").item(0).getChildNodes();
        for (int j = 0; j < constraints.getLength(); j++) {
            Node childNode = constraints.item(j);
            switch (childNode.getNodeName()){
                case "conditions":
                case "responses":
                case "excludes":
                case "includes":
                    addToRelationSet(childNode.getChildNodes());
                    break;

            }
        }

    }

    private void addToRelationSet(NodeList constraintList){
        for(int i = 0; i < constraintList.getLength(); i++){
            Node constraint = constraintList.item(i);

            if(constraint.getNodeType() == Node.ELEMENT_NODE){

                Element constraintElement = (Element) constraint;

                String source = constraintElement.getAttribute("sourceId");
                String target = constraintElement.getAttribute("targetId");

                DcrModel.RELATION relation = DcrModel.RELATION.valueOf(constraint.getNodeName().toUpperCase());
                unionRelationSet.addDcrRelation(Triple.of(source,target, relation));

            }
        }

    }



}
