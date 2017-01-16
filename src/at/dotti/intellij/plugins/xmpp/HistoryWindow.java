package at.dotti.intellij.plugins.xmpp;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.ui.components.JBScrollPane;
import emoji4j.EmojiUtils;

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
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryWindow extends JPanel implements HyperlinkListener {

    private final JTextPane messagePane;
    private String from;
    private String self;
    private SettingsBean settings = ServiceManager.getService(SettingsBean.class);
	boolean emojiSupported = false;

    public HistoryWindow(String self, String from) {

		String fontFamily = "Courier";
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for (Font font: fonts) {
			// search for a font that can display emoji
			if (font.canDisplay('\u23F0')) {
				fontFamily = font.getFontName();
				emojiSupported = true;
			}
		}

		this.self = self;
        this.from = from;
        this.setLayout(new BorderLayout());
        this.messagePane = new JTextPane();
		HTMLEditorKit ei;
		this.messagePane.setEditorKit(ei = new HTMLEditorKit());
        this.messagePane.addHyperlinkListener(this);
        this.messagePane.setEditable(false);
		this.messagePane.setText("<html><head></head><body id='body'></body></html>");
		ei.getStyleSheet().addRule("div { font-size: 10px; font-family: '"+fontFamily+"'; margin: 0; padding: 0; }");
        this.add(new JBScrollPane(this.messagePane), BorderLayout.CENTER);
        if (from.lastIndexOf("/") != -1) {
            from = from.substring(0, from.lastIndexOf("/"));
        }
        java.util.List<XMPPMessage> history = this.settings.getHistory(from);
        if (history != null) {
            history.forEach(this::addMessage);
        }
    }

    public void focus() {
        this.messagePane.grabFocus();
    }

    private String emojify(String text) {
		return this.emojiSupported ? EmojiUtils.emojify(text): text;
	}

    private void addMessage(XMPPMessage historyMessage) {
        boolean self = historyMessage.getFrom().equals(this.self);
        String message = historyMessage.getFrom() + ": " + historyMessage.getMessage();
        SwingUtilities.invokeLater(() -> {
            try {
                String msg = SimpleDateFormat.getTimeInstance().format(historyMessage.getTimestamp()) + " " + emojify(message);
                SimpleAttributeSet styles = new SimpleAttributeSet();
                if (self) {
                    StyleConstants.setForeground(styles, getForeground().darker());
                }
                msg = msg.replace("<", "&lt;").replace(">", "&gt;");
                StringBuilder sb = new StringBuilder();
                Pattern P_HTTP = Pattern.compile("http:[^\\s\\t<]+");
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
                doc.insertBeforeEnd(body, "<div style='color: rgb(" + color + ")'>" + sb + "</pre>");
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
