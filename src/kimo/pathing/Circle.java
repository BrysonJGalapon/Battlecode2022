package kimo.pathing;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kimo.pathing.pathfinder.Fuzzy;
import kimo.utils.Utils;

import java.util.Optional;

import static kimo.RobotPlayer.*;

public class Circle {
    private final Fuzzy fuzzy = new Fuzzy();

    private MapLocation center;
    private int innerRadiusSquared;
    private int outerRadiusSquared;
    private RotationPreference rotationPreference = Utils.getRandomValueFrom(RotationPreference.CONCRETE_ROTATION_PREFERENCES);

    private boolean isDiverging = false;
    private boolean isConverging = false;

    private MapLocation mapCenter;

    public void setCenter(MapLocation center) {
        this.center = center;
    }

    public void setInnerRadiusSquared(int innerRadiusSquared) {
        this.innerRadiusSquared = innerRadiusSquared;
    }

    public void setOuterRadiusSquared(int outerRadiusSquared) {
        this.outerRadiusSquared = outerRadiusSquared;
    }

    public void setRotationPreference(RotationPreference rotationPreference) {
        this.rotationPreference = rotationPreference;
    }

    public int getAverageRadius() {
        return (innerRadiusSquared + outerRadiusSquared) / 2;
    }

    public MapLocation getMapCenter() {
        if (mapCenter != null) {
            return mapCenter;
        }

        mapCenter = new MapLocation(MAP_WIDTH / 2, MAP_HEIGHT / 2);
        return mapCenter;
    }

    public Optional<Direction> circle(RobotController rc, MapLocation src) throws GameActionException {
        // too close to the center, go towards the MAP center (this avoids getting stuck in a corner)
        if (src.isWithinDistanceSquared(center, innerRadiusSquared)) {
            isDiverging = true;
        }

        if (isDiverging) {
            if (src.isWithinDistanceSquared(center, getAverageRadius())) {
                fuzzy.enableLeftAndRight();
                return fuzzy.getFuzzyDirection(src, src.directionTo(getMapCenter()), rc);
            } else {
                isDiverging = false;
            }
        }

        // to far away from the center, go towards the center
        if (!src.isWithinDistanceSquared(center, outerRadiusSquared)) {
            isConverging = true;
        }

        if (isConverging) {
            if (!src.isWithinDistanceSquared(center, getAverageRadius())) {
                fuzzy.enableLeftAndRight();
                return fuzzy.getFuzzyDirection(src, src.directionTo(center), rc);
            } else {
                isConverging = false;
            }
        }

        Optional<Direction> direction = Optional.empty();

        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println("My rotation preference: " + rotationPreference);
        }

        if (rotationPreference.equals(RotationPreference.LEFT)) {
            fuzzy.disableLeftAndRight();
            direction = fuzzy.getFuzzyDirection(src, center.directionTo(src).rotateLeft().rotateLeft(), rc);
        }

        if (rotationPreference.equals(RotationPreference.RIGHT)) {
            fuzzy.disableLeftAndRight();
            direction = fuzzy.getFuzzyDirection(src, center.directionTo(src).rotateRight().rotateRight(), rc);
        }

        // if can't make forward progress (for example if running into a wall), switch rotation preference
        if (!direction.isPresent()) {
            rotationPreference = rotationPreference.opposite();
            return Optional.empty();
        } else {
            return direction;
        }
    }
}
