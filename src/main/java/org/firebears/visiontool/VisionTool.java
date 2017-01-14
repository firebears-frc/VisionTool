package org.firebears.visiontool;

import boofcv.alg.color.ColorHsv;
import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import georegression.metric.UtilAngle;
import georegression.struct.shapes.Rectangle2D_I32;

import com.github.sarxos.webcam.Webcam;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JPanel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class VisionTool {
	
	static double splitFraction = 0.05;
	static double minimumSideFraction = 0.1;
	
	static double temp_sat = 0.0;
	static double temp_val = 0.0;
	static double temp_hue = 0.0;
	
	static BufferedImage webcam_img;
	
	public static void printClickedColor( ImagePanel guk ) {
		guk.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				float[] color = new float[3];
				int rgb = webcam_img.getRGB(e.getX(),e.getY());
				ColorHsv.rgbToHsv((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color);
				System.out.println("H = " + color[0]+" S = "+color[1]+" V = "+color[2]);
				temp_hue = color[0];
				temp_sat = color[1];
				temp_val = color[2];
			}
		});
 
	}
	
	public static void fitCannyBinary( Graphics2D g2, GrayF32 input ) {
		GrayU8 binary = new GrayU8(input.width,input.height);

		// Finds edges inside the image
		CannyEdge<GrayF32,GrayF32> canny = FactoryEdgeDetectors.canny(2, false, true, GrayF32.class, GrayF32.class);

		canny.process(input, 0.1f, 0.3f, binary);

		List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
		g2.setStroke(new BasicStroke(2));

		// used to select colors for each line
		for( Contour c : contours ) {
			// Only the external contours are relevant.
			List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external,true,
					splitFraction, minimumSideFraction,100);

			// Target should have 6 sides
			if(vertexes.size() < 6) {
				continue;
			}
			
			int xpasses = 0;
			int ypasses = 0;
			int minx = vertexes.get(0).x;
			int miny = vertexes.get(0).y;
			int maxx = vertexes.get(0).x;
			int maxy = vertexes.get(0).y;
			for(int i = 1; i < vertexes.size(); i++) {
				int x = vertexes.get(i).x;
				int y = vertexes.get(i).y;
				
				if(x > maxx) {
					maxx = x;
					xpasses ++;
				}
				if(y > maxy) {
					maxy = y;
					ypasses ++;
				}
				if(x < minx) {
					minx = x;
					xpasses ++;
				}
				if(y < miny) {
					miny = y;
					ypasses ++;
				}
			}
			if(xpasses > 2 || ypasses > 2) continue;
			System.out.println("SIZE: " + vertexes.size() + " x:" + xpasses + " y:" + ypasses);
			
			g2.setColor(new Color(1.f, 0.f, 1.f));
			VisualizeShapes.drawPolygon(vertexes,true,g2);
			g2.setColor(new Color(0.f, 0.f, 1.f));
			VisualizeShapes.drawRectangle(new Rectangle2D_I32(minx, miny, maxx, maxy), g2);
		}
	}
	
	/**
	 * Fits a sequence of line-segments into a sequence of points found using the Canny edge detector.  In this case
	 * the points are not connected in a loop. The canny detector produces a more complex tree and the fitted
	 * points can be a bit noisy compared to the others.
	 * @param g22 
	 */
	public static void fitCannyEdges( Graphics2D g2, GrayF32 input ) {

		// Finds edges inside the image
		CannyEdge<GrayF32,GrayF32> canny =
				FactoryEdgeDetectors.canny(2, true, true, GrayF32.class, GrayF32.class);

		canny.process(input,0.1f,0.3f,null);
		List<EdgeContour> contours = canny.getContours();

		g2.setStroke(new BasicStroke(2));

		for( EdgeContour e : contours ) {

			for(EdgeSegment s : e.segments ) {
				// fit line segments to the point sequence.  Note that loop is false
				List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(s.points,false,
						splitFraction, minimumSideFraction,100);

				g2.setColor(new Color(1.f, 1.f, 0.f));
				VisualizeShapes.drawPolygon(vertexes, false, g2);
			}
		}
	}
	
	public static BufferedImage selectorHSV( BufferedImage image, double h, double s, double v ) {
		Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image,null,true,GrayF32.class);
		Planar<GrayF32> hsv = input.createSameShape();
		
		// Convert into HSV
		ColorHsv.rgbToHsv_F32(input,hsv);
		
		// Euclidean distance squared threshold for deciding which pixels are members of the selected set
		float maxDist2h = 0.4f*0.4f;
		float maxDist2s = 0.4f*0.4f;
		float maxDist2v = 0.4f*0.4f;
		 
		// Extract hue and saturation bands which are independent of intensity
		GrayF32 H = hsv.getBand(0);
		GrayF32 S = hsv.getBand(1);
		GrayF32 V = hsv.getBand(2);
		 
		// Adjust the relative importance of Hue and Saturation.
		// Hue has a range of 0 to 2*PI and Saturation from 0 to 1.
		double adjustUnits = Math.PI / 2.0;
		double adjustValue = 1.0 / 255.0;
		 
		// step through each pixel and mark how close it is to the selected color
		BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
		for( int y = 0; y < hsv.height; y++ ) {
			for( int x = 0; x < hsv.width; x++ ) {
				// Hue is an angle in radians, so simple subtraction doesn't work
				double dh = UtilAngle.dist(H.unsafe_get(x,y),h);
				double ds = (S.unsafe_get(x,y)-s)*adjustUnits;
				double dv = (V.unsafe_get(x, y)-v)*adjustValue;
 
				// this distance measure is a bit naive, but good enough for to demonstrate the concept
				double dist2h = dh*dh;
				double dist2s = ds*ds;
				double dist2v = dv*dv;
				if((dist2h <= maxDist2h || Double.isNaN(dist2h))
					&& dist2s <= maxDist2s && v >= 0.5)
				{
					output.setRGB(x,y,image.getRGB(x,y));
				}
			}
		}		

		return output;
		
	}

	public static void main(String[] args) {

		// Open a webcam at a resolution close to 640x480
		Webcam webcam = UtilWebcamCapture.openDefault(1280,760);

		// Create the panel used to display the image and feature tracks
		ListDisplayPanel listpanel = new ListDisplayPanel();
		ImagePanel gui = new ImagePanel();
		ImagePanel guj = new ImagePanel();
		ImagePanel guk = new ImagePanel();
		gui.setPreferredSize(new Dimension(960, 720));
		guj.setPreferredSize(new Dimension(960, 720));
		guk.setPreferredSize(new Dimension(960, 720));
		listpanel.addItem((JPanel)gui, "Raw Camera");
		listpanel.addItem((JPanel)guj, "Processed");
		listpanel.addItem((JPanel) guk, "Test_Zone");

		ShowImages.showWindow(listpanel, "2846 Vision Tool ( 2017 )", true);
		
		
		printClickedColor(gui);

		printClickedColor(guk);

			
		while( true ) {
			BufferedImage image = webcam.getImage();
			webcam_img = image;
//			GrayF32 gray = ConvertBufferedImage.convertFrom(image,(GrayF32)null);

//			Graphics2D g2 = image.createGraphics();
//			fitCannyEdges(g2, gray);
//			fitCannyBinary(g2, gray);

			gui.setBufferedImageSafe(image);
			guj.setBufferedImageSafe(selectorHSV(image, 6.2262726, 0.973544967, 189.0));
			guk.setBufferedImageSafe(selectorHSV(image, temp_hue, temp_sat, temp_val));
		}
	}
}
