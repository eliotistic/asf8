package hello;

import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.*;
;



public class RangeImage extends JPanel{
	
	private static final long serialVersionUID = 1L;
	int w;
	int h;
	
	protected ImageIcon createImageIcon(String path) {
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
            h = img.getHeight();
            w = img.getWidth();
            // System.out.println("H:" + h + " W: " + w );
        } catch (Exception e) {
        }
        return img;
    }
    
	public Dimension getPreferredSize() {
        return new Dimension(w, h);
    }
	public Dimension getMinimumSize() {
        return new Dimension(w, h);
    }

	public BufferedImage image = loadImage("/resource/test.png");
    //private BufferedImage image = Res.testimage;
   
    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, image.getWidth(null), image.getHeight(null),  null);
    }
    public static void main(String args[]) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("Test image load!");
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				
				RangeImage  c = new RangeImage();
			
				frame.add(c);
				
				frame.pack();
				frame.setVisible(true);
			}
		});
	}

}
