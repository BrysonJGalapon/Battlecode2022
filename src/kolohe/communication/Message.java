package kolohe.communication;

import battlecode.common.MapLocation;
import kolohe.robots.archon.ArchonState;

// TODO dayne
public class Message {
    // required fields
    public final MessageType messageType;
    public final Entity entity;
    public final int expirationTimestamp;

    public Message(MessageType messageType, Entity entity, int expirationTimestamp) {
        this.messageType = messageType;
        this.entity = entity;
        this.expirationTimestamp = expirationTimestamp;
    }

    public MessageType getMessageType() {
        return this.messageType;
    }

    public int getExpirationTimestamp() {
        return this.expirationTimestamp;
    }

    // data fields
    public MapLocation location;
    public ArchonState archonState;

    public static Message buildSimpleLocationMessage(MessageType messageType, MapLocation location, Entity entity, int expirationTimestamp) {
        Message m = new Message(messageType, entity, expirationTimestamp);
        m.location = location;
        return m;
    }
}
