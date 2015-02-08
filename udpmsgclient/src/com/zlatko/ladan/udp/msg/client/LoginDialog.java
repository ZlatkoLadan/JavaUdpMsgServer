package com.zlatko.ladan.udp.msg.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JLabel;
import java.awt.Toolkit;

public class LoginDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 2573690239598694692L;
	private final JPanel m_contentPanel;
	private JTextField m_textFieldUserName = null;
	private OnDialogButtonPress m_event = null;
	private JTextField m_textFieldHost = null;

	/**
	 * Create the dialog.
	 */
	public LoginDialog() {
		setIconImage(Toolkit.getDefaultToolkit().getImage(
				LoginDialog.class.getResource("/resources/images/icon.png")));
		setTitle("Login UdpClient");
		m_contentPanel = new JPanel();
		setResizable(false);
		setBounds(100, 100, 228, 125);
		getContentPane().setLayout(new BorderLayout());
		m_contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(m_contentPanel, BorderLayout.CENTER);
		m_contentPanel.setLayout(null);
		{
			JLabel labelLoginUserName = new JLabel("Username:");
			labelLoginUserName.setBounds(16, 12, 77, 15);
			m_contentPanel.add(labelLoginUserName);
		}
		{
			m_textFieldUserName = new JTextField();
			m_textFieldUserName.setBounds(98, 10, 114, 19);
			m_contentPanel.add(m_textFieldUserName);
			m_textFieldUserName.setColumns(10);
		}
		{
			JLabel labelHost = new JLabel("Host:");
			labelHost.setBounds(55, 36, 38, 15);
			m_contentPanel.add(labelHost);
		}
		{
			m_textFieldHost = new JTextField();
			m_textFieldHost.setBounds(98, 34, 114, 19);
			m_textFieldHost.setColumns(10);
			m_contentPanel.add(m_textFieldHost);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
				okButton.addActionListener(this);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
				cancelButton.addActionListener(this);
			}
		}
	}

	public void setInputFields(String a_userName, String a_host) {
		m_textFieldUserName.setText(a_userName);
		m_textFieldHost.setText(a_host);
	}

	public void setOnDiaLogPressEvent(OnDialogButtonPress a_event) {
		m_event = a_event;
	}

	@Override
	public void actionPerformed(ActionEvent a_e) {
		if (m_event == null) {
			return;
		}
		boolean isOk = false;
		String[] eventData = new String[2];

		if (a_e.getActionCommand().equals("OK")) {
			isOk = true;
			eventData[0] = m_textFieldUserName.getText();
			eventData[1] = m_textFieldHost.getText();
		}

		if (m_event.DialogButtonPressed(new DialogButtonPressEvent(isOk,
				eventData))) {
			dispose();
		}
	}

	public class DialogButtonPressEvent {
		private boolean m_isOkButton = false;
		private String m_eventData[] = null;

		public DialogButtonPressEvent(boolean a_isOkButton, String[] a_eventData) {
			m_eventData = a_eventData;
			m_isOkButton = a_isOkButton;
		}

		public boolean getIsOkButton() {
			return m_isOkButton;
		}

		public String[] getEventData() {
			return m_eventData;
		}
	}

	public interface OnDialogButtonPress {
		public boolean DialogButtonPressed(DialogButtonPressEvent a_e);
	}
}
