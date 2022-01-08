package kolohe.robots.laboratory;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import kolohe.state.machine.StateMachine;
import kolohe.state.machine.Stimulus;

/*
    - can produce 1 Au per turn at cost of Pb, variable conversion rate
    - can produce more Au per turn if less friendly robots are nearby
    - can move, but takes 10 turns to transform to 'Portable' mode, and takes 10 turns to transform back to 'Turret' mode
    - Au can only be made in the Laboratory
    - bytecode limit: 5,000
 */
public class Laboratory {
    private static final StateMachine<LaboratoryState> stateMachine = StateMachine.startingAt(LaboratoryState.TRANSMUTE);

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
            case TRANSMUTE: runTransmuteActions(rc, stimulus); break;
            case MOVE:   runMoveActions(rc, stimulus); break;
            default: throw new RuntimeException("Should not be here");
        }
    }

    public static void runTransmuteActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // TODO
    }

    public static void runMoveActions(RobotController rc, Stimulus stimulus) throws GameActionException {
        // TODO
    }
}
