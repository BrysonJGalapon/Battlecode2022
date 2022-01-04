package kolohe.pathing;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Optional;

public interface PathFinder {
    Optional<Direction> findPath(MapLocation src, MapLocation dst, RobotController rc) throws GameActionException;
}
