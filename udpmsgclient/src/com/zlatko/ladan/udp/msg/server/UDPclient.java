package com.zlatko.ladan.udp.msg.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class UDPclient {
	private static final int MAX_SIZE = 512;

	private DatagramSocket m_clientSocket = null;
	private InetAddress m_IpAddress = null;
	private byte[] m_sendData = new byte[MAX_SIZE];
	private byte[] m_receiveData = new byte[MAX_SIZE];
	private int m_port = 0;

	public void Connect(String a_host, int a_port) throws UnknownHostException,
			SocketException {
		m_clientSocket = new DatagramSocket();
		m_IpAddress = InetAddress.getByName(a_host);
		m_port = a_port;
		m_clientSocket.setSoTimeout(30000);
	}

	public String Receive() throws IOException {
		Arrays.fill(m_receiveData, (byte) 0);
		DatagramPacket receivePacket = new DatagramPacket(m_receiveData,
				m_receiveData.length);
		m_clientSocket.receive(receivePacket);

		return new String(receivePacket.getData()).trim();
	}

	public void Send(String a_message) throws IOException {
		m_sendData = a_message.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(m_sendData,
				m_sendData.length, m_IpAddress, m_port);
		m_clientSocket.send(sendPacket);
	}

	public void close() {
		m_clientSocket.close();
	}
}
