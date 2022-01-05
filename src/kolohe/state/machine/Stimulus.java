package kolohe.state.machine;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class Stimulus {
    public MapLocation myLocation;
    public RobotInfo[] nearbyRobotsInfo;
    public MapLocation[] allLocationsWithinRadiusSquared;
}
