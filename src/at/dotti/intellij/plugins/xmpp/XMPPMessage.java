package at.dotti.intellij.plugins.xmpp;

public class XMPPMessage {

    private long timestamp;
    private String from;
    private String message;

    public XMPPMessage() {
    }

    public XMPPMessage(long timestamp, String from, String message) {
        this.timestamp = timestamp;
        this.from = from;
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFrom() {
        return from;
    }

    public String getMessage() {
        return message;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
