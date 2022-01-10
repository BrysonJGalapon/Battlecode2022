package kimo.utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class EdgeVision {
    public static MapLocation[] getEdgeVision(MapLocation src, Direction direction) {
        switch (direction) {
            case SOUTH: return new MapLocation[]{
                    src.translate(-4, -2),
                    src.translate(-3, -3),
                    src.translate(-2, -4),
                    src.translate(-1, -4),
                    src.translate(0, -4),
                    src.translate(1, -4),
                    src.translate(2, -4),
                    src.translate(3, -3),
                    src.translate(4, -2)};
            case NORTH: return new MapLocation[]{
                    src.translate(-4, 2),
                    src.translate(-3, 3),
                    src.translate(-2, 4),
                    src.translate(-1, 4),
                    src.translate(0, 4),
                    src.translate(1, 4),
                    src.translate(2, 4),
                    src.translate(3, 3),
                    src.translate(4, 2)};
            case EAST: return new MapLocation[]{
                    src.translate(2, -4),
                    src.translate(3, -3),
                    src.translate(4, -2),
                    src.translate(4, -1),
                    src.translate(4, 0),
                    src.translate(4, 1),
                    src.translate(4, 2),
                    src.translate(3, 3),
                    src.translate(2, 4)};
            case WEST: return new MapLocation[]{
                    src.translate(-2, -4),
                    src.translate(-3, -3),
                    src.translate(-4, -2),
                    src.translate(-4, -1),
                    src.translate(-4, 0),
                    src.translate(-4, 1),
                    src.translate(-4, 2),
                    src.translate(-3, 3),
                    src.translate(-2, 4)};
            case SOUTHEAST: return new MapLocation[]{
                    src.translate(-2, -4),
                    src.translate(-1, -4),
                    src.translate(0, -4),
                    src.translate(1, -4),
                    src.translate(2, -4),
                    src.translate(2, -3),
                    src.translate(3, -3),
                    src.translate(3, -2),
                    src.translate(4, -2),
                    src.translate(4, -1),
                    src.translate(4, 0),
                    src.translate(4, 1),
                    src.translate(4, 2)};
            case NORTHEAST: return new MapLocation[]{
                    src.translate(4, -2),
                    src.translate(4, -1),
                    src.translate(4, 0),
                    src.translate(4, 1),
                    src.translate(4, 2),
                    src.translate(3, 2),
                    src.translate(3, 3),
                    src.translate(2, 3),
                    src.translate(2, 4),
                    src.translate(1, 4),
                    src.translate(0, 4),
                    src.translate(-1, 4),
                    src.translate(-2, 4)};
            case SOUTHWEST: return new MapLocation[]{
                    src.translate(-4, 2),
                    src.translate(-4, 1),
                    src.translate(-4, 0),
                    src.translate(-4, -1),
                    src.translate(-4, -2),
                    src.translate(-3, -2),
                    src.translate(-3, -3),
                    src.translate(-2, -3),
                    src.translate(-2, -4),
                    src.translate(-1, -4),
                    src.translate(0, -4),
                    src.translate(1, -4),
                    src.translate(2, -4)};
            case NORTHWEST:  return new MapLocation[]{
                    src.translate(2, 4),
                    src.translate(1, 4),
                    src.translate(0, 4),
                    src.translate(-1, 4),
                    src.translate(-2, 4),
                    src.translate(-2, 3),
                    src.translate(-3, 3),
                    src.translate(-3, 2),
                    src.translate(-4, 2),
                    src.translate(-4, 1),
                    src.translate(-4, 0),
                    src.translate(-4, -1),
                    src.translate(-4, -2)};
            default: throw new RuntimeException("Should not be here");
        }
    }
}
