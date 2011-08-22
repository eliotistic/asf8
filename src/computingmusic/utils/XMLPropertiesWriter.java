package computingmusic.utils;




import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLPropertiesWriter {

	private static Transformer transformer;
	private static DOMSource source;
	private static StreamResult result;
	
	
	public static void makeDefaultSource()
	{
		
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement("root");
			
			
			Element instrumentSpecs = document.createElement("instrumentSpecifications");
			rootElement.appendChild(instrumentSpecs);
			
		    Element em = document.createElement("allInstrumentStrings");
		    em.appendChild(document.createTextNode("55 62 69 76"));
		    instrumentSpecs.appendChild(em);
		   
		    
		    Element em2 = document.createElement("allOpen");
		    em2.appendChild(document.createTextNode("true true true true"));
		    instrumentSpecs.appendChild(em2);
		   
		    
		    Element em3 = document.createElement("allPlayable");
		    em3.appendChild(document.createTextNode("true true true true"));
		    instrumentSpecs.appendChild(em3); 

		    
		    Element em4 = document.createElement("allMinHeight");
		    em4.appendChild(document.createTextNode("0 0 0 0"));
		    instrumentSpecs.appendChild(em4);

		    
		    Element em5 = document.createElement("allMaxHeight");
		    em5.appendChild(document.createTextNode("25 25 25 25"));
		    instrumentSpecs.appendChild(em5);
		   
		    
		    
		    Element topoConstraints = document.createElement("topocst");
		    rootElement.appendChild(topoConstraints);
		    
		    Element em10 = document.createElement("triangelo");
		    em10.appendChild(document.createTextNode("true"));
		    topoConstraints.appendChild(em10);
		    
		    Element em11 = document.createElement("tribar");
		    em11.appendChild(document.createTextNode("true"));
		    topoConstraints.appendChild(em11); 

		    
		    Element em12 = document.createElement("diamond");
		    em12.appendChild(document.createTextNode("true"));
		    topoConstraints.appendChild(em12);
		    
			
		    
		    document.appendChild(rootElement);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformer = transformerFactory.newTransformer();
			source = new DOMSource(document);
			
			result =  new StreamResult("DefaultXMLFile.xml");
			
			//transformer.transform(source, result);
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void generateDefaultXMLFile()
	{
		makeDefaultSource();
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void generateXMLFileFromDefault()
	{
		makeDefaultSource();
		StreamResult result2 = new StreamResult("SFProperties.xml");
		try {
			transformer.transform(source, result2);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public static void main(String[] args)
	{
		
		generateDefaultXMLFile();
		generateXMLFileFromDefault();
	}
	
}
