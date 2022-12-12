package beamline.dcr.view;

import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.dfg.ExtendedDFG;
import org.apache.commons.lang3.tuple.Triple;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DcrModelXML {

    private DcrModel model;
    private Document document;

    public DcrModelXML(DcrModel model) {

        this.model = model;

        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        this.document = documentBuilder.newDocument();

    }

    public void toFile(String fileName)  {
        //Skeleton from https://documentation.dcr.design/wp-content/uploads/DCR%20Import/DCRImport.xml
        try {
            // root element
            Element root = document.createElement("dcrgraph");
            root.setAttribute("title","DCR");
            document.appendChild(root);
            Element specification = document.createElement("specification");
            Element constraints = document.createElement("constraints");
            Element runtime = document.createElement("runtime");

            insertResourcesAndMarkings(specification,runtime);

            insertConstraints(constraints);
            specification.appendChild(constraints);
            root.appendChild(specification);
            root.appendChild(runtime);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(fileName + ".xml"));
            transformer.transform(domSource, streamResult);




        } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
            e.printStackTrace();
        }


    }
    private void insertResourcesAndMarkings(Element specification, Element runtime){

        //Events
        Element resources = document.createElement("resources");
        Element events = document.createElement("events");
        Element labels = document.createElement("labels");
        Element labelMappings = document.createElement("labelMappings");
        Element marking = document.createElement("marking");
        marking.appendChild(document.createElement("globalStore"));
        marking.appendChild(document.createElement("executed"));
        Element included = document.createElement("included");
        marking.appendChild(included);
        marking.appendChild(document.createElement("pendingResponses"));
        for (String event : model.getActivities()){
            Element eventElement = document.createElement("event");
            Element customEvent = document.createElement("custom");
            Element visualization = document.createElement("visualization");
            Element location = document.createElement("location");
            location.setAttribute("xLoc","1");
            location.setAttribute("yLoc","1");
            visualization.appendChild(location);
            customEvent.appendChild(visualization);
            eventElement.appendChild(customEvent);
            eventElement.setAttribute("id",event);
            events.appendChild(eventElement);

            //labels and mapping
            Element label = document.createElement("label");
            label.setAttribute("id", event);
            labels.appendChild(label);
            Element labelMapping = document.createElement("labelMapping");
            labelMapping.setAttribute("eventId", event);
            labelMapping.setAttribute("labelId", event);
            labelMappings.appendChild(labelMapping);

            //Markings
            Element eventMark = document.createElement("event");
            eventMark.setAttribute("id", event);
            included.appendChild(eventMark);

        }
        runtime.appendChild(marking);
        resources.appendChild(events);
        resources.appendChild(labels);
        resources.appendChild(labelMappings);
        specification.appendChild(resources);

    }
    private void insertConstraints(Element constraints) {

        Element conditions = document.createElement("conditions");
        Element responses = document.createElement("responses");
        Element coresponses = document.createElement("coresponses");
        Element excludes = document.createElement("excludes");
        Element includes = document.createElement("includes");
        Element milestones = document.createElement("milestones");
        Element spawns = document.createElement("spawns");

        for(Triple<String, String, DcrModel.RELATION> relation : model.getRelations()){
            String relationName = relation.getRight().name().toLowerCase();
            Element relationElement = document.createElement(relationName);
            relationElement.setAttribute("sourceId", relation.getLeft());
            relationElement.setAttribute("targetId", relation.getMiddle());
            switch (relationName){
                case "condition":
                    conditions.appendChild(relationElement);
                    break;
                case "response":
                    responses.appendChild(relationElement);
                    break;
                case "coresponse":
                    coresponses.appendChild(relationElement);
                    break;
                case "exclude":
                    excludes.appendChild(relationElement);
                    break;
                case "include":
                    includes.appendChild(relationElement);
                    break;
                case "milestone":
                    milestones.appendChild(relationElement);
                    break;
                case "spawn":
                    spawns.appendChild(relationElement);
                    break;
            }

        }

        constraints.appendChild(conditions);
        constraints.appendChild(responses);
        constraints.appendChild(coresponses);
        constraints.appendChild(excludes);
        constraints.appendChild(includes);
        constraints.appendChild(milestones);
        constraints.appendChild(spawns);
    }


}
