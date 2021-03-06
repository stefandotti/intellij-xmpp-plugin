package at.dotti.intellij.plugins.xmpp;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@State(name = "SettingsBean", storages = {@Storage("xmpp.xml")})
public class SettingsBean implements PersistentStateComponent<SettingsBean> {

    private String username;
    private String password;
    private String server;
    private short port;
	private String serviceName;
    private String encryption;
    private boolean visible;
    private Map<String, List<XMPPMessage>> messageHistory = new HashMap<>();

    @Override
    public void loadState(@NotNull SettingsBean settingsBean) {
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

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public synchronized void addMessage(long timestamp, String from, String user, String msg) {
        if (from.lastIndexOf("/") != -1) {
            from = from.substring(0, from.lastIndexOf("/"));
        }
        List<XMPPMessage> userHistory = this.messageHistory.computeIfAbsent(from, k -> new ArrayList<>());
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

    public boolean isValid() {
        return this.server != null && this.serviceName != null;
    }

    private CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName("XMPP", key));
    }

    public void storePassword(char[] password) {
        if (server != null && username != null && password != null) {
            CredentialAttributes credentialAttributes = createCredentialAttributes(server);
            Credentials credentials = new Credentials(username, password);
            PasswordSafe.getInstance().set(credentialAttributes, credentials);
        }
    }

    public String getPassword() {
        if (this.password != null) {
            // migrate old password into PasswordSafe
            storePassword(this.password.toCharArray());
            this.password = null;
        }
        if (server != null) {
            CredentialAttributes credentialAttributes = createCredentialAttributes(server);
            return PasswordSafe.getInstance().getPassword(credentialAttributes);
        }
        return null;
    }
}