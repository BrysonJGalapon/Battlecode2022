package kimo.robots.soldier;

import battlecode.common.*;
import kimo.communication.Communicator;
import kimo.communication.Entity;
import kimo.communication.Message;
import kimo.communication.MessageType;
import kimo.communication.advanced.AdvancedCommunicator;
import kimo.pathing.Circle;
import kimo.pathing.Explore;
import kimo.pathing.pathfinder.Fuzzy;
import kimo.pathing.pathfinder.PathFinder;
import kimo.state.machine.StateMachine;
import kimo.state.machine.Stimulus;

import java.util.Optional;

import static kimo.RobotPlayer.*;
import static kimo.utils.Parameters.*;
import static kimo.utils.Utils.getAge;
import static kimo.utils.Utils.tryMove;

/*
    - relatively large attack radius
    - bytecode limit: 10,000
 */
public class Soldier {
    private static final StateMachine<SoldierState> stateMachine = StateMachine.startingAt(SoldierState.DEFEND);
    private static final PathFinder pathFinder = new Fuzzy();
    private static final Explore explorer = new Explore();
    public static final Communicator communicator = new AdvancedCommunicator();
    private static final Circle circler = new Circle();

    private static final int innerRadius = 50;
    private static int outerRadius = 100;

    private static int numTurnsOnSameSquare = 0;
    private static MapLocation previousSquare;

    private static MapLocation primaryArchonLocation;
    private static int BOREDEOM_THRESHOLD = 10;

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
        if (rc.getLocation().equals(previousSquare)) {
            numTurnsOnSameSquare += 1;
        }

        if (getAge(rc) == 0) {
            setPrimaryArchonLocation(rc);
        }
        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));
        switch (stateMachine.getCurrState()) {
            case ATTACK:   runAttackActions(rc, stimulus); break;
            case EXPLORE:   runExploreActions(rc, stimulus); break;
            case DEFEND:    runDefendActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }

        previousSquare = rc.getLocation();
    }

    private static void setPrimaryArchonLocation(RobotController rc) {
        for (RobotInfo robotInfo : rc.senseNearbyRobots(2, MY_TEAM)) {
            if (robotInfo.getType().equals(RobotType.ARCHON)) {
                primaryArchonLocation = robotInfo.getLocation();
            }
        }
    }

    public static RobotInfo chooseEnemyToAttack(RobotInfo[] enemyNearbyRobotsInfo) {
        // attack enemy with lowest health
        // soldiers with lowest health
        //

        double lowestHealth = 1200;
        int lowestHealthRobotIndex = 0;

        for (int i = 0; i < enemyNearbyRobotsInfo.length; i++) {
            switch (enemyNearbyRobotsInfo[i].getType()) {
                case SOLDIER: {
                    double health = (enemyNearbyRobotsInfo[i].getHealth() * 20) * 0.4;
                    if (health < lowestHealth) {
                        lowestHealth = health;
                        lowestHealthRobotIndex = i;
                    }
                    break;
                }
                case SAGE: {
                    double health = (enemyNearbyRobotsInfo[i].getHealth() * 10) * 0.6;
                    if (health < lowestHealth) {
                        lowestHealth = health;
                        lowestHealthRobotIndex = i;
                    }
                    break;
                }
                case BUILDER: {
                    double health = (enemyNearbyRobotsInfo[i].getHealth() * 33) * 0.7;
                    if (health < lowestHealth) {
                        lowestHealth = health;
                        lowestHealthRobotIndex = i;
                    }
                    break;
                }
                case WATCHTOWER: {
                    double health = (enemyNearbyRobotsInfo[i].getHealth() * 8) * 0.8;
                    if (health < lowestHealth) {
                        lowestHealth = health;
                        lowestHealthRobotIndex = i;
                    }
                    break;
                }
                case ARCHON: {
                    double health = enemyNearbyRobotsInfo[i].getHealth() * 0.9;
                    if (health < lowestHealth) {
                        lowestHealth = health;
                        lowestHealthRobotIndex = i;
                    }
                    break;
                }
                case MINER: {
                    double health = (enemyNearbyRobotsInfo[i].getHealth() * 25);
                    if (health < lowestHealth) {
                        lowestHealth = health;
                        lowestHealthRobotIndex = i;
                    }
                    break;
                }
            }
        }

        return enemyNearbyRobotsInfo[lowestHealthRobotIndex];
    }

    public static void runAttackActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // attack an enemy
        if (stimulus.enemyNearbyRobotsInfo.length > 0) {
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
            communicator.sendMessage(rc, Message.buildSimpleLocationMessage(
                    MessageType.LEAD_LOCATION, nearbyLocationsWithLead[0], Entity.ALL_MINERS));
        }

        Optional<Direction> direction = explorer.explore(rc.getLocation(), rc);
        if (direction.isPresent()) {
            tryMove(rc, direction.get());
        }
    }

    public static void runDefendActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (rc.senseNearbyRobots(2, MY_TEAM).length >= 5) {
            outerRadius += 10;
        } else if (rc.senseNearbyRobots(2, MY_TEAM).length <= 2) {
            outerRadius = Math.max(outerRadius-50, 100);
        }

        circler.setCenter(primaryArchonLocation);
        circler.setInnerRadiusSquared(innerRadius);
        circler.setOuterRadiusSquared(outerRadius);

        Optional<Direction> direction = circler.circle(rc, rc.getLocation());


        if (direction.isPresent()) {
            tryMove(rc, direction.get());
        }


        if (stimulus.enemyNearbyRobotsInfo.length > 0) {
            MapLocation attackLocation = chooseEnemyToAttack(stimulus.enemyNearbyRobotsInfo).location;
            if (rc.canAttack(attackLocation)) {
                rc.attack(attackLocation);
            }
        }
    }
}
