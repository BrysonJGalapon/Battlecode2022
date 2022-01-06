package kolohe.pathing.antipathfinder;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kolohe.pathing.pathfinder.Fuzzy;

import java.util.Optional;

public class AntiFuzzy implements AntiPathfinder{
    @Override
    public Optional<Direction> findPathAway(MapLocation src, MapLocation loc, RobotController rc) throws GameActionException {
        return Fuzzy.getFuzzyDirection(src, src.directionTo(loc).opposite(), rc);
    }
}
