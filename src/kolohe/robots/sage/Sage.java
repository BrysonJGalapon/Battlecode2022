package kolohe.robots.sage;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/*
    - can cause anomalies
    - bytecode limit: 10,000

    Anomalies:
    - Abyss: 10% of resources in all squares, as well as team reserves, are lost
    - Charge: The top 5% Droids with the most friendly robots in sensor radius are destroyed
    - Fury: All buildings in turret mode have 5% of their max health burned off
    - Vortex: Modifies the terrain of the map
        * Horizontally-Symmetric Map -> terrain reflected across vertical central line
        * Vertically-Symmetric Map -> terrain reflected across horizontal central line
        * Rotationally-Symmetric Map -> any one of:
            1. terrain reflected across vertical central line
            2. terrain reflected across horizontal central line
            3. clockwise rotation of 90 degrees (on square maps)
    - Singularity: Occurs on turn 2000, represents end of the game
 */
public class Sage {
    public static void run(RobotController rc) throws GameActionException {
        // TODO
    }
}
