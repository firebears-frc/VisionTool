package org.firebears.visiontool;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.DatagramChannel;

public class UDPServer implements Runnable {

	final SocketAddress address;
	public float angle;
	public float distance;
	public float tilt;
	public int confidence;

	public UDPServer(String host, int port/*, float angle, float distance,
		float tilt, int confidence*/)
	{
		address = new InetSocketAddress(host, port);
//		m_angle = angle;
//		m_distance = distance;
//		m_tilt = tilt;
//		m_confidence = confidence;
	}

	public void run() {
		try ( DatagramChannel channel = DatagramChannel.open()) {
			ByteBuffer buffer = ByteBuffer.allocate(512);
			buffer.clear();
			buffer.putFloat(angle);
			buffer.putFloat(distance);
			buffer.putFloat(tilt);
			buffer.putInt(confidence);
			buffer.flip();
			channel.send(buffer, address);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Sent " + angle + " " + distance + " " +
			tilt + " " + confidence);
	}
}
