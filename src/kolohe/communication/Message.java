package kolohe.communication;

import battlecode.common.MapLocation;

// TODO dayne
public class Message {
    // required fields
    public final MessageType messageType;
    public final Entity entity;

    public Message(MessageType messageType, Entity entity) {
        this.messageType = messageType;
        this.entity = entity;
    }

    // data fields
    public MapLocation location;

    public static Message buildSimpleLocationMessage(MessageType messageType, MapLocation location, Entity entity) {
        Message m = new Message(messageType, entity);
        m.location = location;
        return m;
    }
}
