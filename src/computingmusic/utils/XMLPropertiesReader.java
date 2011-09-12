package computingmusic.utils;


import java.io.FileNotFoundException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import computingmusic.Constraints;

public class XMLPropertiesReader {

	 public static void main(String argv[]) {
		 
		 try 
		 {
			 SAXParserFactory factory = SAXParserFactory.newInstance();
		     SAXParser saxParser = factory.newSAXParser();
		     
		     DefaultHandler handler = new DefaultHandler() {
		 
			     boolean bOrigins = false;
			     boolean bOpen = false;
			     boolean bPlayable = false;
			     boolean bMinHeight = false;
			     boolean bMaxHeight = false;
			     boolean bSpan = false;
			     
			     boolean bbar = false;
			     boolean btrilo = false;
			     boolean bdiamond = false;
			     //ArrayList<Integer> intList = new ArrayList<Integer>();
			 
			     public void startElement(String uri, String localName,
			        String qName, Attributes attributes)
			        throws SAXException 
			     {
			 
			       System.out.println("Start Element :" + qName);
			 
			        if (qName.equalsIgnoreCase("allInstrumentStrings")) {
			            bOrigins = true;
			        }
			 
			        if (qName.equalsIgnoreCase("allOpen")) {
			            bOpen = true;
			        }
			 
			        if (qName.equalsIgnoreCase("allPlayable")) {
			            bPlayable = true;
			        }
			 
			        if (qName.equalsIgnoreCase("allMinHeight")) {
			        	bMinHeight = true;
				    }
				        
			        if (qName.equalsIgnoreCase("allMaxHeight")) {
			        	bMaxHeight = true;
				    }
			        
			        if (qName.equalsIgnoreCase("triangelo")) {
			        	String s = attributes.getValue(0);
			        	int triangeloLength = Integer.parseInt(s);
			        	Constraints.TRIANGELO_LENGTH = triangeloLength;
			        	btrilo = true;
				    }
			        
			        if (qName.equalsIgnoreCase("tribar")) {
			        	bbar = true;
				    }
			        
			        if (qName.equalsIgnoreCase("diamond")) {
			        	bdiamond = true;
				    }
			        
			        if(qName.equalsIgnoreCase("handSpan"))
			        {
			        	bSpan = true;
			        }
			        
			 
			     }
		 
			     public void endElement(String uri, String localName,
			          String qName)
			          throws SAXException {
			 
			          System.out.println("End Element :" + qName);
			 
			     }
		 
			     public void characters(char ch[], int start, int length)
			         throws SAXException {
			    	 
			         String str = new String(ch, start, length);
			         if (bOrigins) {
			        	
			            System.out.println("Origins : "
			                + str);
			            String [] numbers = str.split(" ");
			            int[] ints = new int[numbers.length];
			            for(int i = 0; i< numbers.length; i++)
			            {
			            	ints[i] = Integer.parseInt(numbers[i]);
			            }
			            Constraints.VIOLIN_STRINGS = ints;
			            bOrigins = false;
			         }
			 
			         if (bOpen) {
			            System.out.println("Open : "
			                + str);
			            String [] numbers = str.split(" ");
			            boolean[] ints = new boolean[numbers.length];
			            for(int i = 0; i< numbers.length; i++)
			            {
			            	ints[i] = Boolean.parseBoolean(numbers[i]);
			            }
			            Constraints.VIOLIN_OPEN = ints;
			            bOpen = false;
			         }
			         
			         if(bSpan)
			         {
			        	System.out.println("Hand Span : "
					                + str);
			            
			            int inte = Integer.parseInt(str);
			            
			            Constraints.VIOLIN_HAND_SPAN = inte;
			            bSpan = false;
			         }
			 
			         if (bPlayable) {
			            System.out.println("Playable : "
			                + str);
			            String [] numbers = str.split(" ");
			            boolean[] ints = new boolean[numbers.length];
			            for(int i = 0; i< numbers.length; i++)
			            {
			            	ints[i] = Boolean.parseBoolean(numbers[i]);
			            }
			            Constraints.VIOLIN_PLAYABLES = ints;
			            bPlayable = false;
			         }
			 
			         if (bMinHeight) {
			            System.out.println("Min height : "
			                + str);
			            String [] numbers = str.split(" ");
			            int[] ints = new int[numbers.length];
			            for(int i = 0; i< numbers.length; i++)
			            {
			            	ints[i] = Integer.parseInt(numbers[i]);
			            }
			            Constraints.VIOLIN_MIN_HEIGHTS = ints;
			            bMinHeight = false;
			         }
			        
			         if(bMaxHeight)
			         {
			            System.out.println("Max height : "
			                + str);
			            String [] numbers = str.split(" ");
			            int[] ints = new int[numbers.length];
			            for(int i = 0; i< numbers.length; i++)
			            {
			            	ints[i] = Integer.parseInt(numbers[i]);
			            }
			            Constraints.VIOLIN_MAX_HEIGHTS = ints;
			            bMaxHeight = false;
			         }
			          
			          /*if(bIndex)
			          {
			        	  int index = Integer.parseInt(new String(ch, start, length));
			        	  intList.add(index);
			        	  bIndex = false;
			          }*/
			        
			        if(btrilo)
			        {
			            System.out.println("TRIANGELO : "
			                + str);
			            Constraints.TRIANGELO = Boolean.parseBoolean(str);
			            btrilo = false;
			        }
			          
			        if(bbar)
			        {
			            System.out.println("TRIANGLE BAR : "
			                + str);
			            Constraints.TRIBAR = Boolean.parseBoolean(str);
			            bbar = false;
			        }
			          
			        if(bdiamond)
			        {
			            System.out.println("DIAMOND : "
			                + str);
			            Constraints.DIAMOND = Boolean.parseBoolean(str);
			            bdiamond = false;
			        }
			        
			          
			        
			 
			      }
		 
		      };// end defaultHandler
		 
		    try {
				//saxParser.parse("c:\\Users\\JeanBenoit\\McGill\\ComputingMusic\\test4.xml", handler);
		    	saxParser.parse("SFProperties.xml", handler);
			} catch (FileNotFoundException e) {
				System.out.println("GOTTA CREATE A NEW FILE");
				//File newXML = new File("c:\\Users\\JeanBenoit\\McGill\\ComputingMusic\\test3.xml");
				XMLPropertiesWriter.generateDefaultXMLFile();
				XMLPropertiesWriter.generateXMLFileFromDefault();
				
				
				Constraints.DIAMOND = true;
				Constraints.TRIANGELO = true;
				Constraints.TRIANGELO_LENGTH = 3;
				Constraints.TRIBAR = true;
				
				Constraints.VIOLIN_STRINGS = new int[]{55, 62, 69, 76};
				Constraints.VIOLIN_OPEN = new boolean[]{true, true, true, true};
				Constraints.VIOLIN_HAND_SPAN = 8;
				Constraints.VIOLIN_PLAYABLES = new boolean[]{true, true, true, true};
				Constraints.VIOLIN_MIN_HEIGHTS = new int[]{0, 0, 0, 0};
				Constraints.VIOLIN_MAX_HEIGHTS = new int[]{25, 25, 25, 25};
				
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
