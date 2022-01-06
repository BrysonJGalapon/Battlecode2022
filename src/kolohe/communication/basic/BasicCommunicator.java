package kolohe.communication.basic;

import battlecode.common.*;
import kolohe.communication.Communicator;
import kolohe.communication.Entity;
import kolohe.communication.Message;
import kolohe.communication.MessageType;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static kolohe.RobotPlayer.*;
import static kolohe.utils.Utils.RNG;

/*
    Each index in the shared array represents a message. Keeps a pointer to the next index of
    the shared array to write the next message to. Increments pointer after every message is sent. Pointer
    returns to the beginning of the array once it falls off the end.

    Pitfalls:
        - Can't send more than 9 bits of data
        - Can't tell which message was sent before/after (no timestamp information is stored in message)
*/
public class BasicCommunicator implements Communicator {
    private static final int SHARED_ARRAY_LENGTH = 64;

    private static final int ENTITY_BIT_LENGTH = 2; // supports 2^2 = 4 different entities
    private static final int MESSAGE_TYPE_BIT_LENGTH = 2; // supports 2^2 = 4 different message types
    private static final int MAP_LOCATION_BIT_LENGTH = 12; // supports 2^12 = 4,096 different map locations

    private int idx = 0;

    @Override
    public void sendMessage(RobotController rc, Message message) throws GameActionException {
        int encoding = encode(message);
        rc.writeSharedArray(this.idx, encoding);
        idx = (idx + 1) % SHARED_ARRAY_LENGTH;
    }

    @Override
    public List<Message> receiveMessages(RobotController rc, int limit) throws GameActionException {
        List<Message> messages = new LinkedList<>();

        for (int i = 0; i < limit; i++) {
            int encoding = rc.readSharedArray(RNG.nextInt(SHARED_ARRAY_LENGTH));
            Optional<Message> message = decode(encoding);
            if (message.isPresent() && isRecipientOfMessage(ROBOT_TYPE, message.get())) {
                messages.add(message.get());
            }
        }

        return messages;
    }

    private static boolean isRecipientOfMessage(RobotType robotType, Message message) {
        Entity entity = message.entity;
        
        switch (entity) {
            case ALL_ROBOTS: return true;
            case ALL_MINERS: return robotType.equals(RobotType.MINER);
            case ALL_ARCHONS: return robotType.equals(RobotType.ARCHON);
            case ALL_BUILDERS: return robotType.equals(RobotType.BUILDER);
            case ALL_LABORATORIES: return robotType.equals(RobotType.LABORATORY);
            case ALL_SAGES: return robotType.equals(RobotType.SAGE);
            case ALL_SOLDIERS: return robotType.equals(RobotType.SOLDIER);
//            case ALL_WATCHTOWERS: return robotType.equals(RobotType.WATCHTOWER); // TODO not enough bits :(
            default: throw new RuntimeException("Should not be here");
        }
    }

    /* | data | messageType | entity | */
    private static int encode(Message message) {
        int encoding = 0;

        // append data encoding
        switch (message.messageType) {
            case GOLD_LOCATION:
            case LEAD_LOCATION:
            case NO_RESOURCES_LOCATION:
                encoding = append(encoding, encodeMapLocation(message.location), MAP_LOCATION_BIT_LENGTH);
                break;
            default: throw new RuntimeException("Should not be here");
        }

        // append MessageType encoding
        encoding = append(encoding, message.messageType.encode(), MESSAGE_TYPE_BIT_LENGTH);

        // append Entity encoding
        encoding = append(encoding, message.entity.encode(), ENTITY_BIT_LENGTH);

        return encoding;
    }

    private static Optional<Message> decode(int encoding) {
        // extract Entity encoding
        int entityEncoding = encoding & getBitMask(ENTITY_BIT_LENGTH);
        Optional<Entity> entity = Entity.decode(entityEncoding);
        encoding = encoding >> ENTITY_BIT_LENGTH;
        if (!entity.isPresent()) {
            return Optional.empty();
        }

        // extract MessageType encoding
        int messageTypeEncoding = encoding & getBitMask(MESSAGE_TYPE_BIT_LENGTH);
        Optional<MessageType> messageType = MessageType.decode(messageTypeEncoding);
        encoding = encoding >> MESSAGE_TYPE_BIT_LENGTH;
        if (!messageType.isPresent()) {
            return Optional.empty();
        }

        Message message = new Message(messageType.get(), entity.get());

        // extract data encoding
        switch (messageType.get()) {
            case GOLD_LOCATION:
            case LEAD_LOCATION:
            case NO_RESOURCES_LOCATION:
                int mapLocationEncoding = encoding & getBitMask(MAP_LOCATION_BIT_LENGTH);
                Optional<MapLocation> mapLocation = decodeMapLocation(mapLocationEncoding);
                if (!mapLocation.isPresent()) {
                    return Optional.empty();
                }
                encoding = encoding >> MAP_LOCATION_BIT_LENGTH;
                message.location = mapLocation.get();
                break;
            default: throw new RuntimeException("Should not be here");
        }

        return Optional.of(message);
    }

    private static int append(int encoding1, int encoding2, int encoding2Length) {
        return (encoding1 << encoding2Length) | encoding2;
    }

    private static int getBitMask(int length) {
        return (1 << length) - 1;
    }

    private static int encodeMapLocation(MapLocation mapLocation) {
        int x = mapLocation.x;
        int y = mapLocation.y;
        return x + MAP_WIDTH * y;
    }

    private static Optional<MapLocation> decodeMapLocation(int encoding) {
        if (encoding < 0 || encoding >= MAP_WIDTH * MAP_HEIGHT) {
            return Optional.empty();
        }

        int x = encoding % MAP_WIDTH;
        int y = encoding / MAP_WIDTH;

        return Optional.of(new MapLocation(x, y));
    }
}
