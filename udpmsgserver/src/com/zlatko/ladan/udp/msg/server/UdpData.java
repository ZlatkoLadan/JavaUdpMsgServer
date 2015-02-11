package com.zlatko.ladan.udp.msg.server;

import java.net.InetAddress;

//TODO: Add more to comments
public class UdpData {
	private String m_data;
	private byte[] m_rawData;
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
	public UdpData(String a_data, InetAddress a_ipAddress, int a_port) {
		m_data = a_data;
		m_ipAddress = a_ipAddress;
		m_port = a_port;
	}

	/**
	 * Gets the data.
	 *
	 * @return The data
	 */
	public String getData() {
		return m_data;
	}

	/**
	 * Sets the data.
	 *
	 * @param a_data
	 *            The data
	 */
	public void setData(String a_data) {
		m_data = a_data;
	}

	/**
	 * Sets the raw data.
	 *
	 * @param a_data
	 *            The data
	 */
	public byte[] getRawData() {
		return m_rawData;
	}

	/**
	 * Sets the raw data.
	 *
	 * @param a_data
	 *            The data
	 */
	public void setRawData(byte[] a_data) {
		m_rawData = a_data;
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
