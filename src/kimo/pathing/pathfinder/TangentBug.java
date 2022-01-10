package kimo.pathing.pathfinder;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kimo.pathing.RotationPreference;
import kimo.utils.Tuple;
import kimo.utils.Utils;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static kimo.utils.Parameters.DEFAULT_TANGENT_BUG_MAX_BACKTRACK_COUNT;

public class TangentBug implements PathFinder {
    private final int sightRadius;
    private final boolean verbose;

    private final Deque<MapLocation> visitedMapLocations;
    private final int maxBacktrackCount;

    private Bug bug;
    private MapLocation bugDst;
    private Bug chosenBug;

    private TangentBug(int sightRadius, boolean verbose, Deque<MapLocation> visitedMapLocations, int maxBacktrackCount) {
        this.sightRadius = sightRadius;
        this.verbose = verbose;
        this.visitedMapLocations = visitedMapLocations;
        this.maxBacktrackCount = maxBacktrackCount;

        this.bug = null;
        this.bugDst = null;
        this.chosenBug = null;
    }

    public static Builder builder(int sightRadius) {
        return new Builder(sightRadius);
    }

    public static class Builder {
        private final int sightRadius;
        private boolean verbose = true;
        private Deque<MapLocation> visitedMapLocations = new LinkedList<>();
        private int maxBacktrackCount = DEFAULT_TANGENT_BUG_MAX_BACKTRACK_COUNT;

        private Builder(int sightRadius) {
            this.sightRadius = sightRadius;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder visitedMapLocations(Deque<MapLocation> visitedMapLocations) {
            this.visitedMapLocations = visitedMapLocations;
            return this;
        }

        public Builder maxBacktrackCount(int maxBacktrackCount) {
            this.maxBacktrackCount = maxBacktrackCount;
            return this;
        }

        public TangentBug build() {
            return new TangentBug(this.sightRadius, this.verbose, this.visitedMapLocations, this.maxBacktrackCount);
        }
    }

    public void recordToVisitedMapLocations(MapLocation loc) {
        if (visitedMapLocations.size() == maxBacktrackCount) {
            visitedMapLocations.removeFirst();
        }
        visitedMapLocations.addLast(loc);
    }

    public boolean isInVisitedMapLocations(MapLocation loc) {
        return visitedMapLocations.contains(loc);
    }

    public boolean isAnyInVisitedMapLocations(List<MapLocation> locs) {
        for (MapLocation loc : locs) {
            if (isInVisitedMapLocations(loc)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Optional<Direction> findPath(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        if (src.equals(dst)) {
            return Optional.empty();
        }

        // record locations visited
        recordToVisitedMapLocations(src);

        if (bug != null && bugDst != null && !src.equals(bugDst)) {
            rc.setIndicatorLine(src, bugDst, 0, 0, 100);
            return bug.findPath(src, bugDst, rc);
        }

        // simulate two different paths, the left bug path and the right bug path
        Bug leftBug = Bug.builder().verbose(verbose).rotationPreference(RotationPreference.LEFT).build();
        Bug rightBug = Bug.builder().verbose(verbose).rotationPreference(RotationPreference.RIGHT).build();

        Tuple<Optional<Direction>, List<MapLocation>> leftBugSimulation = simulate(leftBug, src, dst, rc);
        Tuple<Optional<Direction>, List<MapLocation>> rightBugSimulation = simulate(rightBug, src, dst, rc);

        List<MapLocation> leftBugPath = leftBugSimulation.getY();
        List<MapLocation> rightBugPath = rightBugSimulation.getY();

        MapLocation leftBugFinalLocation = leftBugPath.get(leftBugPath.size()-1);
        MapLocation rightBugFinalLocation = rightBugPath.get(rightBugPath.size()-1);

        int leftBugDistance = leftBugFinalLocation.distanceSquaredTo(dst);
        int rightBugDistance = rightBugFinalLocation.distanceSquaredTo(dst);

        RotationPreference bestRotationPreference;
        if (leftBugDistance < rightBugDistance) {
            bestRotationPreference = RotationPreference.LEFT;
        } else if (rightBugDistance < leftBugDistance) {
            bestRotationPreference = RotationPreference.RIGHT;
        } else { // leftBugDistance == rightBugDistance
            bestRotationPreference = Utils.getRandomValueFrom(RotationPreference.CONCRETE_ROTATION_PREFERENCES);
        }

        List<MapLocation> bestPath;

        // check if simulation will cause us to move in circles and if so switch rotationPreference
        bestPath = bestRotationPreference.equals(RotationPreference.LEFT) ? leftBugPath : rightBugPath;
        if (isAnyInVisitedMapLocations(bestPath)) {
            bestRotationPreference = bestRotationPreference.opposite();
        }

        // do a check once more, and if both rotationPreferences will cause us to move in circles, re-use the chosen bug, and set up bugDst to force re-simulation
        bestPath = bestRotationPreference.equals(RotationPreference.LEFT) ? leftBugPath : rightBugPath;
        if (isAnyInVisitedMapLocations(bestPath)) {
            Optional<Direction> d = chosenBug.findPath(src, dst, rc);
            if (d.isPresent()) {
                bugDst = src.add(d.get());
            }
            return d;
        }

        MapLocation bestFinalLocation = bestRotationPreference.equals(RotationPreference.LEFT) ? leftBugFinalLocation : rightBugFinalLocation;

        // no valid paths. don't move
        if (src.equals(bestFinalLocation)) {
            return Optional.empty();
        }

        bug = Bug.builder().verbose(verbose).build();
        bugDst = bestFinalLocation;
        chosenBug = bestFinalLocation.equals(leftBugFinalLocation) ? leftBug : rightBug;

        return bug.findPath(src, bugDst, rc);
    }

    private Tuple<Optional<Direction>, List<MapLocation>> simulate(Bug bug, MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        MapLocation curr = src;
        Optional<Direction> firstStep = Optional.empty();
        List<MapLocation> path = new LinkedList<>();
        for (int i = 0; src.distanceSquaredTo(curr) <= sightRadius; i++) {
            if (curr.equals(dst)) {
                break;
            }

            Optional<Direction> d = bug.findPath(curr, dst, rc);
            if (i == 0) {
                firstStep = d;
            }

            if (d.isPresent()) {
                curr = curr.add(d.get());
            }

            path.add(curr);
        }

        return Tuple.of(firstStep, path);
    }
}
