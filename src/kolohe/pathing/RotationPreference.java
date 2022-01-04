package kolohe.pathing;

public enum RotationPreference {
    LEFT,
    RIGHT,
    RANDOM;

    public static RotationPreference[] CONCRETE_ROTATION_PREFERENCES = new RotationPreference[]{LEFT, RIGHT};

    public RotationPreference opposite() {
        switch (this) {
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            default: throw new RuntimeException("Shouldn't be here");
        }
    }
}
