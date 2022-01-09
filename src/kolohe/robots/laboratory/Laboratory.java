package kolohe.robots.laboratory;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kolohe.communication.Communicator;
import kolohe.communication.basic.BasicCommunicator;
import kolohe.resource.allocation.ResourceAllocation;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;

import java.util.Optional;

import static kolohe.RobotPlayer.ROBOT_TYPE;
import static kolohe.resource.allocation.ResourceAllocation.getClosestBroadcastedArchonLocation;
import static kolohe.utils.Parameters.*;

/*
    - can produce 1 Au per turn at cost of Pb, variable conversion rate
    - can produce more Au per turn if less friendly robots are nearby
    - can move, but takes 10 turns to transform to 'Portable' mode, and takes 10 turns to transform back to 'Turret' mode
    - Au can only be made in the Laboratory
    - bytecode limit: 5,000
 */
public class Laboratory {
    private static final StateMachine<LaboratoryState> stateMachine = StateMachine.startingAt(LaboratoryState.TRANSMUTE);
    public static final ResourceAllocation resourceAllocation = new ResourceAllocation();
    public static final Communicator communicator = new BasicCommunicator();

    public static double leadBudget = 0;

    private static MapLocation primaryArchonLocation; // the location of the archon that this laboratory is associated to
    private static int primaryArchonLocationExpiration;

    private static Stimulus collectStimulus(RobotController rc) throws GameActionException {
        Stimulus s = new Stimulus();
        s.messages = communicator.receiveMessages(rc, LABORATORY_RECEIVE_MESSAGE_LIMIT, LABORATORY_RECEIVE_MESSAGE_BYTECODE_LIMIT);
        return s;
    }

    public static void run(RobotController rc) throws GameActionException {
        Stimulus stimulus = collectStimulus(rc);
        stateMachine.transition(stimulus, rc);
        rc.setIndicatorString(String.format("state: %s", stateMachine.getCurrState()));
        switch (stateMachine.getCurrState()) {
            case TRANSMUTE: runTransmuteActions(rc, stimulus); break;
            case MOVE:   runMoveActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runTransmuteActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        resourceAllocation.run(rc, stimulus);
        double leadAllowance = resourceAllocation.getLeadAllowance(rc, ROBOT_TYPE);
        leadBudget += leadAllowance;
        int transmutationRate = rc.getTransmutationRate();
        if (transmutationRate > leadBudget) {
            return;
        }

        // transmute
        if (rc.canTransmute()) {
            leadBudget -= transmutationRate;
            rc.transmute();
        }
    }

    public static void runMoveActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // TODO
    }

    public static Optional<MapLocation> getPrimaryArchonLocation(RobotController rc, Stimulus stimulus) throws GameActionException {
        if (primaryArchonLocation != null && rc.getRoundNum() <= primaryArchonLocationExpiration) {
            return Optional.of(primaryArchonLocation);
        }

        // check for any broadcasted archon locations
        Optional<MapLocation> closestBroadcastedArchonLocation = getClosestBroadcastedArchonLocation(rc, stimulus);
        if (closestBroadcastedArchonLocation.isPresent()) {
            primaryArchonLocation = closestBroadcastedArchonLocation.get();
            primaryArchonLocationExpiration = rc.getRoundNum() + LABORATORY_PRIMARY_ARCHON_LOCATION_SHELF_LIFE;
        }

        return closestBroadcastedArchonLocation;
    }
}
