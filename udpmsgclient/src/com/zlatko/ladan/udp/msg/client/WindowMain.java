package com.zlatko.ladan.udp.msg.client;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Window;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.prefs.Preferences;

import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.JSplitPane;

import org.eclipse.wb.swing.FocusTraversalOnArray;

import com.zlatko.ladan.udp.msg.client.LoginDialog.DialogButtonPressEvent;
import com.zlatko.ladan.udp.msg.client.LoginDialog.OnDialogButtonPress;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Toolkit;

public class WindowMain extends WindowAdapter implements ActionListener,
		OnDialogButtonPress {
	private static final String REGEX_LOG_IN = "IN:[A-Z0-9a-z.,_\\-]{3,20};";
	private static final String REGEX_LOG_OUT = "OUT:[A-Z0-9a-z.,_\\-]{3,20};";
	private static final String REGEX_MESSAGE = "MSG:[A-Z0-9a-z.,_\\-]{3,20}:[\\p{L}\\p{Cntrl}\\p{Punct}\\d\\s]{1,400};";

	private static final String DIRECTORY_AUDIO = "resources/sounds";
	private static final String FILE_EXTENSION_AUDIO = "au";
	private static final String AUDIO_LOG_IN;
	private static final String AUDIO_LOG_OUT;
	private static final String AUDIO_MESSAGE;
	private static final String AUDIO_BURP;
	private static final String AUDIO_TWEET;

	private static final String WINDOW_TITLE = "UDP Messaging Client";
	private static final String MESSAGE_CONNECTION_FAILED = "Noo, couldn't connect, bye!";
	private static final String MESSAGE_SEPARATOR = ":";
	private static final String LOGIN_OK = "OK;";
	private static final String HEARTBEAT = "1;";
	private static final String DISCONNECT = "DISC;";

	private UDPclient m_udpClient = null;

	private JFrame m_frame = null;
	private JTextField m_textFieldInput = null;
	private StringBuilder m_msgsText = null;
	private JButton m_buttonOk = null;
	private JTextPane m_textPaneOutput = null;
	private boolean m_isConnected = false;
	private Clip m_clip = null;

	private final Object m_lock = new Object();

	/**
	 * Creates a string with path to an audio file.
	 *
	 * @param a_name
	 *            the name of the audio
	 * @return the audio file path
	 */
	private static String getAudioFilePath(String a_name) {
		return String.format("%s/%s.%s", DIRECTORY_AUDIO, a_name,
				FILE_EXTENSION_AUDIO);
	}

	static {
		AUDIO_LOG_IN = getAudioFilePath("log_in");
		AUDIO_LOG_OUT = getAudioFilePath("log_out");
		AUDIO_MESSAGE = getAudioFilePath("message");
		AUDIO_BURP = getAudioFilePath("burp");
		AUDIO_TWEET = getAudioFilePath("tweet");
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		final LoginDialog dialog = new LoginDialog();
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WindowMain window = new WindowMain();
					String[] prefs = window.getPreferences();
					System.out.println("z" + prefs[0] + prefs[1]);
					dialog.setInputFields(prefs[0], prefs[1]);
					dialog.setOnDiaLogPressEvent(window);
					dialog.setVisible(true);
					dialog.addWindowStateListener(window);
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
	 * Saves preferences.
	 *
	 * @param a_username
	 *            The user's name to save.
	 * @param a_host
	 *            The host's name to save.
	 */
	private void savePreferences(String a_username, String a_host) {
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		prefs.put("username", a_username);
		prefs.put("host", a_host);
	}

	/**
	 * Loads and returns the preferences.
	 *
	 * @return The preferences, first the user name and second is host.
	 */
	private String[] getPreferences() {
		Preferences prefs = Preferences.userNodeForPackage(getClass());

		return new String[] { prefs.get("username", ""), prefs.get("host", "") };
	}

	private void setIsConnected(boolean a_isConnected) {
		setIsConnected(a_isConnected, false);
	}

	private void setIsConnected(boolean a_isConnected, boolean a_isClosing) {
		synchronized (m_lock) {
			m_isConnected = a_isConnected;
		}
		// TODO: FIX THIS ONE SO THAT IT IS NICER, NICEIFY. DIALOG OR SOMETHING
		if (a_isConnected || a_isClosing) {
			return;
		}
		m_udpClient.close();
		m_frame.dispose();
	}

	private boolean getIsConnected() {
		synchronized (m_lock) {
			return m_isConnected;
		}
	}

	/**
	 * Event which is called by WindowListener
	 */
	@Override
	public void windowOpened(WindowEvent a_e) {
		((Window) a_e.getComponent()).toFront();
		m_textFieldInput.requestFocusInWindow();
	}

	@Override
	public void windowClosing(WindowEvent a_e) {
		if (a_e.getComponent() instanceof LoginDialog) {
			return;
		}
		if (getIsConnected()) {
			try {
				m_udpClient.Send(DISCONNECT);
			} catch (IOException e) {
				e.printStackTrace();
			}

			setIsConnected(false, true);
		}

		super.windowClosing(a_e);
	}

	/**
	 * Event which is called by Action listener.
	 *
	 * @param a_e
	 *            The event
	 */
	public void actionPerformed(ActionEvent a_e) {
		if (a_e.getSource() == m_buttonOk) {
			String text = m_textFieldInput.getText();

			if (text.length() < 1) {
				return;
			}

			try {
				m_udpClient.Send(String.format("MSG:%s;", text));
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
		m_frame.setIconImage(Toolkit.getDefaultToolkit().getImage(
				WindowMain.class.getResource("/resources/images/icon.png")));
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
	 * Initializes the UDP client and starts the server.
	 * <code>messageUpdate</code>.
	 *
	 * @return If connected successfully.
	 */
	private boolean InitUdp(final String a_userName, String a_host) {
		if (getIsConnected()) {
			return false;
		}

		m_udpClient = new UDPclient();
		try {
			m_udpClient.Connect(a_host, 1616);
		} catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
			return false;
		}

		try {
			m_udpClient.Send(String.format("CONN:%s;", a_userName));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		try {
			if (!m_udpClient.Receive().equals(LOGIN_OK)) {
				System.out.println(MESSAGE_CONNECTION_FAILED);
				return false;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}

		messageUpdate(m_udpClient, a_userName);
		this.setIsConnected(true);

		return true;
	}

	private enum AudioType {
		LogIn, LogOut, Message, Burp, Tweet
	}

	private void playAudio(AudioType a_audio)
			throws UnsupportedAudioFileException, IOException,
			LineUnavailableException {
		String audioFile;

		switch (a_audio) {
		case LogIn:
			audioFile = AUDIO_LOG_IN;
			break;

		case LogOut:
			audioFile = AUDIO_LOG_OUT;
			break;

		case Message:
			audioFile = AUDIO_MESSAGE;
			break;

		case Tweet:
			audioFile = AUDIO_TWEET;
			break;

		default:
			audioFile = AUDIO_BURP;
			break;
		}

		URL url = this.getClass().getClassLoader().getResource(audioFile);
		AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);

		AudioFormat format = audioIn.getFormat();
		DataLine.Info info = new DataLine.Info(Clip.class, format);

		if (m_clip != null && m_clip.isActive()) {
			m_clip.stop();
			m_clip.close();
		}

		m_clip = (Clip) AudioSystem.getLine(info);
		m_clip.open(audioIn);
		m_clip.start();
	}

	/**
	 * Starts the messaging update which fetches messages. This method starts a
	 * thread.
	 *
	 * @param udpClient
	 *            The UDP client
	 * @param userName
	 *            The user name
	 */
	private void messageUpdate(final UDPclient udpClient, final String userName) {
		(new Thread(new Runnable() {
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
						setIsConnected(false);
						return;
					}

					if (!getIsConnected()) {
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

					if (serverData.matches(REGEX_LOG_IN)
							|| serverData.matches(REGEX_LOG_OUT)) {
						handleUserEvent(textPaneOutput, serverData
								.matches(REGEX_LOG_IN) ? AudioType.LogIn
								: AudioType.LogOut, serverData.substring(
								serverData.indexOf(MESSAGE_SEPARATOR) + 1,
								serverData.length() - 1));
						continue;
					}

					if (!serverData.matches(REGEX_MESSAGE)) {
						continue;
					}

					msgData = serverData.substring(4).split(MESSAGE_SEPARATOR,
							2);
					msgData[1] = msgData[1].substring(0,
							msgData[1].length() - 1);

					handleMessage(textPaneOutput, msgData[0], msgData[1]);
				}
			}
		})).start();
	}

	private void handleUserEvent(final JTextPane a_textPaneOutput,
			final AudioType a_type, final String a_userName) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_msgsText.append(String.format("User %s logged %s.%n",
						a_userName, a_type == AudioType.LogIn ? "in" : "out"));
				a_textPaneOutput.setText(m_msgsText.toString());

				try {
					playAudio(a_type);
				} catch (UnsupportedAudioFileException | IOException
						| LineUnavailableException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void handleMessage(final JTextPane a_textPaneOutput, String a_user,
			final String a_message) {
		final String text = String.format("%s wrote:%n%s%n",
				a_user.equals(a_user) ? "You" : a_user, a_message);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_msgsText.append(text);
				a_textPaneOutput.setText(m_msgsText.toString());

				AudioType type = AudioType.Message;

				if (a_message.equals("/burp")) {
					type = AudioType.Burp;
				} else if (a_message.equals("/tweet")) {
					type = AudioType.Tweet;
				}

				try {
					playAudio(type);
				} catch (UnsupportedAudioFileException | IOException
						| LineUnavailableException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public boolean DialogButtonPressed(DialogButtonPressEvent a_e) {
		if (!a_e.getIsOkButton() || getIsConnected()) {
			return true;
		}

		if (a_e.getEventData() == null
				|| !a_e.getEventData()[0].matches("[A-Z0-9a-z.,_\\-]{3,20}")
				|| a_e.getEventData()[1].length() < 1
				|| !InitUdp(a_e.getEventData()[0], a_e.getEventData()[1])) {
			return false;
		}

		savePreferences(a_e.getEventData()[0], a_e.getEventData()[1]);

		this.m_frame.setVisible(true);
		this.m_frame.toFront();

		return true;
	}
}
