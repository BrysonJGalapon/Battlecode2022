package kolohe.robots.archon;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import kolohe.utils.Utils;

import static kolohe.utils.Utils.RNG;

public class Archon {

    public static void run(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = Utils.ALL_MOVEMENT_DIRECTIONS[RNG.nextInt(Utils.ALL_MOVEMENT_DIRECTIONS.length)];
        if (RNG.nextBoolean()) {
            // Let's try to build a miner.
            rc.setIndicatorString("Trying to build a miner");
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
            }
        } else {
            // Let's try to build a soldier.
            rc.setIndicatorString("Trying to build a soldier");
            if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
            }
        }
    }
}
