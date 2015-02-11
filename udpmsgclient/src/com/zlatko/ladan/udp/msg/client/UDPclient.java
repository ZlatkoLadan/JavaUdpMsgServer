package com.zlatko.ladan.udp.msg.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;

//TODO: Add more to comments
public class UDPclient {
	private static final int MAX_SIZE = 512;
	private static final Charset CHARACTER_SET = Charset.forName("UTF-8");

	private DatagramSocket m_clientSocket = null;
	private InetAddress m_IpAddress = null;
	private byte[] m_sendData = new byte[MAX_SIZE];
	private byte[] m_receiveData = new byte[MAX_SIZE];
	private int m_port = 0;

	/**
	 * Connects to the server.
	 *
	 * @param a_host
	 *            the server's host name or IP address
	 * @param a_port
	 *            the server's port
	 * @throws UnknownHostException
	 * @throws SocketException
	 */
	public void Connect(String a_host, int a_port) throws UnknownHostException,
			SocketException {
		m_clientSocket = new DatagramSocket();
		m_IpAddress = InetAddress.getByName(a_host);
		m_port = a_port;
	}

	/**
	 * Sets timeout in seconds.
	 *
	 * @param a_seconds
	 *            Seconds.
	 * @throws SocketException
	 *             If error in underlying protocol.
	 */
	public void setTimeoutInSeconds(int a_seconds) throws SocketException {
		m_clientSocket.setSoTimeout(a_seconds * 1000);
	}

	/**
	 * Blocking function that receives the servers message.
	 *
	 * @return The server's data that was received
	 * @throws IOException
	 */
	public byte[] Receive() throws IOException {
		Arrays.fill(m_receiveData, (byte) 0);
		DatagramPacket receivePacket = new DatagramPacket(m_receiveData,
				m_receiveData.length);
		m_clientSocket.receive(receivePacket);
		return Arrays.copyOf(m_receiveData, receivePacket.getLength());
	}

	/**
	 * Sends a text message to the server.
	 *
	 * @param a_message
	 *            The message
	 * @throws IOException
	 */
	public void Send(String a_message) throws IOException {
		m_sendData = a_message.getBytes(CHARACTER_SET);
		DatagramPacket sendPacket = new DatagramPacket(m_sendData,
				m_sendData.length, m_IpAddress, m_port);
		m_clientSocket.send(sendPacket);
	}

	/**
	 * Sends a text message to the server.
	 *
	 * @param a_message
	 *            The message
	 * @throws IOException
	 */
	public void Send(byte[] a_message) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(a_message,
				a_message.length, m_IpAddress, m_port);
		m_clientSocket.send(sendPacket);
	}

	/**
	 * Closes the connection
	 */
	public void close() {
		m_clientSocket.close();
	}
}
