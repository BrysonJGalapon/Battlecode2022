package kolohe.pathing;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Optional;

import static kolohe.utils.Parameters.DEFAULT_WALL_DETECTION_THRESHOLD;

public abstract class WallFollower {
    private Direction lastDirectionFollowingWall = null;

    // TODO make this a dynamic threshold
    private int wallDetectionTreshold = DEFAULT_WALL_DETECTION_THRESHOLD;

    protected boolean isWall(MapLocation loc, RobotController rc) throws GameActionException {
        boolean isWall = rc.canSenseLocation(loc) && rc.senseRubble(loc) >= wallDetectionTreshold;
        if (isWall) {
            rc.setIndicatorDot(loc, 255, 0,0);
        } else {
            rc.setIndicatorDot(loc, 0, 255, 0);
        }
        return isWall;
    }

    protected Optional<Direction> getDirectionOfWallMovingRight(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        Direction initialDirection = this.lastDirectionFollowingWall == null ? src.directionTo(dst) : this.lastDirectionFollowingWall.opposite().rotateRight();
        Direction d = initialDirection;

        while (isWall(src.add(d), rc)) {
            d = d.rotateRight();
            if (d.equals(initialDirection)) {
                return Optional.empty();
            }
        }

        return Optional.of(d);
    }

    protected Optional<Direction> getDirectionOfWallMovingLeft(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        Direction initialDirection = this.lastDirectionFollowingWall == null ? src.directionTo(dst) : this.lastDirectionFollowingWall.opposite().rotateLeft();
        Direction d = initialDirection;
        while (isWall(src.add(d), rc)) {
            d = d.rotateLeft();
            if (d.equals(initialDirection)) {
                return Optional.empty();
            }
        }

        return Optional.of(d);
    }

    protected void setLastDirectionFollowingWall(Direction lastDirectionFollowingWall) {
        this.lastDirectionFollowingWall = lastDirectionFollowingWall;
    }

    protected void resetLastDirectionFollowingWall() {
        this.lastDirectionFollowingWall = null;
    }

    protected Direction getLastDirectionFollowingWall() {
        return this.lastDirectionFollowingWall;
    }
}
