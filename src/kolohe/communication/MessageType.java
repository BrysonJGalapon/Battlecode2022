package kolohe.communication;

import java.util.Optional;

public enum MessageType {
    LEAD_LOCATION(1),
    GOLD_LOCATION(2),
    NO_RESOURCES_LOCATION(3),
    BUILD_WATCHTOWER_LOCATION(4),
    BUILD_LABORATORY_LOCATION(5),
    ARCHON_STATE(6),
//    GOLD_LOCATION(1), // miner
//    LEAD_LOCATION(2),
//    NO_RESOURCES_LOCATION(3),
//    GOLD_RECLAIM_LOCATION(4),
//    LEAD_DEPOSIT_LOCATION(5),
//    BUILDABLE_LOCAITON(6), // builder
//    REPAIR_REQUIRED_LOCATION(7),
//    ENEMY_DROID_LOCATION(8), // solider
//    NO_DROID_LOCATION(9),
//    ENEMY_ARCHON_LOCATION(10), // possibly have another type or addition to encoding abt level/health
//    ENEMY_LABORATORY_LOCATION(11),
//    ENEMY_WATCHTOWER_LOCATION(12),
//    ALLY_LABORATORY_LOCATION(13), // ally types
//    ALLY_ARCHON_STATE(14),
    // ....

    ;

    private final int encoding;

    MessageType(int encoding) {
        this.encoding = encoding;
    }

    public int encode() {
        return encoding;
    }

    public static Optional<MessageType> decode(long encoding) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.encoding == encoding) {
                return Optional.of(messageType);
            }
        }

        return Optional.empty();
    }
}
