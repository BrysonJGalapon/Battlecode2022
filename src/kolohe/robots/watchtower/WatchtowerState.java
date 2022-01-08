package kolohe.robots.watchtower;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

public enum WatchtowerState implements State {
    DEFEND,
    MOVE,
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        switch (this) {
            case DEFEND:
                // TODO
                return DEFEND;
            case MOVE:
                // TODO
                return MOVE;
            default: throw new RuntimeException("Should not be here");
        }
    }
}
