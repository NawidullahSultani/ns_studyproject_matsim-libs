package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class MergePTWithMultimodalNetwork {

	public static void main(String[] args) {
		String ptNetworkFile = "study_project/input/network/mapped-pt-network-01.xml.gz";
		String multimodalNetworkFile = "study_project/input/network/bike-walk-car-network-01.xml.gz";
		String outputMergedNetwork = "study_project/multimodal-input/multimodal-mapped-network-01.xml.gz";

		Config config = ConfigUtils.createConfig();

		// Load PT network
		Scenario ptScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(ptScenario.getNetwork()).readFile(ptNetworkFile);
		Network ptNetwork = ptScenario.getNetwork();

		// Load existing multimodal network
		Scenario multimodalScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(multimodalScenario.getNetwork()).readFile(multimodalNetworkFile);
		Network multimodalNetwork = multimodalScenario.getNetwork();

		Scenario mergedScenario = ScenarioUtils.createScenario(config);
		Network mergedNetwork = mergedScenario.getNetwork();
		NetworkFactory factory = mergedNetwork.getFactory();

		// Merge nodes (ensure no duplicates)
		// First add nodes from multimodal network (these might already have suffixes)
		for (Node node : multimodalNetwork.getNodes().values()) {
			mergedNetwork.addNode(factory.createNode(node.getId(), node.getCoord()));
		}
		// Then add nodes from PT network if they don't exist
		for (Node node : ptNetwork.getNodes().values()) {
			if (!mergedNetwork.getNodes().containsKey(node.getId())) {
				mergedNetwork.addNode(factory.createNode(node.getId(), node.getCoord()));
			}
		}

		// Add links from multimodal network (keep original IDs)
		for (Link link : multimodalNetwork.getLinks().values()) {
			Link newLink = factory.createLink(link.getId(),
				mergedNetwork.getNodes().get(link.getFromNode().getId()),
				mergedNetwork.getNodes().get(link.getToNode().getId()));
			copyLinkAttributes(link, newLink);
			mergedNetwork.addLink(newLink);
		}

		// Add PT links (add "_pt" suffix to ID)
		for (Link link : ptNetwork.getLinks().values()) {
			Id<Link> newId = Id.createLinkId(link.getId().toString());
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
