package kolohe.robots.sage;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import kolohe.robots.watchtower.WatchtowerState;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;

import static kolohe.RobotPlayer.OPP_TEAM;
import static kolohe.RobotPlayer.ROBOT_TYPE;

/*
    - can cause anomalies
    - bytecode limit: 10,000

    Anomalies:
    - Abyss: 10% of resources in all squares, as well as team reserves, are lost
    - Charge: The top 5% Droids with the most friendly robots in sensor radius are destroyed
    - Fury: All buildings in turret mode have 5% of their max health burned off

    ---- sage can not envision the below ----
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
    private static final StateMachine<SageState> stateMachine = StateMachine.startingAt(SageState.DO_NOTHING);

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();
        // TODO
        return s;
    }

    public static void run(RobotController rc) throws GameActionException {
        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));
        switch (stateMachine.getCurrState()) {
            case DO_NOTHING: runDoNothingActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runDoNothingActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // do nothing :)
    }
}
