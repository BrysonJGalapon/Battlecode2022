package kimo.robots.sage;

import battlecode.common.*;
import kimo.communication.Entity;
import kimo.communication.Message;
import kimo.communication.MessageType;
import kimo.pathing.Explore;
import kimo.pathing.pathfinder.Fuzzy;
import kimo.pathing.pathfinder.PathFinder;
import kimo.robots.watchtower.WatchtowerState;
import kimo.state.machine.StateMachine;
import kimo.state.machine.Stimulus;

import java.util.Optional;

import static kimo.RobotPlayer.*;
import static kimo.utils.Parameters.SOLDIER_RECEIVE_MESSAGE_BYTECODE_LIMIT;
import static kimo.utils.Parameters.SOLDIER_RECEIVE_MESSAGE_LIMIT;
import static kimo.utils.Utils.getAge;
import static kimo.utils.Utils.tryMove;

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
    private static final StateMachine<SageState> stateMachine = StateMachine.startingAt(SageState.EXPLORE);
    private static final Explore explorer = new Explore();
    private static final PathFinder pathFinder = new Fuzzy();

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();
        s.myLocation = rc.getLocation();
        s.enemyNearbyRobotsInfo = rc.senseNearbyRobots(ROBOT_TYPE.visionRadiusSquared, OPP_TEAM);

        // state-specific collection of stimuli
        switch (stateMachine.getCurrState()) {
            case ATTACK:  break;
            case EXPLORE:
                s.friendlyAdjacentNearbyRobotsInfo = rc.senseNearbyRobots(2, MY_TEAM);
                break;
            default: throw new RuntimeException("Should not be here");
        }
        return s;
    }

    public static void run(RobotController rc) throws GameActionException {
        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));
        switch (stateMachine.getCurrState()) {
            case ATTACK: runAttackActions(rc, stimulus); break;
            case EXPLORE: runExploreActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runExploreActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // was just born, set initial direction of the explorer to move away from archon
        if (getAge(rc) == 0) {
            for (RobotInfo robotInfo : stimulus.friendlyAdjacentNearbyRobotsInfo) {
                if (!robotInfo.type.equals(RobotType.ARCHON)) {
                    continue;
                }

                Direction oppositeDirection = stimulus.myLocation.directionTo(robotInfo.location).opposite();
                explorer.setDirection(oppositeDirection);
                break;
            }
        }

        Optional<Direction> direction = explorer.explore(rc.getLocation(), rc);
        if (direction.isPresent()) {
            tryMove(rc, direction.get());
        }
    }

    public static void runAttackActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // attack an enemy
        MapLocation attackLocation = chooseEnemyToAttack(stimulus.enemyNearbyRobotsInfo).location;
        if (rc.canAttack(attackLocation)) {
            rc.attack(attackLocation);
        }

        // try to move toward the enemy
        Optional<Direction> direction = pathFinder.findPath(rc.getLocation(), attackLocation, rc);
        if (direction.isPresent() && rc.canMove(direction.get())) {
            rc.move(direction.get());
        }
    }

    private static RobotInfo chooseEnemyToAttack(RobotInfo[] enemyNearbyRobotsInfo) {
        // TODO improve decision-making on which enemy to attack
        return enemyNearbyRobotsInfo[0];
    }
}
