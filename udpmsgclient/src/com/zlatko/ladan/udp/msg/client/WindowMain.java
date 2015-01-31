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
import javax.swing.JScrollBar;

public class WindowMain {
	private JFrame m_frame = null;
	private JTextField m_textFieldInput = null;
	private StringBuilder m_msgsText = new StringBuilder();

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
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		final UDPclient udpClient = new UDPclient();
		JButton btnOk = new JButton("OK");
		final JTextPane textPaneOutput = new JTextPane();
		JScrollPane jScrollPane = new JScrollPane(textPaneOutput);
		m_frame = new JFrame();

		textPaneOutput.setEditable(false);

		m_frame.setTitle("UDP Messaging Client");
		m_frame.setBounds(100, 100, 450, 300);
		m_frame.setMinimumSize(new Dimension(450, 300));
		m_frame.getRootPane().setDefaultButton(btnOk);
		m_frame.getContentPane().add(jScrollPane, BorderLayout.CENTER);
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a_arg) {
				try {
					udpClient.Send(String.format("MSG:%s;",
							m_textFieldInput.getText()));
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
					return;
				}
				m_textFieldInput.setText("");
			}
		});
		m_frame.getContentPane().add(btnOk, BorderLayout.SOUTH);

		m_textFieldInput = new JTextField();
		m_frame.getContentPane().add(m_textFieldInput, BorderLayout.NORTH);
		m_textFieldInput.setColumns(10);

		try {
			udpClient.Connect("localhost", 1616);
		} catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}

		try {
			udpClient.Send(String.format("CONN:JUICE-%x;",
					(int) (1000d + Math.random() * 1001d)));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}

		try {
			if (!udpClient.Receive().equals("OK;")) {
				System.out.println("Noo, couldn't connect, bye!");
				System.exit(1);
				return;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
			return;
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				String serverData = null;
				String[] msgData = null;
				while (true) {
					try {
						serverData = udpClient.Receive();
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
						return;
					}
					if (serverData.equals("1;")) {
						try {
							udpClient.Send("1;");
						} catch (IOException e) {
							e.printStackTrace();
						}
						continue;
					}
					System.out.println(serverData);
					if (!serverData
							.matches("MSG:[A-Z0-9a-z.,_\\-]{3,20}:[\\p{L}\\p{Cntrl}\\p{Punct}\\d\\s]{1,400};")) {
						continue;
					}
					msgData = serverData.substring(4).split(":", 2);

					final String data = String.format("%s wrote:%n%s\n",
							msgData[0],
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
