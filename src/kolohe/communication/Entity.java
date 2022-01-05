package kolohe.communication;

import java.util.Optional;

// TODO dayne
public enum Entity {
    ALL_ROBOTS(1),
    ALL_MINERS(2),
    ALL_ARCHONS(3),
    ALL_BUILDERS(4),
    ALL_LABORATORIES(5),
    ALL_SAGES(6),
    ALL_SOLDIERS(7),
    // ...
    ;

    private final int encoding;

    Entity(int encoding) {
        this.encoding = encoding;
    }

    public int encode() {
        return encoding;
    }

    public static Optional<Entity> decode(int encoding) {
        for (Entity entity : Entity.values()) {
            if (entity.encoding == encoding) {
                return Optional.of(entity);
            }
        }

        return Optional.empty();
    }
}
