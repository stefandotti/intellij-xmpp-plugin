package at.dotti.intellij.plugins.xmpp;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

public class XMPPUsersListModel extends AbstractListModel<XMPPContact> implements Collection<XMPPContact> {

    private TreeSet<XMPPContact> contacts = new TreeSet<>();

    private static Map<XMPP.STATE, XMPPUsersListModel> model = new HashMap<>();

    public static synchronized void create(XMPP.STATE state) {
        if (model.get(state) == null) {
            model.put(state, new XMPPUsersListModel(state));
        }
    }

    public static XMPPUsersListModel getModel(XMPP.STATE state) {
        return model.get(state);
    }

    private XMPP.STATE state;

    private XMPPUsersListModel(XMPP.STATE state) {
        super();
        this.state = state;
    }

    @Override
    public int size() {
        return this.contacts.size();
    }

    @Override
    public boolean isEmpty() {
        return this.contacts.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.contacts.contains(o);
    }

    @NotNull
    @Override
    public Iterator iterator() {
        return this.contacts.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return this.contacts.toArray();
    }

    @Override
    public boolean add(XMPPContact o) {
        boolean b = this.contacts.add(o);
        fireIntervalAdded(this, this.contacts.size() - 1, this.contacts.size());
        return b;
    }

    @Override
    public boolean remove(Object o) {
        boolean b = this.contacts.remove(o);
        fireIntervalRemoved(this, this.contacts.size() - 1, this.contacts.size());
        return b;
    }

    @Override
    public boolean addAll(Collection<? extends XMPPContact> c) {
        return this.contacts.addAll(c);
    }

    @Override
    public void clear() {
        this.contacts.clear();
    }

    @Override
    public boolean retainAll(Collection c) {
        return this.contacts.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection c) {
        return this.contacts.removeAll(c);
    }

    @Override
    public boolean containsAll(Collection c) {
        return this.contacts.containsAll(c);
    }

    @NotNull
    @Override
    public Object[] toArray(Object[] a) {
        return this.contacts.toArray(a);
    }

    @Override
    public int getSize() {
        return size();
    }

    @Override
    public XMPPContact getElementAt(int index) {
        return this.contacts.toArray(new XMPPContact[]{})[index];
    }
}
