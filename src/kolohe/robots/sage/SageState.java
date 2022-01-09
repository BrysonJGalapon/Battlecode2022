package kolohe.robots.sage;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

public enum SageState implements State {
    EXPLORE,
    ATTACK
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        switch (this) {
            case EXPLORE:
                // TODO
                return EXPLORE;
            case ATTACK:
                // TODO
                return ATTACK;
            default: throw new RuntimeException("Should not be here");
        }
    }
}
