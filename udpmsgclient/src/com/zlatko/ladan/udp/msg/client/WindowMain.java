package com.zlatko.ladan.udp.msg.client;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.JSplitPane;

import org.eclipse.wb.swing.FocusTraversalOnArray;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class WindowMain extends WindowAdapter implements ActionListener {
	private static final String REGEX_MESSAGE = "MSG:[A-Z0-9a-z.,_\\-]{3,20}:[\\p{L}\\p{Cntrl}\\p{Punct}\\d\\s]{1,400};";

	private static final String MESSAGE_CONNECTION_FAILED = "Noo, couldn't connect, bye!";
	private static final String WINDOW_TITLE = "UDP Messaging Client";
	private static final String MESSAGE_SEPARATOR = ":";
	private static final String LOGIN_OK = "OK;";
	private static final String HEARTBEAT = "1;";

	private UDPclient m_udpClient = null;

	private JFrame m_frame = null;
	private JTextField m_textFieldInput = null;
	private StringBuilder m_msgsText = null;
	private JButton m_buttonOk = null;
	private JTextPane m_textPaneOutput = null;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WindowMain window = new WindowMain();
					window.m_frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WindowMain() {
		initialize();
		InitUdp();
	}

	@Override
	public void windowOpened(WindowEvent a_e) {
		m_textFieldInput.requestFocusInWindow();
	}

	public void actionPerformed(ActionEvent a_e) {
		if (a_e.getSource() == m_buttonOk) {
			try {
				m_udpClient.Send(String.format("MSG:%s;",
						m_textFieldInput.getText()));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
				return;
			}
			m_textFieldInput.setText("");
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		m_frame = new JFrame();
		m_textFieldInput = new JTextField();
		m_msgsText = new StringBuilder();
		m_buttonOk = new JButton("OK");
		m_textPaneOutput = new JTextPane();

		JSplitPane splitPane = new JSplitPane();
		JScrollPane jScrollPane = new JScrollPane(m_textPaneOutput);

		m_frame.addWindowListener(this);
		m_frame.setTitle(WINDOW_TITLE);
		m_frame.setBounds(100, 100, 450, 300);
		m_frame.setMinimumSize(new Dimension(450, 300));
		m_frame.getRootPane().setDefaultButton(m_buttonOk);
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		m_frame.getContentPane().add(jScrollPane, BorderLayout.CENTER);
		m_frame.getContentPane().add(splitPane, BorderLayout.SOUTH);

		m_frame.setFocusTraversalPolicy(new FocusTraversalOnArray(
				new Component[] { m_textPaneOutput, m_textFieldInput,
						m_buttonOk, m_frame.getContentPane(), jScrollPane,
						splitPane }));

		m_textFieldInput.setColumns(30);
		m_textPaneOutput.setEditable(false);
		m_buttonOk.addActionListener(this);
		splitPane.setRightComponent(m_buttonOk);
		splitPane.setLeftComponent(m_textFieldInput);
	}

	/**
	 * Initializes the UDP client and starts the server starts
	 * <code>messageUpdate</code>.
	 */
	private void InitUdp() {
		m_udpClient = new UDPclient();
		try {
			m_udpClient.Connect("localhost", 1616);
		} catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}
		final String userName = String.format("JUICE-%x",
				(int) (1000d + Math.random() * 1001d));

		try {
			m_udpClient.Send(String.format("CONN:%s;", userName));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}

		try {
			if (!m_udpClient.Receive().equals(LOGIN_OK)) {
				System.out.println(MESSAGE_CONNECTION_FAILED);
				System.exit(1);
				return;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
			return;
		}

		messageUpdate(m_udpClient, userName);
	}

	/**
	 * Starts the messaging update which fetches messages
	 *
	 * @param udpClient
	 *            The UDP client
	 * @param userName
	 *            The user name
	 */
	private void messageUpdate(final UDPclient udpClient, final String userName) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String serverData = null;
				String[] msgData = null;
				final JTextPane textPaneOutput = m_textPaneOutput;
				while (true) {
					try {
						serverData = udpClient.Receive();
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
						return;
					}
					if (serverData.equals(HEARTBEAT)) {
						try {
							udpClient.Send(HEARTBEAT);
						} catch (IOException e) {
							e.printStackTrace();
						}
						continue;
					}
					System.out.println(serverData);
					if (!serverData.matches(REGEX_MESSAGE)) {
						continue;
					}
					msgData = serverData.substring(4).split(MESSAGE_SEPARATOR,
							2);

					final String data = String.format("%s wrote:%n%s%n",
							msgData[0].equals(userName) ? "You" : msgData[0],
							msgData[1].substring(0, msgData[1].length() - 1));
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							m_msgsText.append(data);
							textPaneOutput.setText(m_msgsText.toString());
						}
					});
				}
			}
		}).start();
	}
}
