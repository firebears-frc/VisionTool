package org.firebears.visiontool;

import boofcv.alg.color.ColorHsv;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.webcamcapture.UtilWebcamCapture;

import com.github.sarxos.webcam.Webcam;

import java.awt.*;
import java.awt.Color.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import javax.swing.JPanel;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;

/** OpenCV camera start **/
//import org.opencv;
//import org.opencv.core.*;
//import org.opencv.videoio.VideoCapture;
/** OpenCV camera end **/

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.firebears.visiontool.*;

public class VisionTool implements Runnable {
	
	private static final int NUMBER_OF_THREADS = 8;

	private ForkJoinPool forkJoinPool;
	public ArrayBlockingQueue<VisionProcessTask> queue;

	static UDPServer server;

	enum Mode {
		DEMO_ZONE, // Demonstrate a preset
		TEST_ZONE, // Calibrate on click
		RELEASE, // Demo mode without gui
	}

	final Color WHITE = new Color(1.0f,1.0f,1.0f);

	float hsv_values[] = new float[3];

/** OpenCV camera start **/
//	static Mat frame;
//    static VideoCapture camera;
/** OpenCV camera end **/

	// Static Variables
	static Webcam webcam = null;

	static ListDisplayPanel listpanel = null;
	static ImagePanel gui = null;
	static ImagePanel guj = null;

	// Setup
	final static String CAMERA = "0";
	final static Mode MODE = Mode.RELEASE;

	static long time = 0;

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
				VisionProcessTask.threshold_hue = color[0];
				VisionProcessTask.threshold_sat = color[1];
				VisionProcessTask.threshold_val = color[2];
			}
		});
 
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
		ShowImages.showWindow(listpanel, "2846 Vision Tool ( 2017 )",
			true);
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
		server = new UDPServer("10.28.46.2", 5810);

		VisionTool vision_tool = new VisionTool();
		vision_tool.forkJoinPool = new ForkJoinPool(NUMBER_OF_THREADS);
		vision_tool.queue = new ArrayBlockingQueue(NUMBER_OF_THREADS);

		Thread vision_tool_thread = new Thread(vision_tool);
		vision_tool_thread.start();

		BufferedImage webcam = getImage();
		if(MODE == Mode.TEST_ZONE) {
			print_clicked_colors(webcam);
		}
		
		while(true) {
			VisionProcessTask t;

			webcam = getImage();
			t = new VisionProcessTask(webcam);

			vision_tool.forkJoinPool.execute(t);

//			while(System.currentTimeMillis() <= time + 250);
//			time += 250;//System.currentTimeMillis();
//			System.out.println("Time " + (newtime - time));
//			time = newtime;

			// Keep on waiting for open space in queue....
			try {
			 	vision_tool.queue.offer(t, 100, TimeUnit.DAYS);
			}catch (Exception e) {
				// Ignore exceptions, nothing we can do about it
			}
		}
	}

	/**
	 * This is the main method for the result thread.
	**/
	public void run() {
		while(true) {
			VisionProcessTask task = queue.poll();
			if(task != null) {
				server.send(task.join());
			}
		}
	}
}
