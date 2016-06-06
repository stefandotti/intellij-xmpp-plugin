package com.shark.intellij.plugins.xmpp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class XMPPToolWindowFactory implements ToolWindowFactory {

    public static final String ID = "XMPP";

    public static final Icon JABBER = IconLoader.getIcon("/icons/jabber.png");
    public static final Icon ONLINE = IconLoader.getIcon("/icons/jabber.png");
    public static final Icon OFFLINE = IconLoader.getIcon("/icons/jabber.png");

    public static final Icon MESSAGE = IconLoader.getIcon("/icons/message.png");

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        XMPPToolWindow.create(project, toolWindow);
    }

}