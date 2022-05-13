package xml;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLParser {
	
	
	private static final String CONFIG_FILE_NAME = "appdata/config.xml";
	
	// Lists
	private static final String ADLIST_TAG = "adlist";
	private static final String URL_ATTRIBUTE_TAG = "url";
	private static final String LIST_TAG = "list";
	
	private static void saveDummyDocument(File file) {
		try {
			PrintWriter writer = new PrintWriter(file);
			writer.write("""
					<?xml version="1.0" encoding="UTF-8" standalone="no"?>
					<root>
					  <adlist url="">
					    <list><!--Put here in which list this Ad-list should be included--></list>
					    <list><!--The Ad-list can be included in multiple lists--></list>
					  </adlist>
					</root>""");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Will get the Config.xml or, if not present, create a dummy one and exit
	 *
	 * @return The Document
	 */
	private static Document getDocument() {
		try {
			File inputFile = new File(CONFIG_FILE_NAME);
			if (inputFile.createNewFile()) {
				saveDummyDocument(inputFile);
				System.err.println("There was no " + CONFIG_FILE_NAME
								   + " available. Created a dummy one. Please fill it out");
				System.exit(1);
			}
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile);
		} catch (ParserConfigurationException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (SAXException e) {
			xmlFormatException("something went wrong while parsing the xml");
		}
		return null; // will never get there
	}
	
	/**
	 * Will get AdLists
	 *
	 * @return The AdLists
	 */
	public static Map<String, List<String>> getAdList() {
		NodeList adlist_list = getDocument().getElementsByTagName(ADLIST_TAG);
		Map<String, List<String>> output = new HashMap<>();
		for (int i = 0; i < adlist_list.getLength(); i++) {
			try {
				Element adlist = (Element) adlist_list.item(i);
				String url = adlist.getAttribute(URL_ATTRIBUTE_TAG);
				List<String> lists = new ArrayList<>();
				NodeList list_list = adlist.getElementsByTagName(LIST_TAG);
				for (int j = 0; j < list_list.getLength(); j++) {
					Element countdown = (Element) list_list.item(j);
					lists.add(readTextElement(countdown));
				}
				output.put(url, lists);
			} catch (NullPointerException e) {
				xmlFormatException("Tag missing in AdList");
			}
		}
		return output;
	}
	
	/**
	 * Will trim all '\n' and ' ' at the beginning and end of the Text Element
	 *
	 * @param node The Node were the Text Element is
	 *
	 * @return A String striped of it's unnecessary '\n' and ' '
	 */
	private static String readTextElement(Node node) {
		String text = node.getTextContent();
		if (text == null || text.equals(""))
			return "";
		while (text.charAt(0) == '\n' || text.charAt(0) == ' ') {
			text = text.substring(1);
		}
		while (text.charAt(text.length() - 1) == '\n' || text.charAt(text.length() - 1) == ' ') {
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}
	
	/**
	 * Will output a Error Log and throw a Runtime Exception
	 *
	 * @param reason The Message that should be in the Log
	 */
	private static void xmlFormatException(String reason) {
		System.err.println("XML was wrongly formatted: " + reason);
		throw new RuntimeException("XML was wrongly formatted: " + reason);
	}
	
}
