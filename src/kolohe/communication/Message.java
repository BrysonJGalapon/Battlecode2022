package kolohe.communication;

import battlecode.common.MapLocation;

// TODO dayne
public class Message {
    // required fields
    public final MessageType messageType;
    public final Entity entity;
    public final int expirationTime;
    public final int timeStamp;

    public Message(MessageType messageType, Entity entity) {
        this.messageType = messageType;
        this.entity = entity;
        this.expirationTime = 0;
        this.timeStamp = 0;
    }

    public MessageType getMessageType() {
        return this.messageType;
    }

    public int getExpirationTime() {
        return this.expirationTime;
    }

    public int getTimeStamp() {
        return this.timeStamp;
    }

    // data fields
    public MapLocation location;

    public static Message buildSimpleLocationMessage(MessageType messageType, MapLocation location, Entity entity) {
        Message m = new Message(messageType, entity);
        m.location = location;
        return m;
    }
}
