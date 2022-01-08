package kolohe.robots.sage;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

public enum SageState implements State {
    // TODO define sage state machine
    DO_NOTHING
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        switch (this) {
            case DO_NOTHING:
                return DO_NOTHING;
            default: throw new RuntimeException("Should not be here");
        }
    }
}
