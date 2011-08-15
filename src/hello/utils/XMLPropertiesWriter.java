package hello.utils;


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

	
	
	public static void generateDefaultXMLFile()
	{
		String root = "Root";
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement(root);
			document.appendChild(rootElement);
		
		    String element = "element1";
		    String data = "data1";
		    Element em = document.createElement(element);
		    em.appendChild(document.createTextNode(data));
		    rootElement.appendChild(em);
		   
		    String element2 = "element2";
		    String data2 = "data2";
		    Element em2 = document.createElement(element2);
		    em2.appendChild(document.createTextNode(data2));
		    rootElement.appendChild(em2);
		   
		    String element3 = "a";
		    String data3 = "aa";
		    Element em3 = document.createElement(element3);
		    em3.appendChild(document.createTextNode(data3));
		    rootElement.appendChild(em3); 

		    String element4 = "b";
		    String data4 = "bb";
		    Element em4 = document.createElement(element4);
		    em4.appendChild(document.createTextNode(data4));
		    rootElement.appendChild(em4);


		    String element5 = "c";
		    String data5 = "cc";
		    Element em5 = document.createElement(element5);
		    em5.appendChild(document.createTextNode(data5));
		    rootElement.appendChild(em5);
			   
			TransformerFactory transformerFactory = 
			TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result =  new StreamResult("DefaultXMLFile.xml");
			transformer.transform(source, result);
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	public static void main(String[] args)
	{
		generateDefaultXMLFile();
	}
	
}
