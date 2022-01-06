package kolohe.utils;

public class Parameters {
    public static final int EXPLORER_BOREDOM_THRESHOLD = 10;

    public static final int DEFAULT_TANGENT_BUG_MAX_BACKTRACK_COUNT = 10;
    public static final int DEFAULT_WALL_DETECTION_THRESHOLD = 40;

    public static final int MINER_RESOURCE_IDENTIFICATION_BYTECODE_LIMIT = 1500;

    public static final int MINER_RECEIVE_MESSAGE_BYTECODE_LIMIT = 2000;
    public static final int BUILDER_RECEIVE_MESSAGE_BYTECODE_LIMIT = 3500;
    public static final int SOLDIER_RECEIVE_MESSAGE_BYTECODE_LIMIT = 3500;

    public static final int MINER_RECEIVE_MESSAGE_LIMIT = 10000;
    public static final int BUILDER_RECEIVE_MESSAGE_LIMIT = 10000;
    public static final int SOLDIER_RECEIVE_MESSAGE_LIMIT = 10000;
    public static final int ARCHON_RECEIVE_MESSAGE_LIMIT = 14;

    // Distribution of droid robots to build per Archon state.
    // The distribution is represented by [Miner, Builder, Sage, Soldier]
    public static final int[] ARCHON_RESOURCE_COLLECTION_BUILD_DISTRIBUTION = new int[]{3, 1, 0, 1};
    public static final int[] ARCHON_DEFEND_BUILD_DISTRIBUTION = new int[]{1, 1, 0, 3};
    public static final int[] ARCHON_ATTACK_BUILD_DISTRIBUTION = new int[]{0, 0, 1, 3};
    public static final int[] ARCHON_SURVIVE_BUILD_DISTRIBUTION = new int[]{0, 1, 1, 3};

     // Distribution of lead and gold allocation limits per ResourceAllocation state.
     // The distribution is represented by [Archon, Builder, Laboratory]
     // TODO once resource allocation state is defined, create distributions
     public static final int[] RESOURCE_ALLOCATION_RESOURCE_COLLECTION_LEAD_DISTIBUTION = new int[]{80, 20, 0};
     public static final int[] RESOURCE_ALLOCATION_DEFEND_LEAD_DISTIBUTION = new int[]{20, 70, 10};
     public static final int[] RESOURCE_ALLOCATION_ATTACK_LEAD_DISTIBUTION = new int[]{50, 40, 10};
     public static final int[] RESOURCE_ALLOCATION_SURVIVE_LEAD_DISTIBUTION = new int[]{100, 0, 0};

     public static final int[] RESOURCE_ALLOCATION_RESOURCE_COLLECTION_GOLD_DISTIBUTION = new int[]{20, 40, 40};
     public static final int[] RESOURCE_ALLOCATION_DEFEND_GOLD_DISTIBUTION = new int[]{20, 40, 40};
     public static final int[] RESOURCE_ALLOCATION_ATTACK_GOLD_DISTIBUTION = new int[]{20, 40, 40};
     public static final int[] RESOURCE_ALLOCATION_SURVIVE_GOLD_DISTIBUTION = new int[]{20, 40, 40};

     // Makes resource-utilization entities more aggressive in using resources
     public static final double RESOURCE_ALLOCATION_RESOURCE_OVERLAP_FACTOR = 1.2;

     // Thresholds that govern Archon state transitions
     public static int ARCHON_RESOURCE_COLLECTION_TO_DEFEND_ROBOT_COUNT_THRESHOLD = 15;
     public static int ARCHON_DEFEND_TO_RESOURCE_COLLECTION_ROBOT_COUNT_THRESHOLD = 7;
     public static int ARCHON_DEFEND_TO_ATTACK_ROBOT_COUNT_THRESHOLD = 20;
     public static int ARCHON_ATTACK_TO_DEFEND_ROBOT_COUNT_THRESHOLD = 10;
     public static int ARCHON_ANY_TO_SURVIVE_ROBOT_COUNT_THRESHOLD = 6;
     public static int ARCHON_SURVIVE_TO_DEFEND_ROBOT_COUNT_THRESHOLD = 15;

     // number of rounds that must pass before survival state is reached
     public static int ARCHON_SURVIVE_GRACE_PERIOD = 200;
}
