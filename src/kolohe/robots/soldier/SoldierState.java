package kolohe.robots.soldier;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

public enum SoldierState implements State {
    EXPLORE,
    DEFEND,
    ATTACK,
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        switch (this) {
            case EXPLORE:
                if (stimulus.enemyNearbyRobotsInfo.length > 0) {
                    // TODO let other soldiers know that there are enemies here
                    return ATTACK;
                }

                return EXPLORE;
            case DEFEND:
                // TODO
                return DEFEND;
            case ATTACK:
                if (stimulus.enemyNearbyRobotsInfo.length == 0) {
                    // TODO let other soldiers know that there are no more enemies here
                    return EXPLORE;
                }
                return ATTACK;
            default: throw new RuntimeException("Should not be here");
        }
    }
}
