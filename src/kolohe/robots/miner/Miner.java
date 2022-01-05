package kolohe.robots.miner;

import battlecode.common.*;
import kolohe.communication.Communicator;
import kolohe.communication.Entity;
import kolohe.communication.Message;
import kolohe.communication.MessageType;
import kolohe.communication.basic.BasicCommunicator;
import kolohe.pathing.Fuzzy;
import kolohe.pathing.PathFinder;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;
import kolohe.utils.Tuple;
import kolohe.utils.Utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static kolohe.RobotPlayer.BIRTH_YEAR;
import static kolohe.RobotPlayer.ROBOT_TYPE;
import static kolohe.utils.Parameters.MINER_RECEIVE_MESSAGE_LIMIT;

/*
    - can collect lead AND gold
    - collects resources, instantly usable (no need to path backwards to "deposit" collected resources)
    - bytecode limit: 7,500
    - action radius: 2
    - vision radius: 20
 */
public class Miner {
    private static final StateMachine<MinerState> stateMachine = StateMachine.startingAt(MinerState.EXPLORE);
    private static final PathFinder pathFinder = new Fuzzy();
    public static final Communicator communicator = new BasicCommunicator();

    private static MapLocation targetLocation = null; // the current target location for this robot

    // state
    private static Direction lastDirectionMoved;

    // caches
    private static int localGoldLocationsValidityAge = -1; // the age for which this cache is valid
    public static List<Tuple<MapLocation, Integer>> localGoldLocations = null;
    private static int localLeadLocationsValidityAge = -1; // the age for which this cache is valid
    public static List<Tuple<MapLocation, Integer>> localLeadLocations = null;

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();

        s.nearbyRobotsInfo = rc.senseNearbyRobots();
        s.myLocation = rc.getLocation();
        s.allLocationsWithinRadiusSquared = rc.getAllLocationsWithinRadiusSquared(s.myLocation, ROBOT_TYPE.visionRadiusSquared);
        s.messages = communicator.receiveMessages(rc, MINER_RECEIVE_MESSAGE_LIMIT);

        // state-specific collection of stimuli
        switch (stateMachine.getCurrState()) {
            case COLLECT:  break;
            case EXPLORE:  break;
            case TARGET:   break;
            default: throw new RuntimeException("Should not be here");
        }

        return s;
    }

    public static int getAge(RobotController rc) {
        return rc.getRoundNum()-BIRTH_YEAR;
    }

    public static void run(RobotController rc) throws GameActionException {
        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));

        switch (stateMachine.getCurrState()) {
            case COLLECT:   runCollectActions(rc, stimulus); break;
            case EXPLORE:   runExploreActions(rc, stimulus); break;
            case TARGET:    runTargetActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runCollectActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // TODO re-enable gold collection when either we have more information to know exactly where the gold is, or when
        //  bytecode usage is not an issue
//        // collect gold, if any
//        List<Tuple<MapLocation, Integer>> goldLocationsInView = getGoldLocationsInView(rc, stimulus.allLocationsWithinRadiusSquared);
//        if (goldLocationsInView.size() > 0) {
//            // TODO decide location to go to as a function of gold quantity, instead of just distance
//            MapLocation closestGoldLocation = getClosestResourceLocation(stimulus.myLocation, goldLocationsInView);
//
//            communicator.sendMessage(rc, Message.buildSimpleLocationMessage(MessageType.GOLD_LOCATION, closestGoldLocation, Entity.ALL_MINERS));
//
//            // gold is close enough to mine
//            if (stimulus.myLocation.distanceSquaredTo(closestGoldLocation) < ROBOT_TYPE.actionRadiusSquared) {
//                if (rc.canMineGold(closestGoldLocation)) {
//                    rc.mineGold(closestGoldLocation);
//                }
//            } else { // gold is not close enough to mine, so get closer
//                Optional<Direction> direction = pathFinder.findPath(stimulus.myLocation, closestGoldLocation, rc);
//                if (direction.isPresent() && rc.canMove(direction.get())) {
//                    rc.move(direction.get());
//                }
//            }
//
//            return;
//        }

        // collect lead, if any
        List<Tuple<MapLocation, Integer>> leadLocationsInView = getLeadLocationsInView(rc, stimulus.allLocationsWithinRadiusSquared);
        if (leadLocationsInView.size() > 0) {
            // TODO decide location to go to as a function of lead quantity, instead of just distance
            MapLocation closestLeadLocation = getClosestResourceLocation(stimulus.myLocation, leadLocationsInView);

            communicator.sendMessage(rc, Message.buildSimpleLocationMessage(MessageType.LEAD_LOCATION, closestLeadLocation, Entity.ALL_MINERS));
            rc.setIndicatorDot(closestLeadLocation, 0,0,100);

            // lead is close enough to mine
            if (stimulus.myLocation.distanceSquaredTo(closestLeadLocation) < ROBOT_TYPE.actionRadiusSquared) {
                if (rc.canMineLead(closestLeadLocation)) {
                    rc.mineLead(closestLeadLocation);
                }
            } else { // lead is not close enough to mine, so get closer
                Optional<Direction> direction = pathFinder.findPath(stimulus.myLocation, closestLeadLocation, rc);
                if (direction.isPresent()) {
                    tryMove(rc, direction.get());
                }
            }

            return;
        }
    }

    private static boolean tryMove(RobotController rc, Direction direction) throws GameActionException {
        if (rc.canMove(direction)) {
            lastDirectionMoved = direction;
            rc.move(direction);
            return true;
        }

        return false;
    }

    public static boolean isResourceLocationDepleted(MapLocation loc, List<Message> messages) {
        for (Message message : messages) {
            if (!message.messageType.equals(MessageType.NO_RESOURCES_LOCATION)) {
                continue;
            }

            MapLocation noResourceLocation = message.location;
            // TODO NOTE: This assumes that only miners will be sending 'NO_RESOURCE_LOCATION' messages
            if (loc.isWithinDistanceSquared(noResourceLocation, RobotType.MINER.visionRadiusSquared)) {
                return true;
            }
        }

        return false;
    }

    public static void setTargetLocation(MapLocation targetLocation) {
        Miner.targetLocation = targetLocation;
    }

    public static MapLocation getTargetLocation() {
        return Miner.targetLocation;
    }

    public static void runExploreActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        Direction direction = Utils.getRandomDirection();
        tryMove(rc, direction);
    }

    public static void runTargetActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (targetLocation == null) {
            throw new RuntimeException("Shouldn't be here");
        }

        rc.setIndicatorDot(targetLocation, 0, 0, 255);

        // move in the direction of the target
        Optional<Direction> direction = pathFinder.findPath(stimulus.myLocation, targetLocation, rc);
        if (!direction.isPresent()) {
            return;
        }

        tryMove(rc, direction.get());
    }

    public static boolean isAnyResourceInView(RobotController rc, MapLocation[] allLocationsWithinRadiusSquared) throws GameActionException {
        // TODO re-enable gold collection when either we have more information to know exactly where the gold is, or when
        //  bytecode usage is not an issue
//        List<Tuple<MapLocation, Integer>> localGoldLocations = getGoldLocationsInView(rc, allLocationsWithinRadiusSquared);
//        if (localGoldLocations.size() > 0) {
//            return true;
//        }

        List<Tuple<MapLocation, Integer>> localLeadLocations = getLeadLocationsInView(rc, allLocationsWithinRadiusSquared);
        if (localLeadLocations.size() > 0) {
            return true;
        }

        return false;
    }

    public static Optional<MapLocation> getClosestBroadcastedResourceLocation(MapLocation loc, List<Message> messages) {
        // only consider 'resource location' messages and 'no resource location' messages
        List<MapLocation> noResourceLocations = new LinkedList<>();
        List<Tuple<MapLocation, Integer>> broadcastedResourceLocations = new LinkedList<>();
        for (Message message : messages) {
            switch (message.messageType) {
                case GOLD_LOCATION:
                case LEAD_LOCATION:
                    // TODO once value is associated to a location, include amount of resource as Y()
                    broadcastedResourceLocations.add(Tuple.of(message.location, 0));
                    break;
                case NO_RESOURCES_LOCATION:
                    noResourceLocations.add(message.location);
                    break;
            }
        }

        // TODO NOTE: since we do not know the order for which these messages were written, it is possible that
        //  a 'no resource location' message is still within the message queue when a NEW resource pops up nearby.
        //  This can be fixed by storing an 'age' for which the message was written.
        // get non-depleted resource locations
        List<Tuple<MapLocation, Integer>> nonDepletedBroadcastedResourceLocations = new LinkedList<>();
        for (Tuple<MapLocation, Integer> broadcastedResourceLocation : broadcastedResourceLocations) {
            boolean isNonDepleted = true;
            for (MapLocation noResourceLocation : noResourceLocations) {
                // TODO NOTE: This assumes that only miners will be sending 'NO_RESOURCE_LOCATION' messages
                if (broadcastedResourceLocation.getX().isWithinDistanceSquared(noResourceLocation, RobotType.MINER.visionRadiusSquared)) {
                    isNonDepleted = false;
                    break;
                }
            }

            if (isNonDepleted) {
                nonDepletedBroadcastedResourceLocations.add(broadcastedResourceLocation);
            }
        }

        // found a non-depleted resource location, return the closest one
        if (nonDepletedBroadcastedResourceLocations.size() > 0) {
            return Optional.of(getClosestResourceLocation(loc, nonDepletedBroadcastedResourceLocations));
        }

        return Optional.empty();
    }

    public static List<Tuple<MapLocation, Integer>> getLeadLocationsInView(RobotController rc, MapLocation[] allLocationsWithinRadiusSquared) throws GameActionException {
        int age = getAge(rc);

        if (age == localLeadLocationsValidityAge) {
            return localLeadLocations;
        }

        List<Tuple<MapLocation, Integer>> leadLocationsInView = getLeadLocations(rc, allLocationsWithinRadiusSquared);
        localLeadLocations = leadLocationsInView;
        localLeadLocationsValidityAge = age;
        return leadLocationsInView;
    }

    public static List<Tuple<MapLocation, Integer>> getGoldLocationsInView(RobotController rc, MapLocation[] allLocationsWithinRadiusSquared) throws GameActionException {
        int age = getAge(rc);

        if (age == localGoldLocationsValidityAge) {
            return localGoldLocations;
        }

        List<Tuple<MapLocation, Integer>> goldLocationsInView = getGoldLocations(rc, allLocationsWithinRadiusSquared);
        localGoldLocations = goldLocationsInView;
        localGoldLocationsValidityAge = age;
        return goldLocationsInView;
    }

    private static MapLocation getClosestResourceLocation(MapLocation src, List<Tuple<MapLocation, Integer>> resources) {
        MapLocation closestResourceLocation = null;
        int closestResourceLocationDistance = 0;

        for (Tuple<MapLocation, Integer> resource : resources) {
            MapLocation resourceLocation = resource.getX();
            int resourceLocationDistance = src.distanceSquaredTo(resourceLocation);
            if (closestResourceLocation == null || resourceLocationDistance < closestResourceLocationDistance) {
                closestResourceLocation = resourceLocation;
                closestResourceLocationDistance = resourceLocationDistance;
            }
        }

        return closestResourceLocation;
    }

    private static List<Tuple<MapLocation, Integer>> getLeadLocations(RobotController rc, MapLocation[] locs) throws GameActionException {
        return getLeadLocations(rc, locs, 1500);
    }

    private static List<Tuple<MapLocation, Integer>> getLeadLocations(RobotController rc, MapLocation[] locs, int bytecodeLimit) throws GameActionException {
        int initialBytecodesLeft = Clock.getBytecodesLeft();
        List<Tuple<MapLocation, Integer>> leadLocations = new LinkedList<>();
        for (MapLocation loc : locs) {
            if (initialBytecodesLeft-Clock.getBytecodesLeft() > bytecodeLimit) {
                // TODO set 1500 in tunable parameters, if we make this 'bytecode cap' feature official
                return leadLocations;
            }

            if (!rc.canSenseLocation(loc)) {
                continue;
            }

            int lead = rc.senseLead(loc);

            if (lead == 0) {
                continue;
            }

            leadLocations.add(Tuple.of(loc, lead));
        }

        return leadLocations;
    }

    private static List<Tuple<MapLocation, Integer>> getGoldLocations(RobotController rc, MapLocation[] locs) throws GameActionException {
        List<Tuple<MapLocation, Integer>> goldLocations = new LinkedList<>();
        for (MapLocation loc : locs) {
            int gold = rc.senseGold(loc);

            if (gold == 0) {
                continue;
            }

            goldLocations.add(Tuple.of(loc, gold));
        }

        return goldLocations;
    }
}
