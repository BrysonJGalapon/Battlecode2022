package kimo.communication;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import java.util.List;

// TODO dayne
/*
- Requirements
    * System must be generic enough to send any kind of message
    * Avoid casting -- expensive operation
    * Can send more than 9 bits of data (should be able to send any arbitrary maplocation, even in 60x60 case)
    * Can tell which message was sent before/after (some kind of timestamp information should be stored in message)
 */
public interface Communicator {
    void sendMessage(RobotController rc, Message message) throws GameActionException;
    List<Message> receiveMessages(RobotController rc, int limit, int bytecodeLimit) throws GameActionException;
}
