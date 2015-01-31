package com.zlatko.ladan.udp.msg.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class UDPserver {
	private static final int MAX_SIZE = 512;

	private DatagramSocket m_serverSocket = null;
	private byte[] m_receiveData = new byte[MAX_SIZE];
	private byte[] m_sendData = null;
	private int m_port = 0;

	public void connect(int a_port) throws SocketException {
		m_port = a_port;
		m_serverSocket = new DatagramSocket(m_port);
	}

	private static byte[] resizeArray(byte[] a_array) {
		byte[] newArray = new byte[MAX_SIZE];
		int preserveLength = Math.min(a_array.length, MAX_SIZE);

		if (preserveLength > 0) {
			System.arraycopy(a_array, 0, newArray, 0, preserveLength);
		}
		return newArray;
	}

	public UdpData receive() throws IOException {
		Arrays.fill(m_receiveData, (byte) 0);
		DatagramPacket receivePacket = new DatagramPacket(m_receiveData,
				m_receiveData.length);
		m_serverSocket.receive(receivePacket);

		return new UdpData(new String(receivePacket.getData()).trim(),
				receivePacket.getAddress(), receivePacket.getPort());
	}

	public void send(UdpData a_data) throws IOException {
		m_sendData = a_data.getData().getBytes();
		if (m_sendData.length > MAX_SIZE) {
			m_sendData = resizeArray(m_sendData);
		}
		DatagramPacket sendPacket = new DatagramPacket(m_sendData,
				m_sendData.length, a_data.getIpAddress(), a_data.getPort());
		m_serverSocket.send(sendPacket);
	}

	public void close() {
		m_serverSocket.close();
	}
}
