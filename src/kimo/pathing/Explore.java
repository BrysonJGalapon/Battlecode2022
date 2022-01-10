package kimo.pathing;

import battlecode.common.*;
import kimo.utils.Utils;

import java.util.Optional;

import static kimo.utils.Parameters.EXPLORER_BOREDOM_THRESHOLD;
import static kimo.utils.Utils.getRng;

public class Explore {
    private int boredom = 0;
    private Direction direction;

    public void setDirection(Direction direction) {
        this.direction = direction;
        this.boredom = 0;
    }

    public Optional<Direction> explore(MapLocation src, RobotController rc) throws GameActionException {
        if (direction == null) {
            direction = Utils.getRandomDirection();
            boredom = 0;
        }

        // bored of going in a single direction, change it up
        if (boredom > EXPLORER_BOREDOM_THRESHOLD) {
            switch(getRng().nextInt(3)) {
                case 0: direction = direction.rotateLeft(); break;
                case 1: direction = direction.rotateRight(); break;
            }
            boredom = 0;
        }

        boredom += 1;

        // reflect off the edge of the map
        if (!rc.onTheMap(src.add(direction))) {
            Direction newDirection;
            switch (getRng().nextInt(2)) {
                case 0:
                    newDirection = direction.rotateLeft().rotateLeft();
                    if (!rc.onTheMap(src.add(newDirection))) {
                        newDirection = direction.rotateRight().rotateRight();
                    }
                    break;
                case 1:
                    newDirection = direction.rotateRight().rotateRight();
                    if (!rc.onTheMap(src.add(newDirection))) {
                        newDirection = direction.rotateLeft().rotateLeft();
                    }
                    break;
                default: throw new RuntimeException("Should not be here");
            }

            direction = newDirection;
        }

        return Optional.of(direction);
    }
}
