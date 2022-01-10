package kimo.state.machine;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import kimo.communication.Message;

import java.util.List;

public class Stimulus {
    public MapLocation myLocation;
    public RobotInfo[] friendlyNearbyRobotsInfo;
    public RobotInfo[] enemyNearbyRobotsInfo;
    public MapLocation[] nearbyLocationsWithGold;
    public MapLocation[] nearbyLocationsWithLead;
    public List<Message> messages;
    public List<Message> archonStateMessages;
    public List<MapLocation> archonLocations;
    public RobotInfo[] friendlyAdjacentNearbyRobotsInfo;
}
