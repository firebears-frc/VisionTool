package org.firebears.visiontool;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.DatagramChannel;

public class UDPServer {

	final SocketAddress address;

	public UDPServer(String host, int port) {
		address = new InetSocketAddress(host, port);
	}

	public void send(VisionResult result) {
		try ( DatagramChannel channel = DatagramChannel.open()) {
			ByteBuffer buffer = ByteBuffer.allocate(512);
			buffer.clear();
			buffer.putFloat(result.angle);
			buffer.putFloat(result.distance);
			buffer.putFloat(result.tilt);
			buffer.putInt(result.confidence);
			buffer.flip();
			channel.send(buffer, address);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Sent: Angle " + result.angle +
			", Distance: " + result.distance + " inches, Tilt: " +
			result.tilt + " Confidence: " + result.confidence);
	}
}
