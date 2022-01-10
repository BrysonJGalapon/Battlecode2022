package kimo.pathing.pathfinder;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kimo.pathing.RotationPreference;
import kimo.pathing.WallFollower;
import kimo.utils.Utils;

import java.util.Optional;

public class Bug extends WallFollower implements PathFinder {
    private final RotationPreference rotationPreference;
    private final boolean verbose;

    private MapLocation closestMapLocation;
    private RotationPreference currRotationPreference;

    private Bug(RotationPreference rotationPreference, boolean verbose) {
        this.rotationPreference = rotationPreference;
        this.verbose = verbose;

        this.closestMapLocation = null;
        this.currRotationPreference = (this.rotationPreference == RotationPreference.RANDOM) ? Utils.getRandomValueFrom(RotationPreference.CONCRETE_ROTATION_PREFERENCES) : this.rotationPreference;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean verbose = true;
        private RotationPreference rotationPreference = RotationPreference.RANDOM;

        private Builder() {

        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder rotationPreference(RotationPreference rotationPreference) {
            this.rotationPreference = rotationPreference;
            return this;
        }

        public Bug build() {
            return new Bug(this.rotationPreference, this.verbose);
        }
    }

    public MapLocation getClosestAdjacentMapLocation(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        MapLocation closestAdjacentMapLocation = null;
        int closestMapLocationDistance = 0;
        for (Direction d: Direction.values()) {
            MapLocation newMapLocation = src.add(d);

            // TODO address robots as a 'wall'
            if (isWall(newMapLocation, rc)) {
                continue;
            }

            int dist = newMapLocation.distanceSquaredTo(dst);
            if (closestAdjacentMapLocation == null || dist < closestMapLocationDistance) {
                closestAdjacentMapLocation = newMapLocation;
                closestMapLocationDistance = dist;
            }
        }

        return closestAdjacentMapLocation;
    }

    @Override
    public Optional<Direction> findPath(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        if (this.closestMapLocation == null) {
            this.closestMapLocation = src;
        }

        if (src.equals(dst)) {
            return Optional.empty();
        }

        MapLocation closestAdjacentMapLocation = getClosestAdjacentMapLocation(src, dst, rc);

        // can't move at all
        if (closestAdjacentMapLocation == null) {
            return Optional.empty();
        }

        // an adjacent location is closer than we've ever been before. use that direction
        if (closestAdjacentMapLocation.distanceSquaredTo(dst) < this.closestMapLocation.distanceSquaredTo(dst)) {
            Direction d = src.directionTo(closestAdjacentMapLocation);
            this.closestMapLocation = closestAdjacentMapLocation;
            this.resetLastDirectionFollowingWall();
            return Optional.of(d);
        }

        // go in a direction that follows the wall
        Optional<Direction> d;
        switch (this.currRotationPreference) {
            case LEFT:  d = this.getDirectionOfWallMovingLeft(src, dst, rc); break;
            case RIGHT: d = this.getDirectionOfWallMovingRight(src, dst, rc); break;
            default: throw new RuntimeException("Should not be here");
        }

        if (d.isPresent()) {
            this.setLastDirectionFollowingWall(d.get());
        } else {
            this.resetLastDirectionFollowingWall();
        }

        return d;
    }

    public RotationPreference getCurrRotationPreference() {
        return this.currRotationPreference;
    }

    public void setCurrRotationPreference(RotationPreference rotationPreference) {
        this.currRotationPreference = rotationPreference;
    }

    @Override
    public String toString() {
        return String.format("Bug[%s,%s,%s]", rotationPreference, closestMapLocation, currRotationPreference);
    }
}
