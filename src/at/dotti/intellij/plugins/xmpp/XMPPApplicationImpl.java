package at.dotti.intellij.plugins.xmpp;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

public class XMPPApplicationImpl implements XMPPApplicationInterface, ApplicationComponent {
    @Override
    public void initComponent() {
        /*try {
            XMPPService.create();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (SmackException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void disposeComponent() {
        if (XMPPService.getService() != null) {
            XMPPService.getService().close();
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return XMPP.class.getSimpleName();
    }
}
