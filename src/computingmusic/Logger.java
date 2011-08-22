/*

 */

package computingmusic;

import java.net.*;
import java.io.*;
/**
 *
 * @author eliot
 */
public class Logger {


    public static boolean log (String cginame, String logdata) {
        // TODO code application logic here
        //System.out.println("Running");
    	boolean result = false;
        try {
            String cgi = "http://www.cs.mcgill.ca/~asigle1/eliot/"
            	+ cginame + ".cgi?"
                    //+ "TESTING"
                    + logdata;
            // String cgi = "http://www.cs.mcgill.ca/~asigle1/eliot/tester.html";
            URL url = new URL(cgi);
            //URLConnection c = url.openConnection();



            BufferedReader in = new BufferedReader(
				new InputStreamReader(
				url.openStream()));

	String inputLine;
	while ((inputLine = in.readLine()) != null) {
		//System.out.println(inputLine);
		if (inputLine.equals("OK")) {
			result = true;
			break;
		}
		}
	
	
	
	in.close();
	
	



        } catch (MalformedURLException e) {
            System.err.println("Malformed URL: " + e);
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe);

        }
		return result;

    }
    public static String latestVersion () {
      String result = "";
        try {
            String cgi = "http://www.cs.mcgill.ca/~asigle1/eliot/fingerversion.cgi";
            URL url = new URL(cgi);
            //URLConnection c = url.openConnection();
            BufferedReader in = new BufferedReader(
				new InputStreamReader(
				url.openStream()));
	// String inputLine;
	while ((result = in.readLine()) != null) {
			break;
		}
		
	
	
	
	in.close();
	
	



        } catch (MalformedURLException e) {
            System.err.println("Malformed URL: " + e);
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe);

        }
		return result;

    }
    /*
    public static void main (String[] args){
    	System.out.println(latestVersion());
    }
    
    */
}


