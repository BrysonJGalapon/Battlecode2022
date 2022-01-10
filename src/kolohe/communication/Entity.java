package kolohe.communication;

import java.util.Optional;

public enum Entity {
    ALL_ROBOTS(0),
    ALL_MINERS(1),
    ALL_BUILDERS(2),
    // ...
    ;

    private final int encoding;

    Entity(int encoding) {
        this.encoding = encoding;
    }

    public int encode() {
        return encoding;
    }

    public static Optional<Entity> decode(long encoding) {
        for (Entity entity : Entity.values()) {
            if (entity.encoding == encoding) {
                return Optional.of(entity);
            }
        }

        return Optional.empty();
    }
}
