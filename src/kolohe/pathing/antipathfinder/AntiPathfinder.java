package kolohe.pathing.antipathfinder;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Optional;

public interface AntiPathfinder {
    Optional<Direction> findPathAway(MapLocation src, MapLocation loc, RobotController rc) throws GameActionException;
}
