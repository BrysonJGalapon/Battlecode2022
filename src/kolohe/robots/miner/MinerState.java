package kolohe.robots.miner;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;
import kolohe.utils.Tuple;

import java.util.List;

public enum MinerState implements State {
    // is nearby resources, collects them
    COLLECT,

    // not nearby resources, doesn't know where to go to find them
    EXPLORE,

    // not nearby resources, knows where to go to find them
    TARGET,
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        switch (this) {
            case COLLECT:
                if (!Miner.isAnyResourceInView(rc, stimulus.allLocationsWithinRadiusSquared)) {
                    // TODO if miner receives location of resources, move to TARGET state
                    return EXPLORE;
                }

                return COLLECT;
            case EXPLORE:
                if (Miner.isAnyResourceInView(rc, stimulus.allLocationsWithinRadiusSquared)) {
                    return COLLECT;
                }

                // TODO if miner receives location of resources, move to TARGET state
                return EXPLORE;
            case TARGET:
                if (Miner.isAnyResourceInView(rc, stimulus.allLocationsWithinRadiusSquared)) {
                    return COLLECT;
                }

                return TARGET;
            default: throw new RuntimeException("Should not be here");
        }
    }
}
