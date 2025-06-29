package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.*;

public class OSMCarAndBikeNetworkRunner {

	private static final String inputFile = "study_project/input/odeonsplatz-münchenerfreiheit-osm-map-01.pbf";
	private static final String outputFile = "study_project/input/network/car-bike-network-02.xml.gz";
	private static final CoordinateTransformation coordinateTransformation =
		TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:31468");

	public static void main(String[] args) {

		Set<String> bikeAllowedHighways = Set.of(
			"unclassified", "residential", "living_street"
		);

		SupersonicOsmNetworkReader reader = new SupersonicOsmNetworkReader.Builder()
			.setCoordinateTransformation(coordinateTransformation)
			.setAfterLinkCreated((link, tags, direction) -> {
				String highwayType = tags.get("highway");
				Set<String> allowedModes = new HashSet<>();
				allowedModes.add(TransportMode.car); // always allow car

				if (highwayType != null && bikeAllowedHighways.contains(highwayType)) {
					allowedModes.add("bike"); // allow bike for specific road types
				}

				link.setAllowedModes(allowedModes);
			})
			.build();

		Network network = reader.read(inputFile);

		// Remove all links that are not allowed for car or bike
		List<Id<Link>> linksToRemove = new ArrayList<>();
		for (Link link : network.getLinks().values()) {
			Set<String> allowedModes = link.getAllowedModes();
			if (allowedModes == null || Collections.disjoint(allowedModes, Set.of(TransportMode.car, "bike"))) {
				linksToRemove.add(link.getId());
			}
		}
		for (Id<Link> linkId : linksToRemove) {
			network.removeLink(linkId);
		}

		// Remove unused nodes
		Set<Id<Node>> usedNodeIds = new HashSet<>();
		for (Link link : network.getLinks().values()) {
			usedNodeIds.add(link.getFromNode().getId());
			usedNodeIds.add(link.getToNode().getId());
		}

		List<Id<Node>> nodesToRemove = new ArrayList<>();
		for (Node node : network.getNodes().values()) {
			if (!usedNodeIds.contains(node.getId())) {
				nodesToRemove.add(node.getId());
			}
		}
		for (Id<Node> nodeId : nodesToRemove) {
			network.removeNode(nodeId);
		}

		new NetworkWriter(network).write(outputFile);

		System.out.println("✅ Clean car-and-bike network created.");
		System.out.println("Nodes: " + network.getNodes().size());
		System.out.println("Links: " + network.getLinks().size());
	}
}
