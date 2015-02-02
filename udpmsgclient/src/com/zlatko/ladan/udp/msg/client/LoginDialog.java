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

public class LoginDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 2573690239598694692L;
	private final JPanel contentPanel = new JPanel();
	private JTextField textFieldUserName = null;
	private OnDialogButtonPress m_event = null;

	/**
	 * Create the dialog.
	 */
	public LoginDialog() {
		setResizable(false);
		setBounds(100, 100, 228, 79);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JLabel labelLoginUserName = new JLabel("Username:");
			contentPanel.add(labelLoginUserName);
		}
		{
			textFieldUserName = new JTextField();
			contentPanel.add(textFieldUserName);
			textFieldUserName.setColumns(10);
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

	public void setOnDiaLogPressEvent(OnDialogButtonPress a_event) {
		m_event = a_event;
	}

	@Override
	public void actionPerformed(ActionEvent a_e) {
		if (m_event == null) {
			return;
		}
		boolean isOk = false;
		String eventData = null;

		if (a_e.getActionCommand().equals("OK")) {
			isOk = true;
			eventData = textFieldUserName.getText();
		}

		if (m_event.DialogButtonPressed(new DialogButtonPressEvent(isOk, eventData))) {
			dispose();
		}
	}

	public class DialogButtonPressEvent {
		private boolean m_isOkButton = false;
		private String m_eventData = null;

		public DialogButtonPressEvent(boolean a_isOkButton, String a_eventData) {
			m_eventData = a_eventData;
			m_isOkButton = a_isOkButton;
		}

		public boolean getIsOkButton() {
			return m_isOkButton;
		}

		public String getEventData() {
			return m_eventData;
		}
	}

	public interface OnDialogButtonPress {
		public boolean DialogButtonPressed(DialogButtonPressEvent a_e);
	}
}
