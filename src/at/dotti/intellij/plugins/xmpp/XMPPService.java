package at.dotti.intellij.plugins.xmpp;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.parsing.ExceptionLoggingCallback;
import org.jivesoftware.smack.parsing.UnparsablePacket;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.filetransfer.*;

import javax.net.ssl.*;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;

public class XMPPService implements ChatManagerListener, RosterListener, ConnectionListener, ChatMessageListener, FileTransferListener {

	private static XMPPService service;

	private AbstractXMPPConnection connection;

	private Project project;

	private java.util.Timer timer;

	public static synchronized void create(Project project) throws IOException, XMPPException, SmackException {
		if (service == null) {
			service = new XMPPService(project);
		}
	}

	public static XMPPService getService() {
		return service;
	}

	private SettingsBean settings = ServiceManager.getService(SettingsBean.class);

	private XMPPService(Project project) throws IOException, XMPPException, SmackException {
		this.project = project;
		startup();
	}

	private void startup() throws IOException, XMPPException, SmackException {
		this.connection = startup(settings.getUsername(), settings.getPassword(), settings.getServer(), settings.getPort(), settings.getServiceName(), settings.getEncryption());
		addConnectionListener(this);
		getChatManager().addChatListener(this);
		getRoster().addRosterListener(this);
		FileTransferManager.getInstanceFor(this.connection).addFileTransferListener(this);
	}

	private static AbstractXMPPConnection startup(String username, String password, String server, Short port, String serviceName, String encryption) throws IOException, XMPPException, SmackException {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}

		} };

		SSLContext sc = null;
		try {
			if (encryption != null) {
				sc = SSLContext.getInstance(encryption);
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}

		HostnameVerifier allHostsValid = (hostname, session) -> true;

		XMPPTCPConnectionConfiguration config = null;
		if (encryption == null || encryption.equals("NO")) {
			config = XMPPTCPConnectionConfiguration.builder().setUsernameAndPassword(username, password).setServiceName(serviceName).setHost(server).setPort(port).setResource("IDEA").setSecurityMode(ConnectionConfiguration.SecurityMode.disabled).build();
		} else {
			List<String> cy = new ArrayList<>();
			if (encryption.equals("TLS")) {
				cy.add("TLS");
			} else {
			}
			config = XMPPTCPConnectionConfiguration.builder().setUsernameAndPassword(username, password).setServiceName(serviceName).setHost(server).setPort(port).setResource("IDEA").setSecurityMode(ConnectionConfiguration.SecurityMode.required).setCustomSSLContext(sc).setHostnameVerifier(allHostsValid).build();
		}

		AbstractXMPPConnection con = new XMPPTCPConnection(config);
		AbstractXMPPConnection connection = con.connect();
		connection.setParsingExceptionCallback(new ExceptionLoggingCallback() {
			@Override
			public void handleUnparsablePacket(UnparsablePacket unparsed) throws Exception {
				System.err.println("error on packet: " + unparsed.getContent());
				unparsed.getParsingException().printStackTrace();
			}
		});
		return connection;
	}

	public void addConnectionListener(ConnectionListener c) {
		this.connection.addConnectionListener(c);
	}

	public void login() throws IOException, XMPPException, SmackException {
		this.connection.login();
	}

	public Roster getRoster() {
		return Roster.getInstanceFor(this.connection);
	}

	public ChatManager getChatManager() {
		return ChatManager.getInstanceFor(this.connection);
	}

	public String getSelf() {
		String user = getSelf(getUser());
		return user;
	}

	public String getUser() {
		Presence p = getRoster().getPresence(this.connection.getUser());
		String user = p.getFrom();
		return user;
	}

	public String getSelf(String user) {
		RosterEntry e = getRoster().getEntry(user);
		if (e != null) {
			return e.getName();
		}
		return user;
	}

	public void close() {
		getChatManager().removeChatListener(this);
		getRoster().removeRosterListener(this);
		connection.removeConnectionListener(this);
		connection.disconnect();
	}

	public Chat openChat(String user) {
		return getChatManager().createChat(user);
	}

	public static void test(String username, String password, String server, Short port, String serviceName, String encryption) throws XMPPException, IOException, SmackException {
		AbstractXMPPConnection connection = startup(username, password, server, port, serviceName, encryption);
		connection.login();
	}

	public void reconnect() throws IOException, XMPPException, SmackException {
		this.connection.connect();
	}

	public void transfer(String user, File file) throws SmackException {
		OutgoingFileTransfer ft = FileTransferManager.getInstanceFor(this.connection).createOutgoingFileTransfer(user);
		ft.sendFile(file, file.getName());
	}

	private synchronized ToolWindow activateToolWindow(Runnable runnable) {
		ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(XMPPToolWindowFactory.ID);
		if (tw != null && !tw.isVisible()) {
			SwingUtilities.invokeLater(() -> tw.activate(runnable, true));
		} else if (tw != null && tw.isVisible()) {
			SwingUtilities.invokeLater(runnable);
		}
		return tw;
	}

	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		chat.addMessageListener(this);
		activateToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			while (tool == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			tool.chatCreated(chat, createdLocally);
		});
	}

	@Override
	public void entriesAdded(Collection<String> addresses) {
		createToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			if (tool != null) {
				tool.entriesAdded(addresses);
			}
		});
	}

	@Override
	public void entriesDeleted(Collection<String> addresses) {
		createToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			tool.entriesDeleted(addresses);
		});
	}

	@Override
	public void entriesUpdated(Collection<String> addresses) {
		createToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			tool.entriesUpdated(addresses);
		});
	}

	@Override
	public void presenceChanged(Presence presence) {
		createToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			if (tool != null) {
				tool.presenceChanged(presence);
			}
		});
	}

	@Override
	public void connected(XMPPConnection connection) {
		activateToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			tool.connected(connection);
		});
	}

	@Override
	public void authenticated(XMPPConnection connection, boolean resumed) {
		activateToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			tool.authenticated(connection, resumed);
		});
	}

	@Override
	public void connectionClosed() {
		activateToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			tool.connectionClosed();
		});
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		activateToolWindow(() -> {
			if (this.timer == null) {
				this.timer = new java.util.Timer("xmpp-reconnect", true);
			}
			this.timer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						reconnect();
					} catch (IOException | XMPPException | SmackException e1) {
						e1.printStackTrace();
					}
				}
			}, 3000);

			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			tool.connectionClosedOnError(e);
		});
	}

	@Override
	public void reconnectionSuccessful() {
		activateToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			tool.reconnectionSuccessful();
		});
	}

	@Override
	public void reconnectingIn(int seconds) {
		activateToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			tool.reconnectingIn(seconds);
		});
	}

	@Override
	public void reconnectionFailed(Exception e) {
		activateToolWindow(() -> {
			XMPPToolWindow tool = XMPPToolWindow.getInstance();
			tool.reconnectionFailed(e);
		});
	}

	@Override
	public void processMessage(Chat chat, Message message) {
		if (message.getType() != Message.Type.chat) {
			System.out.println("skipped message: " + message);
			ExtensionElement ext = message.getExtension(ChatStateExtension.NAMESPACE);
			if (ext != null) {
				ChatState state = ((ChatStateExtension) ext).getChatState();
				switch (state) {
					case composing:
						ToolWindowManager.getInstance(project).notifyByBalloon(XMPPToolWindowFactory.ID, MessageType.INFO, chat.getParticipant() + "<br/>Is wring a message!", XMPPToolWindowFactory.JABBER, null);
						break;
				}
			}
			return;
		}
		activateToolWindow(() -> {
			if (XMPPToolWindow.getInstance() == null) {
				processMessage(chat, message);
			} else {
				XMPPToolWindow.getInstance().processMessage(chat, message);
				storeMessageIntoHistory(chat, message);
				ToolWindowManager.getInstance(project).notifyByBalloon(XMPPToolWindowFactory.ID, MessageType.INFO, chat.getParticipant() + "<br/>new message", XMPPToolWindowFactory.JABBER, e -> activateToolWindow(null));
			}
		});
	}

	private void storeMessageIntoHistory(Chat chat, Message message) {
		if (message.getBodies().size() == 1) {
			// store
			String from = chat.getParticipant();
			String msg = message.getBody();
			storeMessageIntoHistory(chat.getParticipant(), from, msg);
		}
	}

	private void storeMessageIntoHistory(String chat, String from, String msg) {
		this.settings.addMessage(System.currentTimeMillis(), chat, from, msg);
	}

	@Override
	public void fileTransferRequest(FileTransferRequest request) {
		IncomingFileTransfer transfer = null;
		try {
			JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("user.home"), request.getFileName()));
			if (fileChooser.showDialog(null, "xmpp incoming file: " + request.getFileName()) == JFileChooser.APPROVE_OPTION) {
				transfer = request.accept();
				transfer.recieveFile(fileChooser.getSelectedFile());
				while (!transfer.isDone()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
				SwingUtilities.invokeLater(() -> {
					String html = "File transfer complete! <a href='" + fileChooser.getSelectedFile().toURI() + "'>" + fileChooser.getSelectedFile().getName() + "</a><br />" + "<a href='explorer:" + fileChooser.getSelectedFile().getPath() + "'>Open in Explorer</a>";
					ToolWindowManager.getInstance(project).notifyByBalloon(XMPPToolWindowFactory.ID, MessageType.INFO, html, XMPPToolWindowFactory.JABBER, e -> {
						if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
							try {
								if (e.getDescription().startsWith("explorer:")) {
									String url = e.getDescription().replace("explorer:", "");
									Desktop.getDesktop().open(new File(url).getParentFile());
								} else {
									Desktop.getDesktop().browse(e.getURL().toURI());
								}
							} catch (IOException e1) {
								e1.printStackTrace();
							} catch (URISyntaxException e1) {
								e1.printStackTrace();
							}
						}
					});
				});

			}
		} catch (IOException e) {
			e.printStackTrace();
			if (transfer != null) {
				transfer.cancel();
			}
		} catch (SmackException e) {
			e.printStackTrace();
			if (transfer != null) {
				transfer.cancel();
			}
		}
	}

	public void setPresence(Presence.Type type) throws SmackException.NotConnectedException {
		Presence p = getRoster().getPresence(getUser());
		p.setType(type);
		this.connection.sendStanza(p);
	}

	private ToolWindow createToolWindow(Runnable runnable) {
		ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(XMPPToolWindowFactory.ID);
		if (tw == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return tw;
			}
			createToolWindow(runnable);
		} else {
			if (runnable != null) {
				runnable.run();
			}
		}
		return tw;
	}

	public void sendToContact(XMPPContact contact, String text) throws SmackException.NotConnectedException {
		Chat chat = this.openChat(contact.getUser());
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		chat.sendMessage(text);
	}

	public XMPPContact selectContact(JComponent parent) {
		XMPPContactsDialog dialog = XMPPToolWindow.getInstance().createContactsDialog(parent);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return dialog.getSelectedContact();
	}

	public void copyToContact(String text, JComponent editor) {
		activateToolWindow(() -> {
			XMPPContact contact = XMPPService.getService().selectContact(editor);
			if (contact != null) {
				SwingUtilities.invokeLater(() -> {
					try {
						XMPPService.getService().sendToContact(contact, text);
					} catch (SmackException.NotConnectedException e1) {
						e1.printStackTrace();
					}
				});
			}
		});
	}
}
