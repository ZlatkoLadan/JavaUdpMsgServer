package com.zlatko.ladan.udp.msg.server;

import java.net.InetAddress;

//TODO: Add more to comments
public class UdpData {
	private byte[] m_data;
	private InetAddress m_ipAddress;
	private int m_port;

	/**
	 * The UdpData class constructor.
	 *
	 * @param a_data
	 *            the data
	 * @param a_ipAddress
	 *            the IP address
	 * @param a_port
	 *            The port
	 */
	public UdpData(byte[] a_data, InetAddress a_ipAddress, int a_port) {
		this.setData(a_data);
		this.setIpAddress(a_ipAddress);
		this.setPort(a_port);
	}

	/**
	 * Sets the data.
	 *
	 * @param a_data
	 *            The data
	 */
	public byte[] getData() {
		return m_data;
	}

	/**
	 * Sets the data.
	 *
	 * @param a_data
	 *            The data
	 */
	public void setData(byte[] a_data) {
		m_data = a_data;
	}

	public void setIpAddress(InetAddress a_ipAdress) {
		m_ipAddress = a_ipAdress;
	}

	public InetAddress getIpAddress() {
		return m_ipAddress;
	}

	public int getPort() {
		return m_port;
	}

	public void setPort(int a_port) {
		m_port = a_port;
	}
}
