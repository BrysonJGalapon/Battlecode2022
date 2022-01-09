package kolohe.robots.builder;

import battlecode.common.*;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;
import kolohe.utils.Tuple;

import java.util.Optional;

import static battlecode.common.RobotMode.PROTOTYPE;
import static kolohe.RobotPlayer.ROBOT_ID;
import static kolohe.RobotPlayer.TEST_ROBOT_ID;

public enum BuilderState implements State {
    START, // initial state

    BUILD, // in the process of building a building

    REPAIR, // find buildings to repair

    MUTATE, // mutate buildings
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        Optional<Tuple<RobotType, MapLocation>> broadcastedBuildingLocation = Builder.getAnyBroadcastedBuildingLocation(stimulus.messages);

        switch (this) {
            case START:
                // check if any buildings need to be created
                if (broadcastedBuildingLocation.isPresent()) {
                    Builder.robotToBuild = broadcastedBuildingLocation.get().getX();
                    Builder.buildLocation = broadcastedBuildingLocation.get().getY();
                    return BUILD;
                }

                return REPAIR;
            case BUILD:
                // not close enough to build location
                if (!rc.canSenseLocation(Builder.buildLocation)) {
                    return BUILD;
                }

                // check if the desired building has already been built at the location and is functioning
                RobotInfo robotAtBuildLocation = rc.senseRobotAtLocation(Builder.buildLocation);
                if (robotAtBuildLocation != null && robotAtBuildLocation.getType().equals(Builder.robotToBuild) && !robotAtBuildLocation.getMode().equals(PROTOTYPE)) {
                    // building has already been built, check if any other build locations are being broadcasted
                    if (broadcastedBuildingLocation.isPresent() && !broadcastedBuildingLocation.get().getY().equals(Builder.buildLocation)) {
                        Builder.robotToBuild = broadcastedBuildingLocation.get().getX();
                        Builder.buildLocation = broadcastedBuildingLocation.get().getY();
                        return BUILD;
                    } else {
                        // no other build locations are being broadcasted
                        return REPAIR;
                    }
                }

                // TODO transition to repair state if no new buildings need to be created, otherwise stay in build state
                //  but modify some Builder state to build in a new location
                return BUILD;

            case REPAIR:
                // check if any buildings need to be created
                if (broadcastedBuildingLocation.isPresent()) {
                    Builder.robotToBuild = broadcastedBuildingLocation.get().getX();
                    Builder.buildLocation = broadcastedBuildingLocation.get().getY();
                    return BUILD;
                }

               return REPAIR;

            default: throw new RuntimeException("Should not be here");
        }
    }
}
