package org.firebears.visiontool;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;

import java.util.concurrent.ForkJoinTask;
import java.util.List;

import boofcv.alg.color.ColorHsv;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.ConnectRule;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;

import georegression.metric.UtilAngle;
import georegression.struct.shapes.Rectangle2D_I32;

public class VisionProcessTask extends ForkJoinTask<VisionResult> {

	static final double splitFraction = 0.05;
	static final double minimumSideFraction = 0.1;
	static final int MINSIZE = 100;	

	static final float HUE_MAX_DISTANCE = 0.2f;
	static final float SAT_MAX_DISTANCE = 0.4f;
	static final float VAL_MINIMUM = 0.1f;

	public static double threshold_hue = 2.6;
	public static double threshold_sat = 0.8;
	public static double threshold_val = 158.0;

	BufferedImage src;
	VisionResult result;

	VisionProcessTask(BufferedImage src_param) {
		src_param = VisionTool.getImage();
		src = src_param;
		result = new VisionResult();
	}

	/**
	 * Find the position of the target if it exists otherwise say
	 * that it doesn't.
	**/
	public void fitCannyBinary(GrayF32 input, BufferedImage webcam_img) {
		Graphics2D g2 = webcam_img.createGraphics();
		GrayU8 binary = new GrayU8(input.width,input.height);

		// Finds edges inside the image
		CannyEdge<GrayF32,GrayF32> canny = FactoryEdgeDetectors.canny(2, false, true, GrayF32.class, GrayF32.class);

		canny.process(input, 0.1f, 0.3f, binary);

		List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
		g2.setStroke(new BasicStroke(2));

		int xmax1 = 0;
		int ymax1 = 0;
		int xmin1 = 0;
		int ymin1 = 0;
	
		int xmax2 = 0;
		int ymax2 = 0;
		int xmin2 = 0;
		int ymin2 = 0;

		int size1 = 0;
		int size2 = 0;

		// used to select colors for each line
		for( Contour c : contours ) {
			// Only the external contours are relevant.
			List<PointIndex_I32> vertexes =
				ShapeFittingOps.fitPolygon(c.external,true,
					splitFraction, minimumSideFraction,100);

			// Target should have 4 sides
			if(vertexes.size() < 3) {
				continue;
			}

			int minx = vertexes.get(0).x;
			int miny = vertexes.get(0).y;
			int maxx = vertexes.get(0).x;
			int maxy = vertexes.get(0).y;
			for(int i = 1; i < vertexes.size(); i++) {
				int x = vertexes.get(i).x;
				int y = vertexes.get(i).y;
			
				if(x > maxx) {
					maxx = x;
				}
				if(y > maxy) {
					maxy = y;
				}
				if(x < minx) {
					minx = x;
				}
				if(y < miny) {
					miny = y;
				}
			}
		
			int thissize = ( maxy - miny ) * (maxx - minx);

			if(thissize < MINSIZE) {
				continue;
			}

			if(thissize > size1) {
				if(thissize > size2) {
					size1 = size2;
					xmax1 = xmax2;
					xmin1 = xmin2;
					ymax1 = ymax2;
					ymin1 = ymin2;
					size2 = thissize;
					xmax2 = maxx;
					xmin2 = minx;
					ymax2 = maxy;
					ymin2 = miny;
				} else {
					size1 = thissize;
					xmax1 = maxx;
					xmin1 = minx;
					ymax1 = maxy;
					ymin1 = miny;
				}
			}
	//			g2.setColor(new Color(0.f, 0.f, 1.f));
	//			VisualizeShapes.drawRectangle(new Rectangle2D_I32(minx, miny, maxx, maxy), g2);
		}
		g2.setColor(new Color(0.f, 0.f, 1.f));
		VisualizeShapes.drawRectangle(new Rectangle2D_I32(xmin1, ymin1, xmax1, ymax1), g2);
		VisualizeShapes.drawRectangle(new Rectangle2D_I32(xmin2, ymin2, xmax2, ymax2), g2);
		int distance = Math.abs((xmax1 + xmin1) - (xmax2 + xmin2)) / 2;
		float angleoff;
		if(distance != 0 && size1 != 0 && size2 != 0) {
			float angle;
			int pixels = ((xmax1 + xmax2) / 2) - 320;

			distance = 7058 / distance;

			if(xmax1 > xmax2) {
				int left = (xmax1 - xmin1);
				int right = (xmax2 - xmin2);
				angle = (size1 - size2) / 100.0f; //left - right;
	//				pixels = Math.abs((xmax1 + xmax2) / 2);
			} else {
				int right = (xmax1 - xmin1);
				int left = (xmax2 - xmin2);
				angle = (size2 - size1) / 100.0f; //left - right;
	//				pixels = -Math.abs((xmax1 - xmax2) / 2);
			}
			angleoff = (float)(Math.atan(((double)pixels) * 0.00132)
				* 180.0f / Math.PI);

			result.angle = angleoff;
			result.distance = distance;
			result.tilt = angle;
			result.confidence = 1;
	//			synchronized(server) {
	//				Thread thread = new Thread(server);
	//				thread.start();
	//			}

//			System.out.println("Angle " + angleoff + ", Distance: "
//				+ distance + " inches, Tilt: " + angle);
		}else {
			result.angle = 0.0f;
			result.distance = 0.0f;
			result.tilt = 0.0f;
			result.confidence = 0;
	//			synchronized(server) {
	//				Thread thread = new Thread(server);
	//				thread.start();
	//			}

//			System.out.println("No confidence");
		}
	}

	/**
	 * Filter out colors depending on the HSV threshold.
	**/
	public BufferedImage selectorHSV( BufferedImage image, double h, double s, double v ) {
		Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image,null,true,GrayF32.class);
		Planar<GrayF32> hsv = input.createSameShape();
	
		// Convert into HSV
		ColorHsv.rgbToHsv_F32(input,hsv);
		 
		// Extract hue and saturation bands which are independent of intensity
		GrayF32 H = hsv.getBand(0);
		GrayF32 S = hsv.getBand(1);
		GrayF32 V = hsv.getBand(2);
		 
		// Adjust the relative importance of Hue and Saturation.
		// Hue has a range of 0 to 2*PI and Saturation from 0 to 1.
		double adjustUnits = Math.PI / 2.0;
		double adjustValue = 1.0 / 255.0;
		 
		// step through each pixel and mark how close it is to the selected color
		BufferedImage output = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_RGB);
		for( int y = 0; y < image.getHeight(); y++ ) {
			for( int x = 0; x < image.getWidth(); x++ ) {
	//				Color color = new Color(image.getRGB(x, y));
	//				Color.RGBtoHSB(color.getRed(), color.getGreen(),
	//					color.getBlue(), hsv_values);

				// Hue is an angle in radians, so simple subtraction doesn't work
				double dh = UtilAngle.dist(H.unsafe_get(x,y)/*
					hsv_values[0]*/,h);
				double ds = (S.unsafe_get(x,y)/*hsv_values[1]*/
					-s)*adjustUnits;
				double dv = V.unsafe_get(x, y)/*hsv_values[2]*/
					*adjustValue;

				// Test if hue, saturation, and value are in range
				double dist2h = Math.abs(dh);
				double dist2s = Math.abs(ds);
				if((dist2h <= HUE_MAX_DISTANCE ||
					Double.isNaN(dist2h)) &&
					dist2s <= SAT_MAX_DISTANCE &&
					dv >= VAL_MINIMUM)
				{
					output.setRGB(x,y,image.getRGB(x,y));
				}
			}
		}		

		return output;
	}

	public boolean exec() {
/** OpenCV camera start **/
//		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	
//		frame = new Mat();
//		camera = new VideoCapture(0);
/** OpenCV camera end **/
	
		// Create the panel used to display the image and feature tracks
		if(VisionTool.MODE == VisionTool.Mode.DEMO_ZONE ||
			VisionTool.MODE == VisionTool.Mode.TEST_ZONE)
		{
			BufferedImage tmp = selectorHSV(src, threshold_hue,
				threshold_sat, threshold_val);
			GrayF32 gray = ConvertBufferedImage.convertFrom(
				tmp, (GrayF32) null);
			fitCannyBinary(gray, src);
			VisionTool.set_window_graphics(src, tmp);
			FrameCounter.measure_fps();
		}else{
			selectorHSV(src, threshold_hue, threshold_sat,
				threshold_val);
			FrameCounter.measure_fps();
		}
		return true;
	}

	public void setRawResult(VisionResult v) {
		result = v;
	}

	public VisionResult getRawResult() {
		return result;
	}
}
