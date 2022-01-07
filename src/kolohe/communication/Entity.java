package kolohe.communication;

import java.util.Optional;

// TODO dayne
public enum Entity {
    ALL_ROBOTS(1), // index 0-6
    ALL_MINERS(2), // 7-13
    ALL_ARCHONS(3), // 14-20
    ALL_BUILDERS(4), // 21-27
    ALL_LABORATORIES(5), // 28-34
    ALL_SAGES(6), // 35-41
    ALL_SOLDIERS(7), // 42-48
    ALL_WATCHTOWERS(8),
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
