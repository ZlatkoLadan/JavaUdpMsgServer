package com.zlatko.ladan.udp.msg.server;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JTextPane;
import javax.swing.JTextField;

public class WindowMain {

	private JFrame m_frame;
	private JTextField m_textFieldInput;

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
		m_frame = new JFrame();
		m_frame.setBounds(100, 100, 450, 300);
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final JTextPane textPaneInput = new JTextPane();
		textPaneInput.setEditable(false);
		m_frame.getContentPane().add(textPaneInput, BorderLayout.CENTER);

		JButton btnOk = new JButton("OK");
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a_arg) {
				try {
					udpClient.Send(m_textFieldInput.getText());
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
			udpClient.Send(String.format("CONN:JEW%d;",
					(int) (100d + Math.random() * 101d)));
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
					final String data = serverData;
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							textPaneInput.setText(textPaneInput.getText()
									+ "\n" + data);
						}
					});
				}
			}
		}).start();
	}
}
