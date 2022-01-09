package kolohe.communication.bettercommunication;
import battlecode.common.*;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import kolohe.communication.Communicator;
import kolohe.communication.Entity;
import kolohe.communication.Message;
import kolohe.communication.MessageType;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static kolohe.RobotPlayer.*;
import static kolohe.utils.Utils.getRng;

/*
    Each index in the shared array represents a message. Keeps a pointer to the next index of
    the shared array to write the next message to. Increments pointer after every message is sent. Pointer
    returns to the beginning of the array once it falls off the end.

    Pitfalls:
        - Can't send more than 9 bits of data
        - Can't tell which message was sent before/after (no timestamp information is stored in message)
*/
public class BetterCommunicator implements Communicator {
    private static final int SHARED_ARRAY_LENGTH = 64; // change to 32 and pair possibly?
    // 64/7 entities
    // var length encoding
    // store message length as part of message
    // use first 4 bits to do length or however long
    // then read that many bits
    // contiguous 1024 bits

    // message types = 4 bits
    // map location = 12 bits


    private static final int ENTITY_BIT_LENGTH = 3; // supports 2^2 = 4 different entities

    // store timestamp
    //private static final int TIMESTAMP = 1;
    // allocations
    private static final int MAP_LOCATION_BIT_LENGTH = 12; // supports 2^12 = 4,096 different map locations
    private static final int MESSAGE_TYPE_BIT_LENGTH = 4; // supports 2^4 = 16 different message types
    private static final int EXPIRATION_TIME_BIT_LENGTH = 11; // supports 2^4 = 16 different message types
    private static final int TIMESTAMP_BIT_LENGTH = 11;

    // 4 indices archon states

    private int idxMiner = 0;
    private int idxArchon = 7;
    private int idxBuilder = 14;
    private int idxLaboratory = 21;
    private int idxSage = 28;
    private int idxSoldier = 35;
    private int idxWatchtower = 42;
    private int idxArchonState = 49; // 49-52

    public static int lastIndex = 0;

    @Override
    public void sendMessage(RobotController rc, Message message) throws GameActionException {
        int encoding = encode(message);
        // add new messagetype, if messagetype = archon
        if (message.getMessageType() == MessageType.ARCHON_STATE) {
            rc.writeSharedArray(this.idxArchonState, encoding);
            idxArchonState = (idxArchonState + 1) % 52;
            if (idxArchonState == 0) { idxArchonState = 49; }
            lastIndex = idxArchonState;
        }
        else {
            switch (rc.getType()) {
                case MINER: {
                    rc.writeSharedArray(this.idxMiner, encoding);
                    idxMiner = (idxMiner + 1) % 7;
                    lastIndex = idxMiner;
                    break;
                }
                case ARCHON: {
                    rc.writeSharedArray(this.idxArchon, encoding);
                    idxArchon = (idxArchon + 1) % 14;
                    if (idxArchon == 0) {
                        idxArchon = 7;
                    }
                    lastIndex = idxArchon;
                    break;
                }
                case BUILDER: {
                    rc.writeSharedArray(this.idxBuilder, encoding);
                    idxBuilder = (idxBuilder + 1) % 21;
                    if (idxBuilder == 0) {
                        idxBuilder = 14;
                    }
                    lastIndex = idxBuilder;
                    break;
                }
                case LABORATORY: {
                    rc.writeSharedArray(this.idxLaboratory, encoding);
                    idxLaboratory = (idxLaboratory + 1) % 28;
                    if (idxLaboratory == 0) {
                        idxLaboratory = 21;
                    }
                    lastIndex = idxLaboratory;
                    break;
                }
                case SAGE: {
                    rc.writeSharedArray(this.idxSage, encoding);
                    idxSage = (idxSage + 1) % 35;
                    if (idxSage == 0) {
                        idxSage = 28;
                    }
                    lastIndex = idxSage;
                    break;
                }
                case SOLDIER: {
                    rc.writeSharedArray(this.idxSoldier, encoding);
                    idxSoldier = (idxSoldier + 1) % 42;
                    if (idxSoldier == 0) {
                        idxSoldier = 35;
                    }
                    lastIndex = idxSoldier;
                    break;
                }
                case WATCHTOWER: {
                    rc.writeSharedArray(this.idxWatchtower, encoding);
                    idxWatchtower = (idxWatchtower + 1) % 49;
                    if (idxWatchtower == 0) {
                        idxWatchtower = 42;
                    }
                    lastIndex = idxWatchtower;
                    break;
                }
                default: {
                    throw new RuntimeException("Should not be here");
                }
            }
        }
    }

    @Override
    public List<Message> receiveMessages(RobotController rc, int limit, int bytecodeLimit) throws GameActionException {
        List<Message> messages = new LinkedList<>();

        for (int i = 0; i < limit; i++) {
            int encoding = rc.readSharedArray(getRng().nextInt(SHARED_ARRAY_LENGTH));
            Optional<Message> message = decode(encoding);
            if (message.isPresent() && isRecipientOfMessage(ROBOT_TYPE, message.get())) {
                messages.add(message.get());
            }
        }

        return messages;
    }

    private static boolean isRecipientOfMessage(RobotType robotType, Message message) {
        Entity entity = message.entity;

        System.out.println(entity);
        switch (entity) {
            case ALL_ROBOTS: return true;
            case ALL_MINERS: return robotType.equals(RobotType.MINER);
            case ALL_ARCHONS: return robotType.equals(RobotType.ARCHON);
            case ALL_BUILDERS: return robotType.equals(RobotType.BUILDER);
            case ALL_LABORATORIES: return robotType.equals(RobotType.LABORATORY);
            case ALL_SAGES: return robotType.equals(RobotType.SAGE);
            case ALL_SOLDIERS: return robotType.equals(RobotType.SOLDIER);
            case ALL_WATCHTOWERS: return robotType.equals(RobotType.WATCHTOWER); // TODO not enough bits :(
            default: throw new RuntimeException("Should not be here");
        }
    }

    /* | data | messageType | expiration | timestamp | entity | length of data | */
    // add to message length of data
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

        encoding = append(encoding, message.getExpirationTime(), EXPIRATION_TIME_BIT_LENGTH);

        encoding = append(encoding, message.getTimeStamp(), TIMESTAMP_BIT_LENGTH);

        // append Entity encoding
        encoding = append(encoding, message.entity.encode(), ENTITY_BIT_LENGTH);

        return encoding;
    }

    public static Optional<Message> decode(int encoding) {
        int messageLength = encoding & getBitMask(ENTITY_BIT_LENGTH);

         //extract Entity encoding
         int entityEncoding = encoding & getBitMask(ENTITY_BIT_LENGTH);
         Optional<Entity> entity = Entity.decode(entityEncoding);
         encoding = encoding >> ENTITY_BIT_LENGTH;
         if (!entity.isPresent()) {
            return Optional.empty();
         }

//        Optional<Entity> entity = Optional.empty();
//        if (lastIndex >= 0 && lastIndex < 7) entity = Optional.of(Entity.ALL_MINERS);
//        if (lastIndex >= 7 && lastIndex < 14) entity = Optional.of(Entity.ALL_ARCHONS);
//        if (lastIndex >= 14 && lastIndex < 21) entity = Optional.of(Entity.ALL_BUILDERS);
//        if (lastIndex >= 21 && lastIndex < 28) entity = Optional.of(Entity.ALL_LABORATORIES);
//        if (lastIndex >= 28 && lastIndex < 35) entity = Optional.of(Entity.ALL_SAGES);
//        if (lastIndex >= 35 && lastIndex < 42) entity = Optional.of(Entity.ALL_SOLDIERS);
//        if (lastIndex >= 42 && lastIndex < 49) entity = Optional.of(Entity.ALL_WATCHTOWERS);



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

