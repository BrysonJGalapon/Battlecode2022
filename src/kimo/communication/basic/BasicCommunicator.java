package kimo.communication.basic;

import battlecode.common.*;
import kimo.communication.Communicator;
import kimo.communication.Entity;
import kimo.communication.Message;
import kimo.communication.MessageType;
import kimo.robots.archon.ArchonState;

import java.util.*;

import static kimo.RobotPlayer.*;
import static kimo.utils.Utils.getRng;

/*
    Allocate first 4 indices of shared array to storing ArchonState messages.

    Each index in the shared array represents a message. Keeps a pointer to the next index of
    the shared array to write the next message to. Increments pointer after every message is sent. Pointer
    returns to the beginning of the array once it falls off the end.

    Pitfalls:
        - Can't send more than 9 bits of data
        - Can't tell which message was sent before/after (no timestamp information is stored in message)
*/
public class BasicCommunicator implements Communicator {
    private static final int SHARED_ARRAY_LENGTH = 64; // change to 32 and pair possibly?
    // 64/7 entities
    // var length encoding
    // store message length as part of message
    // use first 4 bits to do length or however long
    // then read that many bits
    // contiguous 1024 bits

    private static final int NUM_RESERVED_ARCHON_STATE_INDICES = 4;
    private static final int ARCHON_STATE_BIT_LENGTH = 3; // supports 2^3 = 8 difference archon states

    private static final int ENTITY_BIT_LENGTH = 2; // supports 2^2 = 4 different entities
    private static final int MESSAGE_TYPE_BIT_LENGTH = 2; // supports 2^2 = 4 different message types
    private static final int MAP_LOCATION_BIT_LENGTH = 12; // supports 2^12 = 4,096 different map locations

    private int idx = 0;
    private int archonIdx = 0;

    @Override
    public void sendMessage(RobotController rc, Message message) throws GameActionException {
        if (message.messageType.equals(MessageType.ARCHON_STATE)) {
            sendArchonStateMessage(rc, message);
            return;
        }

        // do not write to archon state indices
        if (this.idx < NUM_RESERVED_ARCHON_STATE_INDICES) {
            this.idx = NUM_RESERVED_ARCHON_STATE_INDICES;
        }

        int encoding = encode(message);
        rc.writeSharedArray(this.idx, encoding);
        this.idx = (this.idx + 1) % SHARED_ARRAY_LENGTH;
    }

    @Override
    public List<Message> receiveMessages(RobotController rc, int limit, int bytecodeLimit) throws GameActionException {
        int initialBytecodesLeft = Clock.getBytecodesLeft();
        List<Message> messages = new LinkedList<>();

        int[] indices = new int[SHARED_ARRAY_LENGTH];
        for (int i = 0; i < SHARED_ARRAY_LENGTH; i++) {
            indices[i] = i;
        }

        for (int i = 0; i < SHARED_ARRAY_LENGTH; i++) {
            if (initialBytecodesLeft-Clock.getBytecodesLeft() > bytecodeLimit) {
                break;
            }

            if (messages.size() == limit) {
                break;
            }

            int j = i + getRng().nextInt(SHARED_ARRAY_LENGTH-i);

            // swap i, j indices
            int tmp = indices[i];
            indices[i] = indices[j];
            indices[j] = tmp;

            int indexToRead = indices[i];

            int encoding = rc.readSharedArray(indexToRead);

            if (encoding == 0) {
                continue;
            }

            Optional<Message> message;

            // decode the message, based on the index
            if (indexToRead < NUM_RESERVED_ARCHON_STATE_INDICES) {
                message = decodeArchonStateMessage(encoding);
            } else {
                message = decode(encoding);
            }

            // load the message if current robot is a valid recipient of it
            if (message.isPresent() && isRecipientOfMessage(ROBOT_TYPE, message.get())) {
                messages.add(message.get());
            }
        }

        return messages;
    }

    // reservoir sampling: https://www.geeksforgeeks.org/reservoir-sampling/
    private int[] getKRandomIndices(int n, int k) {
        int[] indices = new int[k];
        for (int i = 0; i < k; i++) {
            indices[i] = i;
        }

        for (int i = k; i < n; i++) {
            int j = getRng().nextInt(i+1);
            if (0 < j && j < k) {
                indices[j] = i;
            }
        }

        return indices;
    }

    private static int[] permutedIndices(int n) {
        Random r = getRng();

        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        for (int i = 0; i < n; i++) {
            int j = i + r.nextInt(n-i);

            // swap
            int tmp = indices[i];
            indices[i] = indices[j];
            indices[j] = tmp;
        }

        return indices;
    }

    private void sendArchonStateMessage(RobotController rc, Message message) throws GameActionException {
        int encoding = encodeArchonStateMessage(message);
        rc.writeSharedArray(this.archonIdx, encoding);
        this.archonIdx = (this.archonIdx + 1) % NUM_RESERVED_ARCHON_STATE_INDICES;
    }

    private static boolean isRecipientOfMessage(RobotType robotType, Message message) {
        Entity entity = message.entity;

        System.out.println(entity);
        switch (entity) {
            case ALL_ROBOTS: return true;
            case ALL_MINERS: return robotType.equals(RobotType.MINER);
//            case ALL_ARCHONS: return robotType.equals(RobotType.ARCHON);
            case ALL_BUILDERS: return robotType.equals(RobotType.BUILDER);
//            case ALL_LABORATORIES: return robotType.equals(RobotType.LABORATORY);
//            case ALL_SAGES: return robotType.equals(RobotType.SAGE);
//            case ALL_SOLDIERS: return robotType.equals(RobotType.SOLDIER);
//            case ALL_WATCHTOWERS: return robotType.equals(RobotType.WATCHTOWER); // TODO not enough bits :(
            default: throw new RuntimeException("Should not be here");
        }
    }

    private static int encodeArchonStateMessage(Message message) {
        int encoding = 0;

        // append map location
        encoding = append(encoding, encodeMapLocation(message.location), MAP_LOCATION_BIT_LENGTH);

        // append archon state
        encoding = append(encoding, message.archonState.encode(), ARCHON_STATE_BIT_LENGTH);

        return encoding;
    }

    private static Optional<Message> decodeArchonStateMessage(int encoding) {
        Message message = new Message(MessageType.ARCHON_STATE, Entity.ALL_ROBOTS);

        // extract archon state encoding
        int archonStateEncoding = encoding & getBitMask(ARCHON_STATE_BIT_LENGTH);
        Optional<ArchonState> archonState = ArchonState.decode(archonStateEncoding);
        encoding = encoding >> ARCHON_STATE_BIT_LENGTH;
        if (!archonState.isPresent()) {
            return Optional.empty();
        }

        // extract map location encoding
        int mapLocationEncoding = encoding & getBitMask(MAP_LOCATION_BIT_LENGTH);
        Optional<MapLocation> mapLocation = decodeMapLocation(mapLocationEncoding);
        if (!mapLocation.isPresent()) {
            return Optional.empty();
        }
        encoding = encoding >> MAP_LOCATION_BIT_LENGTH;

        message.archonState = archonState.get();
        message.location = mapLocation.get();

        return Optional.of(message);
    }

    private static int encode(Message message) {
        int encoding = 0;

        // append data encoding
        switch (message.messageType) {
            case GOLD_LOCATION:
            case LEAD_LOCATION:
            case NO_RESOURCES_LOCATION:
            case BUILD_WATCHTOWER_LOCATION:
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

    public static Optional<Message> decode(int encoding) {
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
            case BUILD_WATCHTOWER_LOCATION:
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
