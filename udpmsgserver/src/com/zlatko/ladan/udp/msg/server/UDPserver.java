package com.zlatko.ladan.udp.msg.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

//TODO: Add more to comments
public class UDPserver {
	private static final int MAX_SIZE = 512;

	private DatagramSocket m_serverSocket = null;
	private byte[] m_receiveData = new byte[MAX_SIZE];
	private byte[] m_sendData = null;
	private int m_port = 0;

	/**
	 * Creates a connection.
	 *
	 * @param a_port
	 *            The port
	 * @throws SocketException
	 */
	public void connect(int a_port) throws SocketException {
		m_port = a_port;
		m_serverSocket = new DatagramSocket(m_port);
	}

	/**
	 * Resizes the data array.
	 *
	 * @param a_array
	 *            The array to resize
	 * @return The resizes array
	 */
	private static byte[] resizeArray(byte[] a_array) {
		byte[] newArray = new byte[MAX_SIZE];
		int preserveLength = Math.min(a_array.length, MAX_SIZE);

		if (preserveLength > 0) {
			System.arraycopy(a_array, 0, newArray, 0, preserveLength);
		}
		return newArray;
	}

	/**
	 * A blocking method which receives the client's message.
	 *
	 * @return The message.
	 * @throws IOException
	 */
	public UdpData receive() throws IOException {
		Arrays.fill(m_receiveData, (byte) 0);
		DatagramPacket receivePacket = new DatagramPacket(m_receiveData,
				m_receiveData.length);
		m_serverSocket.receive(receivePacket);

		return new UdpData(new String(receivePacket.getData()).trim(),
				receivePacket.getAddress(), receivePacket.getPort());
	}

	/**
	 * Sends message to client.
	 *
	 * @param a_data
	 *            the data to send.
	 * @throws IOException
	 */
	public void send(UdpData a_data) throws IOException {
		m_sendData = a_data.getData().getBytes();
		if (m_sendData.length > MAX_SIZE) {
			m_sendData = resizeArray(m_sendData);
		}
		DatagramPacket sendPacket = new DatagramPacket(m_sendData,
				m_sendData.length, a_data.getIpAddress(), a_data.getPort());
		m_serverSocket.send(sendPacket);
	}

	/**
	 * Closes the connection.
	 */
	public void close() {
		m_serverSocket.close();
	}
}
