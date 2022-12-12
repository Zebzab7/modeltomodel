package beamline.dcr.testsoftware.testrunners;

import java.util.*;

import beamline.core.miner.AbstractMiner;
import beamline.core.web.annotations.ExposedMiner;
import beamline.core.web.miner.models.MinerParameterValue;
import beamline.core.web.miner.models.MinerView;
import beamline.core.web.miner.models.MinerView.Type;
import beamline.dcr.model.relations.DcrModel;
import beamline.dcr.view.DcrModelView;
import org.apache.commons.lang3.tuple.Triple;

@ExposedMiner(
	name = "DCR visualization tester",
	description = "This miner discovers a DCR model",
	configurationParameters = { },
	viewParameters = { }
)
public class DcrVisualizerTester extends AbstractMiner {

	@Override
	public void configure(Collection<MinerParameterValue> collection) {

	}

	@Override
	public void consumeEvent(String caseID, String activityName) {
		
	}

	@Override
	public List<MinerView> getViews(Collection<MinerParameterValue> collection) {
		List<MinerView> views = new ArrayList<>();
		views.add(new MinerView("Graphical ", new DcrModelView(getModel()).toString(), Type.GRAPHVIZ));
		return views;
	}

	public DcrModel getModel() {
		DcrModel model = new DcrModel();
		Set<Triple<String, String, DcrModel.RELATION>> dcrRelations = new HashSet<>();

		dcrRelations.add(Triple.of("Start", "Activity 3", DcrModel.RELATION.CONDITION));
		dcrRelations.add(Triple.of("Start", "Activity 2", DcrModel.RELATION.RESPONSE));
		dcrRelations.add(Triple.of("Activity 3", "Activity 2", DcrModel.RELATION.RESPONSE));
		dcrRelations.add(Triple.of("Activity 2", "Activity 4", DcrModel.RELATION.INCLUDE));
		dcrRelations.add(Triple.of("Activity 4", "Activity 4", DcrModel.RELATION.EXCLUDE));
		dcrRelations.add(Triple.of("Start", "Activity 4", DcrModel.RELATION.EXCLUDE));
		dcrRelations.add(Triple.of("Activity 4", "Activity 5", DcrModel.RELATION.SPAWN));
		dcrRelations.add(Triple.of("Activity 4", "Activity 6", DcrModel.RELATION.MILESTONE));

		dcrRelations.add(Triple.of("D: Medical examination", "B: Prescribe medicine", DcrModel.RELATION.RESPONSE));
		dcrRelations.add(Triple.of("C: Further examination needed", "A: 2nd medical examination", DcrModel.RELATION.RESPONSE));
		dcrRelations.add(Triple.of("C: Further examination needed", "A: 2nd medical examination", DcrModel.RELATION.CONDITION));
		dcrRelations.add(Triple.of("D: Medical examination", "B: Prescribe medicine", DcrModel.RELATION.CONDITION));
		dcrRelations.add(Triple.of("D: Medical examination", "C: Further examination needed", DcrModel.RELATION.CONDITION));
		dcrRelations.add(Triple.of("A: 2nd medical examination", "B: Prescribe medicine", DcrModel.RELATION.MILESTONE));

		model.addRelations(dcrRelations);
		return model;
	}
}
