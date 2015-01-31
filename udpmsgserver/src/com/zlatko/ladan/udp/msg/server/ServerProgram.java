package com.zlatko.ladan.udp.msg.server;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ServerProgram {
	private static final String REGEX_MESSAGE = "MSG:[\\p{L}\\p{Punct}\\d\\s]{1,400};";
	private static final String REGEX_CONNECT = "CONN:[A-Z0-9a-z.,_\\-]{3,20};";

	private static final String OK = "OK;";
	private static final String NOT_OK = "NOK;";
	private static final String HEARTBEAT = "1;";
	private static final String USER_DISCONNECTED = "DISC;";

	private static final List<User> m_users = Collections
			.synchronizedList(new ArrayList<User>());

	private static User getUser(UdpData a_data) {
		User user = null;
		synchronized (m_users) {
			for (int i = 0; i < m_users.size(); ++i) {
				user = m_users.get(i);

				if (a_data.getIpAddress().equals(user.m_ipAddress)
						&& a_data.getPort() == user.getPort()) {
					return user;
				}
			}
		}
		return null;
	}

	private static boolean userLoggedIn(String a_userName) {
		synchronized (m_users) {
			for (User user : m_users) {
				if (a_userName.equals(user.getUserName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static void main(String[] a_args) {
		final UDPserver udpServer = new UDPserver();
		UdpData clientData = null;
		User user = null;
		try {
			udpServer.connect(1616);
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Connected");

		(new Thread(new Runnable() {
			@Override
			public void run() {
				UdpData userData = null;
				User lUser = null;
				while (true) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					synchronized (m_users) {
						for (int i = 0; i < m_users.size(); ++i) {
							lUser = m_users.get(i);
							if (lUser.getLastResponse() < System
									.currentTimeMillis() - 30000l) {
								System.out.printf(Locale.ENGLISH,
										"Removed user %s due to inactivity!%n",
										lUser.toString());
								m_users.remove(i);
								--i;
								continue;
							}
							userData = new UdpData(HEARTBEAT, lUser
									.getIpAddress(), lUser.getPort());
							try {
								udpServer.send(userData);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		})).start();

		while (true) {
			try {
				clientData = udpServer.receive();
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

			user = getUser(clientData);

			if (user == null) {
				if (clientData.getData().matches(REGEX_CONNECT)) {
					String userName = clientData.getData().substring(5,
							clientData.getData().length() - 1);
					if (userLoggedIn(userName)) {
						System.out
								.printf(Locale.ENGLISH,
										"Client (%s:%d) tried to login, name \"%s\" was taken!%n",
										clientData.getIpAddress().toString(),
										clientData.getPort(), userName);
						clientData.setData(NOT_OK);
						try {
							udpServer.send(clientData);
						} catch (IOException e) {
							e.printStackTrace();
						}
						continue;
					}

					user = new User(userName, clientData.getIpAddress(),
							clientData.getPort());
					user.updateLastResponse();
					synchronized (m_users) {
						m_users.add(user);
					}
					clientData.setData(OK);
					try {
						udpServer.send(clientData);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.printf(Locale.ENGLISH, "User added: %s!%n",
							user.toString());
				} else {
					System.out.printf(Locale.ENGLISH,
							"Client (%s:%d) tried to send message: \"%s\"!%n",
							clientData.getIpAddress().toString(),
							clientData.getPort(), clientData.getData());
				}
				continue;
			}
			if (clientData.getData().equals(HEARTBEAT)) {
				try {
					udpServer.send(clientData);
					user.updateLastResponse();
				} catch (IOException e) {
					e.printStackTrace();
				}
				continue;
			}
			if (clientData.getData().equals(USER_DISCONNECTED)) {
				synchronized (m_users) {
					for (int i = 0; i < m_users.size(); ++i) {
						if (user.equals(m_users.get(i))) {
							System.out.printf(Locale.ENGLISH,
									"Client %s disconnected!%n", user);
							m_users.remove(i);
						}
					}
					continue;
				}
			}

			if (!clientData.getData().matches(REGEX_MESSAGE)) {
				System.out.printf(Locale.ENGLISH,
						"user %s tried to send \"%s\"!", user.toString(),
						clientData.getData());
				continue;
			}

			System.out.printf(Locale.ENGLISH, "user %s, sent msg: \"%s\"%n",
					user.toString(), clientData.getData());
			user.updateLastResponse();
			try {
				clientData.setData(String.format(Locale.ENGLISH, "MSG:%s:%s",
						user.getUserName(), clientData.getData().substring(4)));
				synchronized (m_users) {
					for (int i = 0; i < m_users.size(); ++i) {
						user = m_users.get(i);
						clientData.setIpAddress(user.getIpAddress());
						clientData.setPort(user.getPort());
						udpServer.send(clientData);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		udpServer.close();
	}
}
