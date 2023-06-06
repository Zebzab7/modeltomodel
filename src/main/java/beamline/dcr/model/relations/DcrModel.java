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

import beamline.dcr.modeltomodelcomparison.testrunners.TraceGenerator;

public class DcrModel {
	private Set<String> activities;
	private Set<Triple<String, String, RELATION>> relations = new HashSet<Triple<String, String, RELATION>>();
	private HashMap<String, String> labelMappings = new HashMap<String, String>();

	private Set<ActivityRelations> profile = null;
	
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
		labelMappings.put(id, id);
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
	public ArrayList<String> getParentActivities() {
		ArrayList<String> parentActivities = new ArrayList<String>();
	    for (HashMap.Entry<String, String> entry : subActivities.entrySet()) {
	        if (!parentActivities.contains(entry.getValue())) {
	            parentActivities.add(entry.getValue());
	        }
		}
	    return parentActivities;
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
			if (relation.getLeft().equals(source)){
				relationsToRemove.add(Triple.of(source, relation.getMiddle(), relation.getRight()));
			}
			if (relation.getMiddle().equals(source)){
				relationsToRemove.add(Triple.of(relation.getLeft(), source, relation.getRight()));
			}
		}
		relations.removeAll(relationsToRemove);
		labelMappings.remove(source);
		activities.remove(source);
		if(isSubActivity(source)) {
			subActivities.remove(source);
		}
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
	public Set<ActivityRelations> getProfile() {
		return profile;
	}
	public void setProfile(Set<ActivityRelations> profile) {
		this.profile = profile;
	}
	public Set<ActivityRelations> getActivityRelationsFromActivitities(String activity1, String activity2) {
		Set<ActivityRelations> activityRelations = new HashSet<ActivityRelations>();
		for (ActivityRelations activityRelation : profile) {
			if (activityRelation.getActivityA().equals(activity1) && activityRelation.getActivityB().equals(activity2)) {
				activityRelations.add(activityRelation);
			}
		}
		return activityRelations;
	}
	public Set<ActivityRelations> createBehavioralProfile() {
		profile = new HashSet<ActivityRelations>();
		ArrayList<ArrayList<String>> traces = new ArrayList<ArrayList<String>>();

		int traceLength = activities.size();
		int numOfTraces = 100;

		TraceGenerator traceGenerator = new TraceGenerator();
		for (int i = 0; i < numOfTraces; i++) {
			traces.add(traceGenerator.generateRandomTraceFromModel(this, traceLength));
		}

		boolean coOccurence = false;
		boolean exclusive = false;
		boolean strictOrder = false;
		boolean interleaving = false;

		boolean foundActivity1 = false;
		boolean foundActivity2 = false;

		boolean foundActivity1BeforeActivity2 = false;
		boolean foundActivity2BeforeActivity1 = false;

		String[] activitiesList = activities.toArray(new String[activities.size()]);

		for (int i = 0; i < activitiesList.length; i++) {
			for (int j = 0; j < activitiesList.length; j++) {
				if (i == j) {
					continue;
				}

				String activity1 = activitiesList[i];
				String activity2 = activitiesList[j];

				ActivityRelations activityRelation = new ActivityRelations(activity1, activity2);

				coOccurence = true;
				exclusive = true;
				strictOrder = true;
				interleaving = false;

				for (int k = 0; k < traces.size(); k++) {
					ArrayList<String> trace = traces.get(k);
					int n = trace.size();

					foundActivity1 = false;
					foundActivity2 = false;

					for (int k2 = 0; k2 < n; k2++) {
						String traceActivity = traces.get(k).get(k2);
						if (!foundActivity1 && traceActivity.equals(activity1)) {
							foundActivity1 = true;
						}
						if (!foundActivity2 && traceActivity.equals(activity2)) {
							foundActivity2 = true;
						}

						// If the second activity is found before the first activity, the order is not strict
						if (traceActivity.equals(activity2) && !foundActivity1) {
							foundActivity2BeforeActivity1 = true;
							strictOrder = false;
						}
						if (traceActivity.equals(activity1) && !foundActivity2) {
							foundActivity1BeforeActivity2 = true;
						}
					}

					// If at any point have found both orders of activities in the trace, they are interleaving
					if (foundActivity1BeforeActivity2 && foundActivity2BeforeActivity1) {
						interleaving = false;
					}

					// If both activities are found in any trace, they can not be exclusive
					if (foundActivity1 && foundActivity2) {
						exclusive = false;
					}

					// If at any point one activity is found and the other is not, they can not be co-occurent
					if (foundActivity1 ^ foundActivity2) {
						coOccurence = false;
					}
				}
				
				if (strictOrder) {
					activityRelation.addRelation("strict-order");
				}
				if (exclusive) {
					activityRelation.addRelation("exclusive");
				}
				if (coOccurence) {
					activityRelation.addRelation("co-occurence");
				}
				if (interleaving) {
					activityRelation.addRelation("interleaving");
				}
				profile.add(activityRelation);
			}
		}
		return profile;
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
				case "milestones":
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
			// System.out.println("XML file written successfully.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a shallow copy of this DCR model
	 */
	public DcrModel getClone() {
	    DcrModel copy = new DcrModel();
		for (String activity : this.activities) {
			String newActivity = activity;
			copy.activities.add(newActivity);
		}
		for (HashMap.Entry<String, String> entry : this.subActivities.entrySet()) {
			String newActivity = entry.getKey();
			String newParent = entry.getValue();
			copy.subActivities.put(newActivity, newParent);
		}
		for (Triple<String, String, RELATION> relation : this.relations) {
			String newSource = relation.getLeft();
			String newTarget = relation.getMiddle();
			RELATION newConstraint = relation.getRight();
			copy.relations.add(Triple.of(newSource, newTarget, newConstraint));
		}
		for (HashMap.Entry<String, String> entry : this.labelMappings.entrySet()) {
		    String newActivity = entry.getKey();
		    String newLabel = entry.getValue();
		    copy.labelMappings.put(newActivity, newLabel);
		}
	    // copy.addActivities(this.activities);
	    // copy.addRelations(this.relations);
	    // copy.setSubActivities(this.subActivities);
	    // copy.setLabelMappings(this.labelMappings);
	    return copy;
	}
}
