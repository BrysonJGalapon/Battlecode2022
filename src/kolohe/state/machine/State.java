package kolohe.state.machine;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public interface State {
    State react(Stimulus stimulus, RobotController rc) throws GameActionException;
}
