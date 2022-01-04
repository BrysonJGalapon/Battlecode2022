package kolohe.robots.soldier;

import battlecode.common.*;
import kolohe.utils.Utils;

import static kolohe.utils.Utils.RNG;

/*
    - relatively large attack radius
    - bytecode limit: 10,000
 */
public class Soldier {
    public static void run(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
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
