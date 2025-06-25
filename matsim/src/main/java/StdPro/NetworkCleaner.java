package StdPro;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

public class NetworkCleaner {
	public static void main(String[] args) {
		// Input and output file paths
		String inputNetwork = "study_project/input/clean_network/mapped-PT-network.xml.gz";
		String outputNetwork = "study_project/input/clean_network/mapped-cleaned-PT-network.xml.gz";

		// Read the network
		Network network = NetworkUtils.readNetwork(inputNetwork);

		// Clean each link
		for (Link link : network.getLinks().values()) {
			// Remove specific OSM attributes
			link.getAttributes().removeAttribute("osm:relation:route");
			link.getAttributes().removeAttribute("osm:way:railway");
			link.getAttributes().removeAttribute("osm:way:tunnel");

		}

		// Write the cleaned network
		new NetworkWriter(network).write(outputNetwork);
		System.out.println("Network cleaned and saved to: " + outputNetwork);
	}
}
