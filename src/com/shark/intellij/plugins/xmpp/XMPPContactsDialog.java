package com.shark.intellij.plugins.xmpp;

import org.jdesktop.swingx.JXDialog;
import org.jdesktop.swingx.JXList;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class XMPPContactsDialog extends JXDialog {

    private XMPPContact selectedContact;
    private final JXList list;

    public XMPPContactsDialog(JComponent parent) {
        super(parent);
        this.setModal(true);
        list = new JXList(XMPPUsersListModel.getModel(XMPP.STATE.ACTIVE));
        this.getContentPane().add(list);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    selectAndClose();
                }
            }
        });
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
        this.getRootPane().registerKeyboardAction(e -> selectAndClose(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
        SwingUtilities.invokeLater(() -> {
            if (list.isSelectionEmpty() && list.getModel().getSize() > 0) {
                list.setSelectedIndex(0);
            }
            list.grabFocus();
        });
    }

    private void selectAndClose() {
        if (!list.isSelectionEmpty()) {
            selectedContact = (XMPPContact) list.getSelectedValue();
            dispose();
        }
    }

    public XMPPContact getSelectedContact() {
        return this.selectedContact;
    }
}
