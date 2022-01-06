package kolohe.robots.builder;

import battlecode.common.*;
import kolohe.communication.Communicator;
import kolohe.communication.Message;
import kolohe.communication.MessageType;
import kolohe.communication.basic.BasicCommunicator;
import kolohe.pathing.pathfinder.Fuzzy;
import kolohe.pathing.pathfinder.PathFinder;
import kolohe.resource.allocation.ResourceAllocation;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;

import java.util.List;
import java.util.Optional;

import static kolohe.RobotPlayer.*;
import static kolohe.utils.Parameters.BUILDER_RECEIVE_MESSAGE_BYTECODE_LIMIT;
import static kolohe.utils.Parameters.BUILDER_RECEIVE_MESSAGE_LIMIT;
import static kolohe.utils.Utils.tryMoveRandomDirection;

/*
    - can build buildings (watchtowers or laboratories)
    - can repair nearby buildings
    - can mutate nearby buildings
    - buildings must be repaired to full health before it becomes a turret
    - bytecode limit: 7,500
 */
public class Builder {
    private static final StateMachine<BuilderState> stateMachine = StateMachine.startingAt(BuilderState.START);
    private static final PathFinder pathFinder = new Fuzzy();
    public static final Communicator communicator = new BasicCommunicator();
    public static final ResourceAllocation resourceAllocation = new ResourceAllocation();

    // state
    public static MapLocation buildLocation; // the location to build the next building
    private static MapLocation primaryArchonLocation; // the location of the archon that this builder is following

    public static double leadBudget = 0;

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();
        s.myLocation = rc.getLocation();
        s.friendlyAdjacentNearbyRobotsInfo = rc.senseNearbyRobots(2, MY_TEAM);
        s.messages = communicator.receiveMessages(rc, BUILDER_RECEIVE_MESSAGE_LIMIT, BUILDER_RECEIVE_MESSAGE_BYTECODE_LIMIT);

        return s;
    }

    // TODO make sure there is a mechanism for builders to discover that archons that have died
    //  and can reset their primary archon (ensure that builders who are too far away from their primary archon
    //  try to move closer to it)
    public static Optional<MapLocation> getPrimaryArchonLocation(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (primaryArchonLocation != null) {
            // check that the archon is still there
            if (rc.canSenseLocation(primaryArchonLocation)) {
                RobotInfo robotInfo = rc.senseRobotAtLocation(primaryArchonLocation);
                if (robotInfo == null || !robotInfo.getType().equals(RobotType.ARCHON)) {
                    // archon has definitely died or moved
                    return updatePrimaryArchonLocation(rc, stimulus);
                }

                // archon is definitely still there
                return Optional.of(primaryArchonLocation);
            } else {
                // archon might be there, but is outside of the vision radius. Assume
                //  that it is alive until we move close enough to be sure
                return Optional.of(primaryArchonLocation);
            }
        }

        // primaryArchonLocation is null
        return updatePrimaryArchonLocation(rc, stimulus);
    }

    private static Optional<MapLocation> updatePrimaryArchonLocation(RobotController rc, Stimulus stimulus) {
        // check if any archons adjacent to the builder (which will always occur on the first round the
        //  builder is born, since archons are the only robots that can create builders)

        // TODO deal with potentially multiple adjacent archons
        for (RobotInfo robotInfo : stimulus.friendlyAdjacentNearbyRobotsInfo) {
            if (robotInfo.getType().equals(RobotType.ARCHON)) {
                primaryArchonLocation = robotInfo.getLocation();
                return Optional.of(primaryArchonLocation);
            }
        }

        // check for broadcasted archon locations, and select the closest one
        MapLocation bestBroadcastedArchonLocation = null;
        int bestBroadcastedArchonLocationDistance = 0;
        for (Message message : stimulus.messages) {
            if (!message.messageType.equals(MessageType.ARCHON_STATE)) {
                continue;
            }

            MapLocation broadcastedArchonLocation = message.location;
            int broadcastedArchonLocationDistance = rc.getLocation().distanceSquaredTo(broadcastedArchonLocation);
            if (bestBroadcastedArchonLocation == null || broadcastedArchonLocationDistance < bestBroadcastedArchonLocationDistance) {
                bestBroadcastedArchonLocation = broadcastedArchonLocation;
                bestBroadcastedArchonLocationDistance = broadcastedArchonLocationDistance;
            }
        }
        primaryArchonLocation = bestBroadcastedArchonLocation;
        return (primaryArchonLocation != null) ? Optional.of(primaryArchonLocation) : Optional.empty();
    }

    public static void run(RobotController rc) throws GameActionException {
        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));

        switch (stateMachine.getCurrState()) {
            case BUILD: runBuildActions(rc, stimulus); break;
            case REPAIR: runRepairActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runBuildActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (buildLocation == null) {
            throw new RuntimeException("Should not be here");
        }

        rc.setIndicatorLine(stimulus.myLocation, buildLocation, 0, 0, 255);

        if (stimulus.myLocation.equals(buildLocation)) {
            tryMoveRandomDirection(rc);
        }

        RobotType buildingRobotType = RobotType.WATCHTOWER;
        // TODO store and extract building type in state (to support laboratory builds) in state
        // building is close enough to build
        if (stimulus.myLocation.distanceSquaredTo(buildLocation) < ROBOT_TYPE.actionRadiusSquared) {
            // wait until have enough resources to build the building
            resourceAllocation.run(rc, stimulus);
            double leadAllowance = resourceAllocation.getLeadAllowance(rc, ROBOT_TYPE);
            leadBudget += leadAllowance;
            if (buildingRobotType.buildCostLead > leadBudget) {
                return;
            }

            // build the building
            if (rc.canBuildRobot(buildingRobotType, stimulus.myLocation.directionTo(buildLocation))) {
                leadBudget -= buildingRobotType.buildCostLead;
                rc.buildRobot(buildingRobotType, stimulus.myLocation.directionTo(buildLocation));
            }
        } else { // building is close enough to build, so get closer
            Optional<Direction> direction = pathFinder.findPath(stimulus.myLocation, buildLocation, rc);
            if (direction.isPresent() && rc.canMove(direction.get())) {
                rc.move(direction.get());
            }
        }
    }

    public static void runRepairActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // TODO
    }

    public static Optional<MapLocation> getAnyBroadcastedBuildingLocation(List<Message> messages) {
        for (Message message : messages) {
            // TODO once there is more space for messageType bits, add another filter for 'BUILD_LABORATORY_LOCATION' messages
            if (!message.messageType.equals(MessageType.BUILD_WATCHTOWER_LOCATION)) {
                continue;
            }

            return Optional.of(message.location);
        }

        return Optional.empty();
    }
}
