package kolohe.robots.miner;

import battlecode.common.*;
import kolohe.communication.Communicator;
import kolohe.communication.Entity;
import kolohe.communication.Message;
import kolohe.communication.MessageType;
import kolohe.communication.basic.BasicCommunicator;
import kolohe.pathing.Explore;
import kolohe.pathing.pathfinder.Fuzzy;
import kolohe.pathing.pathfinder.PathFinder;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;
import kolohe.utils.Tuple;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static kolohe.RobotPlayer.*;
import static kolohe.utils.Parameters.*;
import static kolohe.utils.Utils.tryMove;

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
    private static final Explore explorer = new Explore();
    public static final Communicator communicator = new BasicCommunicator();

    // state
    private static MapLocation targetLocation; // the current target location for this robot
    public static MapLocation[] nearbyLocationsWithGold;
    public static MapLocation[] nearbyLocationsWithLead;

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();

        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("== %s, %s: start collectStimulus", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }
        s.myLocation = rc.getLocation();
        s.enemyNearbyRobotsInfo = rc.senseNearbyRobots(ROBOT_TYPE.visionRadiusSquared, OPP_TEAM);
        s.nearbyLocationsWithGold = rc.senseNearbyLocationsWithGold(ROBOT_TYPE.visionRadiusSquared);
        s.nearbyLocationsWithLead = rc.senseNearbyLocationsWithLead(ROBOT_TYPE.visionRadiusSquared);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("== %s, %s: after nearbyLocationsWithLead", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }
        s.messages = communicator.receiveMessages(rc, MINER_RECEIVE_MESSAGE_LIMIT, MINER_RECEIVE_MESSAGE_BYTECODE_LIMIT);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("== %s, %s: after receiveMessages", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }

        return s;
    }

    public static int getAge(RobotController rc) {
        return rc.getRoundNum()-BIRTH_YEAR;
    }

    public static void run(RobotController rc) throws GameActionException {
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("%s, %s: start run", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }
        Stimulus stimulus = collectStimulus(rc);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("%s, %s: after stimulus", rc.getRoundNum(), Clock.getBytecodesLeft()));
            System.out.println(String.format("I'm in state: %s", stateMachine.getCurrState()));
        }
        stateMachine.transition(stimulus, rc);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("%s, %s: after transition", rc.getRoundNum(), Clock.getBytecodesLeft()));
            System.out.println(String.format("I'm in state: %s", stateMachine.getCurrState()));
        }
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));

        switch (stateMachine.getCurrState()) {
            case COLLECT:   runCollectActions(rc, stimulus); break;
            case EXPLORE:   runExploreActions(rc, stimulus); break;
            case TARGET:    runTargetActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("%s, %s: after run actions", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }
    }

    public static void runCollectActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // collect gold, if any
        List<Tuple<MapLocation, Integer>> goldLocationsInView = getGoldLocationsInView(rc);
        if (goldLocationsInView.size() > 0) {
            // TODO decide location to go to as a function of gold quantity, instead of just distance
            MapLocation closestGoldLocation = getClosestResourceLocation(stimulus.myLocation, goldLocationsInView);

            communicator.sendMessage(rc, Message.buildSimpleLocationMessage(
                    MessageType.GOLD_LOCATION, closestGoldLocation, Entity.ALL_MINERS, GOLD_LOCATION_MESSAGE_SHELF_LIFE));

            // gold is close enough to mine
            if (stimulus.myLocation.distanceSquaredTo(closestGoldLocation) < ROBOT_TYPE.actionRadiusSquared) {
                while (rc.canMineGold(closestGoldLocation)) {
                    rc.mineGold(closestGoldLocation);
                }
            } else { // gold is not close enough to mine, so get closer
                Optional<Direction> direction = pathFinder.findPath(stimulus.myLocation, closestGoldLocation, rc);
                if (direction.isPresent() && rc.canMove(direction.get())) {
                    rc.move(direction.get());
                }
            }

            return;
        }

        // collect lead, if any
        List<Tuple<MapLocation, Integer>> leadLocationsInView = getLeadLocationsInView(rc);
        if (leadLocationsInView.size() > 0) {
            // TODO decide location to go to as a function of lead quantity, instead of just distance
            MapLocation closestLeadLocation = getClosestResourceLocation(stimulus.myLocation, leadLocationsInView);

            communicator.sendMessage(rc, Message.buildSimpleLocationMessage(
                    MessageType.LEAD_LOCATION, closestLeadLocation, Entity.ALL_MINERS, LEAD_LOCATION_MESSAGE_SHELF_LIFE));
            rc.setIndicatorDot(closestLeadLocation, 0,0,100);

            // lead is close enough to mine
            if (stimulus.myLocation.distanceSquaredTo(closestLeadLocation) < ROBOT_TYPE.actionRadiusSquared) {
                while (rc.canMineLead(closestLeadLocation)) {
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

    public static boolean isResourceLocationDepleted(MapLocation loc, List<Message> messages) {
        for (Message message : messages) {
            if (!message.messageType.equals(MessageType.NO_RESOURCES_LOCATION)) {
                continue;
            }

            MapLocation noResourceLocation = message.location;
            // TODO NOTE: This assumes that droids (with all the same vision radius squared) will be sending 'NO_RESOURCE_LOCATION' messages
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
        Optional<Direction> direction = explorer.explore(rc.getLocation(), rc);
        if (direction.isPresent()) {
            tryMove(rc, direction.get());
        }
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

    public static boolean isAnyResourceInView(MapLocation[] nearbyLocationsWithGold, MapLocation[] nearbyLocationsWithLead) {
        if (nearbyLocationsWithGold.length > 0) {
            return true;
        }

        if (nearbyLocationsWithLead.length > 0) {
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
                // TODO NOTE: This assumes that droids (with all the same vision radius squared) will be sending 'NO_RESOURCE_LOCATION' messages
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

    public static List<Tuple<MapLocation, Integer>> getLeadLocationsInView(RobotController rc) throws GameActionException {
        int initialBytecodesLeft = Clock.getBytecodesLeft();
        List<Tuple<MapLocation, Integer>> leadLocationsInView = new LinkedList<>();
        for (MapLocation mapLocation: Miner.nearbyLocationsWithLead) {
            if (initialBytecodesLeft-Clock.getBytecodesLeft() > MINER_RESOURCE_IDENTIFICATION_BYTECODE_LIMIT) {
                return leadLocationsInView;
            }
            leadLocationsInView.add(Tuple.of(mapLocation, rc.senseLead(mapLocation)));
        }

        return leadLocationsInView;
    }

    public static List<Tuple<MapLocation, Integer>> getGoldLocationsInView(RobotController rc) throws GameActionException {
        int initialBytecodesLeft = Clock.getBytecodesLeft();
        List<Tuple<MapLocation, Integer>> goldLocationsInView = new LinkedList<>();
        for (MapLocation mapLocation: Miner.nearbyLocationsWithGold) {
            if (initialBytecodesLeft-Clock.getBytecodesLeft() > MINER_RESOURCE_IDENTIFICATION_BYTECODE_LIMIT) {
                return goldLocationsInView;
            }
            goldLocationsInView.add(Tuple.of(mapLocation, rc.senseLead(mapLocation)));
        }

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
}
