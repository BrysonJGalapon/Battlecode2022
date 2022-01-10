package kolohe.robots.builder;

import battlecode.common.*;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;
import kolohe.utils.Tuple;

import java.util.Optional;

import static battlecode.common.RobotMode.PROTOTYPE;
import static kolohe.RobotPlayer.ROBOT_ID;
import static kolohe.RobotPlayer.TEST_ROBOT_ID;

public enum BuilderState implements State {
    PATROL, // move around, check on the buildings

    BUILD, // build buildings

    REPAIR, // repair buildings that are losing health

    MUTATE, // mutate buildings

    ORPHAN, // no primary archon location :(
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        Optional<MapLocation> primaryArchonLocation = Builder.getPrimaryArchonLocation(rc, stimulus);

        if (!primaryArchonLocation.isPresent()) {
            return ORPHAN;
        }

        Builder.primaryArchonLocation = primaryArchonLocation.get();

        Optional<Tuple<RobotType, MapLocation>> broadcastedBuildingLocation;
        int robotLevel;
        int health;
        int maxHealth;

        switch (this) {
            case ORPHAN:
                Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                return PATROL;

            case PATROL:
                // check if any buildings need to be created
                broadcastedBuildingLocation = Builder.getAnyBroadcastedBuildingLocation(stimulus.messages);
                if (broadcastedBuildingLocation.isPresent()) {
                    Builder.robotToBuild = broadcastedBuildingLocation.get().getX();
                    Builder.buildLocation = broadcastedBuildingLocation.get().getY();
                    return BUILD;
                }

                MapLocation patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                Builder.patrolLocation = patrolLocation;

                // need to get closer to the patrol location
                if (!rc.canSenseLocation(patrolLocation)) {
                    return PATROL;
                }

                // can sense the patrol location, perform checks
                RobotInfo robotAtPatrolLocation = rc.senseRobotAtLocation(patrolLocation);

                if (robotAtPatrolLocation == null) {
                    // robot not at location -- it must have died, or moved
                    Builder.incrementPatrolIndex();
                    Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                    return PATROL;
                }

                if (!robotAtPatrolLocation.getType().isBuilding()) {
                    // robot not at location -- it must have died, or moved
                    Builder.incrementPatrolIndex();
                    Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                    return PATROL;
                }

                robotLevel = robotAtPatrolLocation.getLevel();
                health = robotAtPatrolLocation.getHealth();
                maxHealth = robotAtPatrolLocation.getType().getMaxHealth(robotLevel);

                if (health < maxHealth) {
                    // found a building that can be repaired
                    Builder.repairLocation = patrolLocation;
                    return REPAIR;
                }

                if (robotLevel == 1 && Builder.leadBudget >= robotAtPatrolLocation.getType().getLeadMutateCost(2)) {
                    // found a building to mutate to level 2, and we have the budget for it
                    Builder.mutateLocation = patrolLocation;
                    return MUTATE;
                }

                if (robotLevel == 2 && Builder.goldBudget >= robotAtPatrolLocation.getType().getGoldMutateCost(3)) {
                    // found a building to mutate to level 3, and we have the budget for it
                    Builder.mutateLocation = patrolLocation;
                    return MUTATE;
                }

                // this building looks good, move on to the next patrol location
                Builder.incrementPatrolIndex();
                Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                return PATROL;
            case BUILD:
                // not close enough to build location
                if (!rc.canSenseLocation(Builder.buildLocation)) {
                    return BUILD;
                }

                // check if the desired building has already been built at the location and is functioning
                RobotInfo robotAtBuildLocation = rc.senseRobotAtLocation(Builder.buildLocation);

                if (robotAtBuildLocation == null || !robotAtBuildLocation.getType().equals(Builder.robotToBuild) || robotAtBuildLocation.getMode().equals(PROTOTYPE)) {
                    // robot is not there, or not the type of robot we want to build, or not in a functioning state, so build it
                    return BUILD;
                }

                // building has already been built, check if any other build locations are being broadcasted
                broadcastedBuildingLocation = Builder.getAnyBroadcastedBuildingLocation(stimulus.messages);
                if (broadcastedBuildingLocation.isPresent() && !broadcastedBuildingLocation.get().getY().equals(Builder.buildLocation)) {
                    Builder.robotToBuild = broadcastedBuildingLocation.get().getX();
                    Builder.buildLocation = broadcastedBuildingLocation.get().getY();
                    return BUILD;
                }

                // no other build locations are being broadcasted, go on patrol
                Builder.incrementPatrolIndex();
                Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                return PATROL;
            case REPAIR:
                // not close enough to repair location
                if (!rc.canSenseLocation(Builder.repairLocation)) {
                    return REPAIR;
                }

                RobotInfo robotAtRepairLocation = rc.senseRobotAtLocation(Builder.repairLocation);
                if (robotAtRepairLocation == null) {
                    // robot not at location -- it must have died, or moved
                    Builder.incrementPatrolIndex();
                    Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                    return PATROL;
                }

                if (!robotAtRepairLocation.getType().isBuilding()) {
                    // robot not at location -- it must have died, or moved
                    Builder.incrementPatrolIndex();
                    Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                    return PATROL;
                }

                robotLevel = robotAtRepairLocation.getLevel();
                health = robotAtRepairLocation.getHealth();
                maxHealth = robotAtRepairLocation.getType().getMaxHealth(robotLevel);

                if (health == maxHealth) {
                    // robot is already at max health, go back to patrolling
                    Builder.incrementPatrolIndex();
                    Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                    return PATROL;
                }

                // robot is not at max health, go repair it
               return REPAIR;

            case MUTATE:
                // not close enough to mutate location
                if (!rc.canSenseLocation(Builder.mutateLocation)) {
                    return MUTATE;
                }

                RobotInfo robotAtMutateLocation = rc.senseRobotAtLocation(Builder.repairLocation);
                if (robotAtMutateLocation == null) {
                    // robot not at location -- it must have died, or moved
                    Builder.incrementPatrolIndex();
                    Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                    return PATROL;
                }

                if (!robotAtMutateLocation.getType().isBuilding()) {
                    // robot not at location -- it must have died, or moved
                    Builder.incrementPatrolIndex();
                    Builder.patrolLocation = Builder.getPatrolLocation(primaryArchonLocation.get());
                    return PATROL;
                }

                // check if any buildings need to be created
                broadcastedBuildingLocation = Builder.getAnyBroadcastedBuildingLocation(stimulus.messages);
                if (broadcastedBuildingLocation.isPresent()) {
                    Builder.robotToBuild = broadcastedBuildingLocation.get().getX();
                    Builder.buildLocation = broadcastedBuildingLocation.get().getY();
                    return BUILD;
                }

                robotLevel = robotAtMutateLocation.getLevel();

                if (robotLevel == 3) {
                    // robots at level 3 can not be mutated
                    Builder.incrementPatrolIndex();
                    return PATROL;
                }

                if (robotLevel == 1 && Builder.leadBudget >= robotAtMutateLocation.getType().getLeadMutateCost(2)) {
                    // found a building to mutate to level 2, and we have the budget for it -- stay in mutate state
                    return MUTATE;
                }

                if (robotLevel == 2 && Builder.goldBudget >= robotAtMutateLocation.getType().getGoldMutateCost(3)) {
                    // found a building to mutate to level 3, and we have the budget for it -- stay in mutate state
                    return MUTATE;
                }

                Builder.incrementPatrolIndex();
                return PATROL;

            default: throw new RuntimeException("Should not be here");
        }
    }
}
