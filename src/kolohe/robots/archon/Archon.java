package kolohe.robots.archon;

import battlecode.common.*;
import kolohe.communication.Communicator;
import kolohe.communication.Message;
import kolohe.communication.basic.BasicCommunicator;
import kolohe.resource.allocation.ResourceAllocation;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;

import java.util.Optional;

import static kolohe.RobotPlayer.*;
import static kolohe.communication.Entity.ALL_BUILDERS;
import static kolohe.communication.Entity.ALL_ROBOTS;
import static kolohe.communication.MessageType.ARCHON_STATE;
import static kolohe.communication.MessageType.BUILD_WATCHTOWER_LOCATION;
import static kolohe.utils.Utils.*;

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
    public static final StateMachine<ArchonState> stateMachine = StateMachine.startingAt(ArchonState.RESOURCE_COLLECTION);
    public static final Communicator communicator = new BasicCommunicator();
    public static final ResourceAllocation resourceAllocation = new ResourceAllocation();

    public static int[] buildDistribution = null;
    public static double leadBudget = 0;
    public static double goldBudget = 0;
    public static RobotController rc;
    public static RobotType robotToBuild;

    private static Stimulus collectStimulus(RobotController rc)  {
        Stimulus s = new Stimulus();
        s.friendlyNearbyRobotsInfo = rc.senseNearbyRobots(ROBOT_TYPE.visionRadiusSquared, MY_TEAM);
        s.enemyNearbyRobotsInfo = rc.senseNearbyRobots(ROBOT_TYPE.visionRadiusSquared, OPP_TEAM);
        return s;
    }

    public static void run(RobotController rc) throws GameActionException {
        // TODO delete this
        Archon.rc = rc;

        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);

        rc.setIndicatorString("I'm in state: " + stateMachine.getCurrState());

        // broadcast state to all robots
        Message archonStateMessage = new Message(ARCHON_STATE, ALL_ROBOTS);
        archonStateMessage.location = rc.getLocation();
        archonStateMessage.archonState = stateMachine.getCurrState();
        communicator.sendMessage(rc, archonStateMessage);

        switch (stateMachine.getCurrState()) {
            case RESOURCE_COLLECTION:   runResourceCollectionActions(rc, stimulus); break;
            case DEFEND:                runDefendActions(rc, stimulus); break;
            case ATTACK:                runAttackActions(rc, stimulus); break;
            case SURVIVE:               runSurviveActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runResourceCollectionActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (buildDistribution == null) {
            throw new RuntimeException("Should not be here");
        }

        // TODO after done testing, setup configuration of walltowers and implement it here
        MapLocation newWatchTowerLocation = rc.getLocation().translate(4, 4);

        // check if a watchTower is already there, and if not then tell builders to build there
        RobotInfo robot = rc.senseRobotAtLocation(newWatchTowerLocation);
        if (robot == null || robot.getTeam().equals(OPP_TEAM) || !robot.getType().equals(RobotType.WATCHTOWER)) {
            communicator.sendMessage(rc, Message.buildSimpleLocationMessage(BUILD_WATCHTOWER_LOCATION, newWatchTowerLocation, ALL_BUILDERS));
        }

        // figure out which robot to build
        if (robotToBuild == null) {
            robotToBuild = sampleBuildDistribution(buildDistribution);
        }

        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println("I want to build: " + robotToBuild);
            System.out.println("My lead budget is: " + leadBudget);
        }
        if (isAffordable(rc, stimulus, robotToBuild)) {
            if (ROBOT_ID == TEST_ROBOT_ID) {
                System.out.println("...I can afford it!");
            }
            Optional<Direction> direction = tryBuildRandomDirection(robotToBuild, rc);
            if (direction.isPresent()) {
                // successfully built the robot, deduct from budget and setup re-sampling
                deductBudget(robotToBuild);
                robotToBuild = null;
            }
        } else {
            if (ROBOT_ID == TEST_ROBOT_ID) {
                System.out.println("...I can't afford it");
            }
        }
    }

    private static boolean isAffordable(RobotController rc, Stimulus stimulus, RobotType robotType) throws GameActionException {
        resourceAllocation.run(rc, stimulus);

        switch (robotType) {
            case MINER:
            case BUILDER:
            case SOLDIER:
                double leadAllowance = resourceAllocation.getLeadAllowance(rc, ROBOT_TYPE);
                System.out.println("My lead allowance for this turn as a " + ROBOT_TYPE + " is: " + leadAllowance);
                leadBudget += leadAllowance;
                return robotType.buildCostLead <= leadBudget;
            case SAGE:
                double goldAllowance = resourceAllocation.getGoldAllowance(rc, ROBOT_TYPE);
                goldBudget += goldAllowance;
                return robotType.buildCostGold <= goldBudget;
            default: throw new RuntimeException("Should not be here");
        }
    }

    private static void deductBudget(RobotType robotType) {
        switch (robotType) {
            case MINER:
            case BUILDER:
            case SOLDIER:
                leadBudget -= robotType.buildCostLead;
                break;
            case SAGE:
                goldBudget -= robotType.buildCostGold;
                break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runDefendActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (buildDistribution == null) {
            throw new RuntimeException("Should not be here");
        }

        RobotType robotType = sampleBuildDistribution(buildDistribution);
        tryBuildRandomDirection(robotType, rc);
    }

    public static void runAttackActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (buildDistribution == null) {
            throw new RuntimeException("Should not be here");
        }

        RobotType robotType = sampleBuildDistribution(buildDistribution);
        tryBuildRandomDirection(robotType, rc);
    }

    public static void runSurviveActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (buildDistribution == null) {
            throw new RuntimeException("Should not be here");
        }

        RobotType robotType = sampleBuildDistribution(buildDistribution);
        tryBuildRandomDirection(robotType, rc);
    }

    private static RobotType sampleBuildDistribution(int[] buildDistribution) {
        //   {RobotType.MINER, RobotType.BUILDER, RobotType.SAGE, RobotType.SOLDIER};
        int minerSum = buildDistribution[0];
        int builderSum = minerSum + buildDistribution[1];
        int sageSum = builderSum + buildDistribution[2];
        int totalSum = sageSum + buildDistribution[3];

        int sample = getRng().nextInt(totalSum);

        if (sample < minerSum) {
            return RobotType.MINER;
        } else if (sample < builderSum) {
            return RobotType.BUILDER;
        } else if (sample < sageSum) {
            return RobotType.SAGE;
        } else {
            return RobotType.SOLDIER;
        }
    }
}
