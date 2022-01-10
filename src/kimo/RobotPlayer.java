package kimo;

import battlecode.common.*;
import kimo.robots.archon.Archon;
import kimo.robots.builder.Builder;
import kimo.robots.laboratory.Laboratory;
import kimo.robots.miner.Miner;
import kimo.robots.sage.Sage;
import kimo.robots.soldier.Soldier;
import kimo.robots.watchtower.Watchtower;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {
    public static int MAP_WIDTH;
    public static int MAP_HEIGHT;
    public static Team MY_TEAM;
    public static Team OPP_TEAM;
    public static int ROBOT_ID; // this robot's id
    public static RobotType ROBOT_TYPE; // this robot's type
    public static int BIRTH_YEAR; // the round number this robot was born

    public static int totalLeadLastTurn = 0;
    public static int totalGoldLastTurn = 0;
    public static int totalLeadThisTurn = 0;
    public static int totalGoldThisTurn = 0;

    public static final int TEST_ROBOT_ID = 11161;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!

        MAP_WIDTH = rc.getMapWidth();
        MAP_HEIGHT = rc.getMapHeight();
        MY_TEAM = rc.getTeam();
        OPP_TEAM = rc.getTeam().opponent();
        ROBOT_ID = rc.getID();
        ROBOT_TYPE = rc.getType();
        BIRTH_YEAR = rc.getRoundNum();

        while (true) {
//            if (rc.getRoundNum() > 10) {
//                rc.resign();
//            }

            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                totalLeadThisTurn = rc.getTeamLeadAmount(MY_TEAM);
                totalGoldThisTurn = rc.getTeamGoldAmount(MY_TEAM);

                int currentRoundNumber = rc.getRoundNum();

                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case ARCHON:     Archon.run(rc);        break;
                    case MINER:      Miner.run(rc);         break;
                    case SOLDIER:    Soldier.run(rc);       break;
                    case LABORATORY: Laboratory.run(rc);    break;
                    case WATCHTOWER: Watchtower.run(rc);    break;
                    case BUILDER:    Builder.run(rc);       break;
                    case SAGE:       Sage.run(rc);          break;
                }

                if (rc.getRoundNum() != currentRoundNumber) {
                    System.out.println(String.format("%s exceeded its bytecode limit. Profile it and reduce bottlenecks.", rc.getType()));
                    rc.resign();
                }

                totalLeadLastTurn = totalLeadThisTurn;
                totalGoldLastTurn = totalGoldThisTurn;
            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }
}
