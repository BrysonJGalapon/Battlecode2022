package kolohe.resource.allocation;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kolohe.communication.Message;
import kolohe.communication.MessageType;
import kolohe.robots.archon.Archon;
import kolohe.robots.archon.ArchonState;
import kolohe.robots.builder.Builder;
import kolohe.robots.laboratory.Laboratory;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

import java.util.Optional;

import static kolohe.RobotPlayer.ROBOT_TYPE;

public enum ResourceAllocationState implements State {
    RESOURCE_COLLECTION,
    DEFEND,
    ATTACK,
    SURVIVE
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        // mirror the state of the archon this robot is following
        Optional<ArchonState> primaryArchonState = getPrimaryArchonState(rc, stimulus);

        if (!primaryArchonState.isPresent()) {
            // couldn't determine the state of the primary archon, so remain in current state
            return this;
        }

        switch (primaryArchonState.get()) {
            case RESOURCE_COLLECTION:
                return ResourceAllocationState.RESOURCE_COLLECTION;
            case DEFEND:
                return ResourceAllocationState.DEFEND;
            case ATTACK:
                return ResourceAllocationState.ATTACK;
            case SURVIVE:
                return ResourceAllocationState.SURVIVE;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static Optional<ArchonState> getPrimaryArchonState(RobotController rc, Stimulus stimulus) throws GameActionException {
        Optional<MapLocation> primaryArchonLocation;
        switch(ROBOT_TYPE) {
            case ARCHON: return Optional.of(Archon.stateMachine.getCurrState());
            case BUILDER: primaryArchonLocation = Builder.getPrimaryArchonLocation(rc, stimulus); break;
            case LABORATORY: primaryArchonLocation = Laboratory.getPrimaryArchonLocation(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }

        // could not detect any archon locations
        if (!primaryArchonLocation.isPresent()) {
            return Optional.empty();
        }

        // check message list for archon state messages that match the closest archon location
        for (Message message : stimulus.messages) {
            if (!message.messageType.equals(MessageType.ARCHON_STATE)) {
                continue;
            }

            if (!message.location.equals(primaryArchonLocation.get())) {
                continue;
            }

            return Optional.of(message.archonState);
        }

        return Optional.empty();
    }
}
