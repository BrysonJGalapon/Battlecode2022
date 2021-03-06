package kolohe.robots.miner;

import battlecode.common.*;
import kolohe.communication.Communicator;
import kolohe.communication.Entity;
import kolohe.communication.Message;
import kolohe.communication.MessageType;
import kolohe.communication.advanced.AdvancedCommunicator;
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
import static kolohe.utils.Utils.getAge;
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
    public static final AdvancedCommunicator communicator = new AdvancedCommunicator();

    // state
    private static MapLocation targetLocation; // the current target location for this robot
    public static MapLocation[] nearbyLocationsWithGold;
    public static MapLocation[] nearbyLocationsWithLead;

    public static List<Tuple<MapLocation, Integer>> leadLocationsInView;
    public static List<Tuple<MapLocation, Integer>> goldLocationsInView;
    public static int leadLocationsInViewValidAge = -1;
    public static int goldLocationsInViewValidAge = -1;

    public static List<MapLocation> archonLocations;
    public static int archonLocationsValidAge = -1;
    private static int ARCHON_LOCATIONS_SHELF_LIFE = 100;

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();

        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("== %s, %s: start collectStimulus", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }
        s.myLocation = rc.getLocation();
        s.nearbyLocationsWithGold = rc.senseNearbyLocationsWithGold(ROBOT_TYPE.visionRadiusSquared);
        s.nearbyLocationsWithLead = rc.senseNearbyLocationsWithLead(ROBOT_TYPE.visionRadiusSquared);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("== %s, %s: after nearbyLocationsWithLead", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }
        s.messages = communicator.receiveMessages(rc, MINER_RECEIVE_MESSAGE_LIMIT, MINER_RECEIVE_MESSAGE_BYTECODE_LIMIT);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("== %s, %s: after receiveMessages", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }

        s.archonLocations = getArchonLocations(rc);

        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("== %s, %s: after getArchonLocations", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }

        return s;
    }

    private static List<MapLocation> getArchonLocations(RobotController rc) throws GameActionException {
        int age = getAge(rc);
        if (age <= Miner.archonLocationsValidAge) {
            return Miner.archonLocations;
        }

        List<MapLocation> archonLocations = new LinkedList<>();
        List<Message> archonStateMessages = communicator.receiveArchonStateMessages(rc);
        for (Message message : archonStateMessages) {
            archonLocations.add(message.location);
        }

        Miner.archonLocations = archonLocations;
        Miner.archonLocationsValidAge = age + ARCHON_LOCATIONS_SHELF_LIFE;
        return archonLocations;
    }

    public static void run(RobotController rc) throws GameActionException {
        if (getAge(rc) == 0) {
            return; // lots of bytecode is used to initialize the advanced communicator, so don't do anything on this turn
        }

//        if (ROBOT_ID == TEST_ROBOT_ID) {
//            System.out.println(String.format("%s, %s: start run", rc.getRoundNum(), Clock.getBytecodesLeft()));
//        }
        Stimulus stimulus = collectStimulus(rc);
//        if (ROBOT_ID == TEST_ROBOT_ID) {
//            System.out.println(String.format("%s, %s: after stimulus", rc.getRoundNum(), Clock.getBytecodesLeft()));
//            System.out.println(String.format("I'm in state: %s", stateMachine.getCurrState()));
//        }
        stateMachine.transition(stimulus, rc);

        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("%s, %s: after transition", rc.getRoundNum(), Clock.getBytecodesLeft()));
            System.out.println(String.format("I'm in state: %s", stateMachine.getCurrState()));
        }
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));

        switch (stateMachine.getCurrState()) {
            case COLLECT: runCollectActions(rc, stimulus); break;
            case EXPLORE:   runExploreActions(rc, stimulus); break;
            case TARGET:    runTargetActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
//        if (ROBOT_ID == TEST_ROBOT_ID) {
//            System.out.println(String.format("%s, %s: after run actions", rc.getRoundNum(), Clock.getBytecodesLeft()));
//        }
    }

    private static void runCheapCollectActions() {
        // do nothing for now
    }

    public static void runCollectActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (Clock.getBytecodesLeft() < 4000) {
            runCheapCollectActions();
            return;
        }

        // collect gold, if any
        List<Tuple<MapLocation, Integer>> goldLocationsInView = getGoldLocationsInView(rc);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("%s, %s: after getGoldLocationsInView", rc.getRoundNum(), Clock.getBytecodesLeft()));
        }
        if (goldLocationsInView.size() > 0) {
            // TODO decide location to go to as a function of gold quantity, instead of just distance
            MapLocation closestGoldLocation = getClosestResourceLocation(stimulus.myLocation, goldLocationsInView);

            communicator.sendMessage(rc, Message.buildSimpleLocationMessage(
                    MessageType.GOLD_LOCATION, closestGoldLocation, Entity.ALL_MINERS));

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
        List<Tuple<MapLocation, Integer>> leadLocationsInView = getLeadLocationsInView(rc, stimulus.archonLocations);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("%s, %s: after getLeadLocationsInView", rc.getRoundNum(), Clock.getBytecodesLeft()));
            System.out.println(String.format("size is %s", leadLocationsInView.size()));
        }
        if (leadLocationsInView.size() > 0) {
            // TODO decide location to go to as a function of lead quantity, instead of just distance
            MapLocation closestLeadLocation = getClosestResourceLocation(stimulus.myLocation, leadLocationsInView);

            communicator.sendMessage(rc, Message.buildSimpleLocationMessage(
                    MessageType.LEAD_LOCATION, closestLeadLocation, Entity.ALL_MINERS));
            rc.setIndicatorDot(closestLeadLocation, 0,0,100);

            // lead is close enough to mine
            if (stimulus.myLocation.distanceSquaredTo(closestLeadLocation) < ROBOT_TYPE.actionRadiusSquared) {
                int leadToMine = rc.senseLead(closestLeadLocation);

                if (isFarmableLocation(closestLeadLocation, stimulus.archonLocations)) {
                    leadToMine -= 1; // farm the lead; don't mine all of it
                }

                while (rc.canMineLead(closestLeadLocation) && leadToMine > 0) {
                    rc.mineLead(closestLeadLocation);
                    leadToMine -= 1;
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

    private static boolean isFarmableLocation(MapLocation leadLocation, List<MapLocation> archonLocations) {
        for (MapLocation archonLocation : archonLocations) {
            if (leadLocation.isWithinDistanceSquared(archonLocation, LEAD_FARM_DISTANCE_SQUARED_THRESHOLD)) {
                return true;
            }
        }

        return false;
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

    public static boolean isAnyResourceInView(RobotController rc, List<MapLocation> archonLocations) throws GameActionException {
        if (getLeadLocationsInView(rc, archonLocations).size() > 0) {
            return true;
        }

        if (getGoldLocationsInView(rc).size() > 0) {
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

    public static List<Tuple<MapLocation, Integer>> getLeadLocationsInView(RobotController rc, List<MapLocation> archonLocations) throws GameActionException {
        return getLeadLocationsInViewWithLimit(rc, archonLocations, 10);
    }

    public static List<Tuple<MapLocation, Integer>> getLeadLocationsInViewWithLimit(RobotController rc, List<MapLocation> archonLocations, int limit) throws GameActionException {
        int age = getAge(rc);
        if (Miner.leadLocationsInViewValidAge == age) {
            return Miner.leadLocationsInView;
        }

        int initialBytecodesLeft = Clock.getBytecodesLeft();
        List<Tuple<MapLocation, Integer>> leadLocationsInView = new LinkedList<>();
        for (MapLocation mapLocation : Miner.nearbyLocationsWithLead) {
            if (initialBytecodesLeft-Clock.getBytecodesLeft() > MINER_RESOURCE_IDENTIFICATION_BYTECODE_LIMIT) {
                Miner.leadLocationsInView = leadLocationsInView;
                Miner.leadLocationsInViewValidAge = age;
                return leadLocationsInView;
            }

            int leadAmount = rc.senseLead(mapLocation);

            if (leadAmount < LEAD_FARM_AMOUNT_THRESHOLD && isFarmableLocation(mapLocation, archonLocations)) {
                continue; // ignore farmable locations
            }

            leadLocationsInView.add(Tuple.of(mapLocation, leadAmount));
        }

        Miner.leadLocationsInView = leadLocationsInView;
        Miner.leadLocationsInViewValidAge = age;

        return leadLocationsInView;
    }

    public static List<Tuple<MapLocation, Integer>> getGoldLocationsInView(RobotController rc) throws GameActionException {
        int age = getAge(rc);
        if (Miner.goldLocationsInViewValidAge == age) {
            return Miner.goldLocationsInView;
        }

        int initialBytecodesLeft = Clock.getBytecodesLeft();
        List<Tuple<MapLocation, Integer>> goldLocationsInView = new LinkedList<>();
        for (MapLocation mapLocation: Miner.nearbyLocationsWithGold) {
            if (initialBytecodesLeft-Clock.getBytecodesLeft() > MINER_RESOURCE_IDENTIFICATION_BYTECODE_LIMIT) {
                return goldLocationsInView;
            }
            goldLocationsInView.add(Tuple.of(mapLocation, rc.senseLead(mapLocation)));
        }

        Miner.goldLocationsInView = goldLocationsInView;
        Miner.goldLocationsInViewValidAge = age;
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
