package kimo.pathing;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kimo.pathing.pathfinder.Fuzzy;

import java.util.Optional;

public class Circle {
    private final Fuzzy fuzzy = new Fuzzy();

    private MapLocation center;
    private int radiusSquared;
    private RotationPreference rotationPreference;

    public void setCenter(MapLocation center) {
        this.center = center;
    }

    public void setRadiusSquared(int radiusSquared) {
        this.radiusSquared = radiusSquared;
    }

    public void setRotationPreference(RotationPreference rotationPreference) {
        this.rotationPreference = rotationPreference;
    }

    public Optional<Direction> circle(RobotController rc, MapLocation src) throws GameActionException {
        // too close to the center, go outward
        // TODO disable left/right
        if (src.isWithinDistanceSquared(center, radiusSquared)) {
            return fuzzy.getFuzzyDirection(src, center.directionTo(src), rc);
        }

        // prevent from moving back in towards the center
        if (rotationPreference.equals(RotationPreference.LEFT)) {
            fuzzy.disableLeft();
        }

        if (rotationPreference.equals(RotationPreference.RIGHT)) {
            fuzzy.disableRight();
        }

        return Optional.empty();
    }
}
