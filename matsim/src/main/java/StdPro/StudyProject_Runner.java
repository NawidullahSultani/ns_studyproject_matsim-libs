package StdPro;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.ReRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;
import java.util.Set;

public class StudyProject_Runner {

	public static void main(String[] args) {
		// 1. BASIC CONFIGURATION
		Config config = ConfigUtils.createConfig();
		config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("EPSG:31468");

		// 2. INPUT FILES
		config.network().setInputFile("study_project/multimodal-input/multimodal-mapped-network-01.xml.gz");
		config.plans().setInputFile("study_project/multimodal-input/synthetic-plan-01.xml");

		// 3. TRANSIT CONFIGURATION
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile("study_project/multimodal-input/pt-mapped-schedule-01.xml");
		config.transit().setVehiclesFile("study_project/multimodal-input/pt-vehicle-01.xml");
		config.transit().setTransitModes(Set.of(TransportMode.pt, "subway"));
		config.transit().setRoutingAlgorithmType(TransitConfigGroup.TransitRoutingAlgorithmType.SwissRailRaptor);

		// 4. TRANSIT ROUTER SETTINGS
		config.transitRouter().setMaxBeelineWalkConnectionDistance(1000);
		config.transitRouter().setSearchRadius(1000);
		config.transitRouter().setExtensionRadius(1000);
		config.transitRouter().setAdditionalTransferTime(0);

		// 5. NETWORK AND SIMULATION MODES
		config.routing().setNetworkModes(Set.of("car", "walk", "bike", "non_network_walk"));
		config.qsim().setMainModes(Set.of("car", "subway", "walk", "bike", "non_network_walk"));
		config.routing().clearTeleportedModeParams();
		config.routing().setRoutingRandomness(0.0);
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.routing().addModeRoutingParams(
			new RoutingConfigGroup.ModeRoutingParams("non_network_walk")
				.setBeelineDistanceFactor(1.3)
				.setTeleportedModeSpeed(1.34));

		RoutingConfigGroup.TeleportedModeParams transitWalk = new RoutingConfigGroup.TeleportedModeParams("transit_walk");
		transitWalk.setBeelineDistanceFactor(1.3);
		transitWalk.setTeleportedModeSpeed(5.0 / 3.6);
		config.routing().addTeleportedModeParams(transitWalk);


		// 6. SIMULATION TIME SETTINGS
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(86400);
		config.qsim().setSnapshotPeriod(0);
		config.qsim().setStorageCapFactor(1.0);
		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setUsePersonIdForMissingVehicleId(false);

		// 7. ROUTING CONFIGURATION
		config.plans().setHandlingOfPlansWithoutRoutingMode(
			PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
		config.routing().setAccessEgressType(
			RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		// 8. SCORING PARAMETERS
		config.scoring().setLearningRate(1);
		config.scoring().setBrainExpBeta(2);
		config.scoring().setLateArrival_utils_hr(-30);
		config.scoring().setEarlyDeparture_utils_hr(0);
		config.scoring().setPerforming_utils_hr(6);
		config.scoring().setMarginalUtlOfWaiting_utils_hr(0);
		config.scoring().setMarginalUtlOfWaitingPt_utils_hr(-10);

		// Mode-specific scoring
		ScoringConfigGroup.ModeParams subwayScoring = new ScoringConfigGroup.ModeParams("subway");
		subwayScoring.setConstant(-1.0);
		subwayScoring.setMarginalUtilityOfTraveling(-6.0);
		config.scoring().addModeParams(subwayScoring);

		// 9. ACTIVITY PARAMETERS
		ScoringConfigGroup.ActivityParams home = new ScoringConfigGroup.ActivityParams("HOME");
		home.setActivityType("HOME");
		home.setPriority(1);
		home.setTypicalDuration(12 * 60 * 60);
		config.scoring().addActivityParams(home);

		ScoringConfigGroup.ActivityParams work = new ScoringConfigGroup.ActivityParams("WORK");
		work.setActivityType("WORK");
		work.setPriority(1);
		work.setTypicalDuration(3.8 * 60 * 60);
		config.scoring().addActivityParams(work);

		ScoringConfigGroup.ActivityParams school = new ScoringConfigGroup.ActivityParams("SCHOOL");
		school.setActivityType("SCHOOL");
		school.setPriority(1);
		school.setTypicalDuration(5 * 60 * 60); // Assuming standard school duration
		config.scoring().addActivityParams(school);

		ScoringConfigGroup.ActivityParams shop = new ScoringConfigGroup.ActivityParams("SHOP");
		shop.setActivityType("SHOP");
		shop.setPriority(0.5);
		shop.setTypicalDuration(0.36 * 60 * 60);
		config.scoring().addActivityParams(shop);

		ScoringConfigGroup.ActivityParams pr = new ScoringConfigGroup.ActivityParams("P+R");
		pr.setActivityType("P+R");
		pr.setPriority(0.3); // Lower priority; short stop
		pr.setTypicalDuration(10 * 60); // 10 minutes
		config.scoring().addActivityParams(pr);

		ScoringConfigGroup.ActivityParams university = new ScoringConfigGroup.ActivityParams("UNIVERSITY");
		university.setActivityType("UNIVERSITY");
		university.setPriority(1);
		university.setTypicalDuration(5 * 60 * 60); // Standard university day
		config.scoring().addActivityParams(university);

		// 10. REPLANNING STRATEGIES
		config.replanning().setMaxAgentPlanMemorySize(5);
		ReplanningConfigGroup.StrategySettings timeMutator = new ReplanningConfigGroup.StrategySettings();
		timeMutator.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
		timeMutator.setWeight(0.3);
		config.replanning().addStrategySettings(timeMutator);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
				.setWeight(0.1)
		);

		// 11. OUTPUT CONTROL
		config.controller().setOutputDirectory("study_project/output-01");
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(3);
		config.controller().setOverwriteFileSetting(
			OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		// 12. SCENARIO AND CONTROLER SETUP
		Scenario scenario = ScenarioUtils.loadScenario(config);
		// route factory
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(NetworkRoute.class, new LinkNetworkRouteFactory());

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("ReRoute").toProvider(new ReRoute());
			}
		});

		controler.run();
	}
}
