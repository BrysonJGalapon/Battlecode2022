package kolohe.communication.advanced;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import kolohe.communication.Communicator;
import kolohe.communication.Message;

import java.util.List;

public class AdvancedCommunicator implements Communicator {
    @Override
    public void sendMessage(RobotController rc, Message message) throws GameActionException {

    }

    @Override
    public List<Message> receiveMessages(RobotController rc, int limit, int bytecodeLimit) throws GameActionException {
        return null;
    }
}
