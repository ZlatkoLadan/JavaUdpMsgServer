package com.zlatko.ladan.udp.msg.server;

import java.net.InetAddress;
import java.util.Locale;

public class User {
	private String m_userName = null;
	InetAddress m_ipAddress = null;
	private int m_port = 0;
	private long m_lastResponse = 0l;

	public User(String a_userName, InetAddress a_ipAddress, int a_port) {
		setUserName(a_userName);
		m_ipAddress = a_ipAddress;
		m_port = a_port;
	}

	public void setUserName(String a_userName) {
		m_userName = a_userName;
	}

	public String getUserName() {
		return m_userName;
	}

	public InetAddress getIpAddress() {
		return m_ipAddress;
	}

	public int getPort() {
		return m_port;
	}
	
	public void updateLastResponse() {
		m_lastResponse = System.currentTimeMillis();
	}
	
	public long getLastResponse() {
		return m_lastResponse;
	}

	@Override
	public String toString() {
		return String.format(Locale.ENGLISH, "%s, %s:%d", this.m_userName,
				this.m_ipAddress.toString(), this.m_port);
	}
}
