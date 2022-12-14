package beamline.dcr.testsoftware;

import beamline.dcr.model.relations.DcrModel;
import org.apache.commons.lang3.tuple.Triple;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

public class ModelAdaption {
    
    private DcrModel model;

    public ModelAdaption(String modelPath) throws IOException, SAXException, ParserConfigurationException {
        this.model = new DcrModel();
        this.model.loadModel(modelPath);
    }

    public boolean insertActivitySerial(int numRepeatings){

        for (int i = 0; i < numRepeatings; i++) {
            String activityNew = getRandomNonExistingActivity();

            //Get random activity from all relations
            String randomSource = getRandomExistingActivity(model.getRelations());
            Set<Triple<String,String, DcrModel.RELATION>> relationsWithSource = model.getDcrRelationsWithSource(randomSource);
            while (relationsWithSource.size()==0){
                randomSource = getRandomExistingActivity(model.getRelations());
                relationsWithSource = model.getDcrRelationsWithSource(randomSource);
            }
            //Get random activity from all relations which has randomSource as source
            String randomTarget = getRandomExistingTargetActivity(relationsWithSource);

            //inserts random non existing activity in serial between a dcr relation
            Set<Triple<String, String, DcrModel.RELATION>> relationsToRemove = new HashSet<>();
            Set<Triple<String, String, DcrModel.RELATION>> relationsToAdd = new HashSet<>();
            boolean hasChanged = false;
            for (Triple<String, String, DcrModel.RELATION> relation : model.getRelations()) {
                if (relation.getLeft().equals(randomSource) && relation.getMiddle().equals(randomTarget)) {
                    relationsToAdd.add(Triple.of(randomSource, activityNew, relation.getRight()));
                    relationsToAdd.add(Triple.of(activityNew, randomTarget, relation.getRight()));
                    relationsToRemove.add(Triple.of(randomSource, randomTarget, relation.getRight()));

                    hasChanged = true;
                }
            }
            if (!hasChanged){
                return false;
            }
            model.removeRelations(relationsToRemove);
            model.addRelations(relationsToAdd);
        }
        return true;
    }
    public boolean insertActivityParallel(int numRepeatings){

        for (int i = 0; i < numRepeatings; i++) {
            String randomActivity = getRandomNonExistingActivity();
            model.addActivity(randomActivity);
        }
        return true;
    }
    public boolean deleteActivity(int numRepeatings){
        for (int i = 0; i < numRepeatings; i++) {
            String randomExistingActivity = getRandomExistingActivity(model.getRelations());
            model.removeActivity(randomExistingActivity);
        }
        return true;
    }
    public boolean replaceActivity(int numRepeatings){
        for (int i = 0; i < numRepeatings; i++) {
            String oldActivity = getRandomExistingActivity(model.getRelations());
            String newActivity = getRandomNonExistingActivity();
            Set<Triple<String, String, DcrModel.RELATION>> relationsWithActivity = new HashSet<>();
            relationsWithActivity.addAll(model.getDcrRelationsWithActivity(oldActivity));

            for (Triple<String, String, DcrModel.RELATION> relation : relationsWithActivity) {
                String source = relation.getLeft();
                String target = relation.getMiddle();
                DcrModel.RELATION dcrConstraint = relation.getRight();
                model.removeRelation(source, target, dcrConstraint);
                if (source.equals(oldActivity)) {
                    model.addRelation(Triple.of(newActivity, target, dcrConstraint));
                } else {
                    model.addRelation(Triple.of(source, newActivity, dcrConstraint));
                }
            }
        }
        return true;
    }
    public boolean addConstraint(int numRepeatings){
        for (int i = 0; i < numRepeatings; i++) {
            String source = getRandomExistingActivity(model.getRelations());
            if(source==null){
                return false;
            }
            Set<Triple<String,String, DcrModel.RELATION>> relationsWithSource = model.getDcrRelationsWithSource(source);
            while (relationsWithSource.size()==0){
                source = getRandomExistingActivity(model.getRelations());
                relationsWithSource = model.getDcrRelationsWithSource(source);
            }

            String target = getRandomExistingActivity(relationsWithSource);
            List<DcrModel.RELATION> constraints =
                    Collections.unmodifiableList(Arrays.asList(DcrModel.RELATION.values()));
            int size = constraints.size();
            Random random = new Random();

            DcrModel.RELATION randomConstraint = constraints.get(random.nextInt(size));

            model.addRelation(Triple.of(source, target, randomConstraint));
        }
        return true;
    }
    public boolean removeConstraint(int numRepeatings){
        for (int j = 0; j < numRepeatings; j++) {
            Set<Triple<String, String, DcrModel.RELATION>> relations = model.getRelations();
            Triple<String, String, DcrModel.RELATION> relationToRemove = null;
            int size = relations.size();
            if (size==1){
                relationToRemove = (Triple<String, String, DcrModel.RELATION>) model.getRelations().toArray()[0];
            }else if(size==0){
                return false;
            }else{
                int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
                int i = 0;

                for (Triple<String, String, DcrModel.RELATION> randomRelation : relations) {
                    if (i == item) {
                        relationToRemove = randomRelation;
                        break;
                    }
                    i++;
                }
            }

            model.removeRelation(relationToRemove.getLeft(), relationToRemove.getMiddle(), relationToRemove.getRight());
        }
        return true;
    }
    public boolean swapActivities(int numRepeatings){
        for (int i = 0; i < numRepeatings; i++) {

            Set<Triple<String, String, DcrModel.RELATION>> dcrRelations = model.getRelations();
            if (dcrRelations.size()<=1){
                return false;
            }
            Set<Triple<String, String, DcrModel.RELATION>> relationsToRemove = new HashSet<>();
            Set<Triple<String, String, DcrModel.RELATION>> relationsToAdd = new HashSet<>();

            String activity1 = getRandomExistingActivity(dcrRelations);
            String activity2 = activity1;
            int maxCount = 0;
            while (activity2.equals(activity1) && maxCount < 20) {
                activity2 = getRandomExistingActivity(dcrRelations);
                maxCount++;
            }

            for (Triple<String, String, DcrModel.RELATION> relation : dcrRelations) {
                String source = relation.getLeft();
                String target = relation.getMiddle();
                DcrModel.RELATION dcrConstraint = relation.getRight();
                if (source.equals(activity1)) {
                    relationsToRemove.add(relation);
                    relationsToAdd.add(Triple.of(activity2, target, dcrConstraint));
                } else if (target.equals(activity1)) {
                    relationsToRemove.add(relation);
                    relationsToAdd.add(Triple.of(source, activity2, dcrConstraint));
                } else if (source.equals(activity2)) {
                    relationsToRemove.add(relation);
                    relationsToAdd.add(Triple.of(activity1, target, dcrConstraint));
                } else if (target.equals(activity2)) {
                    relationsToRemove.add(relation);
                    relationsToAdd.add(Triple.of(source, activity1, dcrConstraint));
                }

            }
            model.addRelations(relationsToAdd);
            model.removeRelations(relationsToRemove);
        }
        return true;
    }
    public boolean randomMutation(int numRepeatings) {
        switch((new Random()).nextInt(7)) {
            case 0:
                insertActivitySerial(numRepeatings);
                break;
            case 1:
                insertActivityParallel(numRepeatings);
                break;
            case 2:
                deleteActivity(numRepeatings);
                break;
            case 3:
                replaceActivity(numRepeatings);
                break;
            case 4:
                addConstraint(numRepeatings);
                break;
            case 5:
                removeConstraint(numRepeatings);
                break;
            case 6:
                swapActivities(numRepeatings);
                break;
        }
        return true;
    }
    public DcrModel getModel(){
        return model;
    }
    private String getRandomNonExistingActivity(){
        Set<String> activities = model.getActivities();
        Random r = new Random();

        String alphabet = "123xyz";
        String randomActivity = "A";
        while (activities.contains(randomActivity)){
            randomActivity = "Activity " + String.valueOf(alphabet.charAt(r.nextInt(alphabet.length()))) + String.valueOf(alphabet.charAt(r.nextInt(alphabet.length())));
        }


        return randomActivity;
    }
    public String getRandomExistingActivity(Set<Triple<String,String, DcrModel.RELATION>> relationSet){
        Set<String> activities = new HashSet<>();
        for(Triple<String, String, DcrModel.RELATION> relation : relationSet){
            activities.add(relation.getLeft());
            activities.add(relation.getMiddle());
        }

        int size = activities.size();
        if (size==1){
            return (String) activities.toArray()[0];
        }if (size==0){
            return null;
        }
        int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        int i = 0;
        for(String randomActivity : activities)
        {
            if (i == item)
                return randomActivity;
            i++;
        }
        return null;
    }
    public String getRandomExistingTargetActivity(Set<Triple<String,String, DcrModel.RELATION>> relationSet){
        Set<String> activities = new HashSet<>();
        for(Triple<String, String, DcrModel.RELATION> relation : relationSet){
            activities.add(relation.getMiddle());
        }

        int size = activities.size();
        if (size==1){
            return (String) activities.toArray()[0];
        }if (size==0){
            return null;
        }
        int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        int i = 0;
        for(String randomActivity : activities)
        {
            if (i == item)
                return randomActivity;
            i++;
        }
        return null;
    }


}


