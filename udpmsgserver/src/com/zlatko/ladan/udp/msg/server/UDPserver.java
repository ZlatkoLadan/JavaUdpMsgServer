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

		UdpData udpData = new UdpData(Arrays.copyOfRange(
				receivePacket.getData(), 0, receivePacket.getLength()),
				receivePacket.getAddress(), receivePacket.getPort());

		return udpData;
	}

	/**
	 * Sends message to client.
	 *
	 * @param a_data
	 *            the data to send.
	 * @throws IOException
	 */
	public void send(UdpData a_data) throws IOException {
		if (a_data.getData() != null) {
			DatagramPacket sendPacket = new DatagramPacket(a_data.getData(),
					a_data.getData().length, a_data.getIpAddress(),
					a_data.getPort());
			m_serverSocket.send(sendPacket);
			return;
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
