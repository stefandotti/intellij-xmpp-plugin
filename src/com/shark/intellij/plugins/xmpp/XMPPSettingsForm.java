package com.shark.intellij.plugins.xmpp;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import javax.swing.*;
import java.io.IOException;

public class XMPPSettingsForm implements Configurable {

    public XMPPSettingsForm() {
        testConnectionButton.addActionListener(e -> {
            try {
                textArea1.setVisible(true);
                XMPPService.test(username.getText(), password.getText(), server.getText(), Short.valueOf(port.getText()), TLSRadioButton.isSelected() ? "TLS" : "NO");
                textArea1.setText("success");
            } catch (XMPPException e1) {
                e1.printStackTrace();
                textArea1.setText(e1.getMessage());
            } catch (IOException e1) {
                e1.printStackTrace();
                textArea1.setText(e1.getMessage());
            } catch (SmackException e1) {
                e1.printStackTrace();
                textArea1.setText(e1.getMessage());
            }
        });
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "XMPP Settings";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        SettingsBean bean = ServiceManager.getService(SettingsBean.class);
        username.setText(bean.getUsername());
        password.setText(bean.getPassword());
        server.setText(bean.getServer());
        port.setText(String.valueOf(bean.getPort()));
        TLSRadioButton.setSelected(bean.getEncryption().equals("TLS"));
        noEncryptionRadioButton.setSelected(bean.getEncryption().equals("NO"));
        return panel;
    }

    @Override
    public boolean isModified() {
        SettingsBean bean = ServiceManager.getService(SettingsBean.class);
        if (!username.getText().equals(bean.getUsername())) {
            return true;
        } else if (!password.getText().equals(bean.getPassword())) {
            return true;
        } else if (!server.getText().equals(bean.getServer())) {
            return true;
        } else if (!port.getText().equals(bean.getPort())) {
            return true;
        } else if (!TLSRadioButton.isSelected() != bean.getEncryption().equals("TLS")) {
            return true;
        } else if (!noEncryptionRadioButton.isSelected() != bean.getEncryption().equals("NO")) {
            return true;
        }
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        SettingsBean bean = ServiceManager.getService(SettingsBean.class);
        bean.setUsername(username.getText());
        bean.setPassword(password.getText());
        bean.setServer(server.getText());
        bean.setPort(Short.parseShort(port.getText()));
        bean.setEncryption(TLSRadioButton.isSelected()?"TLS":"NO");
    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }

    private JTextField username;
    private JTextField password;
    private JTextField server;
    private JTextField port;
    private JRadioButton TLSRadioButton;
    private JRadioButton noEncryptionRadioButton;
    private JPanel panel;
    private JButton testConnectionButton;
    private JTextArea textArea1;

    public void setData(SettingsBean data) {
        System.out.println(data);
    }

    public void getData(SettingsBean data) {
        System.out.println(data);
    }

    public boolean isModified(SettingsBean data) {
        return false;
    }
}
