package kolohe.utils;

import battlecode.common.Direction;

import java.util.Random;

public class Utils {
    public static final int SEED = 3241;
    public static final Random RNG = new Random(SEED);

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
}
