package kolohe.utils;

import battlecode.common.Direction;

import java.util.Random;

public class Utils {
    public static final int SEED = 3241;
    public static final Random RNG = new Random(SEED);
    public static int MAX_RUBBLE = 100;

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

    public static <T> T randomValueFrom(T[] arr) {
        int pick = RNG.nextInt(arr.length);
        return arr[pick];
    }
}
