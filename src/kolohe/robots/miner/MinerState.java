package kolohe.robots.miner;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import kolohe.communication.Entity;
import kolohe.communication.Message;
import kolohe.communication.MessageType;
import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

import java.util.Optional;

public enum MinerState implements State {
    // is nearby resources, collects them
    COLLECT,

    // not nearby resources, doesn't know where to go to find them
    EXPLORE,

    // not nearby resources, knows where to go to find them
    TARGET,
    ;

    @Override
    public State react(Stimulus stimulus, RobotController rc) throws GameActionException {
        Optional<MapLocation> nextResourceLocation;

        switch (this) {
            case COLLECT:
                // no more resources
                if (!Miner.isAnyResourceInView(rc, stimulus.allLocationsWithinRadiusSquared)) {
                    // let other miners know that there are no more resources here
                    Miner.communicator.sendMessage(rc, Message.buildSimpleLocationMessage(MessageType.NO_RESOURCES_LOCATION, stimulus.myLocation, Entity.ALL_MINERS));

                    // see if other miners found any other resource locations
                    nextResourceLocation = Miner.getClosestBroadcastedResourceLocation(stimulus.myLocation, stimulus.messages);

                    // found other resources, so target them
                    if (nextResourceLocation.isPresent()) {
                        Miner.setTargetLocation(nextResourceLocation.get());
                        return TARGET;
                    }

                    // no other resources found, so look for them
                    return EXPLORE;
                }

                // still resources to collect
                return COLLECT;
            case EXPLORE:
                // found some resources, go collect them
                if (Miner.isAnyResourceInView(rc, stimulus.allLocationsWithinRadiusSquared)) {
                    return COLLECT;
                }

                // see if other miners found any other resource locations
                nextResourceLocation = Miner.getClosestBroadcastedResourceLocation(stimulus.myLocation, stimulus.messages);

                // found other resources, so target them
                if (nextResourceLocation.isPresent()) {
                    Miner.setTargetLocation(nextResourceLocation.get());
                    return TARGET;
                }

                // no other resources found, so continue to look for them
                return EXPLORE;
            case TARGET:
                // TODO once value is associated to a location, compare value of seen resources to target resource
                // found some resources, go collect them
                if (Miner.isAnyResourceInView(rc, stimulus.allLocationsWithinRadiusSquared)) {
                    return COLLECT;
                }

                // see if other miners found any closer resource locations to go to, and change course if so
                nextResourceLocation = Miner.getClosestBroadcastedResourceLocation(stimulus.myLocation, stimulus.messages);
                if (nextResourceLocation.isPresent()) {
                    if (stimulus.myLocation.distanceSquaredTo(nextResourceLocation.get()) < stimulus.myLocation.distanceSquaredTo(Miner.getTargetLocation())) {
                        Miner.setTargetLocation(nextResourceLocation.get());
                        return TARGET;
                    }
                }

                // no more resources at the target location
                if (Miner.isResourceLocationDepleted(Miner.getTargetLocation(), stimulus.messages)) {
                    // use the next closest possible target location
                    if (nextResourceLocation.isPresent()) {
                        Miner.setTargetLocation(nextResourceLocation.get());
                        return TARGET;
                    }

                    // no other known resource locations
                    return EXPLORE;
                }

                // continue to target known resource location
                return TARGET;
            default: throw new RuntimeException("Should not be here");
        }
    }
}
