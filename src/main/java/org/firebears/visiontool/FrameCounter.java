package org.firebears.visiontool;

public class FrameCounter {
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
