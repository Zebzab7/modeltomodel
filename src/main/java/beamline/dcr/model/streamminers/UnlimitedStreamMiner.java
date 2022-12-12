package beamline.dcr.model.streamminers;

import beamline.dcr.model.relations.dfg.ActivityDecoration;
import beamline.dcr.model.relations.dfg.ExtendedDFG;
import beamline.dcr.model.relations.dfg.RelationDecoration;

import java.util.*;

public class UnlimitedStreamMiner implements StreamMiner {
    private Map<String, String> latestActivityInTrace = new HashMap<String, String>();
    private Map<String, Integer> indexInCase = new HashMap<String, Integer>();
    private Map<String, Set<String>> observedActivitiesInTrace = new HashMap<String, Set<String>>();
    private ExtendedDFG extendedDFG = new ExtendedDFG();


    @Override
    public void observeEvent(String traceId, String activityName) {
        int currentIndex = 1;
        if (indexInCase.containsKey(traceId)) {
            currentIndex = indexInCase.get(traceId);
        }
        boolean firstOccurrance = true;
        if (observedActivitiesInTrace.containsKey(traceId)) {
            if (observedActivitiesInTrace.get(traceId).contains(activityName)) {
                firstOccurrance = false;
            } else {
                observedActivitiesInTrace.get(traceId).add(activityName);
            }
        } else {
            observedActivitiesInTrace.put(traceId, new HashSet<String>(Arrays.asList(activityName)));
        }

        ActivityDecoration activityDecoration = extendedDFG.addActivityIfNeeded(activityName);
        activityDecoration.addNewObservation(currentIndex, firstOccurrance);

        if (latestActivityInTrace.containsKey(traceId)) {
            String previousActivity = latestActivityInTrace.get(traceId);
            RelationDecoration relationDecoration = extendedDFG.addRelationIfNeeded(previousActivity, activityName);
            relationDecoration.addNewObservation();
        }
        latestActivityInTrace.put(traceId, activityName);
        indexInCase.put(traceId, currentIndex + 1);
    }

    @Override
    public ExtendedDFG getExtendedDFG() {
        return extendedDFG;
    }

    @Override
    public void saveLog(String filePath) {
        //Do nothing. Only for saving window
    }

    @Override
    public int getNumberEventsSaved() {

        return 0;
    }
}
