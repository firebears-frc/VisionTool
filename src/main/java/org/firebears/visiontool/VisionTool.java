package org.firebears.visiontool;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Rectangle2D_I32;

import com.github.sarxos.webcam.Webcam;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class VisionTool {
	
	static double splitFraction = 0.05;
	static double minimumSideFraction = 0.1;
	
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

	public static void main(String[] args) {

		// Open a webcam at a resolution close to 640x480
		Webcam webcam = UtilWebcamCapture.openDefault(640,480);

		// Create the panel used to display the image and feature tracks
		ImagePanel gui = new ImagePanel();
		gui.setPreferredSize(webcam.getViewSize());

		ShowImages.showWindow(gui, "2017 Vision Tool", true);

		while( true ) {
			BufferedImage image = webcam.getImage();
			GrayF32 gray = ConvertBufferedImage.convertFrom(image,(GrayF32)null);

			Graphics2D g2 = image.createGraphics();
			
			fitCannyEdges(g2, gray);
			fitCannyBinary(g2, gray);

			gui.setBufferedImageSafe(image);
		}
	}
}