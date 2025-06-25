package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.*;

public class OSMBikeWalkNetworkReader {

	public static void main(String[] args) {
		String osmInput = "study_project/input/mapForP2MATSim_01.pbf";
		String networkOutput = "study_project/input/network/bike-walk-network.xml.gz";

		CoordinateTransformation transformation =
			TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, "EPSG:31468");

		// Build the reader and allow bike + walk modes
		OsmBicycleReader reader = new OsmBicycleReader.Builder()
			.setCoordinateTransformation(transformation)
			.setStoreOriginalGeometry(true)
			.setAfterLinkCreated((link, tags, direction) -> {
				// Set bike and walk only
				Set<String> allowedModes = new HashSet<>();
				allowedModes.add(TransportMode.bike);
				allowedModes.add(TransportMode.walk);
				link.setAllowedModes(allowedModes);
			})
			.build();

		// Step 1: Read network
		Network network = reader.read(osmInput);

		// Step 2: Remove links with modes other than bike or walk
		List<Id<Link>> linksToRemove = new ArrayList<>();
		for (Link link : network.getLinks().values()) {
			Set<String> modes = link.getAllowedModes();
			if (modes == null || modes.isEmpty() || !containsOnlyBikeAndWalk(modes)) {
				linksToRemove.add(link.getId());
			}
		}
		for (Id<Link> linkId : linksToRemove) {
			network.removeLink(linkId);
		}

		// Step 3: Remove unused (orphan) nodes
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

		// Step 4: Write output
		new NetworkWriter(network).write(networkOutput);

		System.out.println("✅ Bicycle+Walk network created:");
		System.out.println("Nodes: " + network.getNodes().size());
		System.out.println("Links: " + network.getLinks().size());
	}

	// Helper to check only walk and bike modes are present
	private static boolean containsOnlyBikeAndWalk(Set<String> modes) {
		for (String mode : modes) {
			if (!mode.equals(TransportMode.walk) && !mode.equals(TransportMode.bike)) {
				return false;
			}
		}
		return true;
	}

	// These aren't used in main, but preserved for reference
	private static boolean isDedicatedCycleway(Map<String, String> tags) {
		return tags.containsKey(OsmTags.CYCLEWAY) &&
			("track".equals(tags.get(OsmTags.CYCLEWAY)) ||
				"lane".equals(tags.get(OsmTags.CYCLEWAY)));
	}

	private static boolean isMajorRoad(String highwayType) {
		return highwayType != null && (
			highwayType.equals(OsmTags.MOTORWAY) ||
				highwayType.equals(OsmTags.MOTORWAY_LINK) ||
				highwayType.equals(OsmTags.TRUNK) ||
				highwayType.equals(OsmTags.TRUNK_LINK) ||
				highwayType.equals(OsmTags.PRIMARY) ||
				highwayType.equals(OsmTags.PRIMARY_LINK) ||
				highwayType.equals(OsmTags.SECONDARY) ||
				highwayType.equals(OsmTags.SECONDARY_LINK));
	}
}
