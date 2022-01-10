package kimo.robots.laboratory;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import kimo.state.machine.State;
import kimo.state.machine.Stimulus;

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
