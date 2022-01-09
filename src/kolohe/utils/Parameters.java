package kolohe.utils;

public class Parameters {
    public static final int EXPLORER_BOREDOM_THRESHOLD = 10;

    public static final int DEFAULT_TANGENT_BUG_MAX_BACKTRACK_COUNT = 10;
    public static final int DEFAULT_WALL_DETECTION_THRESHOLD = 40;

    public static final int MINER_RESOURCE_IDENTIFICATION_BYTECODE_LIMIT = 1500;

    public static final int MINER_RECEIVE_MESSAGE_BYTECODE_LIMIT = 1500;
    public static final int BUILDER_RECEIVE_MESSAGE_BYTECODE_LIMIT = 3500;
    public static final int SOLDIER_RECEIVE_MESSAGE_BYTECODE_LIMIT = 3500;
    public static final int LABORATORY_RECEIVE_MESSAGE_BYTECODE_LIMIT = 3000;

    public static final int MINER_RECEIVE_MESSAGE_LIMIT = 10000;
    public static final int BUILDER_RECEIVE_MESSAGE_LIMIT = 10000;
    public static final int SOLDIER_RECEIVE_MESSAGE_LIMIT = 10000;
    public static final int LABORATORY_RECEIVE_MESSAGE_LIMIT = 10000;
    public static final int ARCHON_RECEIVE_MESSAGE_LIMIT = 14;

    // Distribution of droid robots to build per Archon state.
    // The distribution is represented by [Miner, Builder, Sage, Soldier]
    public static final int[] ARCHON_RESOURCE_COLLECTION_BUILD_DISTRIBUTION = new int[]{3, 1, 0, 1};
    public static final int[] ARCHON_DEFEND_BUILD_DISTRIBUTION = new int[]{1, 1, 0, 3};
    public static final int[] ARCHON_ATTACK_BUILD_DISTRIBUTION = new int[]{1, 1, 0, 6};
    public static final int[] ARCHON_SURVIVE_BUILD_DISTRIBUTION = new int[]{1, 1, 0, 4};

    // Distribution of lead and gold allocation limits per ResourceAllocation state.
    // The distribution is represented by [Archon, Builder, Laboratory]
    public static final int[] RESOURCE_ALLOCATION_RESOURCE_COLLECTION_LEAD_DISTIBUTION = new int[]{80, 20, 0};
    public static final int[] RESOURCE_ALLOCATION_DEFEND_LEAD_DISTIBUTION = new int[]{20, 70, 10};
    public static final int[] RESOURCE_ALLOCATION_ATTACK_LEAD_DISTIBUTION = new int[]{85, 5, 10};
    public static final int[] RESOURCE_ALLOCATION_SURVIVE_LEAD_DISTIBUTION = new int[]{100, 0, 0};

    public static final int[] RESOURCE_ALLOCATION_RESOURCE_COLLECTION_GOLD_DISTIBUTION = new int[]{50, 50, 0};
    public static final int[] RESOURCE_ALLOCATION_DEFEND_GOLD_DISTIBUTION = new int[]{20, 80, 0};
    public static final int[] RESOURCE_ALLOCATION_ATTACK_GOLD_DISTIBUTION = new int[]{95, 5, 0};
    public static final int[] RESOURCE_ALLOCATION_SURVIVE_GOLD_DISTIBUTION = new int[]{100, 0, 0};

    // Makes resource-utilization entities more aggressive in using resources
    public static final double RESOURCE_ALLOCATION_RESOURCE_OVERLAP_FACTOR = 1.5;


    // Thresholds that govern Archon state transitions
    // invariants:
    //      - ARCHON_RESOURCE_COLLECTION_TO_DEFEND_ROBOT_COUNT_THRESHOLD > ARCHON_DEFEND_TO_RESOURCE_COLLECTION_ROBOT_COUNT_THRESHOLD
    //      - ARCHON_DEFEND_TO_ATTACK_ROBOT_COUNT_THRESHOLD > ARCHON_DEFEND_TO_ATTACK_ROBOT_COUNT_THRESHOLD
    //      - ARCHON_SURVIVE_TO_DEFEND_ROBOT_COUNT_THRESHOLD  == ARCHON_RESOURCE_COLLECTION_TO_DEFEND_ROBOT_COUNT_THRESHOLD
    //      - ARCHON_ANY_TO_SURVIVE_ROBOT_COUNT_THRESHOLD is the smallest
    public static final int ARCHON_RESOURCE_COLLECTION_TO_DEFEND_ROBOT_COUNT_THRESHOLD = 15;
    public static final int ARCHON_DEFEND_TO_RESOURCE_COLLECTION_ROBOT_COUNT_THRESHOLD = 7;
    public static final int ARCHON_DEFEND_TO_ATTACK_ROBOT_COUNT_THRESHOLD = 25;
    public static final int ARCHON_ATTACK_TO_DEFEND_ROBOT_COUNT_THRESHOLD = 10;
    public static final int ARCHON_ANY_TO_SURVIVE_ROBOT_COUNT_THRESHOLD = 6;
    public static final int ARCHON_SURVIVE_TO_DEFEND_ROBOT_COUNT_THRESHOLD = 15;

    // number of rounds that must pass before survival state is reached
    public static final int ARCHON_SURVIVE_GRACE_PERIOD = 200;

    // number of turns before a laboratory decides to force-update its primary archon location
    public static final int LABORATORY_PRIMARY_ARCHON_LOCATION_SHELF_LIFE = 50;

//    // expirations
//    public static final int NEVER = 2000;
//    public static final int BUILD_WATCHTOWER_LOCATION_MESSAGE_SHELF_LIFE = 20;
//    public static final int GOLD_LOCATION_MESSAGE_SHELF_LIFE = 20;
//    public static final int LEAD_LOCATION_MESSAGE_SHELF_LIFE = 20;

}
