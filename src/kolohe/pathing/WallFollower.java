package kolohe.pathing;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Optional;

import static kolohe.utils.Utils.MAX_RUBBLE;

public abstract class WallFollower {
    private Direction lastDirectionFollowingWall = null;

    protected Optional<Direction> getDirectionOfWallMovingRight(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        Direction initialDirection = this.lastDirectionFollowingWall == null ? src.directionTo(dst) : this.lastDirectionFollowingWall.opposite().rotateRight();
        Direction d = initialDirection;
        while (rc.senseRubble(src.add(d)) == MAX_RUBBLE) {
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
        while (rc.senseRubble(src.add(d)) == MAX_RUBBLE) {
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
