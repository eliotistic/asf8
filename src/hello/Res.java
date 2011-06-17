package hello;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
//import javax.print.DocFlavor.URL;
import javax.swing.ImageIcon;

public class Res {
	public ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = this.getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}
	public BufferedImage loadImage(String path) {
        // String imgFileName = "images/weather-"+name+".png";
    	java.net.URL imgURL = this.getClass().getResource(path);
        BufferedImage img = null;
        try {
            img =  ImageIO.read(imgURL);
        } catch (Exception e) {
        }
        return img;
    }
    


	public BufferedImage testimage = loadImage("/resource/test.png");
}
