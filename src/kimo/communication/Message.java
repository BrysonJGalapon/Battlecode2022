package kimo.communication;

import battlecode.common.MapLocation;
import kimo.robots.archon.ArchonState;

public class Message {
    // required fields
    public final MessageType messageType;
    public final Entity entity;

    public Message(MessageType messageType, Entity entity) {
        this.messageType = messageType;
        this.entity = entity;
    }

    public MessageType getMessageType() {
        return this.messageType;
    }

    // data fields
    public MapLocation location;
    public ArchonState archonState;

    public static Message buildSimpleLocationMessage(MessageType messageType, MapLocation location, Entity entity) {
        Message m = new Message(messageType, entity);
        m.location = location;
        return m;
    }
}
