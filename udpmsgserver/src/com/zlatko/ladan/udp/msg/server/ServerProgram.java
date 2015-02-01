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

	private static UDPserver m_udpServer = null;

	/**
	 * Gets the user.
	 *
	 * @param a_data
	 *            the data sent by the client.
	 * @return the user or null if the client is not logged in
	 */
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

	/**
	 * Check whether someone with the specified user name is already logged in.
	 *
	 * @param a_userName
	 *            the user name
	 * @return If someone is logged in with that name
	 */
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

	/**
	 * This is where everything starts.
	 *
	 * @param a_args
	 *            The arguments sent by console user.
	 */
	public static void main(String[] a_args) {
		m_udpServer = new UDPserver();
		try {
			m_udpServer.connect(1616);
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Connected");

		doHeartBeat(m_udpServer);

		update();
		m_udpServer.close();
	}

	/**
	 * The update method which runs the updates for the server.
	 */
	private static void update() {
		UdpData clientData = null;
		User user = null;

		while (true) {
			try {
				clientData = m_udpServer.receive();
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
							m_udpServer.send(clientData);
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
						m_udpServer.send(clientData);
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
					m_udpServer.send(clientData);
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
						m_udpServer.send(clientData);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Heart beats to the users. This method starts a thread.
	 *
	 * @param a_udpServer
	 *            the server class to send with.
	 */
	private static void doHeartBeat(final UDPserver a_udpServer) {
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
								a_udpServer.send(userData);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		})).start();
	}
}
