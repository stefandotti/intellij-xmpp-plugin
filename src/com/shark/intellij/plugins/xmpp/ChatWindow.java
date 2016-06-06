package com.shark.intellij.plugins.xmpp;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.ui.components.JBScrollPane;
import emoji4j.EmojiUtils;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.packet.Message;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatWindow extends JPanel implements HyperlinkListener {

	private final JTextPane messagePane;

	private final JTextArea inputBox;

	private Chat chat;

	private String self;

	private SettingsBean settings = ServiceManager.getService(SettingsBean.class);

	public ChatWindow(String self, Chat chat) {
		this.self = self;
		this.chat = chat;
		this.setLayout(new BorderLayout());
		this.messagePane = new JTextPane();
		this.messagePane.setContentType("text/html");
		this.messagePane.setEditorKit(new HTMLEditorKit());
		this.messagePane.addHyperlinkListener(this);
		this.messagePane.setEditable(false);
		this.messagePane.setText("<html><head><style type='text/css'>pre { margin: 0; padding: 0; }</style></head><body id='body'></body></html>");
		this.add(new JBScrollPane(this.messagePane), BorderLayout.CENTER);
		this.inputBox = new JTextArea();
		this.add(new JBScrollPane(this.inputBox), BorderLayout.SOUTH);
		this.inputBox.requestFocus();
		this.inputBox.registerKeyboardAction(e -> send(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
		java.util.List<XMPPMessage> history = settings.getHistory(chat.getParticipant());
		if (history != null && history.size() > 0) {
			history.subList(Math.max(0, history.size() - 5), Math.max(0, history.size() - 1)).forEach(this::addMessage);
		}
	}

	private void addMessage(XMPPMessage historyMessage) {
		boolean self = historyMessage.getFrom().equals(this.self);
		String message = "[" + SimpleDateFormat.getDateTimeInstance().format(historyMessage.getTimestamp()) + "] " + historyMessage.getFrom() + ": " + historyMessage.getMessage();
		addMessage(message, self);
	}

	public void focus() {
		this.inputBox.grabFocus();
	}

	private synchronized void send() {
		String text = this.inputBox.getText();
		try {
			chat.sendMessage(text);
			this.inputBox.setText("");
			addMessage(self + ": " + text, true);
			settings.addMessage(System.currentTimeMillis(), chat.getParticipant(), self, text);
		} catch (SmackException.NotConnectedException e) {
			addMessage(e.getMessage(), true);
		}
	}

	public void addMessage(Message message) {
		if (message.getBodies().size() > 0) {
			this.addMessage(XMPPService.getService().getSelf(message.getFrom()) + ": " + message.getBody(), false);
		} else {
			this.addMessage(XMPPService.getService().getSelf(message.getFrom()) + ": {binary data}", false);
		}
	}

	public void addMessage(String message, boolean self) {
		SwingUtilities.invokeLater(() -> {
			if (!this.messagePane.isShowing()) {
				addMessage(message, self);
				return;
			}
			try {
				String msg = SimpleDateFormat.getTimeInstance().format(System.currentTimeMillis()) + " " + EmojiUtils.hexHtmlify(message);
				SimpleAttributeSet styles = new SimpleAttributeSet();
				if (self) {
					StyleConstants.setForeground(styles, getForeground().darker());
				}
				msg = msg.replace("<", "&lt;").replace(">", "&gt;");
				StringBuilder sb = new StringBuilder();
				Pattern P_HTTP = Pattern.compile("http(s)?:[^\\s\\t<]+");
				Matcher m = P_HTTP.matcher(msg);
				int last = 0;
				while (m.find()) {
					sb.append(msg.substring(last, m.start()));
					sb.append("<a href='").append(m.group()).append("'>").append(m.group()).append("</a>");
					last = m.end();
				}
				if (last < msg.length()) {
					sb.append(msg.substring(last));
				}
				HTMLDocument doc = (HTMLDocument) this.messagePane.getDocument();
				Element body = doc.getElement("body");
				Color c = self ? getForeground().darker() : getForeground();
				String color = c.getRed() + "," + c.getGreen() + "," + c.getBlue();
				doc.insertBeforeEnd(body, "<pre style='color: rgb(" + color + ")'>" + sb + "</pre>");

				Rectangle r = this.messagePane.modelToView(doc.getLength());
				if (r != null) {
					this.messagePane.scrollRectToVisible(r);
				}
			} catch (BadLocationException e) {
				this.messagePane.setText(message);
			} catch (IOException e) {
				this.messagePane.setText(message);
			}
		});
	}

	public void close() {
		this.chat.close();
	}

	public Chat getChat() {
		return chat;
	}

	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		try {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				Desktop.getDesktop().browse(e.getURL().toURI());
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
	}
}
