package kolohe.robots.archon;

import battlecode.common.RobotController;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

public enum ArchonState implements State {
    // prioritize creation of resource-collection units
    RESOURCE_COLLECTION,

    // prioritize creation of defensive units
    DEFEND,

    // prioritize creation of attacking units
    ATTACK,

    // like defend, but more urgent
    SURVIVE,

    // ... plus other states
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) {
        switch (this) {
            case RESOURCE_COLLECTION:
                // TODO after certain amount of resources collected, move to defend state
                return RESOURCE_COLLECTION;
            case DEFEND:
                // TODO after certain amount of defense is created, move to attack state
                return DEFEND;
            case ATTACK:
                // TODO if defense is not good enough, move to defend state
                return ATTACK;
            case SURVIVE:
                // TODO after archon is no longer in urgent danger, move to defend state
                return SURVIVE;

            default: throw new RuntimeException("Should not be here");
        }
    }
}
