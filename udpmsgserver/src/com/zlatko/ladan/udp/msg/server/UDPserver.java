package com.zlatko.ladan.udp.msg.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Arrays;

//TODO: Add more to comments
public class UDPserver {
	private static final String REGEX_FILE = "FIL:";

	private static final int MAX_SIZE = 512;
	public static final Charset CHARACTER_SET = Charset.forName("UTF-8");

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

		String str = new String(receivePacket.getData(), 0,
				receivePacket.getLength(), CHARACTER_SET);

		UdpData udpData = new UdpData(str, receivePacket.getAddress(),
				receivePacket.getPort());

		if (str.startsWith(REGEX_FILE) && receivePacket.getLength() < 487 + 4) {
			byte[] arr = new byte[receivePacket.getLength() - 4 - 1];
			for (int i = 0; i < arr.length; i++) {
				arr[i] = m_receiveData[i + 4];
			}
			udpData.setRawData(arr);
		}
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
		if (a_data.getRawData() != null) {
			DatagramPacket sendPacket = new DatagramPacket(a_data.getRawData(),
					a_data.getRawData().length, a_data.getIpAddress(),
					a_data.getPort());
			m_serverSocket.send(sendPacket);
			return;
		}

		m_sendData = a_data.getData().getBytes(CHARACTER_SET);
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
