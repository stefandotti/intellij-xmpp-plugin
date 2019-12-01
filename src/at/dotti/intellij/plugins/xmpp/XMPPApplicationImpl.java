package at.dotti.intellij.plugins.xmpp;

import com.intellij.openapi.Disposable;

public class XMPPApplicationImpl implements XMPPApplicationInterface, Disposable {

    @Override
    public void dispose() {
        if (XMPPService.getService() != null) {
            XMPPService.getService().close();
        }
    }

}
