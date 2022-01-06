package kolohe.pathing.pathfinder;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kolohe.pathing.WallFollower;

import java.util.Optional;

public class Fuzzy extends WallFollower implements PathFinder {
    private static final int IMMOVABLE_OBJECT_COST = 1000;

    private static int getCost(MapLocation loc, RobotController rc) throws GameActionException {
        // consider edges of the map and other robots as 'immovable objects'
        if (!rc.onTheMap(loc) || rc.senseRobotAtLocation(loc) != null) {
            return IMMOVABLE_OBJECT_COST;
        }

        return rc.senseRubble(loc);
    }

    public static Optional<Direction> getFuzzyDirection(MapLocation src, Direction straightAhead, RobotController rc) throws GameActionException {
        Direction slightlyLeft = straightAhead.rotateLeft();
        Direction slightlyRight = straightAhead.rotateRight();
        Direction left = slightlyLeft.rotateLeft();
        Direction right = slightlyRight.rotateRight();

        MapLocation straightAheadLocation = src.add(straightAhead);
        MapLocation slightlyLeftLocation = src.add(slightlyLeft);
        MapLocation slightlyRightLocation = src.add(slightlyRight);
        MapLocation leftLocation = src.add(left);
        MapLocation rightLocation = src.add(right);

        int[] costs = new int[]{
                10 * getCost(straightAheadLocation, rc),
                10 * getCost(slightlyLeftLocation, rc),
                10 * getCost(slightlyRightLocation, rc),
                35 * getCost(leftLocation, rc),
                35 *  getCost(rightLocation, rc),
        };

        int minCost = 10*IMMOVABLE_OBJECT_COST;
        int minCostIndex = -1;
        for (int i = 0; i < costs.length; i++) {
            if (costs[i] < minCost) {
                minCost = costs[i];
                minCostIndex = i;
            }
        }

        // can't make progress, don't move
        if (minCostIndex == -1) {
            return Optional.empty();
        }

        switch (minCostIndex) {
            case 0: return Optional.of(straightAhead);
            case 1: return Optional.of(slightlyLeft);
            case 2: return Optional.of(slightlyRight);
            case 3: return Optional.of(left);
            case 4: return Optional.of(right);
            default: throw new RuntimeException("Should not be here");
        }
    }

    @Override
    public Optional<Direction> findPath(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        if (src.equals(dst)) {
            return Optional.empty();
        }

        return getFuzzyDirection(src, src.directionTo(dst), rc);
    }
}
