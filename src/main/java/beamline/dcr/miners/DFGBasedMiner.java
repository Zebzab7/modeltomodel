package beamline.dcr.miners;

import java.util.*;

import beamline.core.miner.AbstractMiner;
import beamline.core.web.annotations.ExposedMiner;
import beamline.core.web.annotations.ExposedMinerParameter;
import beamline.core.web.miner.models.MinerParameter;
import beamline.core.web.miner.models.MinerParameterValue;
import beamline.core.web.miner.models.MinerView;
import beamline.core.web.miner.models.MinerViewRaw;
import beamline.core.web.miner.models.MinerView.Type;
import beamline.dcr.annotations.ExposedDcrPattern;
import beamline.dcr.model.*;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.model.relations.DcrModel.RELATION;
import beamline.dcr.model.relations.dfg.ExtendedDFG;
import beamline.dcr.model.patterns.RelationPattern;
import beamline.dcr.model.relations.UnionRelationSet;
import beamline.dcr.model.streamminers.SlidingWindowStreamMiner;
import beamline.dcr.model.streamminers.StreamMiner;
import beamline.dcr.model.streamminers.UnlimitedStreamMiner;
import beamline.dcr.view.DcrModelText;
import beamline.dcr.view.DcrModelView;
import beamline.dcr.view.DcrModelXML;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.reflections.Reflections;

import javax.xml.transform.TransformerException;


@ExposedMiner(
        name = "DFG based DCR miner",
        description = "This miner discovers a DCR model form a DFG",
        //TODO: Get all RELATION values in defaultValue
        configurationParameters = {@ExposedMinerParameter(name = "DCR Patterns",
                type = MinerParameter.Type.CHOICE, defaultValue = "Condition;Response;Exclude;Include"),
                @ExposedMinerParameter(name = "Stream Miner",
                        type = MinerParameter.Type.CHOICE, defaultValue = "Sliding Window;Infinite Memory"),
                @ExposedMinerParameter(name = "Trace Window Size",
                        type = MinerParameter.Type.INTEGER, defaultValue = "10"),
                @ExposedMinerParameter(name = "Max traces",
                        type = MinerParameter.Type.INTEGER, defaultValue = "20"),
                @ExposedMinerParameter(name = "Transitive Reduction",
                        type = MinerParameter.Type.CHOICE, defaultValue = "Condition;Response"),
                @ExposedMinerParameter(name = "Relations Threshold",
                        type = MinerParameter.Type.INTEGER, defaultValue = "0")
        },
        viewParameters = {}
)
public class DFGBasedMiner extends AbstractMiner {
    //Not configured with XML download to beamline
    //XML view only works locally from testrunners


    private Reflections reflections;
    private Set<Class<?>> dcrPatternClasses;

    private StreamMiner streamMiner;
    private UnionRelationSet unionRelationSet;
    private Integer relationsThreshold;
    private String[] transReductionList;


    private String[] dcrPatternList;
    private String[] dcrConstraintList;
    private Set<String> postorderTraversal;

    public DFGBasedMiner(){
        this.reflections = new Reflections("beamline");
        this.dcrPatternClasses = reflections.getTypesAnnotatedWith(ExposedDcrPattern.class);
    }
    @Override
    public void configure(Collection<MinerParameterValue> collection) {
        String streamMiningType = "";
        Integer traceMax = null;
        Integer maxTraces = null;
        for(MinerParameterValue v : collection) {
            switch (v.getName()) {
                case "DCR Patterns":
                    this.dcrPatternList = (String[]) v.getValue();
                    break;
                case "Stream Miner":
                    streamMiningType = (String) v.getValue();
                    break;
                case "Trace Window Size":
                    traceMax = (Integer) v.getValue();
                    break;
                case "Max Traces":
                    maxTraces = (Integer) v.getValue();
                    break;
                case "Transitive Reduction":
                    this.transReductionList = (String[]) v.getValue();
                    break;
                case "Relations Threshold":
                    this.relationsThreshold = (Integer) v.getValue();
                    break;
                case "DCR Constraints":
                    this.dcrConstraintList = (String[]) v.getValue();
                    break;
            }

        }
        switch (streamMiningType){

            case "Sliding Window":
                this.streamMiner = new SlidingWindowStreamMiner(traceMax,maxTraces);
                break;
            default:
                this.streamMiner = new UnlimitedStreamMiner();
        }
    }

    @Override
    public void consumeEvent(String caseID, String activityName) {
        this.streamMiner.observeEvent(caseID,activityName);

    }

    @Override
    public List<MinerView> getViews(Collection<MinerParameterValue> collection) {
        List<MinerView> views = new ArrayList<>();
        ExtendedDFG extendedDFG = streamMiner.getExtendedDFG();
        views.add(new MinerViewRaw("DFG", extendedDFG.toString()));
        DcrModel dcrModelConverted = getDcrModel();
        if (dcrModelConverted != null){
            views.add(new MinerView("Textural", new DcrModelText(dcrModelConverted).toString(), Type.RAW));

            views.add(new MinerView("DCR", new DcrModelView(dcrModelConverted).toString(), Type.GRAPHVIZ));
            //miner parameter from local testrun to save XML
            for(MinerParameterValue v : collection) {
                if (v.getName().equals("filename")) {
                    new DcrModelXML(dcrModelConverted).toFile(v.getValue().toString());
                }
            }
        }

        return views;
    }


    public DcrModel convert(ExtendedDFG dfg) throws IllegalAccessException, InstantiationException {
        DcrModel model = new DcrModel();
        this.postorderTraversal= new LinkedHashSet<>();
        

        this.unionRelationSet = new UnionRelationSet(dfg,relationsThreshold);

        for (String originalPattern : dcrPatternList){

            minePatternsFromPostOrderDependencies(originalPattern);
        }

        TransitiveReduction transitiveReduction = new TransitiveReduction();

        for (String transReduce : transReductionList){
            RELATION enumPattern = RELATION.valueOf(transReduce.toUpperCase());
            transitiveReduction.reduce(unionRelationSet,enumPattern);
        }

        model.addActivities(dfg.getActivities());
        //project user selected patterns to DCR Model
        for (String dcrConstraint : dcrConstraintList){
            RELATION enumConstraint = RELATION.valueOf(dcrConstraint.toUpperCase());
            Set<Triple<String, String, RELATION>> minedConstraints = unionRelationSet.getDcrRelationWithConstraint(enumConstraint);
            model.addRelations(minedConstraints);
        }
        return model;

    }
    private void minePatternsFromPostOrderDependencies(String root) throws IllegalAccessException, InstantiationException {
        int currentRootIndex = 0;
        Stack<Pair> stack = new Stack<>();

        while (root != null || !stack.isEmpty()) {
            if (root != null) {

                stack.push(Pair.of(root, currentRootIndex));
                currentRootIndex = 0;

                String[] dcrDependencies = getDcrDependencies(root);
                if (dcrDependencies.length>=1) {
                    root = dcrDependencies[0];
                }
                else {
                    root = null;
                }
                continue;
            }

            Pair temp = stack.pop();

            if (!postorderTraversal.contains(temp.getLeft().toString())){
                minePattern(temp.getLeft().toString());
                postorderTraversal.add(temp.getLeft().toString());

            }

            while (!stack.isEmpty() && (int) temp.getRight() ==
                    getDcrDependencies(stack.peek().getLeft().toString()).length - 1){
                temp = stack.pop();
                if (!postorderTraversal.contains(temp.getLeft().toString())){
                    minePattern(temp.getLeft().toString());
                    postorderTraversal.add(temp.getLeft().toString());

                }
            }

            if (!stack.isEmpty()) {
                String[] dependencies = getDcrDependencies(stack.peek().getLeft().toString());
                root = dependencies[
                        (int) temp.getRight() + 1];
                currentRootIndex = (int) temp.getRight() + 1;
            }
        }
    }
    private String[] getDcrDependencies(String dcr){
        return getExposedPatternClass(dcr).getAnnotation(ExposedDcrPattern.class).dependencies();
    }
    private RelationPattern getPatternMinerClass (String patternName) throws IllegalAccessException, InstantiationException {
        return (RelationPattern) getExposedPatternClass(patternName).newInstance();
    }
    private Class<?> getExposedPatternClass(String patternName){
        for (Class<?> exposedPatternClass : dcrPatternClasses) {
            ExposedDcrPattern exposedPattern = exposedPatternClass.getAnnotation(ExposedDcrPattern.class);
            if (exposedPattern.name().equals(patternName)) {
                return exposedPatternClass;
            }
        }
        return null;

    }
    private void minePattern(String patternName) throws InstantiationException, IllegalAccessException {
        RelationPattern patternToMine = getPatternMinerClass(patternName);
        patternToMine.populateConstraint(unionRelationSet);

    }

    public DcrModel getDcrModel(){

        try {
            return convert(streamMiner.getExtendedDFG());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }
    //For testsoftware

    public ExtendedDFG getExtendedDFG() {
        return streamMiner.getExtendedDFG();
    }
    public void saveCurrentWindowLog(String filePath) throws TransformerException {
        this.streamMiner.saveLog(filePath);
    }
    public int getNumberEventsInWindow(){
        return this.streamMiner.getNumberEventsSaved();
    }
    public UnionRelationSet getUnionRelationSet(){
        return unionRelationSet;
    }

}
