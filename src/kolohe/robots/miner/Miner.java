package kolohe.robots.miner;

import battlecode.common.*;
import kolohe.communication.Communicate;
import kolohe.pathing.Fuzzy;
import kolohe.pathing.PathFinder;
import kolohe.pathing.TangentBug;
import kolohe.robots.archon.ArchonState;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;
import kolohe.utils.Tuple;
import kolohe.utils.Utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static kolohe.utils.Utils.RNG;

/*
    - can collect lead AND gold
    - collects resources, instantly usable (no need to path backwards to "deposit" collected resources)
    - bytecode limit: 7,500
    - action radius: 2
    - vision radius: 20
 */
public class Miner {
    public static final RobotType robotType = RobotType.MINER;

    private static final StateMachine<MinerState> stateMachine = StateMachine.startingAt(MinerState.EXPLORE);
//    private static final PathFinder pathFinder = TangentBug.builder(robotType.visionRadiusSquared).build();
    private static final PathFinder pathFinder = new Fuzzy();

    private static int age = 0; // the current age of this robot

    // caches
    private static int localGoldLocationsValidityAge = 0; // the age for which this cache is valid
    public static List<Tuple<MapLocation, Integer>> localGoldLocations = null;

    private static int localLeadLocationsValidityAge = 0; // the age for which this cache is valid
    public static List<Tuple<MapLocation, Integer>> localLeadLocations = null;

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();

        s.nearbyRobotsInfo = rc.senseNearbyRobots();
        s.myLocation = rc.getLocation();
        s.allLocationsWithinRadiusSquared = rc.getAllLocationsWithinRadiusSquared(s.myLocation, robotType.visionRadiusSquared);

        // state-specific collection of stimuli
        switch (stateMachine.getCurrState()) {
            case COLLECT:  break;
            case EXPLORE:  break;
            case TARGET:   break;
            default: throw new RuntimeException("Should not be here");
        }

        return s;
    }

    public static void run(RobotController rc) throws GameActionException {
        age += 1;
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
        // collect gold, if any
        List<Tuple<MapLocation, Integer>> goldLocationsInView = getGoldLocationsInView(rc, stimulus.allLocationsWithinRadiusSquared);
        if (goldLocationsInView.size() > 0) {
            // TODO decide location to go to as a function of gold quantity, instead of just distance
            MapLocation closestGoldLocation = getClosestResourceLocation(stimulus.myLocation, goldLocationsInView);
            rc.setIndicatorString(String.format("state: %s gold location: %s", stateMachine.getCurrState(), closestGoldLocation));

            // gold is close enough to mine
            if (stimulus.myLocation.distanceSquaredTo(closestGoldLocation) < robotType.actionRadiusSquared) {
                if (rc.canMineGold(closestGoldLocation)) {
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
        List<Tuple<MapLocation, Integer>> leadLocationsInView = getLeadLocationsInView(rc, stimulus.allLocationsWithinRadiusSquared);
        if (leadLocationsInView.size() > 0) {
            // TODO decide location to go to as a function of lead quantity, instead of just distance
            MapLocation closestLeadLocation = getClosestResourceLocation(stimulus.myLocation, leadLocationsInView);
            rc.setIndicatorDot(closestLeadLocation, 0,0,100);

            // lead is close enough to mine
            if (stimulus.myLocation.distanceSquaredTo(closestLeadLocation) < robotType.actionRadiusSquared) {
                if (rc.canMineLead(closestLeadLocation)) {
                    rc.mineLead(closestLeadLocation);
                }
            } else { // lead is not close enough to mine, so get closer
                Optional<Direction> direction = pathFinder.findPath(stimulus.myLocation, closestLeadLocation, rc);
                if (direction.isPresent() && rc.canMove(direction.get())) {
                    rc.move(direction.get());
                }
            }

            return;
        }
    }

    public static void runExploreActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        Direction direction = Utils.getRandomDirection();
        if (rc.canMove(direction)) {
            rc.move(direction);
        }
    }

    public static void runTargetActions(RobotController rc, Stimulus stimulus) throws GameActionException {

    }

    public static boolean isAnyResourceInView(RobotController rc, MapLocation[] allLocationsWithinRadiusSquared) throws GameActionException {
        List<Tuple<MapLocation, Integer>> localGoldLocations = getGoldLocationsInView(rc, allLocationsWithinRadiusSquared);
        if (localGoldLocations.size() > 0) {
            return true;
        }

        List<Tuple<MapLocation, Integer>> localLeadLocations = getLeadLocationsInView(rc, allLocationsWithinRadiusSquared);
        if (localLeadLocations.size() > 0) {
            return true;
        }

        return false;
    }

    public static List<Tuple<MapLocation, Integer>> getLeadLocationsInView(RobotController rc, MapLocation[] allLocationsWithinRadiusSquared) throws GameActionException {
        if (age == localLeadLocationsValidityAge) {
            return localLeadLocations;
        }

        List<Tuple<MapLocation, Integer>> leadLocationsInView = getLeadLocations(rc, allLocationsWithinRadiusSquared);
        localLeadLocations = leadLocationsInView;
        localLeadLocationsValidityAge = age;
        return leadLocationsInView;
    }

    public static List<Tuple<MapLocation, Integer>> getGoldLocationsInView(RobotController rc, MapLocation[] allLocationsWithinRadiusSquared) throws GameActionException {
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
        List<Tuple<MapLocation, Integer>> leadLocations = new LinkedList<>();
        for (MapLocation loc : locs) {
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

    public static void sample(RobotController rc, Stimulus stimulus) throws GameActionException {
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        // Also try to move randomly.
        Direction dir = Utils.ALL_MOVEMENT_DIRECTIONS[RNG.nextInt(Utils.ALL_MOVEMENT_DIRECTIONS.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
            System.out.println("I moved!");
        }
    }
}
