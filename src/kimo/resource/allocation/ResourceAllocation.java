package kimo.resource.allocation;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import kimo.communication.Message;
import kimo.communication.MessageType;
import kimo.state.machine.StateMachine;
import kimo.state.machine.Stimulus;

import java.util.Optional;

import static kimo.RobotPlayer.*;
import static kimo.utils.Parameters.*;

public class ResourceAllocation {
    private final StateMachine<ResourceAllocationState> stateMachine = StateMachine.startingAt(ResourceAllocationState.RESOURCE_COLLECTION);

    private int[] leadLimitDistribution;
    private int[] goldLimitDistribution;

    private int archonCount;
    private int archonCountValidRoundNumber = -1; // the number for which the current archonCount is valid

    public double getLeadAllowance(RobotController rc, RobotType robotType) {
        int leadProfit = totalLeadThisTurn-totalLeadLastTurn;
        if (leadProfit < 0) {
            return 0;
        }

        int percent = leadLimitDistribution[getIndexInDistribution(robotType)];
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println("My lead profit this turn: " + leadProfit);
            System.out.println("My lead profit percent this turn: " + percent);
        }
        return ((1.0 * leadProfit * percent / 100.0) / getArchonCount(rc)) * RESOURCE_ALLOCATION_RESOURCE_OVERLAP_FACTOR;
    }

    public double getGoldAllowance(RobotController rc, RobotType robotType) {
        int goldProfit = totalGoldThisTurn-totalGoldLastTurn;
        if (goldProfit < 0) {
            return 0;
        }
//        if (ROBOT_ID == TEST_ROBOT_ID) {
//            System.out.println("My gold profit this turn: " + goldProfit);
//        }
        int percent = goldLimitDistribution[getIndexInDistribution(robotType)];
        return ((1.0 * goldProfit * percent / 100) / getArchonCount(rc)) * RESOURCE_ALLOCATION_RESOURCE_OVERLAP_FACTOR;
    }

    public void setLeadLimitDistribution(int[] leadLimitDistribution) {
        this.leadLimitDistribution = leadLimitDistribution;
    }

    public void setGoldLimitDistribution(int[] goldLimitDistribution) {
        this.goldLimitDistribution = goldLimitDistribution;
    }

    public int getArchonCount(RobotController rc) {
        if (rc.getRoundNum() != this.archonCountValidRoundNumber) {
            this.archonCount = rc.getArchonCount();
            this.archonCountValidRoundNumber = rc.getRoundNum();
        }

        return this.archonCount;
    }

    public void run(RobotController rc, Stimulus stimulus) throws GameActionException {
        stateMachine.transition(stimulus, rc);
        if (ROBOT_ID == TEST_ROBOT_ID) {
            System.out.println("Resource Allocation in state: " + stateMachine.getCurrState());
        }
        switch (stateMachine.getCurrState()) {
            case RESOURCE_COLLECTION:
                setLeadLimitDistribution(RESOURCE_ALLOCATION_RESOURCE_COLLECTION_LEAD_DISTIBUTION);
                setGoldLimitDistribution(RESOURCE_ALLOCATION_RESOURCE_COLLECTION_GOLD_DISTIBUTION);
                break;
            case DEFEND:
                setLeadLimitDistribution(RESOURCE_ALLOCATION_DEFEND_LEAD_DISTIBUTION);
                setGoldLimitDistribution(RESOURCE_ALLOCATION_DEFEND_GOLD_DISTIBUTION);
                break;
            case ATTACK:
                setLeadLimitDistribution(RESOURCE_ALLOCATION_ATTACK_LEAD_DISTIBUTION);
                setGoldLimitDistribution(RESOURCE_ALLOCATION_ATTACK_GOLD_DISTIBUTION);
                break;
            case SURVIVE:
                setLeadLimitDistribution(RESOURCE_ALLOCATION_SURVIVE_LEAD_DISTIBUTION);
                setGoldLimitDistribution(RESOURCE_ALLOCATION_SURVIVE_GOLD_DISTIBUTION);
                break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    private static int getIndexInDistribution(RobotType robotType) {
        switch (robotType) {
            case ARCHON: return 0;
            case BUILDER: return 1;
            case LABORATORY: return 2;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static Optional<MapLocation> getClosestBroadcastedArchonLocation(RobotController rc, Stimulus stimulus) {
        // check for broadcasted archon locations, and select the closest one
        MapLocation bestBroadcastedArchonLocation = null;
        int bestBroadcastedArchonLocationDistance = 0;
        for (Message message : stimulus.messages) {
            if (!message.messageType.equals(MessageType.ARCHON_STATE)) {
                continue;
            }

            MapLocation broadcastedArchonLocation = message.location;
            int broadcastedArchonLocationDistance = rc.getLocation().distanceSquaredTo(broadcastedArchonLocation);
            if (bestBroadcastedArchonLocation == null || broadcastedArchonLocationDistance < bestBroadcastedArchonLocationDistance) {
                bestBroadcastedArchonLocation = broadcastedArchonLocation;
                bestBroadcastedArchonLocationDistance = broadcastedArchonLocationDistance;
            }
        }

        return (bestBroadcastedArchonLocation != null) ? Optional.of(bestBroadcastedArchonLocation) : Optional.empty();
    }
}
