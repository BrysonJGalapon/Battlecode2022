package kolohe.state.machine;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import kolohe.communication.Message;

import java.util.List;

public class Stimulus {
    public MapLocation myLocation;
    public RobotInfo[] friendlyNearbyRobotsInfo;
    public RobotInfo[] enemyNearbyRobotsInfo;
    public MapLocation[] nearbyLocationsWithGold;
    public MapLocation[] nearbyLocationsWithLead;
    public List<Message> messages;
}
