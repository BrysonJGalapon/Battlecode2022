package kolohe.pathing;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Optional;

public class Fuzzy implements PathFinder {
    @Override
    public Optional<Direction> findPath(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException {
        if (src.equals(dst)) {
            return Optional.empty();
        }

        Direction d = src.directionTo(dst);

        if (rc.senseRubble(src.add(d)) == 0) {
            // try straight ahead
            return Optional.of(d);
        } else if (rc.senseRubble(src.add(d.rotateLeft())) == 0) {
            // try slightly left
            return Optional.of(d.rotateLeft());
        } else if (rc.senseRubble(src.add(d.rotateRight())) == 0) {
            // try slightly right
            return Optional.of(d.rotateRight());
        } else if (rc.senseRubble(src.add(d.rotateLeft().rotateLeft())) == 0) {
            // try completely left
            return Optional.of(d.rotateLeft().rotateLeft());
        } else if (rc.senseRubble(src.add(d.rotateRight().rotateRight())) == 0) {
            // try completely right
            return Optional.of(d.rotateRight().rotateRight());
        }

        // can't make progress, don't move
        return Optional.empty();
    }
}
