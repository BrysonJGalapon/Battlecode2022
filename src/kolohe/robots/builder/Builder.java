package kolohe.robots.builder;

import battlecode.common.*;
import kolohe.communication.Communicator;
import kolohe.communication.Message;
import kolohe.communication.MessageType;
import kolohe.communication.advanced.AdvancedCommunicator;
import kolohe.communication.basic.BasicCommunicator;
import kolohe.pathing.pathfinder.Fuzzy;
import kolohe.pathing.pathfinder.PathFinder;
import kolohe.resource.allocation.ResourceAllocation;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;
import kolohe.utils.Tuple;

import java.util.List;
import java.util.Optional;

import static battlecode.common.RobotMode.PROTOTYPE;
import static kolohe.RobotPlayer.*;
import static kolohe.resource.allocation.ResourceAllocation.getClosestBroadcastedArchonLocation;
import static kolohe.utils.Parameters.BUILDER_RECEIVE_MESSAGE_BYTECODE_LIMIT;
import static kolohe.utils.Parameters.BUILDER_RECEIVE_MESSAGE_LIMIT;
import static kolohe.utils.Utils.getAge;
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
    public static final Communicator communicator = new AdvancedCommunicator();
    public static final ResourceAllocation resourceAllocation = new ResourceAllocation();

    // state
    public static MapLocation buildLocation; // the location to build the next building
    public static RobotType robotToBuild; // the type of building to build (laboratory or watchtower)
    public static MapLocation primaryArchonLocation; // the location of the archon that this builder is following

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

        // check for any broadcasted archon locations
        Optional<MapLocation> closestBroadcastedArchonLocation = getClosestBroadcastedArchonLocation(rc, stimulus);
        if (!closestBroadcastedArchonLocation.isPresent()) {
            // could not find any broadcasted locations turn, which is unlucky (not even sure if this is possible)
            return Optional.empty();
        }
        
        primaryArchonLocation = closestBroadcastedArchonLocation.get();
        return closestBroadcastedArchonLocation;
    }

    public static void run(RobotController rc) throws GameActionException {
        if (getAge(rc) == 0) {
            return; // lots of bytecode is used to initialize the advanced communicator, so don't do anything on this turn
        }

        if (primaryArchonLocation != null) {
            rc.setIndicatorLine(rc.getLocation(), primaryArchonLocation, 0, 0, 255);
        }

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

        // if the build location is too far away to sense, get closer
        if (!rc.canSenseLocation(buildLocation)) {
            Optional<Direction> direction = pathFinder.findPath(stimulus.myLocation, buildLocation, rc);
            if (direction.isPresent() && rc.canMove(direction.get())) {
                rc.move(direction.get());
            }
            return;
        }

        // don't clog up the build location
        if (stimulus.myLocation.equals(buildLocation)) {
            tryMoveRandomDirection(rc);
        }

        // check if the robot we are attempting to build has already been built and is in PROTOTYPE mode
        RobotInfo robotAtBuildLocation = rc.senseRobotAtLocation(buildLocation);
        if (robotAtBuildLocation != null && robotAtBuildLocation.getType().equals(Builder.robotToBuild) && robotAtBuildLocation.getMode().equals(PROTOTYPE)) {
            // robot is close enough to repair
            if (stimulus.myLocation.distanceSquaredTo(buildLocation) <= ROBOT_TYPE.actionRadiusSquared) {
                if (rc.canRepair(buildLocation)) {
                    rc.repair(buildLocation);
                }
            } else { // robot is not close enough to repair, so get closer
                Optional<Direction> direction = pathFinder.findPath(stimulus.myLocation, buildLocation, rc);
                if (direction.isPresent() && rc.canMove(direction.get())) {
                    rc.move(direction.get());
                }
            }
        }

        // robot is close enough to build
        if (stimulus.myLocation.isAdjacentTo(buildLocation)) {
            // wait until have enough resources to build the robot
            resourceAllocation.run(rc, stimulus);
            double leadAllowance = resourceAllocation.getLeadAllowance(rc, ROBOT_TYPE);
            leadBudget += leadAllowance;
            if (robotToBuild.buildCostLead > leadBudget) {
                return;
            }

            // build the robot
            if (rc.canBuildRobot(robotToBuild, stimulus.myLocation.directionTo(buildLocation))) {
                leadBudget -= robotToBuild.buildCostLead;
                rc.buildRobot(robotToBuild, stimulus.myLocation.directionTo(buildLocation));
            }
        } else { // robot is not close enough to build, so get closer
            Optional<Direction> direction = pathFinder.findPath(stimulus.myLocation, buildLocation, rc);
            if (direction.isPresent() && rc.canMove(direction.get())) {
                rc.move(direction.get());
            }
        }
    }

    public static void runRepairActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        tryMoveRandomDirection(rc);
    }

    public static Optional<Tuple<RobotType, MapLocation>> getAnyBroadcastedBuildingLocation(List<Message> messages) {
        for (Message message : messages) {
            switch (message.messageType) {
                case BUILD_LABORATORY_LOCATION:
                    // only listen to the primary archon's broadcasted messages
                    if (primaryArchonLocation != null && message.location.isWithinDistanceSquared(primaryArchonLocation, 32)) {
                        return Optional.of(Tuple.of(RobotType.LABORATORY, message.location));
                    }
                    break;
                case BUILD_WATCHTOWER_LOCATION:
                    // only listen to the primary archon's broadcasted messages
                    if (primaryArchonLocation != null && message.location.isWithinDistanceSquared(primaryArchonLocation, 32)) {
                        return Optional.of(Tuple.of(RobotType.WATCHTOWER, message.location));
                    }
                    break;
            }
        }

        return Optional.empty();
    }
}
