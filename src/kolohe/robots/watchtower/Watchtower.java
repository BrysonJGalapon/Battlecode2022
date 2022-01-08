package kolohe.robots.watchtower;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;

import static kolohe.RobotPlayer.*;

/*
    - defensive buildings
    - relatively large attack radius, relatively large damage
    - can move, but takes 10 turns to transform to 'Portable' mode, and takes 10 turns to transform back to 'Turret' mode
    - bytecode limit: 10,000
 */
public class Watchtower {
    private static final StateMachine<WatchtowerState> stateMachine = StateMachine.startingAt(WatchtowerState.DEFEND);

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();
        s.myLocation = rc.getLocation();
        s.enemyNearbyRobotsInfo = rc.senseNearbyRobots(ROBOT_TYPE.actionRadiusSquared, OPP_TEAM);
        return s;
    }

    public static void run(RobotController rc) throws GameActionException {
        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));
        switch (stateMachine.getCurrState()) {
            case DEFEND: runDefendActions(rc, stimulus); break;
            case MOVE:   runMoveActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runDefendActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // no enemies to attack
        if (stimulus.enemyNearbyRobotsInfo.length == 0) {
            return;
        }

        // found an enemy, attack it!
        MapLocation attackLocation = chooseEnemyToAttack(stimulus.enemyNearbyRobotsInfo).location;
        if (rc.canAttack(attackLocation)) {
            rc.attack(attackLocation);
        }
    }

    public static void runMoveActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // TODO
    }

    private static RobotInfo chooseEnemyToAttack(RobotInfo[] enemyNearbyRobotsInfo) {
        // TODO improve decision-making on which enemy to attack
        return enemyNearbyRobotsInfo[0];
    }
}
