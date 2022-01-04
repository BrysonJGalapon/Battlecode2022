package kolohe.communication;

public interface Communicate {
    void sendMessage(Message message, Entity entity);
    Message receiveMessage(Entity entity);

    void broadcastMessage(Message message);
    Message[] receiveBroadcastedMessages();
}
