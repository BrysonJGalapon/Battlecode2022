package kolohe.robots.archon;

import battlecode.common.*;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;
import kolohe.utils.Utils;

import java.util.Map;
import java.util.Optional;

import static kolohe.utils.Utils.RNG;

/*
    - can build droids
    - can repair nearby droids
    - can move, but takes 10 turns to transform to 'Portable' mode, and takes 10 turns to transform back to 'Turret' mode
    - at least one of the Archons will initially have a Lead deposit within its vision range
    - will begin with at least 1-4 Archons: rc.getArchonCount()
    - bytecode limit: 20,000

    General Notes:
    - we are given the forecast of all anomalies, we can process it to get useful information
    - if we have bytecode left over after a turn, we can use it to process useful information
    - Vortex Anomaly:
        1. makes passing of map information much more important, and harder (how to deal with stale map information)
        2. argument could be made to detect symmetry of the map
    - map is within 20x20 and 60x60. bottom-left corner is 0,0. this means that given a location, we can pinpoint exactly
        where we are in the map, AND we know the size of the map: rc.getMapHeight() and rc.getMapWidth()
    - we passively collect 2 lead per round, we start with 200 lead and 0 gold
    - every 20 rounds, any square containing at least one lead will generate 5 lead (this means it might be disadvantageous to
        consume ALL of the lead from a square in certain situations)
    - rubble cooldown affects base cooldown c to be floor((1+r/10)*c). since calculating this might be expensive, we can
        approximate it using a more efficient function
    - when a robot is destroyed, 20% of its build value (including gold) will be dropped (this means it might be advantageous
        to send miners to locations of fallen enemy and/or friendly robots)
    - communication: each team has a shared array of 64 integers between 0 and 2^16 (this means that we can store 64*16=1024 bits at a given time)
    - disintegration: a robot is allowed to disintegrate. (this may be useful for movement, getting out of the way of something important, or
        as a way to farm Au or Pb for collection of miners)
    - mutating a building the first time only takes lead, mutating a building for the second time requires gold
    - win condition:
        1. number of Archons
        2. net value of gold
        3. net value of lead
      This means that one strategy is to protect ALL Archons (so as to have more archons than enemy) then stock up on gold/lead, and survive until turn 2000

    Questions:
    - can we interfere with enemy communication? YES
    - can we read enemy communication? YES
 */
public class Archon {
    private static final StateMachine<ArchonState> stateMachine = StateMachine.startingAt(ArchonState.RESOURCE_COLLECTION);

    private static Stimulus collectStimulus(RobotController rc) {
        Stimulus s = new Stimulus();
        s.nearbyRobotsInfo = rc.senseNearbyRobots();
        return s;
    }

    public static void run(RobotController rc) throws GameActionException {
        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);

        switch (stateMachine.getCurrState()) {
            case RESOURCE_COLLECTION:   runResourceCollectionActions(rc, stimulus); break;
            case DEFEND:                runDefendActions(rc, stimulus); break;
            case ATTACK:                runAttackActions(rc, stimulus); break;
            case SURVIVE:               runSurviveActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runResourceCollectionActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        buildMiner(rc);
    }

    public static void runDefendActions(RobotController rc, Stimulus stimulus) throws GameActionException {

    }

    public static void runAttackActions(RobotController rc, Stimulus stimulus) throws GameActionException {

    }

    public static void runSurviveActions(RobotController rc, Stimulus stimulus) throws GameActionException {

    }

    private static Optional<Direction> buildMiner(RobotController rc) throws GameActionException {
        Direction direction = Utils.getRandomDirection();
        if (!rc.canBuildRobot(RobotType.MINER, direction)) {
            return Optional.empty();
        }

        rc.buildRobot(RobotType.MINER, direction);
        return Optional.of(direction);
    }

    public static void sample(RobotController rc, Stimulus stimulus) throws GameActionException {
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
