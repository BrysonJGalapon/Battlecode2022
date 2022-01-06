package kolohe.robots.archon;

import battlecode.common.RobotController;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

import java.util.Optional;

import static kolohe.utils.Parameters.*;

public enum ArchonState implements State {
    // prioritize creation of resource-collection units
    RESOURCE_COLLECTION(0),

    // prioritize creation of defensive units
    DEFEND(1),

    // prioritize creation of attacking units
    ATTACK(2),

    // like defend, but more urgent
    SURVIVE(3),

    // ... plus other states
    ;

    private final int encoding;

    ArchonState(int encoding) {
        this.encoding = encoding;
    }

    public int encode() {
        return encoding;
    }

    public static Optional<ArchonState> decode(int encoding) {
        for (ArchonState archonState : ArchonState.values()) {
            if (archonState.encoding == encoding) {
                return Optional.of(archonState);
            }
        }

        return Optional.empty();
    }

    @Override
    public State react(Stimulus stimulus, RobotController rc) {
        // TODO NOTE: basing transitions on total robot count is cheap (20 bytecode) and requires no communication,
        //  but can greatly skew the distribution of robots that ACTUALLY get created.
        //
        // TODO: Absolutely need to factor in local stimulus to make these transitions

        int robotCount = rc.getRobotCount();
        int archonCount = rc.getArchonCount();
        int averageRobotCountPerArchon = robotCount / archonCount;

//        System.out.println("Average Robot Count per Archon: " + averageRobotCountPerArchon);

        int roundNum = rc.getRoundNum();

        switch (this) {
            case RESOURCE_COLLECTION:
                if (averageRobotCountPerArchon < ARCHON_ANY_TO_SURVIVE_ROBOT_COUNT_THRESHOLD && roundNum > ARCHON_SURVIVE_GRACE_PERIOD) {
                    Archon.buildDistribution = ARCHON_SURVIVE_BUILD_DISTRIBUTION;
                    return SURVIVE;
                }

                if (averageRobotCountPerArchon > ARCHON_RESOURCE_COLLECTION_TO_DEFEND_ROBOT_COUNT_THRESHOLD) {
                    Archon.buildDistribution = ARCHON_DEFEND_BUILD_DISTRIBUTION;
                    return DEFEND;
                }

                Archon.buildDistribution = ARCHON_RESOURCE_COLLECTION_BUILD_DISTRIBUTION;
                return RESOURCE_COLLECTION;
            case DEFEND:
                if (averageRobotCountPerArchon < ARCHON_ANY_TO_SURVIVE_ROBOT_COUNT_THRESHOLD && roundNum > ARCHON_SURVIVE_GRACE_PERIOD) {
                    Archon.buildDistribution = ARCHON_SURVIVE_BUILD_DISTRIBUTION;
                    return SURVIVE;
                }

                if (averageRobotCountPerArchon < ARCHON_DEFEND_TO_RESOURCE_COLLECTION_ROBOT_COUNT_THRESHOLD) {
                    Archon.buildDistribution = ARCHON_RESOURCE_COLLECTION_BUILD_DISTRIBUTION;
                    return RESOURCE_COLLECTION;
                }

                if (averageRobotCountPerArchon > ARCHON_DEFEND_TO_ATTACK_ROBOT_COUNT_THRESHOLD) {
                    Archon.buildDistribution = ARCHON_ATTACK_BUILD_DISTRIBUTION;
                    return ATTACK;
                }

                Archon.buildDistribution = ARCHON_DEFEND_BUILD_DISTRIBUTION;
                return DEFEND;
            case ATTACK:
                if (averageRobotCountPerArchon < ARCHON_ANY_TO_SURVIVE_ROBOT_COUNT_THRESHOLD && roundNum > ARCHON_SURVIVE_GRACE_PERIOD) {
                    Archon.buildDistribution = ARCHON_SURVIVE_BUILD_DISTRIBUTION;
                    return SURVIVE;
                }

                if (averageRobotCountPerArchon < ARCHON_ATTACK_TO_DEFEND_ROBOT_COUNT_THRESHOLD) {
                    Archon.buildDistribution = ARCHON_DEFEND_BUILD_DISTRIBUTION;
                    return DEFEND;
                }

                Archon.buildDistribution = ARCHON_ATTACK_BUILD_DISTRIBUTION;
                return ATTACK;
            case SURVIVE:
                if (averageRobotCountPerArchon > ARCHON_SURVIVE_TO_DEFEND_ROBOT_COUNT_THRESHOLD) {
                    Archon.buildDistribution = ARCHON_DEFEND_BUILD_DISTRIBUTION;
                    return DEFEND;
                }

                Archon.buildDistribution = ARCHON_SURVIVE_BUILD_DISTRIBUTION;
                return SURVIVE;

            default: throw new RuntimeException("Should not be here");
        }
    }
}
