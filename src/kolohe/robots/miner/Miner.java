package kolohe.robots.miner;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kolohe.utils.Utils;

import static kolohe.utils.Utils.RNG;

/*
    - can collect lead AND gold
    - collects resources, instantly usable (no need to path backwards to "deposit" collected resources)
    - bytecode limit: 7,500
 */
public class Miner {
    public static void run(RobotController rc) throws GameActionException {
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        // Also try to move randomly.
        Direction dir = Utils.ALL_MOVEMENT_DIRECTIONS[RNG.nextInt(Utils.ALL_MOVEMENT_DIRECTIONS.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
            System.out.println("I moved!");
        }
    }
}
