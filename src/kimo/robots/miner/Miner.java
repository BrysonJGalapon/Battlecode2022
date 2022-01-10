package kimo.robots.miner;

import battlecode.common.*;
import kimo.communication.Communicator;
import kimo.communication.Entity;
import kimo.communication.Message;
import kimo.communication.MessageType;
import kimo.communication.advanced.AdvancedCommunicator;
import kimo.pathing.Circle;
import kimo.pathing.Explore;
import kimo.pathing.pathfinder.Fuzzy;
import kimo.pathing.pathfinder.PathFinder;
import kimo.state.machine.StateMachine;
import kimo.state.machine.Stimulus;
import kimo.utils.Tuple;
import kimo.utils.Utils;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static kimo.RobotPlayer.*;
import static kimo.utils.Parameters.*;
import static kimo.utils.Utils.*;

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
    private static final Circle circler = new Circle();
    public static final AdvancedCommunicator communicator = new AdvancedCommunicator();
    public static MapLocation birthLocation;

    // state
    private static MapLocation targetLocation; // the current target location for this robot
    public static MapLocation[] nearbyLocationsWithGold;
    public static MapLocation[] nearbyLocationsWithLead;

    public static List<MapLocation> leadLocationsInView;
    public static List<MapLocation> goldLocationsInView;
    public static int leadLocationsInViewValidAge = -1;
    public static int goldLocationsInViewValidAge = -1;

    public static List<MapLocation> archonLocations;
    public static int archonLocationsValidAge = -1;
    private static final int ARCHON_LOCATIONS_SHELF_LIFE = 3000;

    public static RobotController rc;

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
        Miner.rc = rc;
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
            birthLocation = rc.getLocation();
            getArchonLocations(rc);
            return; // lots of bytecode is used to initialize the advanced communicator, so don't do anything on this turn
        }

        Miner.rc = rc;

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

    private static void runCheapCollectActions(RobotController rc) throws GameActionException {
        MapLocation[] adjacentLocationsWithLead = rc.senseNearbyLocationsWithLead(ROBOT_TYPE.actionRadiusSquared);
        if (adjacentLocationsWithLead.length == 0) {
            return;
        }

        while (rc.canMineLead(adjacentLocationsWithLead[0])) {
            rc.mineLead(adjacentLocationsWithLead[0]);
        }
    }

    public static void runCollectActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (Clock.getBytecodesLeft() < 2000) {
            runCheapCollectActions(rc);
            return;
        }

        // collect gold, if any
        List<MapLocation> goldLocationsInView = getGoldLocationsInView(rc);
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
        List<MapLocation> leadLocationsInView = getLeadLocationsInView(rc, stimulus.archonLocations);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println(String.format("%s, %s: after getLeadLocationsInView", rc.getRoundNum(), Clock.getBytecodesLeft()));
            System.out.println(String.format("size is %s", leadLocationsInView.size()));
        }
        if (leadLocationsInView.size() > 0) {
            MapLocation closestLeadLocation = getClosestResourceLocation(stimulus.myLocation, leadLocationsInView);
            if (ROBOT_ID == TEST_ROBOT_ID) {
                System.out.println(String.format("%s, %s: after getClosestResourceLocation", rc.getRoundNum(), Clock.getBytecodesLeft()));
            }

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

//                if (rc.getLocation().isWithinDistanceSquared(birthLocation, 20)) {
//                    circler.setCenter(birthLocation);
//                    circler.setInnerRadiusSquared(32);
//                    circler.setOuterRadiusSquared(72);
//                    Optional<Direction> direction = circler.circle(rc, rc.getLocation());
//                    if (direction.isPresent()) {
//                        tryMove(rc, direction.get());
//                    }
//                }
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
            if (leadLocation.isWithinDistanceSquared(archonLocation, LEAD_FARM_DISTANCE_SQUARED_THRESHOLD)
                    && !leadLocation.isWithinDistanceSquared(archonLocation, 8)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isResourceLocationDepleted(MapLocation loc, List<Message> messages) {
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

        rc.setIndicatorLine(rc.getLocation(), targetLocation, 0, 0, 255);

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
        List<MapLocation> broadcastedResourceLocations = new LinkedList<>();
        for (Message message : messages) {
            switch (message.messageType) {
                case GOLD_LOCATION:
                case LEAD_LOCATION:
                    broadcastedResourceLocations.add(message.location);
                    break;
            }
        }

        // found a non-depleted resource location, return the closest one
        if (broadcastedResourceLocations.size() > 0) {
            return Optional.of(getClosestResourceLocation(loc, broadcastedResourceLocations));
        }

        return Optional.empty();
    }

    public static List<MapLocation> getLeadLocationsInView(RobotController rc, List<MapLocation> archonLocations) throws GameActionException {
        return getLeadLocationsInViewWithLimit(rc, archonLocations, 10000);
    }

    public static List<MapLocation> getLeadLocationsInViewWithLimit(RobotController rc, List<MapLocation> archonLocations, int limit) throws GameActionException {
        int age = getAge(rc);
        if (Miner.leadLocationsInViewValidAge == age) {
            return Miner.leadLocationsInView;
        }

        int initialBytecodesLeft = Clock.getBytecodesLeft();
        List<MapLocation> leadLocationsInView = new LinkedList<>();

        for (int i = 0; i < Miner.nearbyLocationsWithLead.length; i++) {
            int j = i + getRng().nextInt(Miner.nearbyLocationsWithLead.length-i);

            MapLocation tmp = Miner.nearbyLocationsWithLead[i];
            Miner.nearbyLocationsWithLead[i] = Miner.nearbyLocationsWithLead[j];
            Miner.nearbyLocationsWithLead[j] = tmp;

            MapLocation mapLocation = Miner.nearbyLocationsWithLead[i];

            if (initialBytecodesLeft-Clock.getBytecodesLeft() > MINER_RESOURCE_IDENTIFICATION_BYTECODE_LIMIT) {
                Miner.leadLocationsInView = leadLocationsInView;
                Miner.leadLocationsInViewValidAge = age;
                return leadLocationsInView;
            }

            int leadAmount = rc.senseLead(mapLocation);

            if (leadAmount < LEAD_FARM_AMOUNT_THRESHOLD && isFarmableLocation(mapLocation, archonLocations)) {
                continue; // ignore farmable locations
            }

            leadLocationsInView.add(mapLocation);
        }

        Miner.leadLocationsInView = leadLocationsInView;
        Miner.leadLocationsInViewValidAge = age;

        return leadLocationsInView;
    }

    public static List<MapLocation> getGoldLocationsInView(RobotController rc) throws GameActionException {
        int age = getAge(rc);
        if (Miner.goldLocationsInViewValidAge == age) {
            return Miner.goldLocationsInView;
        }

        int initialBytecodesLeft = Clock.getBytecodesLeft();
        List<MapLocation> goldLocationsInView = new LinkedList<>();
        for (MapLocation mapLocation: Miner.nearbyLocationsWithGold) {
            if (initialBytecodesLeft-Clock.getBytecodesLeft() > MINER_RESOURCE_IDENTIFICATION_BYTECODE_LIMIT) {
                return goldLocationsInView;
            }
            goldLocationsInView.add(mapLocation);
        }

        Miner.goldLocationsInView = goldLocationsInView;
        Miner.goldLocationsInViewValidAge = age;
        return goldLocationsInView;
    }

    private static MapLocation getClosestResourceLocation(MapLocation src, List<MapLocation> resources) {
        MapLocation closestResourceLocation = null;
        int closestResourceLocationDistance = 0;

        for (MapLocation resourceLocation : resources) {
            int resourceLocationDistance = src.distanceSquaredTo(resourceLocation);
            if (closestResourceLocation == null || resourceLocationDistance < closestResourceLocationDistance) {
                closestResourceLocation = resourceLocation;
                closestResourceLocationDistance = resourceLocationDistance;
            }
        }

        return closestResourceLocation;
    }
}
