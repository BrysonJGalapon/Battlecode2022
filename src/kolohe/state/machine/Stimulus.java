package kolohe.state.machine;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import kolohe.communication.Message;

import java.util.List;

public class Stimulus {
    public MapLocation myLocation;
    public RobotInfo[] nearbyRobotsInfo;
    public MapLocation[] allLocationsWithinRadiusSquared;
    public List<Message> messages;
}
