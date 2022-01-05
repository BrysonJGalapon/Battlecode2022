package kolohe.pathing;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Optional;

public class Fuzzy extends WallFollower implements PathFinder {
    @Override
    public Optional<Direction> findPath(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        if (src.equals(dst)) {
            return Optional.empty();
        }

        Direction d = src.directionTo(dst);

        if (!isWall(src.add(d), rc) && rc.senseRobotAtLocation(src.add(d)) == null) {
            // try straight ahead
            return Optional.of(d);
        } else if (!isWall(src.add(d.rotateLeft()), rc) && rc.senseRobotAtLocation(src.add(d.rotateLeft())) == null) {
            // try slightly left
            return Optional.of(d.rotateLeft());
        } else if (!isWall(src.add(d.rotateRight()), rc) && rc.senseRobotAtLocation(src.add(d.rotateRight())) == null) {
            // try slightly right
            return Optional.of(d.rotateRight());
        } else if (!isWall(src.add(d.rotateLeft().rotateLeft()), rc) && rc.senseRobotAtLocation(src.add(d.rotateLeft().rotateLeft())) == null) {
            // try completely left
            return Optional.of(d.rotateLeft().rotateLeft());
        } else if (!isWall(src.add(d.rotateRight().rotateRight()), rc) && rc.senseRobotAtLocation(src.add(d.rotateRight().rotateRight())) == null) {
            // try completely right
            return Optional.of(d.rotateRight().rotateRight());
        }

        // can't make progress, don't move
        return Optional.empty();
    }
}
