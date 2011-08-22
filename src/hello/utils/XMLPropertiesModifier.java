package hello.utils;


import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLPropertiesModifier 
{
	
	public static void modifyXML()
	{
		
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse("SFProperties.xml");
			
			//doc.getElementById("element1").setTextContent("content1");
			
			doc.getElementsByTagName("element1").item(0).setTextContent("lapin");
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			
			Transformer transformer = transformerFactory.newTransformer();
			System.out.println("modifying");
			DOMSource source = new DOMSource(doc);
			StreamResult result =  new StreamResult("SFProperties.xml");
			
			transformer.transform(source, result);
			
			
			
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	
	public static void main(String [] args)
	{
		modifyXML();
	}
	
}
