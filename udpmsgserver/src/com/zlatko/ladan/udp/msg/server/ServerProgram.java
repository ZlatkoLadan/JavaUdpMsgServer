package com.zlatko.ladan.udp.msg.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ServerProgram {
	private static final Charset CHARACTER_SET = Charset.forName("UTF-8");

	private static final String SERVER_IP_SOURCE = "https://zlatko.se/myip.php";

	private static final String REGEX_MESSAGE = "MSG:[\\p{L}\\p{Punct}\\d\\s]{1,400};";
	private static final String REGEX_CONNECT = "CONN:[A-Z0-9a-z.,_\\-]{3,20};";
	private static final String START_WITH_FILE = "FIL:";

	private static final byte[] NOT_OK = ("NOK;").getBytes(CHARACTER_SET);
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
		System.out.println("Fetching Server's IP");
		try {
			System.out.printf("Server IP is: %s.%n", getServerGlobalIp());
		} catch (IOException | IllegalArgumentException e) {
			e.printStackTrace();
			System.out.println("Couldn't get server IP");
		}

		doHeartBeat(m_udpServer);

		update();
		m_udpServer.close();
	}

	/**
	 * Fetches the users IP.
	 *
	 * @return return the IP.
	 * @throws MalformedURLException
	 *             If the set URL is malformed.
	 * @throws IOException
	 *             If an IO error occurs.
	 * @throws IllegalArgumentException
	 *             If fetch'd data is malformed.
	 */
	private static String getServerGlobalIp() throws MalformedURLException,
			IOException {
		URLConnection connection = new URL(SERVER_IP_SOURCE).openConnection();
		connection.setRequestProperty("Accept-Charset", "us-ascii");
		int chr = 0, i = 0;
		StringBuilder outputText = new StringBuilder();

		try (InputStream output = connection.getInputStream()) {
			while ((chr = output.read()) >= 0) {
				if (i++ >= 15) {
					throw new IllegalArgumentException(
							"The IP which the server got from source was malformed!!!");
				}
				outputText.append((char) chr);
			}
		}

		return outputText.toString();
	}

	/**
	 * The update method which runs the updates for the server.
	 */
	private static void update() {
		UdpData clientData = null;
		User user = null;
		String receivedText = null;

		while (true) {
			try {
				clientData = m_udpServer.receive();
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

			receivedText = new String(clientData.getData(), CHARACTER_SET);

			user = getUser(clientData);

			if (user == null) {
				if (receivedText.matches(REGEX_CONNECT)) {
					String userName = receivedText.substring(5,
							receivedText.length() - 1);
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
					userLogin(user, clientData);
				} else {
					System.out.printf(Locale.ENGLISH,
							"Client (%s:%d) tried to send message: \"%s\"!%n",
							clientData.getIpAddress().toString(),
							clientData.getPort(), receivedText);
				}
				continue;
			}
			if (receivedText.equals(HEARTBEAT)) {
				try {
					m_udpServer.send(clientData);
					user.updateLastResponse();
				} catch (IOException e) {
					e.printStackTrace();
				}
				continue;
			}
			if (receivedText.equals(USER_DISCONNECTED)) {
				synchronized (m_users) {
					userLogout(m_users.indexOf(user), false, clientData);
					continue;
				}
			}
			if (receivedText.startsWith(START_WITH_FILE)
					&& clientData.getData().length < 487 + 4) {

				user.updateLastResponse();
				byte[] data = clientData.getData();
				byte[] newArr = new byte[user.getUserName().length() + 1
						+ data.length + 1];
				int i = 0;
				byte[] dataPrefixStr = String.format(Locale.ENGLISH, "FIL:%s:",
						user.getUserName()).getBytes(CHARACTER_SET);

				for (i = 0; i < dataPrefixStr.length; ++i) {
					newArr[i] = dataPrefixStr[i];
				}
				for (int j = 4; j < data.length; ++j, ++i) {
					newArr[i] = data[j];
				}
				newArr[i] = (byte) ';';
				clientData.setData(newArr);

				System.out.printf(Locale.ENGLISH,
						"user %s sent a file with size %d!%n", user.toString(),
						i - 4 - 1 - user.getUserName().length() - 1 + 1);
				try {
					broadcastToAllUsers(clientData);
				} catch (IOException e) {
					e.printStackTrace();
				}
				continue;
			}

			if (!receivedText.matches(REGEX_MESSAGE)) {
				System.out.printf(Locale.ENGLISH,
						"user %s tried to send \"%s\"!%n", user.toString(),
						receivedText);
				continue;
			}

			System.out.printf(Locale.ENGLISH, "user %s, sent msg: \"%s\"%n",
					user.toString(), receivedText);
			user.updateLastResponse();
			clientData.setData(String.format(Locale.ENGLISH, "MSG:%s:%s",
					user.getUserName(), receivedText.substring(4)).getBytes(
					CHARACTER_SET));
			try {
				broadcastToAllUsers(clientData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void userLogin(User a_user, UdpData clientData) {
		synchronized (m_users) {
			m_users.add(a_user);
		}

		System.out.printf(Locale.ENGLISH, "User added: %s!%n",
				a_user.toString());

		clientData.setData(String.format(Locale.ENGLISH, "IN:%s;",
				a_user.getUserName()).getBytes(CHARACTER_SET));
		try {
			broadcastToAllUsers(clientData);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void userLogout(int a_index, boolean a_kicked,
			UdpData clientData) {
		User user = m_users.get(a_index);

		if (a_kicked) {
			System.out.printf(Locale.ENGLISH,
					"Removed user %s due to inactivity!%n", user.toString());
		} else {
			System.out
					.printf(Locale.ENGLISH, "Client %s disconnected!%n", user);
		}
		m_users.remove(a_index);

		clientData.setData(String.format(Locale.ENGLISH, "OUT:%s;",
				user.getUserName()).getBytes(CHARACTER_SET));
		try {
			broadcastToAllUsers(clientData);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Broadcasts to all logged in users.
	 *
	 * @param a_clientData
	 *            the data
	 * @throws IOException
	 *             If an IO error occurs
	 */
	private static void broadcastToAllUsers(UdpData a_clientData)
			throws IOException {
		User user = null;
		synchronized (m_users) {
			for (int i = 0; i < m_users.size(); ++i) {
				user = m_users.get(i);
				a_clientData.setIpAddress(user.getIpAddress());
				a_clientData.setPort(user.getPort());
				m_udpServer.send(a_clientData);
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
								userLogout(i, true, userData);
								--i;
								continue;
							}
							userData = new UdpData(HEARTBEAT
									.getBytes(CHARACTER_SET), lUser
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
