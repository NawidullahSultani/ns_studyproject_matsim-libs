package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class BWCMergeNetworksWithSuffix {

	public static void main(String[] args) {
		String carNetworkFile = "study_project/input/network/car-bike-network.xml.gz";
		String walkBikeNetworkFile = "study_project/input/network/bike-walk-network.xml.gz";
		String walkOnlyNetworkFile = "study_project/input/network/walk-only-network.xml.gz"; // new walk-only network file
		String outputMergedNetwork = "study_project/input/network/bike-walk-car-network-01.xml.gz";

		Config config = ConfigUtils.createConfig();

		// Load car network
		Scenario carScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(carScenario.getNetwork()).readFile(carNetworkFile);
		Network carNetwork = carScenario.getNetwork();

		// Load walk-bike network
		Scenario walkBikeScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(walkBikeScenario.getNetwork()).readFile(walkBikeNetworkFile);
		Network walkBikeNetwork = walkBikeScenario.getNetwork();

		// Load walk-only network
		Scenario walkOnlyScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(walkOnlyScenario.getNetwork()).readFile(walkOnlyNetworkFile);
		Network walkOnlyNetwork = walkOnlyScenario.getNetwork();

		Scenario mergedScenario = ScenarioUtils.createScenario(config);
		Network mergedNetwork = mergedScenario.getNetwork();
		NetworkFactory factory = mergedNetwork.getFactory();

		// Merge nodes (ensure no duplicates)
		for (Node node : carNetwork.getNodes().values()) {
			mergedNetwork.addNode(factory.createNode(node.getId(), node.getCoord()));
		}
		for (Node node : walkBikeNetwork.getNodes().values()) {
			if (!mergedNetwork.getNodes().containsKey(node.getId())) {
				mergedNetwork.addNode(factory.createNode(node.getId(), node.getCoord()));
			}
		}
		for (Node node : walkOnlyNetwork.getNodes().values()) {
			if (!mergedNetwork.getNodes().containsKey(node.getId())) {
				mergedNetwork.addNode(factory.createNode(node.getId(), node.getCoord()));
			}
		}

		// Add car links (add "_car" suffix to ID)
		for (Link link : carNetwork.getLinks().values()) {
			Id<Link> newId = Id.createLinkId(link.getId().toString() + "_car");
			Link newLink = factory.createLink(newId,
				mergedNetwork.getNodes().get(link.getFromNode().getId()),
				mergedNetwork.getNodes().get(link.getToNode().getId()));
			copyLinkAttributes(link, newLink);
			mergedNetwork.addLink(newLink);
		}

		// Add walk/bike links (add "_walkbike" suffix to ID)
		for (Link link : walkBikeNetwork.getLinks().values()) {
			Id<Link> newId = Id.createLinkId(link.getId().toString() + "_walkbike");
			Link newLink = factory.createLink(newId,
				mergedNetwork.getNodes().get(link.getFromNode().getId()),
				mergedNetwork.getNodes().get(link.getToNode().getId()));
			copyLinkAttributes(link, newLink);
			mergedNetwork.addLink(newLink);
		}

		// Add walk-only links (add "_walkonly" suffix to ID)
		for (Link link : walkOnlyNetwork.getLinks().values()) {
			Id<Link> newId = Id.createLinkId(link.getId().toString() + "_walkonly");
			Link newLink = factory.createLink(newId,
				mergedNetwork.getNodes().get(link.getFromNode().getId()),
				mergedNetwork.getNodes().get(link.getToNode().getId()));
			copyLinkAttributes(link, newLink);
			mergedNetwork.addLink(newLink);
		}

		// Write merged network
		new NetworkWriter(mergedNetwork).write(outputMergedNetwork);
		System.out.println("✅ Merged network written to " + outputMergedNetwork);
		System.out.println("   Nodes: " + mergedNetwork.getNodes().size());
		System.out.println("   Links: " + mergedNetwork.getLinks().size());
	}

	private static void copyLinkAttributes(Link source, Link target) {
		target.setLength(source.getLength());
		target.setFreespeed(source.getFreespeed());
		target.setCapacity(source.getCapacity());
		target.setNumberOfLanes(source.getNumberOfLanes());
		target.setAllowedModes(source.getAllowedModes());
	}
}
