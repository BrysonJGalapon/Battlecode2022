package kolohe.robots.laboratory;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

public enum LaboratoryState implements State {
    TRANSMUTE,
    MOVE,
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        switch (this) {
            case TRANSMUTE:
                // TODO
                return TRANSMUTE;
            case MOVE:
                // TODO
                return MOVE;
            default: throw new RuntimeException("Should not be here");
        }
    }
}
