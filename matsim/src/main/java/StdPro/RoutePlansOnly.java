package StdPro;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;

import java.util.Set;

public class RoutePlansOnly {

	public static void main(String[] args) {

		// === Load your config ===
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:31468");
		config.global().setRandomSeed(4711);

		config.network().setInputFile("study_project/input/500mOdeonsplatzMünchnerFreiheit.xml");
		config.plans().setInputFile("study_project/input/StdPro_Plan.xml");

		config.qsim().setStartTime(0);
		config.qsim().setEndTime(86400);
		config.qsim().setSnapshotPeriod(0);

		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(3);  // Just route, no simulation
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory("study_project/output_routed");

		// === Make sure routing can happen ===
		config.plans().setHandlingOfPlansWithoutRoutingMode(
			PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
		config.routing().setAccessEgressType(
			org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		// Routing only needs one mode for now
		config.qsim().setMainModes(Set.of("car"));
		config.routing().setNetworkModes(Set.of("car"));


		config.scoring().addActivityParams(
			new ScoringConfigGroup.ActivityParams("HOME")
				.setTypicalDuration(12 * 3600)
				.setMinimalDuration(6 * 3600)
		);

		config.scoring().addActivityParams(
			new ScoringConfigGroup.ActivityParams("WORK")
				.setTypicalDuration(8 * 3600)
				.setOpeningTime(6 * 3600)
				.setClosingTime(20 * 3600)
				.setMinimalDuration(4 * 3600)
		);

		// Replanning configuration
		config.replanning().setMaxAgentPlanMemorySize(5);

// Add strategies
		ReplanningConfigGroup.StrategySettings timeMutator = new ReplanningConfigGroup.StrategySettings();
		timeMutator.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
		timeMutator.setWeight(0.3);
		config.replanning().addStrategySettings(timeMutator);

		// === Load and run for routing only ===
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();
	}
}
