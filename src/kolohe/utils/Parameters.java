package kolohe.utils;

public class Parameters {
    public static final int DEFAULT_TANGENT_BUG_MAX_BACKTRACK_COUNT = 10;
    public static final int DEFAULT_WALL_DETECTION_THRESHOLD = 40;

    public static final int MINER_RECEIVE_MESSAGE_LIMIT = 7;

    // Distribution of droid robots to build per Archon state.
    // The distribution is represented by [Miner, Builder, Sage, Soldier]
    public static final int[] ARCHON_RESOURCE_COLLECTION_BUILD_DISTRIBUTION = new int[]{3, 1, 0, 1};
    public static final int[] ARCHON_DEFEND_BUILD_DISTRIBUTION = new int[]{1, 2, 0, 2};
    public static final int[] ARCHON_ATTACK_BUILD_DISTRIBUTION = new int[]{0, 2, 1, 3};
    public static final int[] ARCHON_SURVIVE_BUILD_DISTRIBUTION = new int[]{0, 0, 1, 3};

    // Thresholds that govern Archon state transitions
    public static int ARCHON_RESOURCE_COLLECTION_TO_DEFEND_ROBOT_COUNT_THRESHOLD = 30;
    public static int ARCHON_DEFEND_TO_RESOURCE_COLLECTION_ROBOT_COUNT_THRESHOLD = 15;
    public static int ARCHON_DEFEND_TO_ATTACK_ROBOT_COUNT_THRESHOLD = 75;
    public static int ARCHON_ATTACK_TO_DEFEND_ROBOT_COUNT_THRESHOLD = 35;
    public static int ARCHON_ANY_TO_SURVIVE_ROBOT_COUNT_THRESHOLD = 8;
    public static int ARCHON_SURVIVE_TO_DEFEND_ROBOT_COUNT_THRESHOLD = 30;

    // number of rounds that must pass before survival state is reached
    public static int ARCHON_SURVIVE_GRACE_PERIOD = 200;
}
