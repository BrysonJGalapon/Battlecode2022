package kolohe.utils;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import kolohe.pathing.RotationPreference;

import java.util.Optional;
import java.util.Random;

import static kolohe.RobotPlayer.BIRTH_YEAR;
import static kolohe.RobotPlayer.ROBOT_ID;
import static kolohe.pathing.RotationPreference.getRandomConcreteRotationPreference;

public class Utils {
    public static final long SEED_FACTOR = 123141;
    private static Random RNG;

    /** Array containing all the possible movement directions. */
    public static final Direction[] ALL_MOVEMENT_DIRECTIONS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public static int getAge(RobotController rc) {
        return rc.getRoundNum()-BIRTH_YEAR;
    }

    public static Random getRng() {
        if (RNG == null) {
            RNG = new Random(SEED_FACTOR * ROBOT_ID);
        }

        return RNG;
    }

    public static <T> T getRandomValueFrom(T[] arr) {
        int pick = getRng().nextInt(arr.length);
        return arr[pick];
    }

    public static Direction getRandomDirection() {
        return getRandomValueFrom(ALL_MOVEMENT_DIRECTIONS);
    }

    public static boolean tryMove(RobotController rc, Direction direction) throws GameActionException {
        if (rc.canMove(direction)) {
            rc.move(direction);
            return true;
        }

        return false;
    }

    public static Optional<Direction> tryMoveRandomDirection(RobotController rc) throws GameActionException {
        RotationPreference rotationPreference = getRandomConcreteRotationPreference();
        Direction direction = Utils.getRandomDirection();
        for (int i = 0; i < Utils.ALL_MOVEMENT_DIRECTIONS.length; i++) {
            if (!rc.canMove(direction)) {
                switch (rotationPreference) {
                    case LEFT: direction = direction.rotateLeft();
                    case RIGHT: direction = direction.rotateRight();
                }
                continue;
            }

            rc.move(direction);
            return Optional.of(direction);
        }

        return Optional.empty();
    }

    public static Optional<Direction> tryBuildRandomDirection(RobotType robotType, RobotController rc) throws GameActionException {
        RotationPreference rotationPreference = getRandomConcreteRotationPreference();
        Direction direction = Utils.getRandomDirection();
        for (int i = 0; i < Utils.ALL_MOVEMENT_DIRECTIONS.length; i++) {
            if (!rc.canBuildRobot(robotType, direction)) {
                switch (rotationPreference) {
                    case LEFT: direction = direction.rotateLeft();
                    case RIGHT: direction = direction.rotateRight();
                }
                continue;
            }

            rc.buildRobot(robotType, direction);
            return Optional.of(direction);
        }

        return Optional.empty();
    }
}
