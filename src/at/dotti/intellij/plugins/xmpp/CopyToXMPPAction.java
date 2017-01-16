package at.dotti.intellij.plugins.xmpp;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import javax.swing.*;

public class CopyToXMPPAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null)
            return;
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            String text = editor.getSelectionModel().getSelectedText();
            SwingUtilities.invokeLater(() -> XMPPService.getService().copyToContact(text, editor.getContentComponent()));
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null)
            return;
        boolean selected = false;
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            selected = editor.getSelectionModel().hasSelection();
        }
        e.getPresentation().setEnabledAndVisible(selected);
    }
}
