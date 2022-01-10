package kolohe.robots.soldier;

import battlecode.common.*;
import kolohe.communication.Communicator;
import kolohe.communication.Entity;
import kolohe.communication.Message;
import kolohe.communication.MessageType;
import kolohe.communication.basic.BasicCommunicator;
import kolohe.pathing.Explore;
import kolohe.pathing.pathfinder.Fuzzy;
import kolohe.pathing.pathfinder.PathFinder;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;

import java.util.Optional;

import static kolohe.RobotPlayer.*;
import static kolohe.utils.Parameters.SOLDIER_RECEIVE_MESSAGE_BYTECODE_LIMIT;
import static kolohe.utils.Parameters.SOLDIER_RECEIVE_MESSAGE_LIMIT;
import static kolohe.utils.Utils.tryMove;

/*
    - relatively large attack radius
    - bytecode limit: 10,000
 */
public class Soldier {
    private static final StateMachine<SoldierState> stateMachine = StateMachine.startingAt(SoldierState.EXPLORE);
    private static final PathFinder pathFinder = new Fuzzy();
    private static final Explore explorer = new Explore();
    public static final Communicator communicator = new BasicCommunicator();

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();
        s.myLocation = rc.getLocation();
        s.enemyNearbyRobotsInfo = rc.senseNearbyRobots(ROBOT_TYPE.visionRadiusSquared, OPP_TEAM);
        s.messages = communicator.receiveMessages(rc, SOLDIER_RECEIVE_MESSAGE_LIMIT, SOLDIER_RECEIVE_MESSAGE_BYTECODE_LIMIT);

        // state-specific collection of stimuli
        switch (stateMachine.getCurrState()) {
            case ATTACK:  break;
            case EXPLORE:
                s.friendlyAdjacentNearbyRobotsInfo = rc.senseNearbyRobots(2, MY_TEAM);
                break;
            case DEFEND:   break;
            default: throw new RuntimeException("Should not be here");
        }
        return s;
    }

    public static void run(RobotController rc) throws GameActionException {
        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));
        switch (stateMachine.getCurrState()) {
            case ATTACK:   runAttackActions(rc, stimulus); break;
            case EXPLORE:   runExploreActions(rc, stimulus); break;
            case DEFEND:    runDefendActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    private static RobotInfo chooseEnemyToAttack(RobotInfo[] enemyNearbyRobotsInfo) {
        // TODO improve decision-making on which enemy to attack
        // priority: archon,
        //           solider,
        //           builder,
        //           miner

        int robotToAttack = 0;

        for (int i = 0; i < enemyNearbyRobotsInfo.length; i++) {
            if (enemyNearbyRobotsInfo[i].getType() == RobotType.ARCHON) {
                robotToAttack = i;
                break;
            }
            else if (enemyNearbyRobotsInfo[i].getType() == RobotType.SOLDIER) {
                robotToAttack = i;
            }
            else if (enemyNearbyRobotsInfo[i].getType() == RobotType.BUILDER) {
                robotToAttack = i;
            }
        }

        return enemyNearbyRobotsInfo[robotToAttack];
    }

    // TODO dayne to improve
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

        // since soldiers do not use much bytecode in this state, use extra bytecode to report lead locations
        MapLocation[] nearbyLocationsWithLead = rc.senseNearbyLocationsWithLead(ROBOT_TYPE.visionRadiusSquared);
        if (nearbyLocationsWithLead.length > 0) {
            communicator.sendMessage(rc, Message.buildSimpleLocationMessage(MessageType.LEAD_LOCATION, nearbyLocationsWithLead[0], Entity.ALL_MINERS));
        }

        Optional<Direction> direction = explorer.explore(rc.getLocation(), rc);
        if (direction.isPresent()) {
            tryMove(rc, direction.get());
        }
    }

    public static void runDefendActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // TODO
    }

    private static int getAge(RobotController rc) {
        return rc.getRoundNum()-BIRTH_YEAR;
    }

    private static void sample(RobotController rc) throws GameActionException {
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

        if (enemies.length == 0) {
            Optional<Direction> direction = explorer.explore(rc.getLocation(), rc);
            if (direction.isPresent() && rc.canMove(direction.get())) {
                rc.move(direction.get());
            }
        } else {
            // try to move toward enemy
            Optional<Direction> direction = pathFinder.findPath(rc.getLocation(), enemies[0].location, rc);
            if (direction.isPresent() && rc.canMove(direction.get())) {
                rc.move(direction.get());
            }
        }
    }
}
