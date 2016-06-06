package com.shark.intellij.plugins.xmpp;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import org.jetbrains.annotations.NotNull;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class XMPPToolWindow implements RosterListener, ChatManagerListener, ConnectionListener, ChatMessageListener {

	private static XMPPToolWindow INSTANCE;

	private JBList listact;

	private ToolWindow tw;

	private JBScrollPane sp;

	private Project project;

	private Map<Chat, ChatWindow> chatMap = new HashMap<>();

	public static final XMPPToolWindow getInstance() {
		return INSTANCE;
	}

	public static final void create(Project project, ToolWindow toolWindow) {
		INSTANCE = new XMPPToolWindow(project, toolWindow);
	}

	private XMPPToolWindow(Project project, ToolWindow toolWindow) {
		this.tw = toolWindow;
		this.project = project;
		SimpleToolWindowPanel contactsPanel = new SimpleToolWindowPanel(true, false);
		ActionToolbar toolbar = createToolbar();
		toolbar.setTargetComponent(contactsPanel);
		contactsPanel.setToolbar(toolbar.getComponent());
		XMPPUsersListModel.create(XMPP.STATE.ACTIVE);
		XMPPUsersListModel.create(XMPP.STATE.INACTIVE);
		JPanel p = new JPanel(new VerticalFlowLayout(FlowLayout.LEFT));
		listact = new JBList((ListModel) XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE));
		listact.setEmptyText("no online contacts");
		listact.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					openChat();
				}
			}
		});
		listact.setTransferHandler(new TransferHandler() {
			@Override
			public boolean canImport(TransferSupport support) {
				return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || support.isDataFlavorSupported(DataFlavor.stringFlavor);
			}

			@Override
			public boolean importData(TransferSupport support) {
				if (!support.isDrop()) {
					return false;
				}
				try {
					XMPPContact contact = (XMPPContact) listact.getSelectedValue();
					if (contact != null) {
						String user = contact.getUser();
						System.out.println("testing");
						if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
							System.out.println("files");
							java.util.List data = (java.util.List) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
							for (Object obj : data) {
								if (obj instanceof File) {
									File file = (File) obj;
									Presence p = XMPPService.getService().getRoster().getPresence(user);
									XMPPService.getService().transfer(p.getFrom(), file);
									openChat();
									SwingUtilities.invokeLater(() -> {
										ChatWindow chatWindow = getChatWindow(user);
										chatWindow.addMessage("file sent: " + file.getPath(), true);
									});
								}
							}
						} else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
							Object td = support.getTransferable().getTransferData(DataFlavor.stringFlavor);
							Chat chat = openChat();
							chat.sendMessage((String) td);
							SwingUtilities.invokeLater(() -> {
								ChatWindow chatWindow = getChatWindow(user);
								chatWindow.addMessage((String) td, true);
							});
						}
					}
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SmackException e) {
					e.printStackTrace();
				}
				return super.importData(support);
			}
		});
		listact.registerKeyboardAction(e -> openChat(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
		JBList listina = new JBList((ListModel) XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE));
		listina.setEmptyText("no offline contacts");
		listina.setEnabled(false);
		p.add(listact);
		p.add(listina);
		sp = new JBScrollPane(p);
		sp.getVerticalScrollBar().setUnitIncrement(10);
		contactsPanel.setContent(sp);
		Content contacts = ContentFactory.SERVICE.getInstance().createContent(contactsPanel, "Contacts", false);
		contacts.setCloseable(false);
		contacts.setToolwindowTitle("Contacts");
		contacts.setIcon(XMPPToolWindowFactory.JABBER);
		toolWindow.getContentManager().addContent(contacts);
		toolWindow.getContentManager().setSelectedContent(contacts);

		toolWindow.getContentManager().addContentManagerListener(new ContentManagerAdapter() {
			@Override
			public void contentRemoved(ContentManagerEvent event) {
				JComponent c = event.getContent().getComponent();
				if (c instanceof ChatWindow) {
					((ChatWindow) c).close();
				}
			}
		});

		if (XMPPService.getService() != null) {
			reconnect();
		}
	}

	@Override
	public void connected(XMPPConnection connection) {
	}

	@Override
	public void authenticated(XMPPConnection connection, boolean resumed) {
		reconnect();
	}

	@Override
	public void connectionClosed() {
		XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE).clear();
		XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE).clear();
		listact.setEmptyText("connection closed");
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE).clear();
		XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE).clear();
		listact.setEmptyText("connection closed: " + e);
	}

	@Override
	public void reconnectionSuccessful() {
		reconnect();
	}

	@Override
	public void reconnectingIn(int seconds) {

	}

	@Override
	public void reconnectionFailed(Exception e) {

	}

	@Override
	public void entriesAdded(Collection<String> addresses) {
		SwingUtilities.invokeLater(() -> addresses.forEach(address -> {
			RosterEntry re = XMPPService.getService().getRoster().getEntry(address);
			XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE).add(new XMPPContact(re));
		}));
	}

	@Override
	public void entriesUpdated(Collection<String> addresses) {
	}

	@Override
	public void entriesDeleted(Collection<String> addresses) {
		SwingUtilities.invokeLater(() -> {
			addresses.forEach(address -> {
				RosterEntry re = XMPPService.getService().getRoster().getEntry(address);
				XMPPContact c = new XMPPContact(re);
				XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE).remove(c);
			});
			addresses.forEach(address -> {
				RosterEntry re = XMPPService.getService().getRoster().getEntry(address);
				XMPPContact c = new XMPPContact(re);
				XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE).remove(c);
			});
		});
	}

	@Override
	public void presenceChanged(Presence presence) {
		RosterEntry re = XMPPService.getService().getRoster().getEntry(presence.getFrom());
		XMPPContact c = new XMPPContact(re);
		if (presence.getType() == Presence.Type.available) {
			SwingUtilities.invokeLater(() -> {
				if (XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE).contains(c)) {
					XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE).remove(c);
				}
				XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE).add(c);
				ToolWindowManager.getInstance(project).notifyByBalloon(XMPPToolWindowFactory.ID, MessageType.INFO, "Contact '" + c + "' is now " + presence.getType(), XMPPToolWindowFactory.JABBER, null);
			});
		} else {
			SwingUtilities.invokeLater(() -> {
				if (XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE).contains(c)) {
					XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE).remove(c);
				}
				XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE).add(c);
				ToolWindowManager.getInstance(project).notifyByBalloon(XMPPToolWindowFactory.ID, MessageType.INFO, "Contact '" + c + "' is now " + presence.getType(), XMPPToolWindowFactory.JABBER, null);
			});
		}
	}

	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		ChatWindow chatWindow = new ChatWindow(XMPPService.getService().getSelf(), chat);
		chatMap.put(chat, chatWindow);
		Content messages = ContentFactory.SERVICE.getInstance().createContent(chatWindow, chat.getParticipant(), false);
		messages.setIcon(XMPPToolWindowFactory.MESSAGE);
		tw.getContentManager().addContent(messages);
		tw.getContentManager().setSelectedContent(messages);
		if (!ToolWindowManager.getInstance(project).getToolWindow(XMPPToolWindowFactory.ID).isVisible()) {
			ToolWindowManager.getInstance(project).getToolWindow(XMPPToolWindowFactory.ID).show(null);
		}
		SwingUtilities.invokeLater(() -> chatWindow.focus());
	}

	private void reconnect() {
		sp.setColumnHeaderView(new JBLabel(XMPPService.getService().getSelf(), JBLabel.RIGHT));
		Roster roster = XMPPService.getService().getRoster();
		SwingUtilities.invokeLater(() -> {
			XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE).clear();
			XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE).clear();
			for (RosterEntry entry : roster.getEntries()) {
				Presence p = roster.getPresence(entry.getUser());
				RosterEntry re = XMPPService.getService().getRoster().getEntry(p.getFrom());
				XMPPContact c = new XMPPContact(re);
				if (p.isAvailable()) {
					XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE).add(c);
				} else {
					XMPPUsersListModel.getModel(XMPP.STATE.INACTIVE).add(c);
				}
			}
		});
	}

	private Chat openChat() {
		XMPPContact contact = (XMPPContact) listact.getSelectedValue();
		if (contact != null) {
			String user = contact.getUser();
			for (int i = 0; i < this.tw.getContentManager().getContentCount(); i++) {
				if (this.tw.getContentManager().getContent(i).getTabName().equals(user)) {
					final int x = i;
					SwingUtilities.invokeLater(() -> this.tw.getContentManager().setSelectedContent(this.tw.getContentManager().getContent(x)));
					return ((ChatWindow) this.tw.getContentManager().getContent(x).getComponent()).getChat();
				}
			}
			return XMPPService.getService().openChat(user);
		}
		return null;
	}

	private ChatWindow getChatWindow(String user) {
		for (int i = 0; i < this.tw.getContentManager().getContentCount(); i++) {
			if (this.tw.getContentManager().getContent(i).getTabName().equals(user)) {
				return ((ChatWindow) this.tw.getContentManager().getContent(i).getComponent());
			}
		}
		return null;
	}

	private ActionToolbar createToolbar() {
		DefaultActionGroup group = new DefaultActionGroup();
		group.add(new DumbAwareAction("Connect", "Connect", AllIcons.Actions.Redo) {
			@Override
			public void actionPerformed(AnActionEvent anActionEvent) {
				try {
					if (XMPPService.getService() == null) {
						XMPPService.create(project);
						XMPPService.getService().login();
					}
					XMPPService.getService().reconnect();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (XMPPException e) {
					e.printStackTrace();
				} catch (SmackException e) {
					e.printStackTrace();
				}
			}
		});
		ComboBoxAction status = new ComboBoxAction() {
			@NotNull
			@Override
			protected DefaultActionGroup createPopupActionGroup(JComponent jComponent) {
				DefaultActionGroup statusActionGroup = new DefaultActionGroup();
				String status = Presence.Type.available.name();
				statusActionGroup.add(new DumbAwareAction(status, status, XMPPToolWindowFactory.ONLINE) {
					@Override
					public void actionPerformed(AnActionEvent anActionEvent) {
						setPresence(Presence.Type.available);
					}
				});
				status = Presence.Type.unavailable.name();
				statusActionGroup.add(new DumbAwareAction(status, status, XMPPToolWindowFactory.OFFLINE) {
					@Override
					public void actionPerformed(AnActionEvent anActionEvent) {
						setPresence(Presence.Type.unavailable);
					}
				});
				return statusActionGroup;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void update(AnActionEvent e) {
				final Presentation presentation = e.getPresentation();
				updateText(presentation);
			}

			/**
			 * Update the text for the combobox
			 *
			 * @param presentation the presentaiton to update
			 */
			private void updateText(Presentation presentation) {
				if (XMPPService.getService() != null) {
					String t = XMPPService.getService().getRoster().getPresence(XMPPService.getService().getUser()).getType().name();
					presentation.setText(t);
				}
			}
		};
		group.add(status);
		group.add(new DumbAwareAction("History", "History", AllIcons.Actions.ShowViewer) {
			@Override
			public void actionPerformed(AnActionEvent anActionEvent) {
				createHistoryWindow();
			}

			@Override
			public void update(AnActionEvent e) {
				e.getPresentation().setEnabled(!listact.isSelectionEmpty());
			}
		});
		ActionToolbar bar = ActionManager.getInstance().createActionToolbar("unknown", group, true);
		return bar;
	}

	private void createHistoryWindow() {
		XMPPContact contact = (XMPPContact) listact.getSelectedValue();
		if (contact != null) {
			String user = contact.getUser();
			HistoryWindow historyWindow = new HistoryWindow(XMPPService.getService().getSelf(), user);
			Content messages = ContentFactory.SERVICE.getInstance().createContent(historyWindow, "history: " + user, false);
			messages.setIcon(XMPPToolWindowFactory.MESSAGE);
			tw.getContentManager().addContent(messages);
			tw.getContentManager().setSelectedContent(messages);
			if (!ToolWindowManager.getInstance(project).getToolWindow(XMPPToolWindowFactory.ID).isVisible()) {
				ToolWindowManager.getInstance(project).getToolWindow(XMPPToolWindowFactory.ID).show(null);
			}
			SwingUtilities.invokeLater(() -> historyWindow.focus());
		}
	}

	private void setPresence(Presence.Type type) {
		try {
			XMPPService.getService().setPresence(type);
		} catch (SmackException.NotConnectedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processMessage(Chat chat, Message message) {
		ChatWindow chatWindow = this.chatMap.get(chat);
		if (chatWindow == null) {
			SwingUtilities.invokeLater(() -> processMessage(chat, message));
		} else {
			chatWindow.addMessage(message);
		}
	}

	public XMPPContactsDialog createContactsDialog(JComponent parent) {
		return new XMPPContactsDialog(parent);
	}
}