package StdPro;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;




import java.util.Set;

public class StdPro500_Runner {

    public static void main(String[] args) {

        //
        Config emptyConfig = ConfigUtils.createConfig();
		emptyConfig.global().setRandomSeed(4711);
        emptyConfig.global().setCoordinateSystem("EPSG:31468");

        emptyConfig.network().setInputFile("study_project/input/StdPro_PTmultimodalMapped500.xml");
		emptyConfig.plans().setInputFile("study_project/input/StdPro_Plan.xml");

		emptyConfig.transit().setUseTransit(true);
		emptyConfig.transit().setTransitScheduleFile("study_project/input/StdPro_ScheduleMapped.xml");
		emptyConfig.transit().setVehiclesFile("study_project/input/StdPro_Vehicle.xml");

		emptyConfig.transitRouter().setMaxBeelineWalkConnectionDistance(1000);
		emptyConfig.transitRouter().setSearchRadius(1000);
		emptyConfig.transitRouter().setExtensionRadius(1000);
		emptyConfig.transitRouter().setAdditionalTransferTime(0);

		emptyConfig.transit().setTransitModes(Set.of(TransportMode.pt, "subway"));
		emptyConfig.transit().setRoutingAlgorithmType(TransitConfigGroup.TransitRoutingAlgorithmType.SwissRailRaptor);

		emptyConfig.qsim().setMainModes(Set.of("car", "subway"));
		emptyConfig.routing().setNetworkModes(Set.of("car"));
		emptyConfig.qsim().setStartTime(0);
		emptyConfig.qsim().setEndTime(86400);
		emptyConfig.qsim().setSnapshotPeriod(0);

		// Configure the router to create routes during simulation
		emptyConfig.plans().setHandlingOfPlansWithoutRoutingMode(
			PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
		emptyConfig.routing().setAccessEgressType(
			RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);


        emptyConfig.scoring().setLearningRate(1);
        emptyConfig.scoring().setBrainExpBeta(2);
        emptyConfig.scoring().setLateArrival_utils_hr(-30);
        emptyConfig.scoring().setEarlyDeparture_utils_hr(0);
        emptyConfig.scoring().setPerforming_utils_hr(6);
        emptyConfig.scoring().setMarginalUtlOfWaiting_utils_hr(-6);
        emptyConfig.scoring().setMarginalUtlOfWaitingPt_utils_hr(-10);

		// Transit
		ScoringConfigGroup.ModeParams subwayScoring = new ScoringConfigGroup.ModeParams("subway");
		subwayScoring.setConstant(-1.0);  // Small penalty for using subway
		subwayScoring.setMarginalUtilityOfTraveling(-6.0);  // Better than walking
		emptyConfig.scoring().addModeParams(subwayScoring);




        //      defining activities
		ScoringConfigGroup.ActivityParams home = new ScoringConfigGroup.ActivityParams("HOME");
		home.setActivityType("HOME");
		home.setPriority(1);
		home.setTypicalDuration(12 * 60 * 60);
		emptyConfig.scoring().addActivityParams(home);

		ScoringConfigGroup.ActivityParams work = new ScoringConfigGroup.ActivityParams("WORK");
		work.setActivityType("WORK");
		work.setPriority(1);
		work.setTypicalDuration(3.8 * 60 * 60);
		emptyConfig.scoring().addActivityParams(work);

		ScoringConfigGroup.ActivityParams school = new ScoringConfigGroup.ActivityParams("SCHOOL");
		school.setActivityType("SCHOOL");
		school.setPriority(1);
		school.setTypicalDuration(5 * 60 * 60); // Assuming standard school duration
		emptyConfig.scoring().addActivityParams(school);

		ScoringConfigGroup.ActivityParams shop = new ScoringConfigGroup.ActivityParams("SHOP");
		shop.setActivityType("SHOP");
		shop.setPriority(0.5);
		shop.setTypicalDuration(0.36 * 60 * 60);
		emptyConfig.scoring().addActivityParams(shop);

		ScoringConfigGroup.ActivityParams pr = new ScoringConfigGroup.ActivityParams("P+R");
		pr.setActivityType("P+R");
		pr.setPriority(0.3); // Lower priority; short stop
		pr.setTypicalDuration(10 * 60); // 10 minutes
		emptyConfig.scoring().addActivityParams(pr);

		ScoringConfigGroup.ActivityParams university = new ScoringConfigGroup.ActivityParams("UNIVERSITY");
		university.setActivityType("UNIVERSITY");
		university.setPriority(1);
		university.setTypicalDuration(5 * 60 * 60); // Standard university day
		emptyConfig.scoring().addActivityParams(university);



// Replanning configuration
		emptyConfig.replanning().setMaxAgentPlanMemorySize(5);

		ReplanningConfigGroup.StrategySettings timeMutator = new ReplanningConfigGroup.StrategySettings();
		timeMutator.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
		timeMutator.setWeight(0.3);
		emptyConfig.replanning().addStrategySettings(timeMutator);
//
        emptyConfig.controller().setOutputDirectory("study_project/output");
        emptyConfig.controller().setFirstIteration(0);
        emptyConfig.controller().setLastIteration(3);
        emptyConfig.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        emptyConfig.qsim().setStorageCapFactor(1.0);
        emptyConfig.qsim().setFlowCapFactor(1.0);

        //Implementing teleportation modes for "walk" in router and scoring
        RoutingConfigGroup.TeleportedModeParams walk = new RoutingConfigGroup.TeleportedModeParams("walk");
        walk.setBeelineDistanceFactor(1.3);
        walk.setTeleportedModeSpeed(5/3.6);
        emptyConfig.routing().addTeleportedModeParams(walk);
//        //Implementing teleportation modes for "bike" in router and scoring
//		RoutingConfigGroup.TeleportedModeParams bike = new RoutingConfigGroup.TeleportedModeParams("bike");
//        bike.setBeelineDistanceFactor(1.3);
//        bike.setTeleportedModeSpeed(15/3.6);
//        emptyConfig.routing().addTeleportedModeParams(bike);


        Scenario myScenario = ScenarioUtils.loadScenario(emptyConfig);
        Controler myControler = new Controler(myScenario);
        myControler.run();
    }
}
