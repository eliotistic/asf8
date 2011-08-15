package hello.utils;

import hello.Constraints;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

public class XMLPropertiesReader {

	 public static void main(String argv[]) {
		 
		 try 
		 {
			 SAXParserFactory factory = SAXParserFactory.newInstance();
		     SAXParser saxParser = factory.newSAXParser();
		     //final ArrayList<Integer> intList = new ArrayList<Integer>();
		     final int numChords;
		     final int maxHeight;
		     final int minHeight;
		     final int salary;
		     
		     DefaultHandler handler = new DefaultHandler() {
		 
			     boolean bnumchord = false;
			     boolean bminh = false;
			     boolean bmaxh = false;
			     boolean bsalary = false;
			     boolean ballow = false;
			     
			     boolean bbar = false;
			     boolean btrilo = false;
			     boolean bdiamond = false;
			     boolean binstrStrings = false;
			     //ArrayList<Integer> intList = new ArrayList<Integer>();
			 
			     public void startElement(String uri, String localName,
			        String qName, Attributes attributes)
			        throws SAXException 
			     {
			 
			        System.out.println("Start Element :" + qName);
			 
			        if (qName.equalsIgnoreCase("numchords")) {
			           bnumchord = true;
			        }
			 
			        if (qName.equalsIgnoreCase("minheight")) {
			           bminh = true;
			        }
			 
			        if (qName.equalsIgnoreCase("maxheight")) {
			           bmaxh = true;
			        }
			 
			        if (qName.equalsIgnoreCase("SALARY")) {
				           bsalary = true;
				    }
				        
			        if (qName.equalsIgnoreCase("allowopen")) {
				           ballow = true;
				    }
			        
			        if (qName.equalsIgnoreCase("triangelo")) {
				           btrilo = true;
				    }
			        
			        if (qName.equalsIgnoreCase("tribar")) {
				           bbar = true;
				    }
			        
			        if (qName.equalsIgnoreCase("diamond")) {
				           bdiamond = true;
				    }
			        
			        if (qName.equalsIgnoreCase("instrumentStrings")) {
				           binstrStrings = true;
				    }
			        
			        
			 
			     }
		 
			     public void endElement(String uri, String localName,
			          String qName)
			          throws SAXException {
			 
			          System.out.println("End Element :" + qName);
			 
			     }
		 
			     public void characters(char ch[], int start, int length)
			         throws SAXException {
			 
			        if (bnumchord) {
			        	String str = new String(ch, start, length);
			            System.out.println("Num Chords : "
			                + str);
			            Constraints.NUM_CHORDS = Integer.parseInt(str);
			            bnumchord = false;
			        }
			 
			        if (bminh) {
			        	String str = new String(ch, start, length);
			            System.out.println("Min Height : "
			                + str);
			            Constraints.MIN_HEIGHT = Integer.parseInt(str);
			            bminh = false;
			        }
			 
			        if (bmaxh) {
			        	String str = new String(ch, start, length);
			            System.out.println("Max Height : "
			                + str);
			            Constraints.MAX_HEIGHT = Integer.parseInt(str);
			            bmaxh = false;
			        }
			 
			        if (bsalary) {
			        	String str = new String(ch, start, length);
			            System.out.println("Salary : "
			                + str);
			            bsalary = false;
			        }
			        
			        if(ballow)
			        {
			        	String str = new String(ch, start, length);
			            System.out.println("ALLOW OPEN : "
			                + str);
			            Constraints.ALLOW_OPEN = Boolean.parseBoolean(str);
			            ballow = false;
			        }
			          
			          /*if(bIndex)
			          {
			        	  int index = Integer.parseInt(new String(ch, start, length));
			        	  intList.add(index);
			        	  bIndex = false;
			          }*/
			        
			        if(btrilo)
			        {
			        	String str = new String(ch, start, length);
			            System.out.println("TRIANGELO : "
			                + str);
			            Constraints.TRIANGELO = Boolean.parseBoolean(str);
			            btrilo = false;
			        }
			          
			        if(bbar)
			        {
			        	String str = new String(ch, start, length);
			            System.out.println("TRIANGLE BAR : "
			                + str);
			            Constraints.TRIBAR = Boolean.parseBoolean(str);
			            bbar = false;
			        }
			          
			        if(bdiamond)
			        {
			        	String str = new String(ch, start, length);
			            System.out.println("DIAMOND : "
			                + str);
			            Constraints.DIAMOND = Boolean.parseBoolean(str);
			            bdiamond = false;
			        }
			          
			        if(binstrStrings)
			        {
			        	String str = new String(ch, start, length);
			        	str.trim();
			            System.out.println("Instr strings : "
			                + str);
			            String[] strTab = str.split(",");
			            int[] intTab = new int[strTab.length];
			            for(int i = 0; i<strTab.length; i++)
			            {
			            	intTab[i] = Integer.parseInt(strTab[i]);
			            }
			            Constraints.VIOLIN_STRINGS = intTab;
			            binstrStrings = false;
			        }
			          
			        
			 
			      }
		 
		      };// end defaultHandler
		 
		    try {
				saxParser.parse("c:\\Users\\JeanBenoit\\McGill\\ComputingMusic\\test3.xml", handler);
			} catch (FileNotFoundException e) {
				System.out.println("GOTTA CREATE A NEW FILE");
				File newXML = new File("c:\\Users\\JeanBenoit\\McGill\\ComputingMusic\\test3.xml");
				
				//TODO create default xml file with default values
				
				Constraints.MAX_HEIGHT = 25;
				Constraints.NUM_CHORDS = 4;
				Constraints.MIN_HEIGHT = 1;
				Constraints.ALLOW_OPEN = true;
				
				Constraints.DIAMOND = true;
				Constraints.TRIANGELO = true;
				Constraints.TRIBAR = true;
				
				Constraints.VIOLIN_STRINGS = new int[]{55, 62, 69, 76};
				
				//e.printStackTrace();
				System.err.println("THE XML FILE WAS NOT FOUND; DEFAULT VALUES ENTERED (AND PERHAPS CREATED DEFAULT FILE)");
			}
			
		    //System.out.println("Integer List is: " + intList);
		}
		catch (Exception e)
		{
		    e.printStackTrace();
		}
	}
	 
	
}
