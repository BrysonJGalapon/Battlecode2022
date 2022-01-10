package kimo.robots.builder;

import battlecode.common.*;
import kimo.communication.Communicator;
import kimo.communication.Message;
import kimo.communication.advanced.AdvancedCommunicator;
import kimo.pathing.Circle;
import kimo.pathing.Explore;
import kimo.pathing.RotationPreference;
import kimo.pathing.pathfinder.Fuzzy;
import kimo.pathing.pathfinder.PathFinder;
import kimo.resource.allocation.ResourceAllocation;
import kimo.robots.builder.BuilderState;
import kimo.state.machine.StateMachine;
import kimo.state.machine.Stimulus;
import kimo.utils.Tuple;
import kimo.utils.Utils;

import java.util.List;
import java.util.Optional;

import static battlecode.common.RobotMode.PROTOTYPE;
import static kimo.RobotPlayer.*;
import static kimo.resource.allocation.ResourceAllocation.getClosestBroadcastedArchonLocation;
import static kimo.utils.Parameters.BUILDER_RECEIVE_MESSAGE_BYTECODE_LIMIT;
import static kimo.utils.Parameters.BUILDER_RECEIVE_MESSAGE_LIMIT;
import static kimo.utils.Utils.*;

/*
    - can build buildings (watchtowers or laboratories)
    - can repair nearby buildings
    - can mutate nearby buildings
    - buildings must be repaired to full health before it becomes a turret
    - bytecode limit: 7,500
 */
public class Builder {
    private static final StateMachine<kimo.robots.builder.BuilderState> stateMachine = StateMachine.startingAt(BuilderState.PATROL);
    private static final Fuzzy pathFinder = new Fuzzy();
    private static final Explore explorer = new Explore();
    private static final Circle circler = new Circle();
    public static final AdvancedCommunicator communicator = new AdvancedCommunicator();
    public static final ResourceAllocation resourceAllocation = new ResourceAllocation();

    // state
    public static MapLocation buildLocation; // the location to build the next building
    public static RobotType robotToBuild; // the type of building to build (laboratory or watchtower)

    public static MapLocation repairLocation;
    public static MapLocation mutateLocation;

    public static MapLocation patrolLocation;
    public static int patrolIndex = 0;
    public static int[][] patrolDeltas = new int[][]{
      new int[]{0, 4},  // north laboratory
      new int[]{3, 3},  // north-east watchtower
      new int[]{4, 0}, // east laboratory
      new int[]{3, -3}, // south-east watchtower
      new int[]{0, -4}, // south laboratory
      new int[]{-3, -3}, // south-west watchtower
      new int[]{-4, 0}, // west laboratory
      new int[]{-3, 3}, // north-west watchtower
    };

    public static MapLocation primaryArchonLocation; // the location of the archon that this builder is following

    public static double leadBudget = 0;
    public static double goldBudget = 0;

    public static MapLocation getPatrolLocation(MapLocation primaryArchonLocation) {
        int[] parolDelta = patrolDeltas[patrolIndex];
        return primaryArchonLocation.translate(parolDelta[0], parolDelta[1]);
    }

    public static void incrementPatrolIndex(MapLocation primaryArchonLocation, RobotController rc) throws GameActionException {
        patrolIndex = (patrolIndex + 1) % patrolDeltas.length;
        while (!Utils.onTheMap(getPatrolLocation(primaryArchonLocation))) {
            patrolIndex = (patrolIndex + 1) % patrolDeltas.length;
        }
    }

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();
        s.myLocation = rc.getLocation();
        s.friendlyNearbyRobotsInfo = rc.senseNearbyRobots(ROBOT_TYPE.visionRadiusSquared, MY_TEAM);
        s.friendlyAdjacentNearbyRobotsInfo = rc.senseNearbyRobots(2, MY_TEAM);
        s.messages = communicator.receiveMessages(rc, BUILDER_RECEIVE_MESSAGE_LIMIT, BUILDER_RECEIVE_MESSAGE_BYTECODE_LIMIT);
        s.archonStateMessages = communicator.receiveArchonStateMessages(rc);
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

        resourceAllocation.run(rc, stimulus);
        double leadAllowance = resourceAllocation.getLeadAllowance(rc, ROBOT_TYPE);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println("My lead allowance is: " + leadAllowance);
        }

        leadBudget += leadAllowance;
        double goldAllowance = resourceAllocation.getGoldAllowance(rc, ROBOT_TYPE);
        goldBudget += goldAllowance;

        switch (stateMachine.getCurrState()) {
            case ORPHAN: runOrphanActions(rc, stimulus); break;
            case PATROL: runPatrolActions(rc, stimulus); break;
            case BUILD: runBuildActions(rc, stimulus); break;
            case REPAIR: runRepairActions(rc, stimulus); break;
            case MUTATE: runMutateActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }
    public static void runOrphanActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        Optional<Direction> direction = explorer.explore(rc.getLocation(), rc);
        if (direction.isPresent()) {
            Utils.tryMove(rc, direction.get());
        }
    }

    public static void runBuildActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (buildLocation == null) {
            throw new RuntimeException("Should not be here");
        }

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

        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println("I want to build a: " + robotToBuild);
            System.out.println("My budget is: " + leadBudget);
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
        // not close enough to repair location, move towards it
        if (!stimulus.myLocation.isWithinDistanceSquared(Builder.repairLocation, ROBOT_TYPE.actionRadiusSquared)) {
            Optional<Direction> direction = pathFinder.findPath(rc.getLocation(), Builder.repairLocation, rc);
            if (direction.isPresent()) {
                tryMove(rc, direction.get());
            }
        }

        // repair the robot
        if (rc.canRepair(Builder.repairLocation)) {
            rc.repair(Builder.repairLocation);
        }
    }

    public static void runPatrolActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // circle around the archon
        circler.setCenter(primaryArchonLocation);
        circler.setInnerRadiusSquared(32);
        circler.setOuterRadiusSquared(72);

        Optional<Direction> direction = circler.circle(rc, stimulus.myLocation);
        if (direction.isPresent()) {
            tryMove(rc, direction.get());
        }
    }

    public static void runMutateActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // not close enough to mutate location, move towards it
        if (!stimulus.myLocation.isWithinDistanceSquared(Builder.mutateLocation, ROBOT_TYPE.actionRadiusSquared)) {
            Optional<Direction> direction = pathFinder.findPath(rc.getLocation(), Builder.mutateLocation, rc);
            if (direction.isPresent()) {
                tryMove(rc, direction.get());
            }
        }

        // mutate
        if (rc.canMutate(Builder.mutateLocation)) {
            rc.mutate(Builder.mutateLocation);
        }
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
