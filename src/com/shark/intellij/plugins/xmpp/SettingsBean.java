package com.shark.intellij.plugins.xmpp;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.containers.HashMap;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@State(name = "SettingsBean", storages = {@Storage("xmpp.xml")})
public class SettingsBean implements PersistentStateComponent<SettingsBean> {

    private String username;
    private String password;
    private String server;
    private short port;
    private String encryption;
    private boolean visible;
    private Map<String, List<XMPPMessage>> messageHistory = new HashMap<>();

    @Override
    public void loadState(SettingsBean settingsBean) {
        XmlSerializerUtil.copyBean(settingsBean, this);
    }

    @Nullable
    @Override
    public SettingsBean getState() {
        return this;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setPort(short port) {
        this.port = port;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getServer() {
        return server;
    }

    public short getPort() {
        return port;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public synchronized void addMessage(long timestamp, String from, String user, String msg) {
        if (from.lastIndexOf("/") != -1) {
            from = from.substring(0, from.lastIndexOf("/"));
        }
        List<XMPPMessage> userHistory = this.messageHistory.get(from);
        if (userHistory == null) {
            userHistory = new ArrayList<>();
            this.messageHistory.put(from, userHistory);
        }
        userHistory.add(new XMPPMessage(timestamp, user, msg));
    }

    public List<XMPPMessage> getHistory(String from) {
        return this.messageHistory.get(from);
    }

    public void setMessageHistory(Map<String, List<XMPPMessage>> messageHistory) {
        this.messageHistory = messageHistory;
    }

    public Map<String, List<XMPPMessage>> getMessageHistory() {
        return messageHistory;
    }
}