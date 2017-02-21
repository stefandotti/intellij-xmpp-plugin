package at.dotti.intellij.plugins.xmpp;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import emoji4j.EmojiUtils;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.packet.Message;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatWindow extends JPanel implements HyperlinkListener {

	private final JTextPane messagePane;

	private final JTextArea inputBox;

	private Chat chat;

	private String self;

	private SettingsBean settings = ServiceManager.getService(SettingsBean.class);

	private boolean emojiSupported = false;

	public static final Pattern P_HTTP = Pattern.compile("(http:)[^\\s\\t<]+");

	private java.util.Timer timer = new Timer("highlight", true);

	private Color origColor;

	public ChatWindow(String self, Chat chat) {

		String fontFamily = "Courier";
		Font uiFont = getFont();
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for (Font font : fonts) {
			// search for a font that can display emoji
			if (font.canDisplay('\u23F0')) {
				fontFamily = font.getFontName();
				uiFont = font;
				emojiSupported = true;
			}
		}

		this.self = self;
		this.chat = chat;
		this.setLayout(new BorderLayout());
		this.messagePane = new JTextPane();
		this.messagePane.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int idx = messagePane.viewToModel(e.getPoint());
				if (idx != -1) {
					Element elem = ((DefaultStyledDocument) messagePane.getDocument()).getParagraphElement(idx);
					try {
						String text = elem.getDocument().getText(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset());
						int start = idx;
						while (start > elem.getStartOffset() && text.charAt(start - elem.getStartOffset()) != ' ') {
							start--;
						}
						Pattern P_HTTP = Pattern.compile("(http|www)[^\\s\\t<]+");
						Matcher m = P_HTTP.matcher(text.substring(start - elem.getStartOffset()));
						if (m.find()) {
							Desktop.getDesktop().browse(new java.net.URI(m.group()));
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				int idx = messagePane.viewToModel(e.getPoint());
				if (idx != -1) {
					Element elem = ((DefaultStyledDocument) messagePane.getDocument()).getParagraphElement(idx);
					try {
						String text = elem.getDocument().getText(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset());
						System.out.println(text);
					} catch (BadLocationException e1) {
					}
				}
			}
		});
		this.messagePane.setContentType("text/plain;charset=utf-8");
		this.messagePane.addHyperlinkListener(this);
		this.messagePane.setEditable(false);
		StyleContext st = new StyleContext();
		this.messagePane.setDocument(new DefaultStyledDocument(st));
		Style style = st.addStyle("self", null);
		style.addAttribute(StyleConstants.Foreground, getForeground().equals(Color.WHITE) ? getForeground().brighter() : getForeground().darker());
		style.addAttribute(StyleConstants.Underline, false);
		Style other = st.addStyle("other", null);
		other.addAttribute(StyleConstants.Foreground, getForeground());
		other.addAttribute(StyleConstants.Underline, false);
		Style link = st.addStyle("link", null);
		link.addAttribute(StyleConstants.Foreground, getForeground().equals(Color.WHITE) ? Color.BLUE : Color.BLUE.darker());
		link.addAttribute(StyleConstants.Underline, true);
		//this.messagePane.setText("<html><head></head><body id='body'></body></html>");
		//ei.getStyleSheet().addRule("div { font-size: 10px; font-family: '"+fontFamily+"'; margin: 0; padding: 0; }");
		this.messagePane.setFont(uiFont);
		this.add(new JBScrollPane(this.messagePane), BorderLayout.CENTER);
		JPanel south = new JPanel();
		south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
		this.inputBox = new JTextArea();
		south.add(new JBScrollPane(this.inputBox));
		this.add(south, BorderLayout.SOUTH);
		this.inputBox.requestFocus();
		this.inputBox.registerKeyboardAction(e -> send(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
		java.util.List<XMPPMessage> history = settings.getHistory(chat.getParticipant());
		if (history != null && history.size() > 0) {
			history.subList(Math.max(0, history.size() - 5), Math.max(0, history.size())).forEach(this::addMessage);
		}
	}

	private String emojify(String text) {
		if (this.emojiSupported) {
			StringBuilder sb = new StringBuilder();
			Matcher m = P_HTTP.matcher(text);
			int last = 0;
			while (m.find()) {
				sb.append(EmojiUtils.emojify(text.substring(last, m.start())));
				sb.append(m.group());
				last = m.end();
			}
			if (last < text.length()) {
				sb.append(EmojiUtils.emojify(text.substring(last)));
			}
			return sb.toString();
		} else {
			return text;
		}
	}

	private void addMessage(XMPPMessage historyMessage) {
		boolean self = historyMessage.getFrom().equals(this.self);
		String message = "[" + SimpleDateFormat.getDateTimeInstance().format(historyMessage.getTimestamp()) + "] " + historyMessage.getFrom() + ": " + emojify(historyMessage.getMessage());
		addMessage(message, self);
	}

	void focus() {
		this.inputBox.grabFocus();
	}

	private synchronized void send() {
		String text = this.inputBox.getText();
		try {
			chat.sendMessage(text);
			this.inputBox.setText("");
			addMessage(self + ": " + emojify(text), true);
			settings.addMessage(System.currentTimeMillis(), chat.getParticipant(), self, text);
		} catch (Exception e) {
			addMessage(e.getMessage(), true);
		}
	}

	void addMessage(Message message) {
		if (message.getBodies().size() > 0) {
			this.addMessage(XMPPService.getService().getSelf(message.getFrom()) + ": " + emojify(message.getBody()), false);
		} else {
			this.addMessage(XMPPService.getService().getSelf(message.getFrom()) + ": {binary data}", false);
		}
	}

	void addMessage(String message, boolean self) {
		SwingUtilities.invokeLater(() -> {
			if (!this.messagePane.isShowing()) {
				addMessage(message, self);
				return;
			}
			try {
				String msg = SimpleDateFormat.getTimeInstance().format(System.currentTimeMillis()) + " " + message;

				StringBuilder sb = new StringBuilder();
				sb.append(msg);

				//msg = msg.replace("<", "&lt;").replace(">", "&gt;");
				DefaultStyledDocument doc = (DefaultStyledDocument) this.messagePane.getDocument();
				//				Element body = doc.getElement("body");
				Color c = self ? getForeground().darker() : getForeground();
				String color = c.getRed() + "," + c.getGreen() + "," + c.getBlue();
				sb.append("\n");
				int pos = doc.getLength();
				int len = sb.toString().length();
				doc.insertString(pos, sb.toString(), null);
				doc.setParagraphAttributes(pos, len, doc.getStyle(self ? "self" : "other"), true);

				Matcher m = P_HTTP.matcher(msg);
				while (m.find()) {
					doc.setCharacterAttributes(pos + m.start(), m.end() + m.start(), doc.getStyle("link"), true);
				}

				Rectangle r = this.messagePane.modelToView(doc.getLength());
				if (r != null) {
					this.messagePane.scrollRectToVisible(r);
				}

				highlight();

			} catch (BadLocationException e) {
				this.messagePane.setText(message);
			}
		});
	}

	private void highlight() {
		if (this.origColor == null) {
			this.origColor = messagePane.getBackground();
		}
		this.messagePane.setBackground(JBColor.ORANGE);
		this.timer.schedule(new TimerTask() {
			@Override
			public void run() {
				messagePane.setBackground(origColor);
			}
		}, 3000);
	}

	public void setChat(Chat chat) {
		this.chat = chat;
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
