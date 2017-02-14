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
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.List;
import javax.swing.JPanel;

/** OpenCV camera start **/
//import org.opencv;
//import org.opencv.core.*;
//import org.opencv.videoio.VideoCapture;
/** OpenCV camera end **/

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class VisionTool implements Runnable {
	
	enum Mode {
		DEMO_ZONE, // Demonstrate a preset
		TEST_ZONE, // Calibrate on click
		RELEASE, // Demo mode without gui
	}

	double splitFraction = 0.05;
	double minimumSideFraction = 0.1;

	final float HUE_MAX_DISTANCE = 0.8f;
	final float SAT_MAX_DISTANCE = 0.4f;
	final float VAL_MINIMUM = 0.1f;

	final int MINSIZE = 100;	

	final Color WHITE = new Color(1.0f,1.0f,1.0f);

/** OpenCV camera start **/
//	static Mat frame;
//    static VideoCapture camera;
/** OpenCV camera end **/

	// Static Variables
	static Webcam webcam = null;

	static ListDisplayPanel listpanel = null;
	static ImagePanel gui = null;
	static ImagePanel guj = null;

	static double threshold_hue = 2.6;
	static double threshold_sat = 0.8;
	static double threshold_val = 158.0;

	// Setup
	final static String CAMERA = "0";
	final static Mode MODE = Mode.TEST_ZONE;

	/**
	 * Initialize color picker for real-time calibration in HSV for a
	 * specific tab.
	**/
	public static void printClickedColor(ImagePanel gui, BufferedImage from) {
		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				float[] color = new float[3];
				int rgb = from.getRGB(e.getX(),e.getY());
				ColorHsv.rgbToHsv((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color);
				System.out.println("H = " + color[0]+" S = "+color[1]+" V = "+color[2]);
				threshold_hue = color[0];
				threshold_sat = color[1];
				threshold_val = color[2];
			}
		});
 
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
			System.out.println("Angle " + angleoff + ", Distance: "
				+ distance + " inches, Tilt: " + angle);
		}else {
			System.out.println("No confidence");
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
		BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
		for( int y = 0; y < hsv.height; y++ ) {
			for( int x = 0; x < hsv.width; x++ ) {
				// Hue is an angle in radians, so simple subtraction doesn't work
				double dh = UtilAngle.dist(H.unsafe_get(x,y),h);
				double ds = (S.unsafe_get(x,y)-s)*adjustUnits;
				double dv = V.unsafe_get(x, y)*adjustValue;
 
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

/** OpenCV camera start **/
/*	public static BufferedImage opencamera(){
		camera.read(frame);

        BufferedImage image = MatToBufferedImage(frame);
        return image;
	}
	
	
	public static BufferedImage MatToBufferedImage(Mat frame) {
        //Mat() to BufferedImage
        int type = 0;
        if (frame.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (frame.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
        WritableRaster raster = image.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        frame.get(0, 0, data);

        return image;
    }
*/
/** OpenCV camera end **/

	/**
	 * Open up the window with 2 tabs.
	**/
	public static void launch_window() {
		listpanel = new ListDisplayPanel();
		gui = new ImagePanel();
		guj = new ImagePanel();
		
		gui.setPreferredSize(new Dimension(640, 480));
		guj.setPreferredSize(new Dimension(640, 480));
		listpanel.addItem((JPanel)gui, "Raw Camera");
		listpanel.addItem((JPanel)guj, MODE == Mode.DEMO_ZONE ?
			"Processed" : "Test_Zone");
		ShowImages.showWindow(listpanel,
			"2846 Vision Tool ( 2017 )", true);
	}

	/**
	 * Initialize color picker for real-time calibration in HSV for both
	 * tabs.
	**/
	public static void print_clicked_colors(BufferedImage from) {
		synchronized (gui) {
			printClickedColor(gui, from);
			printClickedColor(guj, from);
		}
	}

	/**
	 * Update the contents of the window.
	**/
	public static void set_window_graphics(BufferedImage raw, BufferedImage processed) {
		synchronized (gui) {
			gui.setBufferedImageSafe(raw);
			guj.setBufferedImageSafe(processed);
		}
	}

	/**
	 * Retrieve the data from the webcam.
	**/
	public static BufferedImage getImage() {
		synchronized (webcam) {
			return webcam.getImage();
		}
	}

	/**
	 * This is the start of the program.  It should start 4 threads after
	 * initializing shared data.
	**/
	public static void main(String[] args) {
		// Open a webcam at a resolution close to 640x480
		webcam = UtilWebcamCapture.openDevice(CAMERA, 640, 480);
		if(MODE == Mode.DEMO_ZONE || MODE == Mode.TEST_ZONE) {
			launch_window();
		}
		FrameCounter.last_frame = System.currentTimeMillis();
		VisionTool vision_tool = new VisionTool();
		for(int i = 0; i < 4; i++) {
			Thread vision_tool_thread = new Thread(vision_tool);
			vision_tool_thread.start();
		}
	}

	/**
	 * Method is executed for each thread.  Contains main loop.
	**/
	public void run() {
		BufferedImage webcam_img = getImage();

/** OpenCV camera start **/
//		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
//		frame = new Mat();
//		camera = new VideoCapture(0);
/** OpenCV camera end **/
		
		// Create the panel used to display the image and feature tracks
		if(MODE == Mode.DEMO_ZONE || MODE == Mode.TEST_ZONE) {
			if(MODE == Mode.TEST_ZONE) {
				print_clicked_colors(webcam_img);
			}
			while( true ) {
				webcam_img = getImage();
/** OpenCV camera start **/
//				webcam_img = opencamera();
/** OpenCV camera end **/
				BufferedImage tmp = selectorHSV(webcam_img,
					threshold_hue, threshold_sat,
					threshold_val);
				GrayF32 gray = ConvertBufferedImage.convertFrom(
					tmp, (GrayF32) null);
				fitCannyBinary(gray, webcam_img);
				set_window_graphics(webcam_img, tmp);
				FrameCounter.measure_fps();
			}
		}else{
			while( true ) {
				webcam_img = getImage();
/** OpenCV camera start **/
//				webcam_img = opencamera();
/** OpenCV camera end **/
				selectorHSV(webcam_img, threshold_hue,
					threshold_sat, threshold_val);
				FrameCounter.measure_fps();
			}
		}
	}

	/**
	 * A private class to count frames per second.
	**/
	private static class FrameCounter {
		public static long last_frame = 0;
		private static int frame_count = 0;

		public static synchronized void measure_fps() {
			long this_frame = System.currentTimeMillis();
			if((this_frame - last_frame) >= 1000) {
				System.out.println("FPS: " + frame_count);
				last_frame = this_frame;
				frame_count = 0;
			}
			frame_count++;
		}
	}
}
