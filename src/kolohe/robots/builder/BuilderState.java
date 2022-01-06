package kolohe.robots.builder;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

import java.util.Optional;

public enum BuilderState implements State {
    START, // initial state

    BUILD, // in the process of building a building

    REPAIR, // find buildings to repair
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        Optional<MapLocation> broadcastedBuildingLocation = Builder.getAnyBroadcastedBuildingLocation(stimulus.messages);

        switch (this) {
            case START:
                // TODO fix this check
//                // check if any buildings need to be created
//                if (broadcastedBuildingLocation.isPresent()) {
//                    Builder.buildLocation = broadcastedBuildingLocation.get();
//                    return BUILD;
//                }
//
//                return REPAIR;
                Builder.buildLocation = stimulus.myLocation.translate(3, 3);
                return BUILD;

            case BUILD:
                // TODO transition to repair state if no new buildings need to be created, otherwise stay in build state
                //  but modify some Builder state to build in a new location
                return BUILD;

            case REPAIR:
                // check if any buildings need to be created
                if (broadcastedBuildingLocation.isPresent()) {
                    Builder.buildLocation = broadcastedBuildingLocation.get();
                    return BUILD;
                }

               return REPAIR;

            default: throw new RuntimeException("Should not be here");
        }
    }
}
