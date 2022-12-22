package beamline.dcr.model.relations;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DcrModel {
	private Set<String> activites;
	private Set<Triple<String, String, RELATION>> relations = new HashSet<Triple<String, String, RELATION>>();
	
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
		this.activites = new HashSet<>();
	}
	
	public void addRelations(Set<Triple<String, String, RELATION>> setOfRelations) {
		for (Triple<String, String, RELATION> relation : setOfRelations){
			activites.add(relation.getLeft());
			activites.add(relation.getMiddle());
		}
		relations.addAll(setOfRelations);
	}
	public void addRelation(Triple<String, String, RELATION> relation) {
		activites.add(relation.getLeft());
		activites.add(relation.getMiddle());
		relations.add(relation);
	}
	public void addActivity(String id){
		activites.add(id);
	}
	public void addActivities(Set<String> activities){
		activites.addAll(activities);
	}
	public Set<String> getActivities() {
		return activites;
	}
	public Set<Triple<String, String, RELATION>> getRelations() {
		return relations;
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
		activites.remove(source);
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
	public void loadModel(String xmlGraphPath) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory factory =
				DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Document doc = builder.parse(new File(xmlGraphPath));

		//Set activity list
		NodeList eventList = doc.getElementsByTagName("events").item(0).getChildNodes();

		for (int i = 0; i < eventList.getLength(); i++) {
			Node activity = eventList.item(i);
			if (activity.getNodeName().equals("event")){
				Element eventElement = (Element) activity;
				String activityId = eventElement.getAttribute("id");
				addActivity(activityId);


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
		activites.add(sourceActivity);
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
				activites.add(targetActivity);
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
	
	
	
	/*
	 * Returns a shallow copy of this DCR model
	 */
	public DcrModel getClone() {
	    DcrModel copy = new DcrModel();
	    copy.addActivities(activites);
	    copy.addRelations(relations);
	    return copy;
	}

}
