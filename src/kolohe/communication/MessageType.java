package kolohe.communication;

import java.util.Optional;

// TODO dayne
public enum MessageType {
    GOLD_LOCATION(1),
    LEAD_LOCATION(2),
    NO_RESOURCES_LOCATION(3),
    // ....

    ;

    private final int encoding;

    MessageType(int encoding) {
        this.encoding = encoding;
    }

    public int encode() {
        return encoding;
    }

    public static Optional<MessageType> decode(int encoding) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.encoding == encoding) {
                return Optional.of(messageType);
            }
        }

        return Optional.empty();
    }
}
