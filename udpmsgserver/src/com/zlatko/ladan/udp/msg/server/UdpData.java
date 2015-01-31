package com.zlatko.ladan.udp.msg.server;

import java.net.InetAddress;

public class UdpData {
	private String m_data;
	private InetAddress m_ipAddress;
	private int m_port;

	public UdpData(String a_data, InetAddress a_ipAddress, int a_port) {
		m_data = a_data;
		m_ipAddress = a_ipAddress;
		m_port = a_port;
	}

	public String getData() {
		return m_data;
	}
	
	public void setData(String a_data) {
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
