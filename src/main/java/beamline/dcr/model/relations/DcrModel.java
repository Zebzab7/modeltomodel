package beamline.dcr.model.relations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.tuple.Triple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DcrModel {
	private Set<String> activities;
	private Set<Triple<String, String, RELATION>> relations = new HashSet<Triple<String, String, RELATION>>();
	private HashMap<String, String> labelMappings = new HashMap<String, String>();
	
	HashMap<String, String> subActivities = new HashMap<String, String>();
	
	public enum RELATION {
		PRECONDITION,
		CONDITION,
		RESPONSE,
		INCLUDE,
		EXCLUDE,
		SPAWN,
		MILESTONE,
		NORESPONSE,
		SEQUENCE;
	}
	
	public DcrModel()  {
		this.activities = new HashSet<>();
	}
	public void addRelations(Set<Triple<String, String, RELATION>> setOfRelations) {
		for (Triple<String, String, RELATION> relation : setOfRelations){
			activities.add(relation.getLeft());
			activities.add(relation.getMiddle());
		}
		relations.addAll(setOfRelations);
	}
	public void addRelation(Triple<String, String, RELATION> relation) {
		activities.add(relation.getLeft());
		activities.add(relation.getMiddle());
		relations.add(relation);
	}
	public void addActivity(String id){
		activities.add(id);
	}
	public void addSubActivity(String subActivity, String parent) {
	    subActivities.put(subActivity, parent);
	}
	public void addActivities(Set<String> activities){
		this.activities.addAll(activities);
	}
	public Set<String> getActivities() {
		return activities;
	}
	public HashMap<String, String> getAllSubActivities() {
        return subActivities;
    }
	public ArrayList<String> getLabels() {
	    ArrayList<String> activitiesList = new ArrayList<String>(activities);
	    ArrayList<String> labels = new ArrayList<String>();
	    for (int i = 0; i < activitiesList.size(); i++) {
	        labels.add(labelMappings.get(activitiesList.get(i)));
        }
	    return labels;
	}
	public boolean isSubActivity(String activity) {
	    if (subActivities.containsKey(activity)) return true;
	    return false;
	}
	public ArrayList<String> getSubActivitiesFromParent(String activity) {
	    ArrayList<String> children = new ArrayList<String>();
	    for (HashMap.Entry<String, String> entry : subActivities.entrySet()) {
	        if (entry.getValue().equals(activity)) {
	            children.add(entry.getKey());
	        }
        }
	    return children;
    }
	public void setSubActivities(HashMap<String, String> subActivities) {
        this.subActivities = subActivities;
    }
	public HashMap<String, String> getLabelMappings() {
        return labelMappings;
    }
	public void setLabelMappings(HashMap<String, String> labelMappings) {
        this.labelMappings = labelMappings;
    }
	public Set<Triple<String, String, RELATION>> getRelations() {
		return relations;
	}
	public String getParentActivity(String subActivity) {
	    return subActivities.get(subActivity);
	}
	public boolean containsRelation(Triple<String, String, RELATION> relation){
		return relations.contains(relation);
	}
	public void removeRelation(String source, String target, RELATION relation){
		relations.remove(Triple.of(source, target, relation));
	}
	public void removeActivity(String source){
		Set<Triple<String, String, DcrModel.RELATION>> relationsToRemove = new HashSet<>();
		for (Triple<String, String, RELATION> relation : relations){
			if (relation.getLeft().equals(source) ){
				relationsToRemove.add(Triple.of(source, relation.getMiddle(), relation.getRight()));

			}
		}
		relations.removeAll(relationsToRemove);
		activities.remove(source);
	}
	public void removeRelations(String source, String target){
		for (Triple<String, String, RELATION> relation : relations){
			if (relation.getLeft().equals(source) && relation.getMiddle().equals(target)){
				relations.remove(Triple.of(source, target, relation.getRight()));
			}
		}
	}
	public void removeRelations(Set<Triple<String,String,RELATION>> relationsToRemove){
		relations.removeAll(relationsToRemove);
	}
	public Set<Triple<String, String, DcrModel.RELATION>> getDcrRelationsWithActivity(String activity){
		return relations.stream()
				.filter(entry -> entry.getMiddle().equals(activity) ||entry.getLeft().equals(activity))
				.collect(Collectors.toSet());
	}
	public Set<Triple<String, String, DcrModel.RELATION>> getDcrRelationsWithSource(String source){
		return relations.stream()
		        .filter(entry -> entry.getLeft().equals(source))
				.collect(Collectors.toSet());
	}
	public Set<Triple<String, String, DcrModel.RELATION>> getDcrRelationWithConstraint(DcrModel.RELATION constraint){
		return relations.stream()
				.filter(entry -> entry.getRight() == constraint)
				.collect(Collectors.toSet());
	}
    public boolean isParentActivity(String activity) {
        int n = subActivities.size();
        if (subActivities.containsValue(activity)) return true;
        return false;
    }



	public void loadModel(String xmlGraphPath) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory factory =
				DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Document doc = builder.parse(new File(xmlGraphPath));
		
		//Set activity list
		NodeList eventList = doc.getElementsByTagName("events").item(0).getChildNodes();
		
		// TODO Extend capability to include sub-sub and sub-sub-sub activities etc.
		for (int i = 0; i < eventList.getLength(); i++) {
			Node activity = eventList.item(i);
			if (activity.getNodeName().equals("event")){
				Element eventElement = (Element) activity;
				String activityId = eventElement.getAttribute("id");
				addActivity(activityId);
				NodeList childNodes = activity.getChildNodes();
				
				// Add any that may exist sub activities
				for (int j = 0; j < childNodes.getLength(); j++) {
				    Node childActivity = childNodes.item(j);
				    if (childActivity.getNodeName().equals("event")) {
				        Element childEventElement = (Element) childActivity;
				        String childActivityId = childEventElement.getAttribute("id");
				        addActivity(childActivityId);
				        addSubActivity(childActivityId, activityId);
				    }
                }
			}
		}
		
		NodeList labelMappingList = doc.getElementsByTagName("labelMappings").item(0).getChildNodes();
		for (int i = 0; i < labelMappingList.getLength(); i++) {
            Node node = labelMappingList.item(i);
            if (node.getNodeName().equals("labelMapping")) {
                Element labelMappingElement = (Element) node;
                String activityName = labelMappingElement.getAttribute("eventId");
                String labelName = labelMappingElement.getAttribute("labelId");
                labelMappings.put(activityName, labelName);
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
	public void loadModelFromString(String xmlContents) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory factory =
				DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Document doc = builder.parse(new InputSource(new StringReader(xmlContents)));
		
		//Set activity list
		NodeList eventList = doc.getElementsByTagName("events").item(0).getChildNodes();
		
		// TODO Extend capability to include sub-sub and sub-sub-sub activities etc.
		for (int i = 0; i < eventList.getLength(); i++) {
			Node activity = eventList.item(i);
			if (activity.getNodeName().equals("event")){
				Element eventElement = (Element) activity;
				String activityId = eventElement.getAttribute("id");
				addActivity(activityId);
				NodeList childNodes = activity.getChildNodes();
				
				// Add any that may exist sub activities
				for (int j = 0; j < childNodes.getLength(); j++) {
				    Node childActivity = childNodes.item(j);
				    if (childActivity.getNodeName().equals("event")) {
				        Element childEventElement = (Element) childActivity;
				        String childActivityId = childEventElement.getAttribute("id");
				        addActivity(childActivityId);
				        addSubActivity(childActivityId, activityId);
				    }
                }
			}
		}
		
		NodeList labelMappingList = doc.getElementsByTagName("labelMappings").item(0).getChildNodes();
		for (int i = 0; i < labelMappingList.getLength(); i++) {
            Node node = labelMappingList.item(i);
            if (node.getNodeName().equals("labelMapping")) {
                Element labelMappingElement = (Element) node;
                String activityName = labelMappingElement.getAttribute("eventId");
                String labelName = labelMappingElement.getAttribute("labelId");
                labelMappings.put(activityName, labelName);
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
	public void loadModelFromTexturalConstraintFile(String path){
		try
		{
			File file=new File(path);    //creates a new file instance
			FileReader fr=new FileReader(file);   //reads the file
			BufferedReader br=new BufferedReader(fr);  //creates a buffering character input stream

			String line;
			while((line=br.readLine())!=null)
			{
				convertTextToConstraints(line);
			}
			fr.close();    //closes the stream and release the resources


		} catch (IOException e) {
			e.printStackTrace();
		}


	}
	private void convertTextToConstraints(String row){
		String[] rowSplit = row.split(" ",3);

		String sourceActivity = rowSplit[0].replace("\"","");
		activities.add(sourceActivity);
		RELATION relation = null;
		switch (rowSplit[1]){
			//Ignore excludes and includes
			case "-->*":
				relation = RELATION.CONDITION;
				break;
			case "*-->":
				relation = RELATION.RESPONSE;
				break;
			default:

		}
		if (relation != null) {
			for(String target : rowSplit[2].split(" ")){

				String targetActivity = target.replace("\"","").replace("(","").replace(")","");
				relations.add(Triple.of(sourceActivity,targetActivity,relation));
				activities.add(targetActivity);
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

				DcrModel.RELATION relation = RELATION.valueOf(constraint.getNodeName().toUpperCase());
				addRelation(Triple.of(source,target, relation));
			}
		}
	}

	public static void writeXmlToFile(String xmlString, String filePath) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
			writer.write(xmlString);
			writer.close();
			System.out.println("XML file written successfully.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a shallow copy of this DCR model
	 */
	public DcrModel getClone() {
	    DcrModel copy = new DcrModel();
	    copy.addActivities(this.activities);
	    copy.addRelations(this.relations);
	    copy.setSubActivities(this.subActivities);
	    copy.setLabelMappings(this.labelMappings);
	    return copy;
	}
}
