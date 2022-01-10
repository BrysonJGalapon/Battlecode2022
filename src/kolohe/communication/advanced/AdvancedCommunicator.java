package kolohe.communication.advanced;

import battlecode.common.*;
import kolohe.communication.Communicator;
import kolohe.communication.Entity;
import kolohe.communication.Message;
import kolohe.communication.MessageType;
import kolohe.robots.archon.ArchonState;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static kolohe.RobotPlayer.*;
import static kolohe.utils.Utils.getRng;

public class AdvancedCommunicator implements Communicator {
    private static class FlatIndex {
        public final int globalIndex; // 0-63
        public final int localIndex;  // 0-15

        private FlatIndex(int globalIndex, int localIndex) {
            this.globalIndex = globalIndex;
            this.localIndex = localIndex;
        }

        private static FlatIndex from(int flatArrayIndex) {
            return new FlatIndex(flatArrayIndex / SHARED_ARRAY_INDEX_BIT_LENGTH, flatArrayIndex % SHARED_ARRAY_INDEX_BIT_LENGTH);
        }
    }

    private static class Channel {
        public final FlatIndex startFlatIndex;
        public final FlatIndex endFlatIndex;

        private Channel(FlatIndex startFlatIndex, FlatIndex endFlatIndex) {
            this.startFlatIndex = startFlatIndex;
            this.endFlatIndex = endFlatIndex;
        }

        private static Channel get(int i) {
            FlatIndex startFlatIndex = FlatIndex.from(TOTAL_MESSAGE_BIT_LENGTH * i);
            FlatIndex endFlatIndex = FlatIndex.from(TOTAL_MESSAGE_BIT_LENGTH * (i+1));
            return new Channel(startFlatIndex, endFlatIndex);
        }
    }

    private static final int NUM_RESERVED_ARCHON_STATE_CHANNELS = 4;

    private static final int SHARED_ARRAY_INDEX_BIT_LENGTH = 16; // change to 32 and pair possibly?
    private static final int SHARED_ARRAY_LENGTH = 64;

    // ASSUMES this will be less than or equal to 32
    private static final int TOTAL_MESSAGE_BIT_LENGTH = 18; // the largest possible message

    private static final int ENTITY_BIT_LENGTH = 3; // supports 2^3 = 8 different entities
    private static final int MESSAGE_TYPE_BIT_LENGTH = 3; // supports 2^2 = 8 different message types

    private static final int ARCHON_STATE_BIT_LENGTH = 2; // supports 2^2 = 4 different archon states
    private static final int MAP_LOCATION_BIT_LENGTH = 12; // supports 2^12 = 4,096 different map locations

    private static final Channel[] channels = getChannels();

    private int channelIdx = 0;
    private int archonChannelIdx = -1;

    private static Channel[] getChannels() {
        Channel[] channels = new Channel[(SHARED_ARRAY_LENGTH*SHARED_ARRAY_INDEX_BIT_LENGTH)/TOTAL_MESSAGE_BIT_LENGTH];

        for (int i = 0; i < channels.length; i++) {
            channels[i] = Channel.get(i);
        }

        return channels;
    }

    @Override
    public void sendMessage(RobotController rc, Message message) throws GameActionException {
        if (message.messageType.equals(MessageType.ARCHON_STATE)) {
            sendArchonStateMessage(rc, message);
            return;
        }

        // do not write to archon state channels
        if (this.channelIdx < NUM_RESERVED_ARCHON_STATE_CHANNELS) {
            this.channelIdx = NUM_RESERVED_ARCHON_STATE_CHANNELS;
        }

        int encoding = encode(message);
        writeToChannel(rc, channels[this.channelIdx], encoding);
        this.channelIdx = (this.channelIdx + 1) % channels.length;
    }

    @Override
    public List<Message> receiveMessages(RobotController rc, int limit, int bytecodeLimit) throws GameActionException {
        int initialBytecodesLeft = Clock.getBytecodesLeft();
        List<Message> messages = new LinkedList<>();

        int[] indices = new int[channels.length-NUM_RESERVED_ARCHON_STATE_CHANNELS];
        for (int i = 0; i < channels.length-NUM_RESERVED_ARCHON_STATE_CHANNELS; i++) {
            indices[i] = i+NUM_RESERVED_ARCHON_STATE_CHANNELS;
        }

        for (int i = 0; i < indices.length; i++) {
            if (initialBytecodesLeft-Clock.getBytecodesLeft() > bytecodeLimit) {
                break;
            }

            if (messages.size() == limit) {
                break;
            }

            int j = i + getRng().nextInt(indices.length-i);

            // swap i, j indices
            int tmp = indices[i];
            indices[i] = indices[j];
            indices[j] = tmp;

            int indexToRead = indices[i];

            int encoding = readFromChannel(rc, channels[indexToRead]);

            if (encoding == 0) {
                continue;
            }

            Optional<Message> message = decode(encoding);

            // load the message if current robot is a valid recipient of it
            if (message.isPresent() && isRecipientOfMessage(ROBOT_TYPE, message.get())) {
                messages.add(message.get());
            }
        }

        return messages;
    }

    private static int encode(Message message) {
        int encoding = 0;

        // append data encoding
        switch (message.messageType) {
            case GOLD_LOCATION:
            case LEAD_LOCATION:
            case NO_RESOURCES_LOCATION:
            case BUILD_WATCHTOWER_LOCATION:
            case BUILD_LABORATORY_LOCATION:
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
        int entityEncoding = encoding & getTailBitMask(ENTITY_BIT_LENGTH);
        Optional<Entity> entity = Entity.decode(entityEncoding);
        encoding = encoding >> ENTITY_BIT_LENGTH;
        if (!entity.isPresent()) {
            return Optional.empty();
        }

        // extract MessageType encoding
        int messageTypeEncoding = encoding & getTailBitMask(MESSAGE_TYPE_BIT_LENGTH);
        Optional<MessageType> messageType = MessageType.decode(messageTypeEncoding);
        encoding = encoding >> MESSAGE_TYPE_BIT_LENGTH;
        if (!messageType.isPresent()) {
            return Optional.empty();
        }

        Message message = new Message(messageType.get(), entity.get());

        // extract data encoding
        switch (messageType.get()) {
            case LEAD_LOCATION:
            case GOLD_LOCATION:
            case NO_RESOURCES_LOCATION:
            case BUILD_WATCHTOWER_LOCATION:
            case BUILD_LABORATORY_LOCATION:
                int mapLocationEncoding = encoding & getTailBitMask(MAP_LOCATION_BIT_LENGTH);
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

    private void sendArchonStateMessage(RobotController rc, Message message) throws GameActionException {
        int encoding = encodeArchonStateMessage(message);
        writeToChannel(rc, channels[getArchonChannelIdx(rc)], encoding);
    }

    private int getArchonChannelIdx(RobotController rc) throws GameActionException {
        if (this.archonChannelIdx != -1) {
            return this.archonChannelIdx;
        }

        for (int i = 0; i < NUM_RESERVED_ARCHON_STATE_CHANNELS; i++) {
            if (readFromChannel(rc, channels[i]) == 0) {
                this.archonChannelIdx = i;
                return i;
            }
        }

        throw new RuntimeException("Should not be here");
    }

    private static int getBit(int n, int k) {
        return (n >> k) & 1;
    }

    private static String getBitRepresentation(int n, int totalLength) {
        String representation = "";
        for (int i = 0; i < totalLength; i++) {
            representation = getBit(n, i) + representation;
        }
        return representation;
    }

    private static void writeToChannel(RobotController rc, Channel channel, int encoding) throws GameActionException {
        int first =  rc.readSharedArray(channel.startFlatIndex.globalIndex);
        int second = rc.readSharedArray(channel.endFlatIndex.globalIndex);

        int l1 = (SHARED_ARRAY_INDEX_BIT_LENGTH-channel.startFlatIndex.localIndex);
        int l2 = channel.endFlatIndex.localIndex;

        int firstEncoding = (encoding & getHeadBitMask(l1, TOTAL_MESSAGE_BIT_LENGTH)) >> (TOTAL_MESSAGE_BIT_LENGTH-l1);
        int secondEncoding = (encoding & getTailBitMask(l2)) << (SHARED_ARRAY_INDEX_BIT_LENGTH-l2);

        int newFirstEncoding = destroyTail(first, l1) | firstEncoding;
        int newSecondEncoding = destroyHead(second, l2, SHARED_ARRAY_INDEX_BIT_LENGTH) | secondEncoding;

        rc.writeSharedArray(channel.startFlatIndex.globalIndex, newFirstEncoding);
        rc.writeSharedArray(channel.endFlatIndex.globalIndex, newSecondEncoding);
    }

    private static int readFromChannel(RobotController rc, Channel channel) throws GameActionException {
        int first =  rc.readSharedArray(channel.startFlatIndex.globalIndex);
        int second = rc.readSharedArray(channel.endFlatIndex.globalIndex);

        int l1 = (SHARED_ARRAY_INDEX_BIT_LENGTH-channel.startFlatIndex.localIndex);
        int l2 = channel.endFlatIndex.localIndex;

        int firstEncoding = (first & getTailBitMask(l1)) << l2;
        int secondEncoding = (second & getHeadBitMask(l2, SHARED_ARRAY_INDEX_BIT_LENGTH)) >> (SHARED_ARRAY_INDEX_BIT_LENGTH-l2);

        return firstEncoding | secondEncoding;
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
        int archonStateEncoding = encoding & getTailBitMask(ARCHON_STATE_BIT_LENGTH);
        Optional<ArchonState> archonState = ArchonState.decode(archonStateEncoding);
        encoding = encoding >> ARCHON_STATE_BIT_LENGTH;
        if (!archonState.isPresent()) {
            return Optional.empty();
        }

        // extract map location encoding
        int mapLocationEncoding = encoding & getTailBitMask(MAP_LOCATION_BIT_LENGTH);
        Optional<MapLocation> mapLocation = decodeMapLocation(mapLocationEncoding);
        if (!mapLocation.isPresent()) {
            return Optional.empty();
        }
        encoding = encoding >> MAP_LOCATION_BIT_LENGTH;

        message.archonState = archonState.get();
        message.location = mapLocation.get();

        return Optional.of(message);
    }

    private static int append(int encoding1, int encoding2, int encoding2Length) {
        return (encoding1 << encoding2Length) | encoding2;
    }

    private static int getTailBitMask(int length) {
        return (1 << length) - 1;
    }

    private static int getHeadBitMask(int length, int totalLength) {
        return getTailBitMask(length) << (totalLength-length);
    }

    private static int destroyTail(int value, int length) {
        return value & ~(getTailBitMask(length));
    }

    private static int destroyHead(int value, int length, int totalLength) {
        return value & ~(getHeadBitMask(length, totalLength));
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

    private static boolean isRecipientOfMessage(RobotType robotType, Message message) {
        Entity entity = message.entity;

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

    public List<Message> receiveArchonStateMessages(RobotController rc) throws GameActionException {
        List<Message> archonStateMessages = new LinkedList<>();
        for (int i = 0; i < NUM_RESERVED_ARCHON_STATE_CHANNELS; i++) {
            int encoding = readFromChannel(rc, channels[i]);
            Optional<Message> message = decodeArchonStateMessage(encoding);
            if (message.isPresent()) {
                archonStateMessages.add(message.get());
            }
        }

        return archonStateMessages;
    }
}
