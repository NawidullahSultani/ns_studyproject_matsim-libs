package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.*;

public class OSMWalkOnlyNetworkReader {

	public static void main(String[] args) {
		String osmInput = "study_project/input/odeonsplatz-münchenerfreiheit-osm-map-01.pbf";
		String networkOutput = "study_project/input/network/walk-only-network-02.xml.gz";

		CoordinateTransformation transformation =
			TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, "EPSG:31468");

		Set<String> walkableHighways = Set.of(
			"primary", "primary_link",
			"secondary", "secondary_link",
			"tertiary", "tertiary_link",
			"unclassified", "residential"
		);

		OsmBicycleReader reader = new OsmBicycleReader.Builder()
			.setCoordinateTransformation(transformation)
			.setStoreOriginalGeometry(true)
			.setAfterLinkCreated((link, tags, direction) -> {
				String highway = tags.get("highway");
				if (highway != null && walkableHighways.contains(highway)) {
					// Allow only walking
					Set<String> allowedModes = Set.of(TransportMode.walk);
					link.setAllowedModes(allowedModes);

					// Set realistic pedestrian link attributes
					link.setFreespeed(1.4);         // average walking speed in m/s (~5 km/h)
					link.setCapacity(1800);         // people per hour
					link.setNumberOfLanes(1.0);     // conceptually 1 lane for walking
				} else {
					link.setAllowedModes(Set.of()); // not walkable, will be removed
				}
			})
			.build();

		Network network = reader.read(osmInput);

		// Step 1: Remove non-walkable links
		List<Id<Link>> linksToRemove = new ArrayList<>();
		for (Link link : network.getLinks().values()) {
			Set<String> allowedModes = link.getAllowedModes();
			if (allowedModes == null || !allowedModes.contains(TransportMode.walk)) {
				linksToRemove.add(link.getId());
			}
		}
		for (Id<Link> linkId : linksToRemove) {
			network.removeLink(linkId);
		}

		// Step 2: Remove unused nodes
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

		new NetworkWriter(network).write(networkOutput);

		System.out.println("✅ Walk-only network created.");
		System.out.println("Nodes: " + network.getNodes().size());
		System.out.println("Links: " + network.getLinks().size());
	}
}
