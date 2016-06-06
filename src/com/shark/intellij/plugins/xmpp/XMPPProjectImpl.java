package com.shark.intellij.plugins.xmpp;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import org.jetbrains.annotations.NotNull;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class XMPPProjectImpl implements XMPPProjectInterface, ProjectComponent {

    private Project project;

    private SettingsBean settings = ServiceManager.getService(SettingsBean.class);

    public XMPPProjectImpl(Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void initComponent() {
        try {
            XMPPService.create(this.project);
            XMPPService.getService().login();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return XMPP.class.getSimpleName();
    }
}
