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
//import org.opencv;

import com.github.sarxos.webcam.Webcam;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.List;
import javax.swing.JPanel;

//import org.opencv.core.*;
//import org.opencv.videoio.VideoCapture;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class VisionTool {
	
	enum Mode {
		DEMO_ZONE, // Demonstrate a preset
		TEST_ZONE, // Calibrate on click
		RELEASE, // Demo mode without gui
	}

	static final Mode MODE = Mode.TEST_ZONE;

	static double splitFraction = 0.05;
	static double minimumSideFraction = 0.1;
	
	static double threshold_hue = 6.2262726;
	static double threshold_sat = 0.973544967;
	static double threshold_val = 189.0;

	static final float HUE_MAX_DISTANCE = 0.1f;
	static final float SAT_MAX_DISTANCE = 0.4f;
	static final float VAL_MAX_DISTANCE = 0.1f;
	
	static BufferedImage webcam_img;
	static final Color WHITE = new Color(1.0f,1.0f,1.0f);
//	static Mat frame;
//    static VideoCapture camera;

	
	
	static long last_frame = 0;
	static int frame_count = 0;
	
	public static void printClickedColor( ImagePanel gui ) {
		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				float[] color = new float[3];
				int rgb = webcam_img.getRGB(e.getX(),e.getY());
				ColorHsv.rgbToHsv((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color);
				System.out.println("H = " + color[0]+" S = "+color[1]+" V = "+color[2]);
				threshold_hue = color[0];
				threshold_sat = color[1];
				threshold_val = color[2];
			}
		});
 
	}
	
	public static void fitCannyBinary(GrayF32 input) {
		Graphics2D g2 = webcam_img.createGraphics();
		GrayU8 binary = new GrayU8(input.width,input.height);

		// Finds edges inside the image
		CannyEdge<GrayF32,GrayF32> canny = FactoryEdgeDetectors.canny(2, false, true, GrayF32.class, GrayF32.class);

		canny.process(input, 0.1f, 0.3f, binary);

		List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
		g2.setStroke(new BasicStroke(2));

		// used to select colors for each line
		for( Contour c : contours ) {
			// Only the external contours are relevant.
			List<PointIndex_I32> vertexes =
				ShapeFittingOps.fitPolygon(c.external,true,
					splitFraction, minimumSideFraction,100);

			// Target should have 4 sides
			if(vertexes.size() < 1) {
				continue;
			}

//			int xpasses = 0;
	//		int ypasses = 0;
			int minx = vertexes.get(0).x;
			int miny = vertexes.get(0).y;
			int maxx = vertexes.get(0).x;
			int maxy = vertexes.get(0).y;
			for(int i = 1; i < vertexes.size(); i++) {
				int x = vertexes.get(i).x;
				int y = vertexes.get(i).y;
				
				if(x > maxx) {
					maxx = x;
		//			xpasses ++;
				}
				if(y > maxy) {
					maxy = y;
		//			ypasses ++;
				}
				if(x < minx) {
					minx = x;
	//				xpasses ++;
				}
				if(y < miny) {
					miny = y;
	//				ypasses ++;
				}
			}
//			if(xpasses > 2 || ypasses > 2) continue;
			
			g2.setColor(new Color(0.f, 0.f, 1.f));
			VisualizeShapes.drawRectangle(new Rectangle2D_I32(minx, miny, maxx, maxy), g2);
		}
	}
	
	public static BufferedImage selectorHSV( BufferedImage image, double h, double s, double v ) {
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
				double dv = (V.unsafe_get(x, y)-v)*adjustValue;
 
				// Test if hue, saturation, and value are in range
				double dist2h = Math.abs(dh);
				double dist2s = Math.abs(ds);
				double dist2v = Math.abs(dv);
				if((dist2h <= HUE_MAX_DISTANCE ||
					Double.isNaN(dist2h)) &&
					dist2s <= SAT_MAX_DISTANCE &&
					dist2v <= VAL_MAX_DISTANCE)
				{
					output.setRGB(x,y,image.getRGB(x,y));
				}
			}
		}		

		return output;
	}

	public static void measure_fps() {
		long this_frame = System.currentTimeMillis();
		if((this_frame - last_frame) >= 1000) {
			System.out.println("FPS: " + frame_count);
			last_frame = this_frame;
			frame_count = 1;
		}
		frame_count++;
	}
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
	
	
	public static void main(String[] args) {
		// Open a webcam at a resolution close to 640x480
//		Webcam webcam = UtilWebcamCapture.openDefault(640, 480);
		Webcam webcam = UtilWebcamCapture.openDevice("1", 640, 480);

//		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
//		frame = new Mat();
//		camera = new VideoCapture(0);
		
		last_frame = System.currentTimeMillis();
		// Create the panel used to display the image and feature tracks
		if(MODE == Mode.DEMO_ZONE || MODE == Mode.TEST_ZONE) {
			ListDisplayPanel listpanel = new ListDisplayPanel();
			ImagePanel gui = new ImagePanel();
			ImagePanel guj = new ImagePanel();
			gui.setPreferredSize(new Dimension(640, 480));
			guj.setPreferredSize(new Dimension(640, 480));
			listpanel.addItem((JPanel)gui, "Raw Camera");
			listpanel.addItem((JPanel)guj, MODE == Mode.DEMO_ZONE ?
				"Processed" : "Test_Zone");
			ShowImages.showWindow(listpanel,
				"2846 Vision Tool ( 2017 )", true);
			if(MODE == Mode.TEST_ZONE) {
				printClickedColor(gui);
				printClickedColor(guj);
				while( true ) {
					webcam_img = webcam.getImage();
//					webcam_img = opencamera();
					BufferedImage tmp = selectorHSV(
						webcam_img, threshold_hue,
						threshold_sat, threshold_val);
					GrayF32 gray =
						 ConvertBufferedImage
						.convertFrom(tmp,(GrayF32)null);
					fitCannyBinary(gray);
					gui.setBufferedImageSafe(webcam_img);
					guj.setBufferedImageSafe(tmp);
					measure_fps();
				}
			}else{
				while( true ) {
					webcam_img = webcam.getImage();
//					webcam_img = opencamera();
					BufferedImage set = selectorHSV(
						webcam_img, threshold_hue,
						threshold_sat, threshold_val);
					GrayF32 gray =
						 ConvertBufferedImage
						.convertFrom(set,(GrayF32)null);
					fitCannyBinary(gray);
					gui.setBufferedImageSafe(webcam_img);
					guj.setBufferedImageSafe(set);
					measure_fps();
				}
			}
		}else{
			while( true ) {
				webcam_img = webcam.getImage();
//				webcam_img = opencamera();
				selectorHSV(webcam_img, threshold_hue,
					threshold_sat, threshold_val);
				measure_fps();
			}
		}
	}
}
