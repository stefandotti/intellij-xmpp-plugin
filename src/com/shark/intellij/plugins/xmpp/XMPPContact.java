package com.shark.intellij.plugins.xmpp;

import org.jivesoftware.smack.roster.RosterEntry;

public class XMPPContact implements Comparable<XMPPContact> {

    private String DEFAULT_GROUP = "DEFAULT_GROUP";

    private RosterEntry re;

    public XMPPContact(RosterEntry re) {
        this.re = re;
    }

    @Override
    public String toString() {
        String leftGroup = this.re.getGroups().size() > 0 ? this.re.getGroups().get(0).getName() : "";
        return (this.re.getName() != null ? this.re.getName() : this.re.getUser()) + " ["+leftGroup+"]";
    }

    @Override
    public int compareTo(XMPPContact o) {
        String leftGroup = this.re.getGroups().size() > 0 ? this.re.getGroups().get(0).getName() : DEFAULT_GROUP;
        String rightGroup = o.re.getGroups().size() > 0 ? o.re.getGroups().get(0).getName() : DEFAULT_GROUP;
        int c = leftGroup.compareTo(rightGroup);
        return c == 0 ? this.re.getName() != null && o.re.getName() != null ? this.re.getName().toLowerCase().compareTo(o.re.getName().toLowerCase()) : this.re.getUser().compareTo(o.re.getUser()) : c;
    }

    public String getUser() {
        return this.re.getUser();
    }
}
