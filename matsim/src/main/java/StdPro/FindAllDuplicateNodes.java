package StdPro;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import java.io.File;
import java.util.*;

public class FindAllDuplicateNodes {
	public static void main(String[] args) throws Exception {
		String networkFile = "study_project/input/StdPro_PTmultimodalMapped500.xml";
		Map<String, List<String>> nodeMap = new HashMap<>(); // ID → List of coordinates

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		DefaultHandler handler = new DefaultHandler() {
			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) {
				if (qName.equalsIgnoreCase("node")) {
					String id = attributes.getValue("id");
					String x = attributes.getValue("x");
					String y = attributes.getValue("y");
					String coord = String.format("(%.2f, %.2f)", Double.parseDouble(x), Double.parseDouble(y));
					nodeMap.computeIfAbsent(id, k -> new ArrayList<>()).add(coord);
				}
			}
		};

		saxParser.parse(new File(networkFile), handler);

		// Filter and print duplicates
		System.out.println("=== Duplicate Nodes Report ===");
		nodeMap.entrySet().stream()
			.filter(entry -> entry.getValue().size() > 1)
			.forEach(entry -> {
				System.out.printf("\n🚨 Node ID: %s (count: %d)\n", entry.getKey(), entry.getValue().size());
				entry.getValue().forEach(coord -> System.out.println("  - " + coord));
			});

		System.out.println("\n✅ Total duplicate nodes: " +
			nodeMap.values().stream().filter(v -> v.size() > 1).count());
	}
}
